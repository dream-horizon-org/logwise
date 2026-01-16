#!/bin/bash

# Post-install script for Logwise Helm chart
# This script sets up port forwarding automatically after Helm installation
# Can be run manually or integrated into CI/CD pipelines

set -e

NAMESPACE="${HELM_NAMESPACE:-logwise}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== Logwise Post-Install: Port Forwarding Setup ===${NC}\n"

# Check if namespace exists
if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
    echo -e "${RED}Error: Namespace '$NAMESPACE' does not exist${NC}"
    exit 1
fi

# Check if services exist
if ! kubectl get svc -n "$NAMESPACE" >/dev/null 2>&1; then
    echo -e "${YELLOW}Warning: No services found in namespace '$NAMESPACE'${NC}"
    echo -e "${YELLOW}Port forwarding will be skipped${NC}"
    exit 0
fi

# Function to check if port is in use
check_port() {
    lsof -i :$1 >/dev/null 2>&1
}

# Function to start port forward
start_port_forward() {
    local service=$1
    local local_port=$2
    local service_port=$3
    local pid_file="/tmp/logwise-pf-${service}.pid"
    
    # Check if service exists
    if ! kubectl get svc "$service" -n "$NAMESPACE" >/dev/null 2>&1; then
        echo -e "${YELLOW}Service '$service' not found, skipping${NC}"
        return
    fi
    
    # Check if port forward already running
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p "$pid" >/dev/null 2>&1; then
            echo -e "${YELLOW}Port forward for $service already running (PID: $pid)${NC}"
            return
        fi
    fi
    
    # Check if port is in use
    if check_port $local_port; then
        echo -e "${YELLOW}Port $local_port is already in use, skipping $service${NC}"
        return
    fi
    
    # Start port forward
    echo -e "${GREEN}Starting port forward: $service (localhost:$local_port -> $service:$service_port)${NC}"
    kubectl port-forward -n "$NAMESPACE" svc/$service $local_port:$service_port >/dev/null 2>&1 &
    local pid=$!
    echo $pid > "$pid_file"
    echo -e "${GREEN}✓ Port forward started (PID: $pid)${NC}"
}

# Clean up old port forwards
echo -e "${YELLOW}Cleaning up any existing port forwards...${NC}"
for pid_file in /tmp/logwise-pf-*.pid; do
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        if ps -p "$pid" >/dev/null 2>&1; then
            kill $pid 2>/dev/null || true
        fi
        rm -f "$pid_file"
    fi
done

# Start port forwards
echo -e "\n${YELLOW}Setting up port forwarding...${NC}\n"

start_port_forward "orchestrator" 30081 8080
start_port_forward "grafana" 30080 3000
start_port_forward "spark-master" 30082 8080

# Try spark-worker-ui first, fallback to spark-worker
if kubectl get svc "spark-worker-ui" -n "$NAMESPACE" >/dev/null 2>&1; then
    start_port_forward "spark-worker-ui" 30083 8081
else
    start_port_forward "spark-worker" 30083 8081
fi

start_port_forward "vector-logs" 30418 4318

# Wait a moment for port forwards to establish
sleep 2

echo -e "\n${GREEN}=== Port Forwarding Complete ===${NC}\n"
echo -e "${GREEN}Services are now accessible:${NC}"
echo -e "  - Orchestrator: ${GREEN}http://localhost:30081${NC}"
echo -e "  - Grafana: ${GREEN}http://localhost:30080${NC} (admin/admin)"
echo -e "  - Spark Master: ${GREEN}http://localhost:30082${NC}"
echo -e "  - Spark Worker UI: ${GREEN}http://localhost:30083${NC}"
echo -e "  - Vector OTLP: ${GREEN}http://localhost:30418${NC}"

echo -e "\n${YELLOW}To stop port forwarding:${NC}"
echo -e "  ${GREEN}pkill -f 'kubectl port-forward'${NC}"
echo -e "  ${GREEN}rm -f /tmp/logwise-pf-*.pid${NC}"

echo -e "\n${YELLOW}Port forward PIDs saved to /tmp/logwise-pf-*.pid${NC}"
