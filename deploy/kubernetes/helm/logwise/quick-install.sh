#!/bin/bash

# Quick install/upgrade script that handles namespace and release existence

set -e

NAMESPACE="logwise"
RELEASE_NAME="logwise"

# Check if namespace exists and has Helm metadata
NAMESPACE_EXISTS=false
HAS_HELM_METADATA=false

if kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
    NAMESPACE_EXISTS=true
    # Check if namespace has Helm metadata
    if kubectl get namespace "$NAMESPACE" -o jsonpath='{.metadata.labels.app\.kubernetes\.io/managed-by}' 2>/dev/null | grep -q "Helm"; then
        HAS_HELM_METADATA=true
        echo "✓ Namespace '$NAMESPACE' exists and is managed by Helm"
    else
        echo "⚠ Namespace '$NAMESPACE' exists but is not managed by Helm"
        # Check if namespace is empty (no resources except maybe default service account)
        RESOURCE_COUNT=$(kubectl get all -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l | tr -d ' ')
        if [ "$RESOURCE_COUNT" -le 1 ]; then
            echo "Namespace appears empty, deleting to let Helm manage it..."
            kubectl delete namespace "$NAMESPACE" --wait=true
            # Wait for namespace to be fully deleted (with timeout)
            echo "Waiting for namespace deletion to complete..."
            MAX_WAIT=60
            WAIT_COUNT=0
            while kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; do
                if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
                    echo "Error: Namespace deletion timed out after ${MAX_WAIT}s"
                    echo "Please delete it manually: kubectl delete namespace $NAMESPACE --force --grace-period=0"
                    exit 1
                fi
                sleep 1
                WAIT_COUNT=$((WAIT_COUNT + 1))
            done
            echo "✓ Namespace deleted successfully"
            NAMESPACE_EXISTS=false
        else
            echo "Error: Namespace '$NAMESPACE' contains resources and is not Helm-managed."
            echo "Please either:"
            echo "  1. Delete the namespace manually: kubectl delete namespace $NAMESPACE"
            echo "  2. Or add Helm metadata to the namespace"
            exit 1
        fi
    fi
fi

# Check if release exists
if helm list -n "$NAMESPACE" 2>/dev/null | grep -q "^${RELEASE_NAME}"; then
    ACTION="upgrade"
    echo "Existing release found, upgrading..."
else
    ACTION="install"
    echo "No existing release found, installing..."
fi

# Ensure namespace exists before Helm install
# Helm requires namespace to exist before it can install resources
if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
    echo "Creating namespace '$NAMESPACE'..."
    kubectl create namespace "$NAMESPACE"
    
    # Add Helm labels and annotations
    kubectl label namespace "$NAMESPACE" \
        app.kubernetes.io/managed-by=Helm \
        app.kubernetes.io/name="$RELEASE_NAME" \
        app.kubernetes.io/instance="$RELEASE_NAME" \
        --overwrite
    
    kubectl annotate namespace "$NAMESPACE" \
        meta.helm.sh/release-name="$RELEASE_NAME" \
        meta.helm.sh/release-namespace="$NAMESPACE" \
        --overwrite
    
    echo "✓ Namespace '$NAMESPACE' created with Helm metadata"
else
    NAMESPACE_PHASE=$(kubectl get namespace "$NAMESPACE" -o jsonpath='{.status.phase}' 2>/dev/null || echo "Active")
    if [ "$NAMESPACE_PHASE" = "Terminating" ]; then
        echo "⚠ Namespace '$NAMESPACE' is in Terminating state, waiting for deletion..."
        MAX_WAIT=60
        WAIT_COUNT=0
        while kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; do
            if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
                echo "Error: Namespace termination timed out"
                exit 1
            fi
            sleep 1
            WAIT_COUNT=$((WAIT_COUNT + 1))
        done
        # Recreate namespace after termination
        kubectl create namespace "$NAMESPACE"
        kubectl label namespace "$NAMESPACE" \
            app.kubernetes.io/managed-by=Helm \
            app.kubernetes.io/name="$RELEASE_NAME" \
            app.kubernetes.io/instance="$RELEASE_NAME" \
            --overwrite
        kubectl annotate namespace "$NAMESPACE" \
            meta.helm.sh/release-name="$RELEASE_NAME" \
            meta.helm.sh/release-namespace="$NAMESPACE" \
            --overwrite
        echo "✓ Namespace '$NAMESPACE' recreated after termination"
    else
        echo "✓ Namespace '$NAMESPACE' exists (phase: $NAMESPACE_PHASE)"
    fi
fi

# Build command (namespace always exists at this point, so no --create-namespace needed)
HELM_CMD="helm $ACTION $RELEASE_NAME ."
HELM_CMD="$HELM_CMD --namespace $NAMESPACE"

HELM_CMD="$HELM_CMD --values values-local.yaml"

# Add AWS settings if provided
if [ -n "$1" ] && [ "$1" != "YOUR_KEY" ]; then
    HELM_CMD="$HELM_CMD --set aws.accessKeyId=$1"
fi
if [ -n "$2" ] && [ "$2" != "YOUR_SECRET" ]; then
    HELM_CMD="$HELM_CMD --set aws.secretAccessKey=$2"
fi
if [ -n "$3" ] && [ "$3" != "logwise-varun-test" ]; then
    HELM_CMD="$HELM_CMD --set aws.s3BucketName=$3"
fi
if [ -n "$4" ]; then
    HELM_CMD="$HELM_CMD --set aws.s3AthenaOutput=$4"
fi

echo "Running: $HELM_CMD"
eval $HELM_CMD
