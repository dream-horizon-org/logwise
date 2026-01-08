#!/usr/bin/env bash
# Common utility functions for LogWise deployment scripts

set -euo pipefail

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly BOLD='\033[1m'
readonly RESET='\033[0m'

# Logging functions
log_info() {
  echo -e "${CYAN}[INFO]${RESET} $*" >&2
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${RESET} $*" >&2
}

log_warn() {
  echo -e "${YELLOW}[WARN]${RESET} $*" >&2
}

log_error() {
  echo -e "${RED}[ERROR]${RESET} $*" >&2
}

log_debug() {
  if [ "${DEBUG:-false}" = "true" ]; then
    echo -e "${BLUE}[DEBUG]${RESET} $*" >&2
  fi
}

# Get script directory
get_script_dir() {
  cd "$(dirname "${BASH_SOURCE[0]}")" && pwd
}

# Get repository root directory
get_repo_root() {
  local script_dir
  script_dir="$(get_script_dir)"
  # Script is in deploy/shared/scripts, so go up 3 levels
  cd "$script_dir/../../.." && pwd
}

# Check if command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Require command or exit
require_command() {
  if ! command_exists "$1"; then
    log_error "$1 is required but not installed"
    exit 1
  fi
}

# Load YAML value (simple parser, works for key: value format)
yaml_get_value() {
  local file="$1"
  local key="$2"
  grep "^${key}:" "$file" 2>/dev/null | sed "s/^${key}:[[:space:]]*//" | sed 's/^"//' | sed 's/"$//' || echo ""
}

# Get default value from defaults.yaml
get_default() {
  local key="$1"
  local defaults_file
  defaults_file="$(get_repo_root)/deploy/shared/config/defaults.yaml"
  
  if [ -f "$defaults_file" ]; then
    yaml_get_value "$defaults_file" "$key"
  fi
}

# Check if running in CI environment
is_ci() {
  [ -n "${CI:-}" ] || [ -n "${GITHUB_ACTIONS:-}" ] || [ -n "${GITLAB_CI:-}" ] || [ -n "${JENKINS_URL:-}" ]
}

# Get current git SHA (short)
get_git_sha() {
  git rev-parse --short HEAD 2>/dev/null || echo "unknown"
}

# Get current git tag
get_git_tag() {
  git describe --tags --exact-match 2>/dev/null || echo ""
}

# Generate image tag based on strategy
generate_image_tag() {
  local strategy="${1:-semantic}"
  local version="${2:-1.0.0}"
  
  case "$strategy" in
    semantic)
      echo "$version"
      ;;
    git-sha)
      get_git_sha
      ;;
    git-tag)
      local tag
      tag="$(get_git_tag)"
      if [ -n "$tag" ]; then
        echo "$tag"
      else
        echo "latest"
      fi
      ;;
    timestamp)
      date +"%Y%m%d%H%M%S"
      ;;
    *)
      echo "latest"
      ;;
  esac
}

# Wait for condition with timeout
wait_for() {
  local condition="$1"
  local timeout="${2:-300}"
  local interval="${3:-5}"
  local elapsed=0
  
  while [ $elapsed -lt $timeout ]; do
    if eval "$condition"; then
      return 0
    fi
    sleep "$interval"
    elapsed=$((elapsed + interval))
  done
  
  return 1
}

# Retry command with exponential backoff
retry() {
  local max_attempts="${1:-3}"
  local delay="${2:-1}"
  shift 2
  local attempt=1
  
  while [ $attempt -le $max_attempts ]; do
    if "$@"; then
      return 0
    fi
    
    if [ $attempt -lt $max_attempts ]; then
      log_warn "Attempt $attempt failed, retrying in ${delay}s..."
      sleep "$delay"
      delay=$((delay * 2))  # Exponential backoff
    fi
    
    attempt=$((attempt + 1))
  done
  
  log_error "Command failed after $max_attempts attempts"
  return 1
}


