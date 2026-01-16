#!/usr/bin/env bash
# Common setup script for creating a kind cluster and loading images for LogWise
# Works for both Kustomize and Helm deployments

set -euo pipefail

# Configuration
CLUSTER_NAME="${KIND_CLUSTER_NAME:-logwise-local}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
KIND_CONFIG="${KIND_CONFIG:-$SCRIPT_DIR/kind-config-local.yaml}"
BUILD_IMAGES="${BUILD_IMAGES:-true}"
LOAD_IMAGES="${LOAD_IMAGES:-true}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_error() { echo -e "${RED}[✗]${NC} $*"; }

usage() {
  cat <<EOF
Usage: $0 [OPTIONS]

Setup a kind cluster for LogWise (works for both Kustomize and Helm deployments)

Options:
  -n, --name NAME          Cluster name (default: logwise-local)
  -c, --config PATH        Path to kind config file (default: scripts/kind-config-local.yaml)
  --no-build              Skip building images (only load existing ones)
  --no-load               Skip loading images into kind
  --skip-existing         Skip if cluster already exists (don't prompt)
  -h, --help              Show this help message

Environment Variables:
  KIND_CLUSTER_NAME       Cluster name (overrides -n)
  KIND_CONFIG             Path to kind config (overrides -c)
  BUILD_IMAGES            Set to 'false' to skip building (overrides --no-build)
  LOAD_IMAGES             Set to 'false' to skip loading (overrides --no-load)

Examples:
  # For Kustomize deployment (default)
  $0

  # For Helm deployment
  KIND_CLUSTER_NAME=logwise $0

  # Skip building, only load existing images
  $0 --no-build

  # Just create cluster, don't build/load images
  $0 --no-build --no-load
EOF
}

# Parse arguments
SKIP_EXISTING_PROMPT=false
while [[ $# -gt 0 ]]; do
  case $1 in
    -n|--name)
      CLUSTER_NAME="$2"
      shift 2
      ;;
    -c|--config)
      KIND_CONFIG="$2"
      shift 2
      ;;
    --no-build)
      BUILD_IMAGES=false
      shift
      ;;
    --no-load)
      LOAD_IMAGES=false
      shift
      ;;
    --skip-existing)
      SKIP_EXISTING_PROMPT=true
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

log_info "=== LogWise Kind Cluster Setup ==="
log_info "Cluster name: $CLUSTER_NAME"
log_info "Kind config: $KIND_CONFIG"
echo ""

# Check prerequisites
log_info "Checking prerequisites..."

command -v kind >/dev/null 2>&1 || { 
  log_error "kind is not installed. Install with: brew install kind (macOS) or see https://kind.sigs.k8s.io/docs/user/quick-start/"
  exit 1
}
command -v docker >/dev/null 2>&1 || { 
  log_error "docker is not installed"
  exit 1
}
command -v kubectl >/dev/null 2>&1 || { 
  log_error "kubectl is not installed"
  exit 1
}

# Check if Docker is running
if ! docker ps >/dev/null 2>&1; then
  log_error "Docker is not running. Please start Docker Desktop."
  exit 1
fi

log_success "Prerequisites check passed"
echo ""

# Check if cluster already exists
USE_EXISTING=false
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  if [ "$SKIP_EXISTING_PROMPT" = "true" ]; then
    log_info "Cluster '$CLUSTER_NAME' already exists, reusing it"
    USE_EXISTING=true
  else
    log_warn "Cluster '$CLUSTER_NAME' already exists."
    read -p "Do you want to delete and recreate it? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
      log_info "Deleting existing cluster..."
      kind delete cluster --name "$CLUSTER_NAME"
      log_success "Cluster deleted"
      echo ""
    else
      log_info "Using existing cluster"
      USE_EXISTING=true
    fi
  fi
fi

