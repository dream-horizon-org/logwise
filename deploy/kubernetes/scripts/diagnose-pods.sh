#!/bin/bash

# Diagnostic script for pods stuck in Init or ContainerCreating state

set -e

NAMESPACE="${NAMESPACE:-logwise}"

echo "=========================================="
echo "LogWise Pod Diagnostic Script"
echo "=========================================="
echo ""

# Check if namespace exists
if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
  echo "❌ ERROR: Namespace '$NAMESPACE' does not exist!"
  exit 1
fi

echo "📋 Checking pods in namespace: $NAMESPACE"
echo ""

# Get all pods and their status
echo "=== Pod Status ==="
kubectl get pods -n "$NAMESPACE" -o wide
echo ""

# Check for pods in Init or ContainerCreating
STUCK_PODS=$(kubectl get pods -n "$NAMESPACE" -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.phase}{"\t"}{.status.containerStatuses[0].state.waiting.reason}{"\n"}{end}' | grep -E "(Init|ContainerCreating)" || true)

if [ -z "$STUCK_PODS" ]; then
  echo "✅ No pods stuck in Init or ContainerCreating state"
  exit 0
fi

echo "⚠️  Found pods in problematic states. Diagnosing..."
echo ""

# Check each stuck pod
for pod in $(kubectl get pods -n "$NAMESPACE" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}'); do
  phase=$(kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")
  
  if [[ "$phase" == "Pending" ]] || [[ "$phase" == "Init:0"* ]] || [[ "$phase" == "ContainerCreating" ]]; then
    echo "=========================================="
    echo "🔍 Diagnosing pod: $pod"
    echo "=========================================="
    
    # Get pod events
    echo ""
    echo "📝 Recent Events:"
    kubectl describe pod "$pod" -n "$NAMESPACE" | grep -A 20 "Events:" || echo "No events found"
    echo ""
    
    # Check init container status
    echo "🔧 Init Container Status:"
    kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{range .status.initContainerStatuses[*]}{.name}{": "}{.state.waiting.reason}{" - "}{.state.waiting.message}{"\n"}{end}' 2>/dev/null || echo "No init containers or status unavailable"
    echo ""
    
    # Check main container status
    echo "📦 Main Container Status:"
    kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{range .status.containerStatuses[*]}{.name}{": "}{.state.waiting.reason}{" - "}{.state.waiting.message}{"\n"}{end}' 2>/dev/null || echo "No container status available"
    echo ""
    
    # Check image pull secrets
    echo "🔐 Image Pull Secrets:"
    kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.spec.imagePullSecrets[*].name}{"\n"}' 2>/dev/null || echo "None configured"
    echo ""
    
    # Check images being used
    echo "🖼️  Images:"
    echo "Init Containers:"
    kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{range .spec.initContainers[*]}{.name}{": "}{.image}{" (pullPolicy: "}{.imagePullPolicy}{")\n"}{end}' 2>/dev/null || echo "No init containers"
    echo "Main Containers:"
    kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{range .spec.containers[*]}{.name}{": "}{.image}{" (pullPolicy: "}{.imagePullPolicy}{")\n"}{end}' 2>/dev/null || echo "No containers"
    echo ""
  fi
done

echo ""
echo "=========================================="
echo "🔍 Common Issues & Solutions"
echo "=========================================="
echo ""

# Check for dockerhub-secret
echo "1. Checking for dockerhub-secret..."
if kubectl get secret dockerhub-secret -n "$NAMESPACE" >/dev/null 2>&1; then
  echo "   ✅ dockerhub-secret exists"
else
  echo "   ❌ dockerhub-secret is MISSING!"
  echo "   💡 Solution: Create it with:"
  echo "      kubectl create secret docker-registry dockerhub-secret \\"
  echo "        --docker-server=https://index.docker.io/v1/ \\"
  echo "        --docker-username=YOUR_USERNAME \\"
  echo "        --docker-password=YOUR_PASSWORD \\"
  echo "        -n $NAMESPACE"
fi
echo ""

# Check for required secrets
echo "2. Checking for required secrets..."
REQUIRED_SECRETS=("aws-credentials" "orch-db-secret" "grafana-db-secret")
for secret in "${REQUIRED_SECRETS[@]}"; do
  if kubectl get secret "$secret" -n "$NAMESPACE" >/dev/null 2>&1; then
    echo "   ✅ $secret exists"
  else
    echo "   ❌ $secret is MISSING!"
  fi
done
echo ""

# Check for configmap
echo "3. Checking for logwise-config ConfigMap..."
if kubectl get configmap logwise-config -n "$NAMESPACE" >/dev/null 2>&1; then
  echo "   ✅ logwise-config exists"
else
  echo "   ❌ logwise-config is MISSING!"
  echo "   💡 Solution: Run sync-config.sh to create it"
fi
echo ""

# Check node resources
echo "4. Checking node resources..."
kubectl top nodes 2>/dev/null || echo "   ⚠️  Metrics server not available (this is OK for some clusters)"
echo ""

# Check for image pull errors
echo "5. Common Image Pull Issues:"
echo "   - If using 'local' environment with kind: Images must be loaded into kind"
echo "   - If using 'nonprod/prod': Images must be pushed to registry"
echo "   - Check image names match what's in registry"
echo ""

# Check init container dependencies
echo "6. Init Container Dependencies:"
echo "   - Vector waits for: Kafka"
echo "   - Orchestrator waits for: MySQL, Kafka, Vector, Spark Master"
echo "   - Healthcheck waits for: Vector, Kafka"
echo "   - Make sure base services (Kafka, MySQL) start first"
echo ""

echo "=========================================="
echo "💡 Quick Fixes"
echo "=========================================="
echo ""
echo "If images can't be pulled:"
echo "  For local/kind:"
echo "    kind load docker-image <image-name> --name <cluster-name>"
echo ""
echo "  For nonprod/prod:"
echo "    Ensure images are built and pushed to registry"
echo "    Check REGISTRY and TAG environment variables"
echo ""
echo "If init containers are stuck:"
echo "  Check if dependencies are ready:"
echo "    kubectl get pods -n $NAMESPACE | grep -E '(kafka|mysql|vector|spark)'"
echo ""
echo "If secrets are missing:"
echo "  Run: ./scripts/sync-config.sh .env"
echo "  Or create secrets manually"
echo ""

