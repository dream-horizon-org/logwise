#!/usr/bin/env bash
# Helper script to create .env files for Docker and/or Kubernetes

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Script is in deploy/scripts/, so go up two levels to get repo root
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common functions
SHARED_SCRIPTS_DIR="$REPO_ROOT/deploy/shared/scripts"
# shellcheck source=shared/scripts/common.sh
if [ -f "$SHARED_SCRIPTS_DIR/common.sh" ]; then
  source "$SHARED_SCRIPTS_DIR/common.sh"
else
  echo "Error: Could not find common.sh at $SHARED_SCRIPTS_DIR/common.sh" >&2
  exit 1
fi

usage() {
  cat <<EOF
Usage: $0 [OPTIONS]

Create .env files for Docker and/or Kubernetes deployments.

Options:
  -d, --docker          Create .env for Docker (default: deploy/docker/.env)
  -k, --kubernetes      Create .env for Kubernetes (default: deploy/kubernetes/.env)
  -a, --all             Create .env for both Docker and Kubernetes
  -t, --template PATH   Use custom template (default: deploy/shared/templates/env.template)
  -f, --force           Overwrite existing .env files
  -h, --help            Show this help message

Examples:
  # Create Docker .env
  $0 --docker

  # Create Kubernetes .env
  $0 --kubernetes

  # Create both
  $0 --all

  # Create with custom template
  $0 --docker --template /path/to/custom.template
EOF
}

create_env_file() {
  local target_file="$1"
  local template_file="${2:-$REPO_ROOT/deploy/shared/templates/env.template}"
  local force="${3:-false}"
  
  if [ -f "$target_file" ] && [ "$force" != "true" ]; then
    log_warn "File already exists: $target_file"
    log_info "Use --force to overwrite"
    return 1
  fi
  
  if [ ! -f "$template_file" ]; then
    log_error "Template file not found: $template_file"
    return 1
  fi
  
  local target_dir
  target_dir="$(dirname "$target_file")"
  mkdir -p "$target_dir"
  
  cp "$template_file" "$target_file"
  log_success "Created .env file: $target_file"
  log_info "Please edit $target_file and fill in your configuration values"
  
  return 0
}

main() {
  local create_docker=false
  local create_k8s=false
  local template_file="$REPO_ROOT/deploy/shared/templates/env.template"
  local force=false
  
  while [[ $# -gt 0 ]]; do
    case $1 in
      -d|--docker)
        create_docker=true
        shift
        ;;
      -k|--kubernetes)
        create_k8s=true
        shift
        ;;
      -a|--all)
        create_docker=true
        create_k8s=true
        shift
        ;;
      -t|--template)
        template_file="$2"
        shift 2
        ;;
      -f|--force)
        force=true
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        log_error "Unknown option: $1"
        usage
        exit 1
        ;;
    esac
  done
  
  # Default to Docker if nothing specified
  if [ "$create_docker" = false ] && [ "$create_k8s" = false ]; then
    create_docker=true
  fi
  
  local errors=0
  
  if [ "$create_docker" = true ]; then
    if ! create_env_file "$REPO_ROOT/deploy/docker/.env" "$template_file" "$force"; then
      errors=$((errors + 1))
    fi
  fi
  
  if [ "$create_k8s" = true ]; then
    if ! create_env_file "$REPO_ROOT/deploy/kubernetes/.env" "$template_file" "$force"; then
      errors=$((errors + 1))
    fi
  fi
  
  if [ $errors -eq 0 ]; then
    log_success "Environment file(s) created successfully"
    echo ""
    log_info "Next steps:"
    if [ "$create_docker" = true ]; then
      echo "  1. Edit deploy/docker/.env with your configuration"
      echo "  2. Run: cd deploy/docker && make setup"
    fi
    if [ "$create_k8s" = true ]; then
      echo "  1. Edit deploy/kubernetes/.env with your configuration"
      echo "  2. Sync to Kubernetes: ./deploy/kubernetes/scripts/sync-config.sh deploy/kubernetes/.env"
    fi
  else
    log_error "Failed to create some environment files"
    exit 1
  fi
}

main "$@"

