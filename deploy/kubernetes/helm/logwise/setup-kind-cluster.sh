#!/bin/bash

# Setup script for creating a kind cluster and loading images for Logwise

set -e

CLUSTER_NAME="logwise"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== Logwise Kind Cluster Setup ===${NC}\n"

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

command -v kind >/dev/null 2>&1 || { echo -e "${RED}Error: kind is not installed. Install with: brew install kind${NC}" >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo -e "${RED}Error: docker is not installed${NC}" >&2; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo -e "${RED}Error: kubectl is not installed${NC}" >&2; exit 1; }

# Check if Docker is running
if ! docker ps >/dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker Desktop.${NC}" >&2
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites check passed${NC}\n"

# Check if cluster already exists
if kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
    echo -e "${YELLOW}Cluster '${CLUSTER_NAME}' already exists.${NC}"
    read -p "Do you want to delete and recreate it? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}Deleting existing cluster...${NC}"
        kind delete cluster --name "$CLUSTER_NAME"
        echo -e "${GREEN}✓ Cluster deleted${NC}\n"
    else
        echo -e "${YELLOW}Using existing cluster${NC}\n"
        USE_EXISTING=true
    fi
else
    USE_EXISTING=false
fi

# Create cluster if needed
if [ "$USE_EXISTING" != "true" ]; then
    echo -e "${YELLOW}Creating kind cluster '${CLUSTER_NAME}'...${NC}"
    
    # Check if kind-config.yaml exists
    KIND_CONFIG="$SCRIPT_DIR/kind-config.yaml"
    if [ -f "$KIND_CONFIG" ]; then
        echo -e "${GREEN}Using kind-config.yaml for port mappings (NodePort services will work directly)${NC}"
        kind create cluster --name "$CLUSTER_NAME" --config "$KIND_CONFIG"
    else
        echo -e "${YELLOW}kind-config.yaml not found, creating cluster without port mappings${NC}"
        echo -e "${YELLOW}Note: You'll need to use kubectl port-forward to access services${NC}"
        kind create cluster --name "$CLUSTER_NAME"
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Cluster created successfully${NC}\n"
    else
        echo -e "${RED}✗ Failed to create cluster${NC}"
        exit 1
    fi
fi

# Verify cluster is accessible
echo -e "${YELLOW}Verifying cluster connection...${NC}"
if kubectl cluster-info >/dev/null 2>&1; then
    echo -e "${GREEN}✓ Cluster is accessible${NC}\n"
else
    echo -e "${RED}✗ Cannot connect to cluster${NC}"
    exit 1
fi

# Build and load images
echo -e "${YELLOW}Building and loading Docker images...${NC}\n"

IMAGES=(
    "orchestrator/docker:logwise-orchestrator:latest"
    "spark/docker:logwise-spark:latest"
    "vector:logwise-vector:latest"
    "deploy/healthcheck-dummy:logwise-healthcheck-dummy:latest"
)

for image_spec in "${IMAGES[@]}"; do
    IFS=':' read -r build_path image_name <<< "$image_spec"
    
    echo -n "Building $image_name from $build_path... "
    
    # Check if image already exists
    if docker images "$image_name" --format "{{.Repository}}:{{.Tag}}" | grep -q "$image_name"; then
        echo -e "${YELLOW}exists${NC}"
        SKIP_BUILD=true
    else
        SKIP_BUILD=false
    fi
    
    # Build image if needed
    if [ "$SKIP_BUILD" != "true" ]; then
        if docker build -t "$image_name" "$PROJECT_ROOT/$build_path" >/dev/null 2>&1; then
            echo -e "${GREEN}built${NC}"
        else
            echo -e "${RED}failed${NC}"
            echo -e "${YELLOW}Warning: Failed to build $image_name. You may need to build it manually.${NC}"
            continue
        fi
    fi
    
    # Load image into kind
    echo -n "  Loading into kind... "
    if kind load docker-image "$image_name" --name "$CLUSTER_NAME" >/dev/null 2>&1; then
        echo -e "${GREEN}loaded${NC}"
    else
        echo -e "${RED}failed${NC}"
        echo -e "${YELLOW}Warning: Failed to load $image_name into kind${NC}"
    fi
done

echo -e "\n${GREEN}=== Setup Complete ===${NC}\n"

echo -e "${GREEN}Cluster Information:${NC}"
kubectl cluster-info
echo ""
kubectl get nodes
echo ""

echo -e "${GREEN}Next steps:${NC}"
echo -e "1. Install the Helm chart:"
echo -e "   ${YELLOW}cd $SCRIPT_DIR${NC}"
echo -e "   ${YELLOW}helm install logwise . --namespace logwise --create-namespace --values values-local.yaml${NC}"
echo ""
echo -e "2. Or use the test script (includes automatic port forwarding):"
echo -e "   ${YELLOW}./test-local.sh${NC}"
echo ""
echo -e "3. After installation, set up port forwarding (if not using test script):"
echo -e "   ${YELLOW}./post-install.sh${NC}"
echo -e "   ${YELLOW}or${NC}"
echo -e "   ${YELLOW}./access-services.sh${NC}"
echo ""
echo -e "4. To delete the cluster when done:"
echo -e "   ${YELLOW}kind delete cluster --name $CLUSTER_NAME${NC}"
echo ""
echo -e "${YELLOW}Note:${NC} If you want NodePort services to work directly (without port forwarding),"
echo -e "recreate the cluster using: ${GREEN}kind create cluster --name $CLUSTER_NAME --config kind-config.yaml${NC}"
