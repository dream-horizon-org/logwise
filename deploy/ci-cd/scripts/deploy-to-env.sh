#!/usr/bin/env bash
# CI script for deploying to an environment
# This script is used in CI/CD pipelines to deploy LogWise to a Kubernetes cluster

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Source common functions
SHARED_SCRIPTS_DIR="$REPO_ROOT/deploy/shared/scripts"
# shellcheck source=../../shared/scripts/common.sh
source "$SHARED_SCRIPTS_DIR/common.sh"

cd "$REPO_ROOT"

# Configuration from environment
ENV="${ENV:-nonprod}"
NAMESPACE="${NAMESPACE:-logwise}"
DRY_RUN="${DRY_RUN:-false}"

if [ -z "$ENV" ]; then
  log_error "ENV environment variable is required"
  exit 1
fi

log_info "Deploying to environment: $ENV"

# Validate environment
case "$ENV" in
  local|nonprod|prod)
    ;;
  *)
    log_error "Invalid environment: $ENV. Must be one of: local, nonprod, prod"
    exit 1
    ;;
esac

# Check kubectl
require_command kubectl

# Verify cluster connection
if ! kubectl cluster-info >/dev/null 2>&1; then
  log_error "Cannot connect to Kubernetes cluster. Check your kubeconfig."
  exit 1
fi

# Deploy
export ENV NAMESPACE DRY_RUN
"$REPO_ROOT/deploy/kubernetes/scripts/deploy.sh"

log_success "Deployment to $ENV completed"


