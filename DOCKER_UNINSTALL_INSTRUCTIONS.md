# Docker Desktop Uninstall Instructions for macOS

## Current Status
Docker processes have been stopped. However, some files are protected by macOS security features and require manual steps.

## Step-by-Step Uninstallation

### Step 1: Remove Docker Desktop Application
Run this command (you'll be prompted for your password):
```bash
sudo rm -rf /Applications/Docker.app
```

**OR** manually drag `Docker.app` from `/Applications` to Trash and empty it.

### Step 2: Remove Docker Data Files
The following files/directories are protected by macOS. You have two options:

#### Option A: Grant Full Disk Access (Recommended)
1. Open **System Settings** (or System Preferences on older macOS)
2. Go to **Privacy & Security** → **Full Disk Access**
3. Add **Terminal** (or iTerm, or whatever terminal you're using)
4. Restart your terminal
5. Run the updated script again:
   ```bash
   ./uninstall-docker-desktop.sh
   ```

#### Option B: Manual Removal via Finder
1. Open Finder
2. Press `Cmd + Shift + G` to open "Go to Folder"
3. Navigate to and delete these folders manually:
   - `~/Library/Containers/com.docker.docker`
   - `~/Library/Application Support/Docker Desktop`
   - `~/Library/Group Containers/group.com.docker`
   - `~/Library/Caches/com.docker.docker`
   - `~/Library/Saved Application State/com.docker.docker.savedState`
4. Delete these files:
   - `~/Library/Preferences/com.docker.docker.plist`
   - `~/.docker` (if it exists)

### Step 3: Remove Docker CLI (if installed separately)
If you installed Docker CLI via Homebrew:
```bash
brew uninstall docker docker-compose docker-credential-helper
```

### Step 4: Clean Up Shell Configuration
Check your shell config files for Docker-related entries:
```bash
grep -i docker ~/.zshrc ~/.bash_profile ~/.bashrc ~/.profile 2>/dev/null
```

Remove any Docker-related PATH entries if found.

### Step 5: Restart
Restart your Mac or at least log out and log back in to ensure all Docker processes are completely stopped.

## Quick Command Summary

After granting Full Disk Access to Terminal, run:
```bash
# Remove Docker.app
sudo rm -rf /Applications/Docker.app

# Remove all Docker data
rm -rf ~/Library/Containers/com.docker.docker
rm -rf ~/Library/Application\ Support/Docker\ Desktop
rm -rf ~/Library/Group\ Containers/group.com.docker
rm -rf ~/Library/Caches/com.docker.docker
rm -rf ~/Library/Saved\ Application\ State/com.docker.docker.savedState
rm -f ~/Library/Preferences/com.docker.docker.plist
rm -rf ~/.docker
```

## Verification
After uninstallation, verify Docker is removed:
```bash
which docker  # Should return nothing if Docker CLI is removed
docker --version  # Should fail if Docker is removed
```

