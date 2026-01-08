#!/usr/bin/env bash
# CI script for building images
# This script is used in CI/CD pipelines to build all LogWise images

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Source common functions
SHARED_SCRIPTS_DIR="$REPO_ROOT/deploy/shared/scripts"
# shellcheck source=../../shared/scripts/common.sh
source "$SHARED_SCRIPTS_DIR/common.sh"

cd "$REPO_ROOT"

# Configuration from environment
REGISTRY="${REGISTRY:-}"
TAG="${TAG:-latest}"
PUSH="${PUSH:-true}"

log_info "Building images with REGISTRY='$REGISTRY' TAG='$TAG' PUSH='$PUSH'"

# Use the build-and-push script
export REGISTRY TAG
export PUSH_IMAGES="$PUSH"
export PARALLEL_BUILD=true

"$REPO_ROOT/deploy/kubernetes/scripts/build-and-push.sh"

log_success "All images built successfully"


