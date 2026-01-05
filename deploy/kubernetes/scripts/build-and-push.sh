#!/usr/bin/env bash
# Build and push Docker images for LogWise
# This script builds all required images and optionally pushes them to a registry

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
REGISTRY="${REGISTRY:-}"
TAG="${TAG:-1.0.0}"
ENV="${ENV:-local}"
CLUSTER_TYPE="${CLUSTER_TYPE:-docker-desktop}"
PARALLEL_BUILD="${PARALLEL_BUILD:-true}"
PUSH_IMAGES="${PUSH_IMAGES:-true}"

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

# Helper to get image path
img() {
  local service="$1"
  local image_name
  image_name="$(get_image_name "$service")"
  
  # Handle local docker registry (no registry prefix)
  if [ -n "$REGISTRY" ] && [ "$REGISTRY" != "local" ] && [ "$REGISTRY" != "docker" ]; then
    echo "$REGISTRY/$image_name:$TAG"
  else
    echo "$image_name:$TAG"
  fi
}

# Build a single image
build_image() {
  local service="$1"
  local image_tag="$2"
  local dockerfile_path="$3"
  local context_path="$4"
  
  log_info "Building $service image: $image_tag"
  
  # Build docker command - use conditional expansion for empty arrays
  if [ -n "${DOCKER_BUILDKIT:-}" ]; then
    if docker build \
      --tag "$image_tag" \
      --file "$dockerfile_path" \
      --progress=plain \
      --build-arg DOCKER_BUILDKIT=1 \
      "$context_path"; then
      log_success "Built $service image: $image_tag"
      return 0
    else
      log_error "Failed to build $service image: $image_tag"
      return 1
    fi
  else
    if docker build \
      --tag "$image_tag" \
      --file "$dockerfile_path" \
      --progress=plain \
      "$context_path"; then
      log_success "Built $service image: $image_tag"
      return 0
    else
      log_error "Failed to build $service image: $image_tag"
      return 1
    fi
  fi
}

# Push a single image
push_image() {
  local image_tag="$1"
  
  # Skip push for local docker or when no registry is specified
  if [ -z "$REGISTRY" ] || [ "$REGISTRY" = "local" ] || [ "$REGISTRY" = "docker" ]; then
    log_info "Using local Docker registry, skipping push for $image_tag"
    return 0
  fi
  
  log_info "Pushing image: $image_tag"
  
  if docker push "$image_tag"; then
    log_success "Pushed image: $image_tag"
    return 0
  else
    log_error "Failed to push image: $image_tag"
    return 1
  fi
}

# Load image into kind cluster
load_kind_image() {
  local image_tag="$1"
  local cluster_name="${KIND_CLUSTER_NAME:-kind}"
  
  log_info "Loading image into kind cluster '$cluster_name': $image_tag"
  
  if kind load docker-image "$image_tag" --name "$cluster_name"; then
    log_success "Loaded image into kind cluster: $image_tag"
    return 0
  else
    log_error "Failed to load image into kind cluster: $image_tag"
    return 1
  fi
}

# Main build function
main() {
  log_info "Building images with TAG=$TAG REGISTRY='${REGISTRY}' (env=$ENV, cluster=$CLUSTER_TYPE)"
  
  # Define images to build (using space-separated format for bash 3.2 compatibility)
  # Format: "service:dockerfile:context"
  local images=(
    "orchestrator:orchestrator/docker/Dockerfile:orchestrator"
    "spark:spark/docker/Dockerfile:spark"
    "healthcheck:deploy/healthcheck-dummy/Dockerfile:deploy/healthcheck-dummy"
    "vector:vector/Dockerfile:vector"
  )
  
  # Build images
  local pids=()
  local services=()
  
  for image_spec in "${images[@]}"; do
    IFS=':' read -r service dockerfile context <<< "$image_spec"
    local image_tag
    image_tag="$(img "$service")"
    
    if [ "$PARALLEL_BUILD" = "true" ]; then
      log_info "Starting parallel build for $service"
      build_image "$service" "$image_tag" "$dockerfile" "$context" &
      pids+=($!)
      services+=("$service:$image_tag")
    else
      build_image "$service" "$image_tag" "$dockerfile" "$context" || return 1
    fi
  done
  
  # Wait for parallel builds
  if [ "$PARALLEL_BUILD" = "true" ] && [ ${#pids[@]} -gt 0 ]; then
    log_info "Waiting for all builds to complete..."
    local failed=0
    local i=0
    for pid in "${pids[@]}"; do
      if ! wait "$pid"; then
        local service_name="${services[$i]:-unknown}"
        log_error "Build failed for $service_name"
        failed=1
      fi
      i=$((i + 1))
    done
    
    if [ $failed -eq 1 ]; then
      log_error "One or more builds failed"
      return 1
    fi
  fi
  
  # Push images if registry is set and not local docker
  if [ "$PUSH_IMAGES" = "true" ] && [ -n "$REGISTRY" ] && [ "$REGISTRY" != "local" ] && [ "$REGISTRY" != "docker" ]; then
    log_info "Pushing images to registry: $REGISTRY"
    for image_spec in "${images[@]}"; do
      IFS=':' read -r service dockerfile context <<< "$image_spec"
      local image_tag
      image_tag="$(img "$service")"
      push_image "$image_tag" || return 1
    done
  elif [ "$PUSH_IMAGES" = "true" ] && [ -n "$REGISTRY" ] && ([ "$REGISTRY" = "local" ] || [ "$REGISTRY" = "docker" ]); then
    log_info "Using local Docker registry, images will not be pushed"
  fi
  
  # Load images into kind if needed
  if [ "$CLUSTER_TYPE" = "kind" ]; then
    local cluster_name="${KIND_CLUSTER_NAME:-kind}"
    log_info "Loading images into kind cluster '$cluster_name'"
    for image_spec in "${images[@]}"; do
      IFS=':' read -r service dockerfile context <<< "$image_spec"
      local image_tag
      image_tag="$(img "$service")"
      load_kind_image "$image_tag" || return 1
    done
  fi
  
  log_success "All images built successfully"
}

# Run main function
main "$@"

