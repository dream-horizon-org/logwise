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
WAIT_TIMEOUT="${WAIT_TIMEOUT:-300}"
ROLLBACK_ON_FAILURE="${ROLLBACK_ON_FAILURE:-true}"
REGISTRY="${REGISTRY:-}"
TAG="${TAG:-latest}"

# Image registry config
IMAGE_REGISTRY_CONFIG="$REPO_ROOT/deploy/shared/config/image-registry.yaml"

# Helper to get image name from config
get_image_name() {
  local service="$1"
  local name
  name=$(grep -A 5 "^  ${service}:" "$IMAGE_REGISTRY_CONFIG" 2>/dev/null | grep "name:" | sed 's/.*name:[[:space:]]*//' | tr -d '"' || echo "")
  if [ -z "$name" ]; then
    # Fallback to default naming
    echo "logwise-${service}"
  else
    echo "$name"
  fi
}

# Helper to get full image path (matches build-and-push.sh logic)
get_image_path() {
  local service="$1"
  local image_name
  image_name="$(get_image_name "$service")"
  
  # Handle special registry cases
  if [ -z "$REGISTRY" ] || [ "$REGISTRY" = "local" ] || [ "$REGISTRY" = "docker" ]; then
    # Local Docker - no registry prefix
    echo "$image_name:$TAG"
  elif [ "$REGISTRY" = "dockerhub" ]; then
    # Docker Hub requires username
    if [ -z "${DOCKERHUB_USERNAME:-}" ]; then
      log_error "DOCKERHUB_USERNAME must be set when using REGISTRY=dockerhub"
      log_error "Example: DOCKERHUB_USERNAME=myuser REGISTRY=dockerhub ./deploy.sh"
      echo ""  # Return empty string to indicate error
      return 1
    fi
    echo "${DOCKERHUB_USERNAME}/${image_name}:${TAG}"
  else
    # Other registries (ECR, GHCR, etc.)
    echo "$REGISTRY/$image_name:$TAG"
  fi
}

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

# Ensure namespace exists
ensure_namespace() {
  if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
    log_info "Creating namespace: $NAMESPACE"
    if kubectl create namespace "$NAMESPACE" 2>/dev/null; then
      log_success "Created namespace: $NAMESPACE"
    else
      log_warn "Namespace $NAMESPACE may already exist or creation failed"
    fi
  fi
}

