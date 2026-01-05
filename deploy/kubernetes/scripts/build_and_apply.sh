#!/usr/bin/env bash
# Backward compatibility wrapper for build_and_apply.sh
# This script now delegates to build-and-push.sh and deploy.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Preserve original behavior: build and apply in one command
ENV="${ENV:-local}"
CLUSTER_TYPE="${CLUSTER_TYPE:-docker-desktop}"
REGISTRY="${REGISTRY:-}"
TAG="${TAG:-1.0.0}"

# Build and push images
"$SCRIPT_DIR/build-and-push.sh" || exit 1

# Deploy to Kubernetes
"$SCRIPT_DIR/deploy.sh" || exit 1

echo "==> Done."


