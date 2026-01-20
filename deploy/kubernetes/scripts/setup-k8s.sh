#!/usr/bin/env bash
set -euo pipefail

bold() { printf "\033[1m%s\033[0m\n" "$*"; }
note() { printf "[+] %s\n" "$*"; }
warn() { printf "[!] %s\n" "$*"; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1
}

script_root() {
  # This script lives in deploy/kubernetes/scripts; repo root is three levels up.
  cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd
}

install_k8s_tools_mac() {
  if ! need_cmd brew; then
    warn "Homebrew not found. Please install Homebrew from https://brew.sh and re-run."
    exit 1
  fi
  note "Installing Kubernetes prerequisites via Homebrew..."
  brew update || true
  brew install docker kubectl kind || true
  note "If Docker Desktop was just installed, open it once to finish setup."
}

install_k8s_tools_linux_apt() {
  note "Installing Kubernetes prerequisites via apt (sudo required)..."
  sudo apt-get update -y
  sudo apt-get install -y ca-certificates curl gnupg lsb-release || true

  if ! need_cmd docker; then
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/$(. /etc/os-release && echo "$ID")/gpg | \
      sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/$(. /etc/os-release && echo "$ID") \
      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin || true
    sudo usermod -aG docker "$USER" || true
    note "Docker installed. You may need to log out/in for group changes to take effect."
  fi

  if ! need_cmd kubectl; then
    if ! sudo apt-get install -y kubectl 2>/dev/null; then
      note "Installing kubectl via curl..."
      curl -LO "https://dl.k8s.io/release/$(curl -Ls https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
      sudo install -m 0755 kubectl /usr/local/bin/kubectl
      rm -f kubectl
    fi
  fi

  if ! need_cmd kind; then
    note "Installing kind via curl..."
    curl -Lo ./kind "https://kind.sigs.k8s.io/dl/v0.23.0/kind-linux-amd64"
    chmod +x ./kind
    sudo mv ./kind /usr/local/bin/kind
  fi
}

ensure_k8s_tools() {
  local require_kind="${1:-false}"

  if ! need_cmd docker || ! need_cmd kubectl || { [ "$require_kind" = "true" ] && ! need_cmd kind; }; then
    note "Some Kubernetes tools are missing; attempting to install them..."
    local OS
    OS=$(uname -s)
    case "$OS" in
      Darwin)
        install_k8s_tools_mac
        ;;
      Linux)
        if [ -f /etc/debian_version ] || [ -f /etc/lsb-release ]; then
          install_k8s_tools_linux_apt
        else
          warn "Unsupported Linux distro for auto-install. Please install docker, kubectl, and kind manually."
        fi
        ;;
      *)
        warn "Unsupported OS: $OS. Please install docker, kubectl, and kind manually."
        ;;
    esac
  fi

  if ! need_cmd docker; then
    warn "docker is still not available after attempted install."
    exit 1
  fi
  if ! need_cmd kubectl; then
    warn "kubectl is still not available after attempted install."
    exit 1
  fi
  if [ "$require_kind" = "true" ] && ! need_cmd kind; then
    warn "kind is required for local mode but is not available."
    exit 1
  fi
}

wait_for_namespace_pods() {
  local ns="$1"
  local timeout="${2:-600}" # seconds
  local interval=5

  note "Waiting for pods in namespace '${ns}' to become Ready (timeout=${timeout}s)..."

  local start
  start=$(date +%s)

  while true; do
    # If namespace doesn't exist yet, keep waiting up to timeout
    if ! kubectl get ns "$ns" >/dev/null 2>&1; then
      sleep "$interval"
      if [ $(( $(date +%s) - start )) -ge "$timeout" ]; then
        warn "Namespace '${ns}' still not found after ${timeout}s."
        return 1
      fi
      continue
    fi

    # Get pods; if none yet, keep waiting
    local total
    total=$(kubectl get pods -n "$ns" --no-headers 2>/dev/null | wc -l | tr -d ' ' || echo 0)
    if [ "$total" -eq 0 ]; then
      sleep "$interval"
      if [ $(( $(date +%s) - start )) -ge "$timeout" ]; then
        warn "No pods found in namespace '${ns}' after ${timeout}s."
        return 1
      fi
      continue
    fi

    # Count pods not in Running, Completed, or Pending
    # Pending pods are excluded as they may be resource-constrained and shouldn't block deployment
    local not_ready
    not_ready=$(kubectl get pods -n "$ns" --no-headers 2>/dev/null | \
      awk '($3!="Running" && $3!="Completed" && $3!="Pending"){c++} END{print c+0}')

    if [ "$not_ready" -eq 0 ]; then
      note "All pods in namespace '${ns}' are Running/Completed (Pending pods excluded)."
      kubectl get pods -n "$ns"
      
      # Check for pending pods and inform user
      local pending_count
      pending_count=$(kubectl get pods -n "$ns" --no-headers 2>/dev/null | \
        awk '($3=="Pending"){c++} END{print c+0}')
      if [ "$pending_count" -gt 0 ]; then
        warn "${pending_count} pod(s) are Pending (likely due to resource constraints)."
      fi
      
      return 0
    fi

    echo "   ... still waiting in '${ns}': ${not_ready} pod(s) not ready (of ${total})"

    if [ $(( $(date +%s) - start )) -ge "$timeout" ]; then
      warn "Timeout while waiting for pods in '${ns}' to become Ready."
      kubectl get pods -n "$ns"
      return 1
    fi

    sleep "$interval"
  done
}

