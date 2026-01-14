#!/usr/bin/env bash
set -euo pipefail

bold() { printf "\033[1m%s\033[0m\n" "$*"; }
note() { printf "[+] %s\n" "$*"; }
warn() { printf "[!] %s\n" "$*"; }

CLUSTER_NAME="logwise-local"
COLIMA_PROFILE="${COLIMA_PROFILE:-default}"
TARGET_CPUS=6
TARGET_MEMORY=10

detect_docker_runtime() {
  if command -v colima >/dev/null 2>&1 && colima status >/dev/null 2>&1; then
    echo "colima"
  elif docker context ls 2>/dev/null | grep -q "colima"; then
    echo "colima"
  elif docker info 2>/dev/null | grep -q "Docker Desktop"; then
    echo "docker-desktop"
  else
    echo "unknown"
  fi
}

get_colima_resources() {
  local profile="$1"
  # Try JSON format first (if jq is available)
  if command -v jq >/dev/null 2>&1; then
    colima list --format json 2>/dev/null | \
      jq -r ".[] | select(.profile == \"$profile\") | \"\(.cpus)|\(.memory)\"" 2>/dev/null && return
  fi
  # Fallback to parsing table format
  colima list 2>/dev/null | awk -v profile="$profile" '
    NR == 1 { 
      # Find column positions
      for (i=1; i<=NF; i++) {
        if ($i == "CPUS") cpus_col = i
        if ($i == "MEMORY") memory_col = i
      }
      next
    }
    $1 == profile {
      cpus = $cpus_col
      memory = $memory_col
      gsub(/[^0-9]/, "", memory)  # Extract number from "2GiB"
      print cpus "|" memory
    }
  '
}

main() {
  bold "Increasing Kubernetes Node Resources"
  echo ""
  
  local runtime
  runtime=$(detect_docker_runtime)
  
  if [ "$runtime" = "colima" ]; then
    note "Detected Colima as Docker runtime"
    
    note "Current Colima resources:"
    colima list 2>/dev/null | grep -E "PROFILE|${COLIMA_PROFILE}" || warn "Could not check Colima resources"
    echo ""
    
    local current_resources
    current_resources=$(get_colima_resources "$COLIMA_PROFILE")
    local current_cpus current_memory
    current_cpus=$(echo "$current_resources" | cut -d'|' -f1 | sed 's/[^0-9]//g')
    current_memory=$(echo "$current_resources" | cut -d'|' -f2 | sed 's/[^0-9]//g')
    
    if [ -z "$current_cpus" ] || [ -z "$current_memory" ]; then
      warn "Could not determine current Colima resources"
      current_cpus=0
      current_memory=0
    fi
    
    note "Current resources:"
    echo "  CPUs: $current_cpus"
    echo "  Memory: ${current_memory}GiB"
    echo ""
    note "Target resources:"
    echo "  CPUs: $TARGET_CPUS"
    echo "  Memory: ${TARGET_MEMORY}GiB"
    echo ""
    
    if [ "$current_cpus" -ge "$TARGET_CPUS" ] && [ "$current_memory" -ge "$TARGET_MEMORY" ]; then
      note "Colima already has sufficient resources!"
    else
      warn "Stopping Colima to update resources..."
      colima stop --profile "$COLIMA_PROFILE" || warn "Colima may already be stopped"
      
      note "Starting Colima with increased resources..."
      note "This may take a minute..."
      colima start \
        --profile "$COLIMA_PROFILE" \
        --cpu "$TARGET_CPUS" \
        --memory "${TARGET_MEMORY}" \
        --runtime docker || {
        warn "Failed to start Colima with new resources"
        exit 1
      }
      
      note "Waiting for Colima to be ready..."
      sleep 5
      
      note "Verifying Colima resources..."
      colima list 2>/dev/null | grep -E "PROFILE|${COLIMA_PROFILE}"
    fi
    
    echo ""
    note "Verifying Docker resources..."
    docker info 2>/dev/null | grep -E "CPUs|Total Memory" || warn "Could not verify Docker resources"
    echo ""
    
  elif [ "$runtime" = "docker-desktop" ]; then
    warn "Detected Docker Desktop"
    note "Current Docker Desktop resources:"
    docker info 2>/dev/null | grep -E "CPUs|Total Memory" || warn "Could not check Docker resources"
    echo ""
    warn "Please manually increase Docker Desktop resources:"
    echo "  1. Open Docker Desktop"
    echo "  2. Go to Settings → Resources → Advanced"
    echo "  3. Set CPUs to at least $TARGET_CPUS"
    echo "  4. Set Memory to at least ${TARGET_MEMORY}GB"
    echo "  5. Click 'Apply & Restart'"
    echo ""
    read -p "Have you updated Docker Desktop resources? (y/n) " -n 1 -r
    echo ""
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      warn "Please update Docker Desktop resources first, then run this script again."
      exit 1
    fi
    
    note "Verifying Docker Desktop resources..."
    local cpus memory
    cpus=$(docker info 2>/dev/null | grep "CPUs:" | awk '{print $2}' || echo "0")
    memory=$(docker info 2>/dev/null | grep "Total Memory:" | awk '{print $3}' | sed 's/GiB//' || echo "0")
    
    local memory_num
    memory_num=$(echo "$memory" | awk '{print int($1)}')
    
    if [ "$cpus" -lt 4 ] || [ "$memory_num" -lt 8 ]; then
      warn "Docker Desktop resources are still insufficient:"
      echo "  CPUs: $cpus (recommended: $TARGET_CPUS+)"
      echo "  Memory: ${memory}GiB (recommended: ${TARGET_MEMORY}GB+)"
      exit 1
    fi
    
    note "Docker Desktop resources look good:"
    echo "  CPUs: $cpus"
    echo "  Memory: ${memory}GiB"
    echo ""
  else
    warn "Could not detect Docker runtime. Please ensure Docker is running."
    exit 1
  fi
  
  if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}\$"; then
    warn "Deleting existing kind cluster '${CLUSTER_NAME}'..."
    kind delete cluster --name "${CLUSTER_NAME}"
    note "Cluster deleted."
  else
    note "No existing cluster found."
  fi
  
  note "Creating new kind cluster '${CLUSTER_NAME}' with updated configuration..."
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  kind create cluster --name "${CLUSTER_NAME}" --config "${script_dir}/kind-config-local.yaml"
  
  note "Waiting for cluster to be ready..."
  kubectl wait --for=condition=Ready nodes --all --timeout=120s || true
  
  note "Verifying node resources..."
  kubectl get nodes -o custom-columns=NAME:.metadata.name,CPU:.status.capacity.cpu,MEMORY:.status.capacity.memory
  
  bold "Cluster recreated successfully!"
  echo ""
  note "Next steps:"
  echo "  1. Rebuild and deploy your applications:"
  echo "     cd deploy/kubernetes/scripts"
  echo "     ENV=local CLUSTER_TYPE=kind REGISTRY=\"\" TAG=latest KIND_CLUSTER_NAME=logwise-local ./build-and-push.sh"
  echo "     ENV=local ./deploy.sh"
}

main "$@"

