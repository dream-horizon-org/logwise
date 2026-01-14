#!/bin/bash
# Wrapper script for Helm deployment - calls the common setup-kind-cluster.sh
# This ensures both Kustomize and Helm use the same cluster setup logic

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMON_SCRIPT="$SCRIPT_DIR/../../scripts/setup-kind-cluster.sh"

# Default cluster name for Helm deployments
CLUSTER_NAME="${KIND_CLUSTER_NAME:-logwise}"

# Use Helm's kind-config.yaml if it exists, otherwise use the common one
HELM_KIND_CONFIG="$SCRIPT_DIR/kind-config.yaml"
COMMON_KIND_CONFIG="$SCRIPT_DIR/../../scripts/kind-config-local.yaml"

if [ -f "$HELM_KIND_CONFIG" ]; then
  KIND_CONFIG="$HELM_KIND_CONFIG"
elif [ -f "$COMMON_KIND_CONFIG" ]; then
  KIND_CONFIG="$COMMON_KIND_CONFIG"
else
  KIND_CONFIG=""
fi

# Call the common script with Helm-specific defaults
KIND_CLUSTER_NAME="$CLUSTER_NAME" \
  KIND_CONFIG="$KIND_CONFIG" \
  "$COMMON_SCRIPT" "$@"
