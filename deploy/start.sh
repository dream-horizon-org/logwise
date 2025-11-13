#!/usr/bin/env bash
set -euo pipefail

bold() { printf "\033[1m%s\033[0m\n" "$*"; }
note() { printf "[+] %s\n" "$*"; }
warn() { printf "[!] %s\n" "$*"; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1
}

install_mac() {
  if ! need_cmd brew; then
    warn "Homebrew not found. Install from https://brew.sh and re-run."
    exit 1
  fi
  note "Installing prerequisites via Homebrew..."
  brew update || true
  brew install make curl jq wget awscli || true
  brew install --cask docker || true
  note "If Docker Desktop was just installed, open it once to finish setup."
}

install_linux_apt() {
  note "Installing prerequisites via apt (sudo required)..."
  sudo apt-get update -y
  sudo apt-get install -y ca-certificates curl gnupg lsb-release make jq wget awscli

  if ! need_cmd docker; then
    # Docker Engine install
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/$(. /etc/os-release && echo "$ID")/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/$(. /etc/os-release && echo "$ID") \
      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo usermod -aG docker "$USER" || true
    note "Docker installed. You may need to log out/in for group changes to take effect."
  fi
}

ensure_compose() {
  if docker compose version >/dev/null 2>&1; then
    return 0
  fi
  warn "Docker Compose v2 plugin not found. Installing buildx/compose plugins may be required."
}

main() {
  bold "Bootstrapping prerequisites for Log Central..."
  OS=$(uname -s)
  case "$OS" in
    Darwin) install_mac ;;
    Linux)
      if [ -f /etc/debian_version ] || [ -f /etc/lsb-release ]; then
        install_linux_apt
      else
        warn "Unsupported Linux distro for auto-install. Please install Docker, Docker Compose v2, Make, curl, jq, wget, and AWS CLI manually."
      fi
      ;;
    *)
      warn "Unsupported OS: $OS. Please install Docker, Docker Compose v2, Make, curl, jq, wget, and AWS CLI manually."
      ;;
  esac

  ensure_compose || true

  note "Checking versions:"
  (docker --version || true)
  (docker compose version || true)
  (make --version | head -n1 || true)
  (aws --version || true)
  (curl --version | head -n1 || true)
  (jq --version || true)
  (wget --version | head -n1 || true)

  chmod +x ./spark/submit.sh || true

  bold "Done. Next steps:"
  echo "1) Copy .env.example to .env and fill values (AWS creds, etc.)"
}

main "$@"