# Create cluster if needed
if [ "$USE_EXISTING" != "true" ]; then
  log_info "Creating kind cluster '$CLUSTER_NAME'..."
  
  # Check if kind config exists
  if [ -f "$KIND_CONFIG" ]; then
    log_info "Using $KIND_CONFIG for port mappings (NodePort services will work directly)"
    kind create cluster --name "$CLUSTER_NAME" --config "$KIND_CONFIG"
  else
    log_warn "Kind config not found at $KIND_CONFIG, creating cluster without port mappings"
    log_warn "Note: You'll need to use kubectl port-forward to access services"
    kind create cluster --name "$CLUSTER_NAME"
  fi
  
  if [ $? -eq 0 ]; then
    log_success "Cluster created successfully"
    echo ""
  else
    log_error "Failed to create cluster"
    exit 1
  fi
fi

# Verify cluster is accessible
log_info "Verifying cluster connection..."
if kubectl cluster-info >/dev/null 2>&1; then
  log_success "Cluster is accessible"
  echo ""
else
  log_error "Cannot connect to cluster"
  exit 1
fi

# Build and load images if requested
if [ "$BUILD_IMAGES" = "true" ] || [ "$LOAD_IMAGES" = "true" ]; then
  log_info "Building and loading Docker images..."
  echo ""
  
  # Image specifications: "build_path:image_name:tag"
  IMAGES=(
    "orchestrator/docker:logwise-orchestrator:latest"
    "spark/docker:logwise-spark:latest"
    "vector:logwise-vector:latest"
    "deploy/healthcheck-dummy:logwise-healthcheck-dummy:latest"
  )
  
  for image_spec in "${IMAGES[@]}"; do
    IFS=':' read -r build_path image_name image_tag <<< "$image_spec"
    full_image_name="${image_name}:${image_tag}"
    
    # Build image if requested
    if [ "$BUILD_IMAGES" = "true" ]; then
      echo -n "  Building $full_image_name from $build_path... "
      
      # Check if image already exists
      if docker images "$full_image_name" --format "{{.Repository}}:{{.Tag}}" 2>/dev/null | grep -q "$full_image_name"; then
        echo -e "${YELLOW}exists${NC}"
        SKIP_BUILD=true
      else
        SKIP_BUILD=false
      fi
      
      # Build image if needed
      if [ "$SKIP_BUILD" != "true" ]; then
        if docker build -t "$full_image_name" "$PROJECT_ROOT/$build_path" >/dev/null 2>&1; then
          echo -e "${GREEN}built${NC}"
        else
          echo -e "${RED}failed${NC}"
          log_warn "Failed to build $full_image_name. You may need to build it manually."
          continue
        fi
      fi
    fi
    
    # Load image into kind if requested
    if [ "$LOAD_IMAGES" = "true" ]; then
      echo -n "  Loading $full_image_name into kind... "
      if kind load docker-image "$full_image_name" --name "$CLUSTER_NAME" >/dev/null 2>&1; then
        echo -e "${GREEN}loaded${NC}"
      else
        echo -e "${RED}failed${NC}"
        log_warn "Failed to load $full_image_name into kind"
      fi
    fi
  done
  echo ""
fi

log_success "=== Setup Complete ==="
echo ""

log_info "Cluster Information:"
kubectl cluster-info
echo ""
kubectl get nodes
echo ""

log_info "Next steps:"
echo ""
echo "For Kustomize deployment:"
echo "  ${YELLOW}cd $PROJECT_ROOT${NC}"
echo "  ${YELLOW}ENV=local ./deploy/kubernetes/scripts/setup-k8s.sh local${NC}"
echo ""
echo "For Helm deployment:"
echo "  ${YELLOW}cd $PROJECT_ROOT/deploy/kubernetes/helm/logwise${NC}"
echo "  ${YELLOW}./quick-install.sh [AWS_ACCESS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT] [AWS_SESSION_TOKEN]${NC}"
echo ""
echo "To delete the cluster when done:"
echo "  ${YELLOW}kind delete cluster --name $CLUSTER_NAME${NC}"
echo ""