create_kind_cluster_local() {
  local name="logwise-local"
  local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  
  note "Setting up kind cluster '${name}' using common setup script..."
  KIND_CLUSTER_NAME="${name}" \
    BUILD_IMAGES=true \
    LOAD_IMAGES=true \
    "${script_dir}/setup-kind-cluster.sh" --skip-existing
}

run_local() {
  ensure_k8s_tools true

  local ROOT
  ROOT="$(script_root)"
  cd "$ROOT"

  # Setup kind cluster (will build and load images)
  create_kind_cluster_local

  # Note: Images are already built and loaded by setup-kind-cluster.sh.
  # We still run build-and-push.sh to ensure the tags/names match the repo config,
  # but it MUST build for the cluster's architecture (e.g., linux/arm64 on Apple Silicon).
  note "Applying kubernetes/overlays/local overlay to kind cluster..."
  # - REGISTRY=local: ensures the script does NOT attempt to push anywhere.
  # - PLATFORM left unset: build-and-push.sh will auto-detect the kind node platform.
  ENV=local CLUSTER_TYPE=kind REGISTRY=local TAG=latest PUSH_IMAGES=false PLATFORM="" KIND_CLUSTER_NAME=logwise-local \
    ./deploy/kubernetes/scripts/build-and-push.sh && \
  ENV=local ./deploy/kubernetes/scripts/deploy.sh

  # Wait for core namespaces to be healthy
  wait_for_namespace_pods "logwise" 600 || {
    warn "Some pods in 'logwise' did not become Ready; please inspect with 'kubectl get pods -n logwise'."
  }

  bold "Local (kind) setup complete."
  echo "Endpoints (via NodePort + extraPortMappings):"
  echo "  - Orchestrator:  http://localhost:30081"
  echo "  - Grafana:       http://localhost:30080"
  echo "  - Spark Master:  http://localhost:30082"
  echo "  - Spark Worker:  http://localhost:30083"
  echo "  - Vector OTLP:   http://localhost:30418"
}

run_env() {
  local env="$1"   # nonprod | prod

  ensure_k8s_tools false

  local ROOT
  ROOT="$(script_root)"
  cd "$ROOT"

  note "Using current kube-context for ENV=${env}."
  note "Building images (REGISTRY='${REGISTRY:-}', TAG='${TAG:-latest}') and applying deploy/kubernetes/overlays/${env}..."

  ENV="${env}" CLUSTER_TYPE="${CLUSTER_TYPE:-docker-desktop}" REGISTRY="${REGISTRY:-}" TAG="${TAG:-latest}" \
    ./deploy/kubernetes/scripts/build-and-push.sh && \
  ENV="${env}" ./deploy/kubernetes/scripts/deploy.sh

  # Wait for core namespaces to be healthy
  wait_for_namespace_pods "logwise" 600 || {
    warn "Some pods in 'logwise' did not become Ready; please inspect with 'kubectl get pods -n logwise'."
  }

  bold "Kubernetes deployment for '${env}' applied."
  echo "Next steps:"
  echo "  - Ensure Ingress controller is installed."
  echo "  - Point DNS for your org to the Ingress hosts defined in:"
  echo "      deploy/kubernetes/overlays/${env}/ingress.yaml"
}

usage() {
  cat <<EOF
Usage: $(basename "$0") <mode>

Modes:
  local      Create a kind cluster and deploy the full Logwise stack using deploy/kubernetes/overlays/local.
  nonprod    Build images and apply deploy/kubernetes/overlays/nonprod to the current Kubernetes context.
  prod       Build images and apply deploy/kubernetes/overlays/prod to the current Kubernetes context.

Env vars (for nonprod/prod):
  REGISTRY   Container registry prefix (e.g. ghcr.io/your-org)
  TAG        Image tag to use (default: latest)
  CLUSTER_TYPE  docker-desktop | kind | other (default: docker-desktop)
EOF
}

main() {
  local mode="${1:-}"

  case "$mode" in
    local)
      run_local
      ;;
    nonprod)
      run_env "nonprod"
      ;;
    prod)
      run_env "prod"
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"


