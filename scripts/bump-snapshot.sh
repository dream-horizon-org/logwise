#!/bin/bash
# Script to bump version to next snapshot
# Usage: ./bump-snapshot.sh <current-version>
# Example: ./bump-snapshot.sh 0.0.1 (will become 0.0.2-SNAPSHOT)

set -e

CURRENT_VERSION=$1

if [ -z "$CURRENT_VERSION" ]; then
    echo "Error: Current version is required"
    echo "Usage: $0 <current-version>"
    exit 1
fi

# Remove -SNAPSHOT if present
CURRENT_VERSION=${CURRENT_VERSION%-SNAPSHOT}

# Parse version parts
IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
MAJOR=${VERSION_PARTS[0]:-0}
MINOR=${VERSION_PARTS[1]:-0}
PATCH=${VERSION_PARTS[2]:-0}

# Increment patch version
PATCH=$((PATCH + 1))

# Construct next version
NEXT_VERSION="${MAJOR}.${MINOR}.${PATCH}-SNAPSHOT"

echo "Bumping version from $CURRENT_VERSION to $NEXT_VERSION"

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Call update-versions.sh with snapshot flag
"$SCRIPT_DIR/update-versions.sh" "${MAJOR}.${MINOR}.${PATCH}" --snapshot

echo ""
echo "Version bumped to: $NEXT_VERSION"

