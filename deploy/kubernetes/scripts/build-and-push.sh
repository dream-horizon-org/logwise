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
# PLATFORM controls the build target for docker buildx/build.
# IMPORTANT:
# - For local kind clusters on Apple Silicon, the node is often linux/arm64.
# - If we build linux/amd64 images and load them into a linux/arm64 kind node,
#   pods will fail with "no match for platform" / ErrImagePull / ImagePullBackOff.
# We therefore auto-detect a sane default when PLATFORM is not explicitly set.
PLATFORM="${PLATFORM:-}"

# Image registry config
IMAGE_REGISTRY_CONFIG="$REPO_ROOT/deploy/shared/config/image-registry.yaml"

# Resolve a default platform if PLATFORM is not set by the caller.
# - Prefer reading the actual cluster node architecture when CLUSTER_TYPE=kind.
# - Fallback to the local Docker daemon architecture.
resolve_default_platform() {
  # If user explicitly set it (including multi-platform), respect it.
  if [ -n "${PLATFORM:-}" ]; then
    return 0
  fi

  local arch=""

  if [ "${CLUSTER_TYPE:-}" = "kind" ] && command -v kubectl >/dev/null 2>&1; then
    # Try to read node arch from the active kube-context.
    arch="$(kubectl get nodes -o jsonpath='{.items[0].status.nodeInfo.architecture}' 2>/dev/null || true)"
  fi

  if [ -z "$arch" ] && command -v docker >/dev/null 2>&1; then
    arch="$(docker info --format '{{.Architecture}}' 2>/dev/null || true)"
  fi

  # Normalize common values
  case "$arch" in
    arm64|aarch64) PLATFORM="linux/arm64" ;;
    amd64|x86_64)  PLATFORM="linux/amd64" ;;
    "")
      # Conservative fallback. For non-kind clusters, linux/amd64 is still the
      # most common; kind on Apple Silicon will generally be caught above.
      PLATFORM="linux/amd64"
      ;;
    *)
      # Unknown architecture; don't force a platform flag.
      log_warn "Unknown architecture '$arch'; building without explicit --platform."
      PLATFORM=""
      ;;
  esac

  if [ -n "${PLATFORM:-}" ]; then
    log_info "Auto-selected build platform: ${PLATFORM}"
  else
    log_info "No explicit build platform selected; relying on Docker default."
  fi
}

# Helper to get image name from config
get_image_name() {
  local service="$1"
  local name
  # Try to read from config file, with better error handling
  if [ ! -f "$IMAGE_REGISTRY_CONFIG" ]; then
    log_warn "Image registry config not found at $IMAGE_REGISTRY_CONFIG, using default naming"
    echo "logwise-${service}"
    return 0
  fi
  
  name=$(grep -A 10 "^  ${service}:" "$IMAGE_REGISTRY_CONFIG" 2>/dev/null | grep -m 1 "name:" | sed 's/.*name:[[:space:]]*//' | sed 's/^"//' | sed 's/"$//' | tr -d '"' || echo "")
  if [ -z "$name" ]; then
    # Fallback to default naming
    log_warn "Could not find image name for service '$service' in config, using default: logwise-${service}"
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

# Build a single image
build_image() {
  local service="$1"
  local image_tag="$2"
  local dockerfile_path="$3"
  local context_path="$4"
  
  log_info "Building $service image: $image_tag"
  
  # Build docker command - use conditional expansion for empty arrays
  # Use buildx for multi-platform support if platform is specified
  if [ -n "${PLATFORM:-}" ] && command -v docker buildx &> /dev/null; then
    log_info "Building for platform: $PLATFORM"
    if docker buildx build \
      --platform "$PLATFORM" \
      --tag "$image_tag" \
      --file "$dockerfile_path" \
      --progress=plain \
      --load \
      "$context_path"; then
      log_success "Built $service image: $image_tag"
      return 0
    else
      log_error "Failed to build $service image: $image_tag"
      return 1
    fi
  elif [ -n "${DOCKER_BUILDKIT:-}" ]; then
    local build_args=(
      --tag "$image_tag"
      --file "$dockerfile_path"
      --progress=plain
      --build-arg DOCKER_BUILDKIT=1
    )
    if [ -n "${PLATFORM:-}" ]; then
      build_args+=(--platform "$PLATFORM")
    fi
    build_args+=("$context_path")
    if docker build "${build_args[@]}"; then
      log_success "Built $service image: $image_tag"
      return 0
    else
      log_error "Failed to build $service image: $image_tag"
      return 1
    fi
  else
    local build_args=(
      --tag "$image_tag"
      --file "$dockerfile_path"
      --progress=plain
    )
    if [ -n "${PLATFORM:-}" ]; then
      build_args+=(--platform "$PLATFORM")
    fi
    build_args+=("$context_path")
    if docker build "${build_args[@]}"; then
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
  
  # Check if image exists locally before pushing
  if ! docker image inspect "$image_tag" >/dev/null 2>&1; then
    log_error "Image $image_tag does not exist locally. Cannot push."
    log_error "This usually means the build failed. Please check the build logs above."
    return 1
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
  resolve_default_platform
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
      local service_name="${services[$i]:-unknown}"
      if ! wait "$pid"; then
        log_error "Build failed for $service_name"
        failed=1
      else
        log_success "Build completed successfully for $service_name"
        # Verify the image actually exists
        IFS=':' read -r svc img_tag <<< "$service_name"
        if ! docker image inspect "$img_tag" >/dev/null 2>&1; then
          log_error "Build reported success but image $img_tag does not exist!"
          failed=1
        fi
      fi
      i=$((i + 1))
    done
    
    if [ $failed -eq 1 ]; then
      log_error "One or more builds failed. Cannot proceed with push."
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