# Ensure Docker Hub secret exists for authenticated pulls
ensure_dockerhub_secret() {
  if [ "$REGISTRY" = "dockerhub" ] && [ -n "${DOCKERHUB_USERNAME:-}" ]; then
    # Ensure namespace exists first
    ensure_namespace
    
    if ! kubectl get secret dockerhub-secret -n "$NAMESPACE" >/dev/null 2>&1; then
      log_warn "Docker Hub secret 'dockerhub-secret' not found in namespace $NAMESPACE"
      log_info "Creating Docker Hub secret for authenticated image pulls..."
      
      if [ -z "${DOCKERHUB_PASSWORD:-}" ]; then
        log_error "DOCKERHUB_PASSWORD environment variable is required to create Docker Hub secret"
        log_error "Please set DOCKERHUB_PASSWORD and run the deployment again"
        log_info "Alternatively, create the secret manually:"
        log_info "  kubectl create secret docker-registry dockerhub-secret \\"
        log_info "    --docker-server=docker.io \\"
        log_info "    --docker-username=$DOCKERHUB_USERNAME \\"
        log_info "    --docker-password=YOUR_PASSWORD \\"
        log_info "    --docker-email=YOUR_EMAIL \\"
        log_info "    -n $NAMESPACE"
        return 1
      fi
      
      local create_output
      create_output=$(kubectl create secret docker-registry dockerhub-secret \
        --docker-server=docker.io \
        --docker-username="$DOCKERHUB_USERNAME" \
        --docker-password="$DOCKERHUB_PASSWORD" \
        --docker-email="${DOCKERHUB_EMAIL:-$DOCKERHUB_USERNAME@example.com}" \
        -n "$NAMESPACE" 2>&1)
      local create_status=$?
      
      if [ $create_status -eq 0 ]; then
        log_success "Created Docker Hub secret"
      elif echo "$create_output" | grep -q "already exists"; then
        log_info "Docker Hub secret already exists (created by another process)"
      else
        log_error "Failed to create Docker Hub secret"
        log_error "Error: $create_output"
        return 1
      fi
    else
      log_info "Docker Hub secret already exists"
    fi
  fi
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
  
  # Ensure Docker Hub secret exists if using Docker Hub
  if ! ensure_dockerhub_secret; then
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
  
  # Handle image replacement if REGISTRY is set (for nonprod/prod)
  if [ -n "$REGISTRY" ] && [ "$REGISTRY" != "local" ] && [ "$REGISTRY" != "docker" ] && [ "$ENV" != "local" ]; then
    log_info "Setting image registry: $REGISTRY, tag: $TAG"
    
    # Get image paths for all services
    local services=("orchestrator" "spark" "vector" "healthcheck")
    local image_map=()
    
    for service in "${services[@]}"; do
      local image_path
      image_path="$(get_image_path "$service")"
      
      if [ -z "$image_path" ]; then
        log_error "Failed to determine $service image path"
        return 1
      fi
      
      # Map service name to kustomize image name
      local kustomize_name
      case "$service" in
        orchestrator) kustomize_name="logwise-orchestrator" ;;
        spark) kustomize_name="logwise-spark" ;;
        vector) kustomize_name="logwise-vector" ;;
        healthcheck) kustomize_name="logwise-healthcheck-dummy" ;;
        *) kustomize_name="logwise-${service}" ;;
      esac
      
      image_map+=("${kustomize_name}=${image_path}")
      log_info "Setting ${kustomize_name} image: $image_path"
    done
    
    # Use kustomize to set images
    if command -v kustomize >/dev/null 2>&1; then
      cd "$overlay_dir"
      for image_spec in "${image_map[@]}"; do
        if kustomize edit set image "$image_spec"; then
          log_success "Updated image via kustomize: $image_spec"
        else
          log_error "Failed to set image via kustomize: $image_spec"
          cd "$REPO_ROOT"
          return 1
        fi
      done
      cd "$REPO_ROOT"
    else
      log_warn "kustomize command not found, manually updating kustomization.yaml"
      # Manually update kustomization.yaml if kustomize is not available
      # Note: kubectl has kustomize built-in but doesn't support 'edit' subcommand
      local kustomization_file="$overlay_dir/kustomization.yaml"
      if [ ! -f "$kustomization_file" ]; then
        log_error "kustomization.yaml not found at $kustomization_file"
        return 1
      fi
      
      for image_spec in "${image_map[@]}"; do
        IFS='=' read -r image_name image_path <<< "$image_spec"
        # Extract registry/name and tag from image_path (format: registry/name:tag)
        IFS=':' read -r image_repo image_tag <<< "$image_path"
        
        # Use sed to update newName and newTag
        if grep -q "name: $image_name" "$kustomization_file"; then
          # Create backup and update using sed range pattern
          cp "$kustomization_file" "${kustomization_file}.bak"
          sed -i.tmp "/- name: $image_name$/,/newTag:/ {
            s|^\([[:space:]]*newName:\).*|\1 $image_repo|
            s|^\([[:space:]]*newTag:\).*|\1 $image_tag|
          }" "$kustomization_file"
          rm -f "${kustomization_file}.tmp"
          
          # Verify the update worked
          if grep -A 2 "name: $image_name" "$kustomization_file" | grep -q "newName: $image_repo"; then
            log_success "Updated $image_name in kustomization.yaml: $image_path"
            rm -f "${kustomization_file}.bak"
          else
            log_error "Failed to update $image_name in kustomization.yaml"
            mv "${kustomization_file}.bak" "$kustomization_file"
            return 1
          fi
        else
          log_warn "Image $image_name not found in kustomization.yaml"
        fi
      done
    fi
  fi
  
  if [ "$DRY_RUN" = "true" ]; then
    log_info "DRY RUN: Would apply manifests"
    kubectl apply --dry-run=client -k "$overlay_dir"
    return 0
  fi
  
  # Apply manifests
  if kubectl apply -k "$overlay_dir"; then
    log_success "Manifests applied successfully"
    
    # If REGISTRY is set, also update images directly via kubectl set image
    if [ -n "$REGISTRY" ] && [ "$REGISTRY" != "local" ] && [ "$REGISTRY" != "docker" ] && [ "$ENV" != "local" ]; then
      log_info "Updating deployment images to use registry: $REGISTRY"
      
      # Wait a moment for deployments to be created
      sleep 2
      
      # Update all service images that were built
      local services=("orchestrator" "spark" "vector" "healthcheck")
      
      for service in "${services[@]}"; do
        local image_path
        image_path="$(get_image_path "$service")"
        
        if [ -z "$image_path" ]; then
          log_warn "Could not determine $service image path, skipping"
          continue
        fi
        
        # Handle special cases for deployment/container name mapping
        case "$service" in
          orchestrator)
            if kubectl get deployment/orchestrator -n "$NAMESPACE" >/dev/null 2>&1; then
              kubectl set image deployment/orchestrator \
                orchestrator="$image_path" \
                -n "$NAMESPACE" && log_success "Updated orchestrator image to $image_path" || log_warn "Failed to update orchestrator image"
            else
              log_warn "Deployment orchestrator does not exist yet"
            fi
            ;;
          spark)
            # Spark has two deployments: master and worker with different container names
            if kubectl get deployment/spark-master -n "$NAMESPACE" >/dev/null 2>&1; then
              kubectl set image deployment/spark-master \
                spark-master="$image_path" \
                -n "$NAMESPACE" && log_success "Updated spark-master image to $image_path" || log_warn "Failed to update spark-master image"
            else
              log_warn "Deployment spark-master does not exist yet"
            fi
            if kubectl get deployment/spark-worker -n "$NAMESPACE" >/dev/null 2>&1; then
              kubectl set image deployment/spark-worker \
                spark-worker="$image_path" \
                -n "$NAMESPACE" && log_success "Updated spark-worker image to $image_path" || log_warn "Failed to update spark-worker image"
            else
              log_warn "Deployment spark-worker does not exist yet"
            fi
            ;;
          vector)
            if kubectl get deployment/vector-logs -n "$NAMESPACE" >/dev/null 2>&1; then
              kubectl set image deployment/vector-logs \
                vector="$image_path" \
                -n "$NAMESPACE" && log_success "Updated vector-logs image to $image_path" || log_warn "Failed to update vector-logs image"
            else
              log_warn "Deployment vector-logs does not exist yet"
            fi
            ;;
          healthcheck)
            if kubectl get deployment/healthcheck-dummy -n "$NAMESPACE" >/dev/null 2>&1; then
              kubectl set image deployment/healthcheck-dummy \
                healthcheck-dummy="$image_path" \
                -n "$NAMESPACE" && log_success "Updated healthcheck-dummy image to $image_path" || log_warn "Failed to update healthcheck-dummy image"
            else
              log_warn "Deployment healthcheck-dummy does not exist yet"
            fi
            ;;
        esac
      done
    fi
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

