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
  
  # Handle special registry cases
  if [ -z "$REGISTRY" ] || [ "$REGISTRY" = "local" ] || [ "$REGISTRY" = "docker" ]; then
    # Local Docker - no registry prefix
    echo "$image_name:$TAG"
  elif [ "$REGISTRY" = "dockerhub" ]; then
    # Docker Hub requires username
    if [ -z "${DOCKERHUB_USERNAME:-}" ]; then
      log_error "DOCKERHUB_USERNAME must be set when using REGISTRY=dockerhub"
      log_error "Example: DOCKERHUB_USERNAME=myuser REGISTRY=dockerhub ./build-and-push.sh"
      echo ""  # Return empty string to indicate error
      return 1
    fi
    echo "${DOCKERHUB_USERNAME}/${image_name}:${TAG}"
  else
    # Other registries (ECR, GHCR, etc.)
    echo "$REGISTRY/$image_name:$TAG"
  fi
}

# Check if docker buildx is available
has_buildx() {
  docker buildx version >/dev/null 2>&1
}

# Check if docker build supports --progress flag
supports_progress_flag() {
  # Check regular docker build first
  if docker build --help 2>&1 | grep -q "\-\-progress"; then
    return 0
  fi
  # Check buildx if available
  if has_buildx && docker buildx build --help 2>&1 | grep -q "\-\-progress"; then
    return 0
  fi
  return 1
}

# Check if Dockerfile uses BuildKit-specific features
uses_buildkit_features() {
  local dockerfile_path="$1"
  if [ -f "$dockerfile_path" ]; then
    grep -q "syntax=docker/dockerfile" "$dockerfile_path" || \
    grep -q "--mount=type=cache" "$dockerfile_path" || \
    grep -q "RUN --mount" "$dockerfile_path"
  else
    return 1
  fi
}

# Build a single image
build_image() {
  local service="$1"
  local image_tag="$2"
  local dockerfile_path="$3"
  local context_path="$4"
  
  log_info "Building $service image: $image_tag"
  
  # Check if Dockerfile requires BuildKit
  local needs_buildkit=false
  if uses_buildkit_features "$dockerfile_path"; then
    needs_buildkit=true
    log_info "Dockerfile uses BuildKit features, BuildKit will be enabled"
  fi
  
  # Determine build command and flags
  local build_cmd="docker build"
  local build_args=(
    --tag "$image_tag"
    --file "$dockerfile_path"
  )
  
  # Use buildx if available (provides BuildKit support)
  if has_buildx; then
    build_cmd="docker buildx build"
    # Enable BuildKit when using buildx (it's enabled by default)
    build_args+=(--load)  # Load image into local Docker daemon
  elif [ "$needs_buildkit" = "true" ]; then
    # BuildKit features detected but buildx not available
    # Try to enable BuildKit via environment variable
    log_info "BuildKit features detected but buildx not available, attempting with DOCKER_BUILDKIT=1"
    export DOCKER_BUILDKIT=1
  fi
  
  # Add --progress flag only if supported
  if supports_progress_flag; then
    build_args+=(--progress=plain)
  fi
  
  # Add BuildKit build arg if DOCKER_BUILDKIT is set
  if [ -n "${DOCKER_BUILDKIT:-}" ]; then
    build_args+=(--build-arg DOCKER_BUILDKIT=1)
  fi
  
  # Add context path
  build_args+=("$context_path")
  
  # Execute build
  if $build_cmd "${build_args[@]}"; then
    log_success "Built $service image: $image_tag"
    return 0
  else
    log_error "Failed to build $service image: $image_tag"
    if [ "$needs_buildkit" = "true" ] && ! has_buildx; then
      log_error "This Dockerfile requires BuildKit. Install docker-buildx: brew install docker-buildx"
    fi
    return 1
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
  
  # Handle Docker Hub authentication
  if [ "$REGISTRY" = "dockerhub" ]; then
    log_info "Checking Docker Hub authentication..."
    
    # Check if already logged in to Docker Hub by checking config file
    local docker_config="${HOME}/.docker/config.json"
    local is_logged_in=false
    
    if [ -f "$docker_config" ]; then
      # Check if Docker Hub (index.docker.io) auth exists in config
      if grep -q "index.docker.io" "$docker_config" 2>/dev/null || \
         grep -q '"https://index.docker.io/v1/"' "$docker_config" 2>/dev/null; then
        is_logged_in=true
      fi
    fi
    
    # If not logged in, attempt login
    if [ "$is_logged_in" = "false" ]; then
      log_warn "Not logged into Docker Hub. Attempting login..."
      if [ -z "${DOCKERHUB_USERNAME:-}" ] || [ -z "${DOCKERHUB_PASSWORD:-}" ]; then
        log_error "DOCKERHUB_USERNAME and DOCKERHUB_PASSWORD must be set for Docker Hub"
        log_error "Example: DOCKERHUB_USERNAME=myuser DOCKERHUB_PASSWORD=mypass REGISTRY=dockerhub ./build-and-push.sh"
        log_error "Or login manually first: docker login"
        return 1
      fi
      echo "$DOCKERHUB_PASSWORD" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin || {
        log_error "Failed to login to Docker Hub"
        log_error "Please check your credentials or login manually: docker login"
        return 1
      }
      log_success "Successfully logged into Docker Hub"
    else
      log_info "Already authenticated with Docker Hub"
    fi
  fi
  
  # Handle ECR authentication
  if [[ "$REGISTRY" =~ \.dkr\.ecr\. ]]; then
    log_info "Authenticating with ECR..."
    local region
    region=$(echo "$REGISTRY" | sed -n 's/.*\.dkr\.ecr\.\([^.]*\)\.amazonaws\.com.*/\1/p')
    if [ -n "$region" ]; then
      aws ecr get-login-password --region "$region" | \
        docker login --username AWS --password-stdin "$REGISTRY" || {
        log_error "Failed to login to ECR"
        return 1
      }
    fi
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
    
    # Check if img() returned an error (empty string)
    if [ -z "$image_tag" ]; then
      log_error "Failed to determine image tag for $service"
      return 1
    fi
    
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

