#!/usr/bin/env bash
# Sync configuration between Docker and Kubernetes deployments

set -euo pipefail

# Source common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

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
  
  # Read .env file and convert to ConfigMap format
  while IFS='=' read -r key value || [ -n "$key" ]; do
    # Skip comments and empty lines
    [[ "$key" =~ ^#.*$ ]] && continue
    [[ -z "$key" ]] && continue
    
    # Remove quotes from value
    value="${value#\"}"
    value="${value%\"}"
    value="${value#\'}"
    value="${value%\'}"
    
    # Skip secret variables (they should be in Secrets, not ConfigMaps)
    if [[ "$key" =~ (PASSWORD|SECRET|TOKEN|KEY) ]]; then
      continue
    fi
    
    # Add to ConfigMap
    echo "  $key: \"$value\"" >> "$output_file"
  done < "$env_file"
  
  log_success "Generated ConfigMap at $output_file"
}

# Generate Kubernetes Secrets from .env file (for local dev only)
generate_k8s_secrets() {
  local env_file="$1"
  local output_file="$2"
  
  log_info "Generating Kubernetes Secrets from $env_file (base64 encoded)"
  log_warn "This is for local development only. Use proper secret management in production!"
  
  cat > "$output_file" <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: aws-credentials
  namespace: logwise
type: Opaque
data:
EOF
  
  # Extract secret variables
  while IFS='=' read -r key value || [ -n "$key" ]; do
    # Skip comments and empty lines
    [[ "$key" =~ ^#.*$ ]] && continue
    [[ -z "$key" ]] && continue
    
    # Only process secret variables
    if [[ "$key" =~ (AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|AWS_SESSION_TOKEN) ]]; then
      # Remove quotes
      value="${value#\"}"
      value="${value%\"}"
      value="${value#\'}"
      value="${value%\'}"
      
      # Base64 encode
      encoded_value=$(echo -n "$value" | base64)
      echo "  $key: $encoded_value" >> "$output_file"
    fi
  done < "$env_file"
  
  log_success "Generated Secrets at $output_file"
}

# Sync configuration
sync_config() {
  local env_file="${1:-$REPO_ROOT/deploy/docker/.env}"
  local k8s_config_dir="${2:-$REPO_ROOT/deploy/kubernetes/base}"
  
  if [ ! -f "$env_file" ]; then
    log_error "Environment file not found: $env_file"
    log_info "Create it from deploy/shared/templates/env.template"
    return 1
  fi
  
  log_info "Syncing configuration from $env_file to Kubernetes manifests"
  
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

