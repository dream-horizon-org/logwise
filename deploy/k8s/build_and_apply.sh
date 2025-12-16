#!/usr/bin/env bash
set -euo pipefail

# -------- CONFIGURE THESE (or override via env) --------
# Registry to push to (leave empty to skip push, e.g. for pure-local Docker Desktop)
REGISTRY="${REGISTRY:-}"     # e.g. "ghcr.io/your-org" or "123456789012.dkr.ecr.ap-south-1.amazonaws.com"
TAG="${TAG:-1.0.0}"          # e.g. "1.0.0" or a git SHA

# Target environment overlay: local | nonprod | prod
ENV="${ENV:-local}"

# Cluster type: docker-desktop | kind | other
CLUSTER_TYPE="${CLUSTER_TYPE:-docker-desktop}"
# -------------------------------------------------------

# Repo root is two levels up from this script: deploy/k8s/build_and_apply.sh
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "==> Building images with TAG=$TAG REGISTRY='${REGISTRY}' (env=${ENV}, cluster=${CLUSTER_TYPE})"

# Helper to prefix registry if set
img() {
  if [ -n "$REGISTRY" ]; then
    echo "$REGISTRY/$1:$TAG"
  else
    echo "$1:$TAG"
  fi
}

ORCH_IMAGE="$(img logwise-orchestrator)"
SPARK_IMAGE="$(img logwise-spark)"
HEALTH_IMAGE="$(img logwise-healthcheck-dummy)"
VECTOR_IMAGE="$(img logwise-vector)"

echo "Orchestrator image:        $ORCH_IMAGE"
echo "Spark image:               $SPARK_IMAGE"
echo "healthcheck-dummy image:   $HEALTH_IMAGE"
echo "Vector image:              $VECTOR_IMAGE"
echo

# Build Orchestrator
echo "==> Building Orchestrator image"
docker build -t "$ORCH_IMAGE" -f orchestrator/docker/Dockerfile orchestrator

# Build Spark
echo "==> Building Spark image"
docker build -t "$SPARK_IMAGE" -f spark/docker/Dockerfile spark

# Build healthcheck-dummy
echo "==> Building healthcheck-dummy image"
docker build -t "$HEALTH_IMAGE" deploy/healthcheck-dummy

# Build Vector (custom image with protobuf descriptor)
echo "==> Building Vector image"
docker build -t "$VECTOR_IMAGE" vector

# Optional push
if [ -n "$REGISTRY" ]; then
  echo "==> Pushing images to registry: $REGISTRY"
  docker push "$ORCH_IMAGE"
  docker push "$SPARK_IMAGE"
  docker push "$HEALTH_IMAGE"
  docker push "$VECTOR_IMAGE"
else
  echo "==> REGISTRY not set, skipping push (using local images only)"
fi

# Optional kind load
if [ "$CLUSTER_TYPE" = "kind" ]; then
  echo "==> Loading images into kind cluster"
  kind load docker-image "$ORCH_IMAGE"
  kind load docker-image "$SPARK_IMAGE"
  kind load docker-image "$HEALTH_IMAGE"
  kind load docker-image "$VECTOR_IMAGE"
fi

echo "==> Applying kustomize overlay: $ENV"
kubectl apply -k "deploy/k8s/$ENV"

echo "==> Done."


