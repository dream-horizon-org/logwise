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

destroy_local() {
  if ! need_cmd kind; then
    warn "kind not found; nothing to destroy for local kind cluster."
    return 0
  fi

  local name="logwise-local"
  if kind get clusters 2>/dev/null | grep -q "^${name}\$"; then
    note "Deleting kind cluster '${name}'..."
    kind delete cluster --name "${name}"
    bold "Deleted kind cluster '${name}'."
  else
    warn "kind cluster '${name}' not found; nothing to delete."
  fi
}

destroy_env() {
  local env="$1"  # nonprod | prod

  if ! need_cmd kubectl; then
    warn "kubectl not found; cannot delete namespaces. Please install kubectl."
    exit 1
  fi

  note "Deleting 'logwise' namespace from current Kubernetes context for '${env}'..."

  # Delete primary namespace but ignore if it doesn't exist.
  kubectl delete namespace logwise --ignore-not-found=true

  bold "Requested deletion of namespace 'logwise'."
  note "You can watch progress with: kubectl get ns && kubectl get pods -A"
}

usage() {
  cat <<EOF
Usage: $(basename "$0") <mode>

Modes:
  local      Delete the local kind cluster 'logwise-local'.
  nonprod    Delete the 'logwise' namespace from the current cluster.
  prod       Delete the 'logwise' namespace from the current cluster.

This script does NOT remove images from your registry or undo DNS/Ingress changes.
EOF
}

main() {
  local mode="${1:-}"

  case "$mode" in
    local)
      destroy_local
      ;;
    nonprod)
      destroy_env "nonprod"
      ;;
    prod)
      destroy_env "prod"
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"


