#!/bin/bash

# Docker Desktop Uninstall Script for macOS
# This script removes Docker Desktop and all its associated files

echo "Starting Docker Desktop uninstallation..."

# Step 1: Force quit Docker Desktop and all related processes
echo "Step 1: Stopping Docker Desktop and all related processes..."
osascript -e 'quit app "Docker"' 2>/dev/null || echo "Attempting to quit Docker Desktop..."

# Wait a moment for Docker to quit gracefully
sleep 5

# Force kill any remaining Docker processes
echo "Checking for remaining Docker processes..."
if pgrep -f "com.docker" > /dev/null 2>&1; then
    echo "Force killing Docker processes..."
    killall -9 "Docker" 2>/dev/null || true
    killall -9 "com.docker.backend" 2>/dev/null || true
    killall -9 "com.docker.hyperkit" 2>/dev/null || true
    killall -9 "com.docker.vpnkit" 2>/dev/null || true
    sleep 3
fi

# Double check processes are gone
if pgrep -f "com.docker" > /dev/null 2>&1; then
    echo "⚠ Warning: Some Docker processes may still be running. You may need to restart your Mac."
else
    echo "✓ All Docker processes stopped"
fi

# Step 2: Remove Docker Desktop application
echo "Step 2: Removing Docker Desktop application..."
if [ -d "/Applications/Docker.app" ]; then
    sudo rm -rf "/Applications/Docker.app"
    echo "✓ Removed Docker.app"
else
    echo "⚠ Docker.app not found in /Applications"
fi

# Step 3: Remove Docker-related files and directories
echo "Step 3: Removing Docker-related files and directories..."

# Remove Docker data directories
DOCKER_DIRS=(
    "$HOME/Library/Containers/com.docker.docker"
    "$HOME/Library/Application Support/Docker Desktop"
    "$HOME/Library/Group Containers/group.com.docker"
    "$HOME/Library/Preferences/com.docker.docker.plist"
    "$HOME/Library/Caches/com.docker.docker"
    "$HOME/.docker"
    "$HOME/Library/Saved Application State/com.docker.docker.savedState"
)

for dir in "${DOCKER_DIRS[@]}"; do
    if [ -e "$dir" ]; then
        if rm -rf "$dir" 2>/dev/null; then
            echo "✓ Removed $dir"
        else
            echo "⚠ Could not remove $dir (may require Full Disk Access permission)"
            echo "  Try: sudo rm -rf \"$dir\""
        fi
    else
        echo "⚠ Not found: $dir"
    fi
done

# Step 4: Remove Docker CLI tools (if installed separately)
echo "Step 4: Checking for Docker CLI tools..."
if command -v docker &> /dev/null; then
    echo "⚠ Docker CLI is still available. It may be installed via Homebrew or another method."
    echo "  To remove it, run: brew uninstall docker docker-compose docker-credential-helper"
fi

# Step 5: Remove Docker from PATH (if added to shell config)
echo "Step 5: Checking shell configuration files..."
SHELL_CONFIGS=(
    "$HOME/.zshrc"
    "$HOME/.bash_profile"
    "$HOME/.bashrc"
    "$HOME/.profile"
)

for config in "${SHELL_CONFIGS[@]}"; do
    if [ -f "$config" ] && grep -q "docker" "$config" 2>/dev/null; then
        echo "⚠ Found Docker references in $config"
        echo "  You may want to manually review and remove Docker-related PATH entries"
    fi
done

echo ""
echo "✓ Docker Desktop uninstallation complete!"
echo ""
echo "Note: You may need to restart your terminal or log out/in for all changes to take effect."
echo "If you installed Docker CLI tools separately (e.g., via Homebrew), you'll need to remove them separately."

