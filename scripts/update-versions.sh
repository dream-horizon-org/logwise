#!/bin/bash
# Hybrid approach: Use Maven Versions Plugin (standard) for POMs, script for docs
# Usage: ./update-versions.sh <version> [--snapshot]
# Example: ./update-versions.sh 0.0.1 (for release)
# Example: ./update-versions.sh 0.0.1 --snapshot (for snapshot)

set -e

VERSION=$1
SNAPSHOT_FLAG=$2

if [ -z "$VERSION" ]; then
    echo "Error: Version is required"
    echo "Usage: $0 <version> [--snapshot]"
    exit 1
fi

# Determine if this is a snapshot version
if [ "$SNAPSHOT_FLAG" == "--snapshot" ]; then
    FULL_VERSION="${VERSION}-SNAPSHOT"
else
    FULL_VERSION="${VERSION}"
fi

echo "Updating versions to: $FULL_VERSION"

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Update orchestrator/pom.xml using Maven Versions Plugin (standard approach)
ORCHESTRATOR_DIR="$PROJECT_ROOT/orchestrator"
if [ -f "$ORCHESTRATOR_DIR/pom.xml" ]; then
    echo "Updating orchestrator/pom.xml using Maven Versions Plugin..."
    cd "$ORCHESTRATOR_DIR"
    mvn -q versions:set -DnewVersion="$FULL_VERSION" -DgenerateBackupPoms=false
    echo "✓ Updated orchestrator/pom.xml to $FULL_VERSION"
    cd "$PROJECT_ROOT"
else
    echo "Warning: orchestrator/pom.xml not found"
fi

# Update spark/pom.xml using Maven Versions Plugin (standard approach)
SPARK_DIR="$PROJECT_ROOT/spark"
if [ -f "$SPARK_DIR/pom.xml" ]; then
    echo "Updating spark/pom.xml using Maven Versions Plugin..."
    cd "$SPARK_DIR"
    mvn -q versions:set -DnewVersion="$FULL_VERSION" -DgenerateBackupPoms=false
    echo "✓ Updated spark/pom.xml to $FULL_VERSION"
    cd "$PROJECT_ROOT"
else
    echo "Warning: spark/pom.xml not found"
fi

# Update docs/VERSION
VERSION_FILE="$PROJECT_ROOT/docs/VERSION"
if [ -f "$VERSION_FILE" ]; then
    echo "Updating docs/VERSION..."
    echo "$FULL_VERSION" > "$VERSION_FILE"
    echo "✓ Updated docs/VERSION"
else
    echo "Warning: docs/VERSION not found"
fi

# Update docs/package.json (optional, for reference)
PACKAGE_JSON="$PROJECT_ROOT/docs/package.json"
if [ -f "$PACKAGE_JSON" ]; then
    echo "Updating docs/package.json..."
    if command -v jq &> /dev/null; then
        # Use jq if available
        jq ".version = \"$VERSION\"" "$PACKAGE_JSON" > "${PACKAGE_JSON}.tmp" && mv "${PACKAGE_JSON}.tmp" "$PACKAGE_JSON"
    else
        # Fallback to sed
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s/\"version\": \".*\"/\"version\": \"$VERSION\"/" "$PACKAGE_JSON"
        else
            sed -i "s/\"version\": \".*\"/\"version\": \"$VERSION\"/" "$PACKAGE_JSON"
        fi
    fi
    echo "✓ Updated docs/package.json"
else
    echo "Warning: docs/package.json not found"
fi

echo ""
echo "All versions updated to: $FULL_VERSION"

