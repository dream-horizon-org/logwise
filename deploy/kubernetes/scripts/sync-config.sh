#!/usr/bin/env bash
# Sync Kubernetes configuration from .env file to ConfigMaps and Secrets
# This script is Kubernetes-specific and should only be used for Kubernetes deployments

set -euo pipefail

# Source common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../../shared/scripts/common.sh
source "$SCRIPT_DIR/../../shared/scripts/common.sh"

# Get repository root
REPO_ROOT="$(get_repo_root)"

# Generate Kubernetes ConfigMap from .env file
generate_k8s_configmap() {
  local env_file="$1"
  local output_file="$2"
  
  log_info "Generating Kubernetes ConfigMap from $env_file"
  
  cat > "$output_file" <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: logwise-config
  namespace: logwise
data:
EOF
  
  # Read .env file and convert to ConfigMap format with better parsing
  while IFS= read -r line || [ -n "$line" ]; do
    # Skip comments and empty lines
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "${line// }" ]] && continue
    
    # Parse key=value, handling quoted values and spaces
    if [[ "$line" =~ ^[[:space:]]*([^=]+)=(.*)$ ]]; then
      key="${BASH_REMATCH[1]}"
      value="${BASH_REMATCH[2]}"
      
      # Trim whitespace from key
      key="${key#"${key%%[![:space:]]*}"}"
      key="${key%"${key##*[![:space:]]}"}"
      
      # Trim whitespace from value
      value="${value#"${value%%[![:space:]]*}"}"
      value="${value%"${value##*[![:space:]]}"}"
      
      # Remove surrounding quotes (handles both single and double quotes)
      if [[ "$value" =~ ^\".*\"$ ]]; then
        value="${value#\"}"
        value="${value%\"}"
      elif [[ "$value" =~ ^\'.*\'$ ]]; then
        value="${value#\'}"
        value="${value%\'}"
      fi
      
      # Skip secret variables (they should be in Secrets, not ConfigMaps)
      if [[ "$key" =~ (PASSWORD|SECRET|TOKEN|KEY) ]]; then
        continue
      fi
      
      # Add to ConfigMap
      echo "  $key: \"$value\"" >> "$output_file"
    fi
  done < "$env_file"
  
  log_success "Generated ConfigMap at $output_file"
}

# Generate Kubernetes Secrets from .env file (for local dev only)
generate_k8s_secrets() {
  local env_file="$1"
  local output_file="$2"
  
  log_info "Generating Kubernetes Secrets from $env_file"
  log_warn "This is for local development only. Use proper secret management in production!"
  
  cat > "$output_file" <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: aws-credentials
  namespace: logwise
type: Opaque
stringData:
EOF
  
  # Extract secret variables with better parsing
  while IFS= read -r line || [ -n "$line" ]; do
    # Skip comments and empty lines
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "${line// }" ]] && continue
    
    # Parse key=value, handling quoted values and spaces
    if [[ "$line" =~ ^[[:space:]]*([^=]+)=(.*)$ ]]; then
      key="${BASH_REMATCH[1]}"
      value="${BASH_REMATCH[2]}"
      
      # Trim whitespace from key
      key="${key#"${key%%[![:space:]]*}"}"
      key="${key%"${key##*[![:space:]]}"}"
      
      # Trim whitespace from value
      value="${value#"${value%%[![:space:]]*}"}"
      value="${value%"${value##*[![:space:]]}"}"
      
      # Remove surrounding quotes (handles both single and double quotes)
      if [[ "$value" =~ ^\".*\"$ ]]; then
        value="${value#\"}"
        value="${value%\"}"
      elif [[ "$value" =~ ^\'.*\'$ ]]; then
        value="${value#\'}"
        value="${value%\'}"
      fi
      
      # Only process AWS credential variables
      if [[ "$key" =~ ^(AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|AWS_SESSION_TOKEN)$ ]]; then
        # Skip if value is empty (e.g., optional AWS_SESSION_TOKEN)
        if [[ -z "$value" ]]; then
          continue
        fi
        
        # Write plain text value directly (Kubernetes will handle encoding)
        echo "  $key: \"$value\"" >> "$output_file"
      fi
    fi
  done < "$env_file"
  
  log_success "Generated Secrets at $output_file"
}

# Sync configuration from Kubernetes .env file to Kubernetes manifests
sync_config() {
  local env_file="${1:-}"
  local k8s_config_dir="${2:-$REPO_ROOT/deploy/kubernetes/base}"
  
  # Require explicit env file path
  if [ -z "$env_file" ]; then
    log_error "Environment file path is required"
    log_info "Usage: $0 <path-to-kubernetes-.env-file> [k8s-config-dir]"
    log_info "Example: $0 deploy/kubernetes/.env"
    log_info "Create it from deploy/shared/templates/env.template"
    return 1
  fi
  
  if [ ! -f "$env_file" ]; then
    log_error "Environment file not found: $env_file"
    log_info "Create it from deploy/shared/templates/env.template"
    return 1
  fi
  
  log_info "Syncing Kubernetes configuration from $env_file to Kubernetes manifests"
  
  # Generate ConfigMap
  generate_k8s_configmap "$env_file" "$k8s_config_dir/configmap-logwise-config.yaml"
  
  # Generate Secrets (for local dev)
  generate_k8s_secrets "$env_file" "$k8s_config_dir/secret-aws.yaml"
  
  log_success "Configuration sync completed"
  log_warn "Review generated files before committing. Secrets should be managed properly in production!"
}

# Main
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
  sync_config "$@"
fi

