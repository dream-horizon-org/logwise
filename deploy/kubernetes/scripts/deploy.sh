#!/usr/bin/env bash
# Unified deployment script for LogWise Kubernetes deployments
# Handles pre-deployment validation, deployment, and health checks

set -euo pipefail

# Source common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SHARED_SCRIPTS_DIR="$SCRIPT_DIR/../../shared/scripts"
# shellcheck source=../../shared/scripts/common.sh
source "$SHARED_SCRIPTS_DIR/common.sh"

# Get repository root
REPO_ROOT="$(get_repo_root)"
cd "$REPO_ROOT"

# Configuration
ENV="${ENV:-local}"
NAMESPACE="${NAMESPACE:-logwise}"
DRY_RUN="${DRY_RUN:-false}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-600}"
ROLLBACK_ON_FAILURE="${ROLLBACK_ON_FAILURE:-true}"

# Validate environment
validate_environment() {
  local env="$1"
  
  case "$env" in
    local|nonprod|prod)
      return 0
      ;;
    *)
      log_error "Invalid environment: $env. Must be one of: local, nonprod, prod"
      return 1
      ;;
  esac
}

# Pre-deployment validation
pre_deploy_validation() {
  log_info "Running pre-deployment validation"
  
  # Check kubectl
  require_command kubectl
  
  # Check kubeconfig
  if ! kubectl cluster-info >/dev/null 2>&1; then
    log_error "Cannot connect to Kubernetes cluster. Check your kubeconfig."
    return 1
  fi
  
  # Validate manifests
  local overlay_dir="$REPO_ROOT/deploy/kubernetes/overlays/$ENV"
  if [ ! -d "$overlay_dir" ]; then
    log_error "Overlay directory not found: $overlay_dir"
    return 1
  fi
  
  # Dry-run apply to validate
  if kubectl apply --dry-run=client -k "$overlay_dir" >/dev/null 2>&1; then
    log_success "Manifest validation passed"
  else
    log_error "Manifest validation failed"
    kubectl apply --dry-run=client -k "$overlay_dir"
    return 1
  fi
}

# Deploy to Kubernetes
deploy() {
  local overlay_dir="$REPO_ROOT/deploy/kubernetes/overlays/$ENV"
  
  log_info "Deploying to environment: $ENV"
  log_info "Using overlay: $overlay_dir"
  
  if [ "$DRY_RUN" = "true" ]; then
    log_info "DRY RUN: Would apply manifests"
    kubectl apply --dry-run=client -k "$overlay_dir"
    return 0
  fi
  
  # Apply manifests
  if kubectl apply -k "$overlay_dir"; then
    log_success "Manifests applied successfully"
  else
    log_error "Failed to apply manifests"
    return 1
  fi
}

# Wait for deployment to be ready
wait_for_deployment() {
  log_info "Waiting for deployment to be ready (timeout: ${WAIT_TIMEOUT}s)"
  
  local start_time
  start_time=$(date +%s)
  
  # Wait for namespace
  if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
    log_warn "Namespace $NAMESPACE does not exist yet, waiting..."
    sleep 5
  fi
  
  # Wait for critical pods to be ready (exclude Pending pods that may be resource-constrained)
  # Check that we have at least the core services running
  local condition="kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | awk '\$3!=\"Running\" && \$3!=\"Completed\" && \$3!=\"Pending\" {exit 1}' && kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | grep -E '(orchestrator|kafka|spark-master|vector-logs)' | grep -q Running"
  
  if wait_for "$condition" "$WAIT_TIMEOUT" 5; then
    log_success "Core pods are ready"
    kubectl get pods -n "$NAMESPACE"
    
    # Check for any pods in error states (excluding Pending which may be resource constraints)
    local error_pods
    error_pods=$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null | awk '$3!="Running" && $3!="Completed" && $3!="Pending" {print $1, $3}' || true)
    
    if [ -n "$error_pods" ]; then
      log_warn "Some pods are in error states:"
      echo "$error_pods" | while read -r pod status; do
        log_warn "  - $pod: $status"
      done
    fi
    
    # Check for pending pods (informational)
    local pending_pods
    pending_pods=$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null | awk '$3=="Pending" {print $1}' || true)
    
    if [ -n "$pending_pods" ]; then
      log_info "Some pods are pending (likely due to resource constraints):"
      echo "$pending_pods" | while read -r pod; do
        log_info "  - $pod: Pending"
      done
    fi
    
    return 0
  else
    log_error "Timeout waiting for core pods to be ready"
    kubectl get pods -n "$NAMESPACE"
    return 1
  fi
}

# Rollback deployment
rollback() {
  log_warn "Rolling back deployment"
  
  # Get previous revision
  local previous_revision
  previous_revision=$(kubectl rollout history deployment -n "$NAMESPACE" 2>/dev/null | tail -2 | head -1 | awk '{print $1}' || echo "")
  
  if [ -n "$previous_revision" ]; then
    if kubectl rollout undo deployment -n "$NAMESPACE" --to-revision="$previous_revision"; then
      log_success "Rolled back to revision $previous_revision"
      return 0
    else
      log_error "Failed to rollback"
      return 1
    fi
  else
    log_warn "No previous revision found, cannot rollback"
    return 1
  fi
}

# Health check
health_check() {
  log_info "Running health checks"
  
  # Check if services are accessible
  local services=("orchestrator" "grafana" "vector")
  local failed=0
  
  for service in "${services[@]}"; do
    if kubectl get service "$service" -n "$NAMESPACE" >/dev/null 2>&1; then
      log_success "Service $service exists"
    else
      log_warn "Service $service not found"
      failed=1
    fi
  done
  
  if [ $failed -eq 0 ]; then
    log_success "Health checks passed"
    return 0
  else
    log_warn "Some health checks failed"
    return 1
  fi
}

# Main deployment function
main() {
  local exit_code=0
  
  # Validate environment
  if ! validate_environment "$ENV"; then
    exit 1
  fi
  
  # Pre-deployment validation
  if ! pre_deploy_validation; then
    log_error "Pre-deployment validation failed"
    exit 1
  fi
  
  # Deploy
  if ! deploy; then
    log_error "Deployment failed"
    exit_code=1
    
    # Rollback on failure if enabled
    if [ "$ROLLBACK_ON_FAILURE" = "true" ] && [ "$DRY_RUN" != "true" ]; then
      rollback || true
    fi
    
    exit $exit_code
  fi
  
  # Wait for deployment
  if [ "$DRY_RUN" != "true" ]; then
    if ! wait_for_deployment; then
      log_error "Deployment did not become ready"
      exit_code=1
    fi
  fi
  
  # Health check
  if [ "$DRY_RUN" != "true" ] && [ $exit_code -eq 0 ]; then
    health_check || exit_code=1
  fi
  
  if [ $exit_code -eq 0 ]; then
    log_success "Deployment completed successfully"
  else
    log_error "Deployment completed with errors"
  fi
  
  exit $exit_code
}

# Run main function
main "$@"

