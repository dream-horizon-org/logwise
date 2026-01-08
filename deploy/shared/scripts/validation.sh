#!/usr/bin/env bash
# Configuration validation utilities for LogWise

set -euo pipefail

# Source common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

# Validate environment file
validate_env_file() {
  local env_file="$1"
  local errors=0
  
  if [ ! -f "$env_file" ]; then
    log_error "Environment file not found: $env_file"
    return 1
  fi
  
  log_info "Validating environment file: $env_file"
  
  # Check for required variables
  local required_vars=(
    "AWS_REGION"
    "AWS_ACCESS_KEY_ID"
    "AWS_SECRET_ACCESS_KEY"
    "S3_BUCKET_NAME"
    "S3_ATHENA_OUTPUT"
    "ATHENA_WORKGROUP"
    "ATHENA_DATABASE"
  )
  
  for var in "${required_vars[@]}"; do
    if ! grep -q "^${var}=" "$env_file" || grep -q "^${var}=$" "$env_file" || grep -q "^${var}=your-" "$env_file"; then
      log_error "Required variable $var is missing or has placeholder value"
      errors=$((errors + 1))
    fi
  done
  
  # Check for placeholder values
  if grep -q "your-access-key-here\|your-secret-key-here\|your-logwise-bucket\|your-bucket-name" "$env_file"; then
    log_warn "Found placeholder values in environment file"
    errors=$((errors + 1))
  fi
  
  if [ $errors -eq 0 ]; then
    log_success "Environment file validation passed"
    return 0
  else
    log_error "Environment file validation failed with $errors error(s)"
    return 1
  fi
}

# Validate Kubernetes manifests
validate_k8s_manifests() {
  local manifest_dir="$1"
  local errors=0
  
  if [ ! -d "$manifest_dir" ]; then
    log_error "Manifest directory not found: $manifest_dir"
    return 1
  fi
  
  log_info "Validating Kubernetes manifests in: $manifest_dir"
  
  # Check if kubeval is available
  if command_exists kubeval; then
    log_info "Using kubeval for validation"
    if ! kubeval "$manifest_dir"/*.yaml "$manifest_dir"/*/*.yaml 2>/dev/null; then
      log_error "kubeval validation failed"
      errors=$((errors + 1))
    fi
  elif command_exists kubectl; then
    log_info "Using kubectl --dry-run for validation"
    for file in "$manifest_dir"/*.yaml "$manifest_dir"/*/*.yaml; do
      if [ -f "$file" ]; then
        if ! kubectl apply --dry-run=client -f "$file" >/dev/null 2>&1; then
          log_error "Validation failed for: $file"
          errors=$((errors + 1))
        fi
      fi
    done
  else
    log_warn "Neither kubeval nor kubectl found, skipping manifest validation"
  fi
  
  if [ $errors -eq 0 ]; then
    log_success "Kubernetes manifest validation passed"
    return 0
  else
    log_error "Kubernetes manifest validation failed with $errors error(s)"
    return 1
  fi
}

# Validate Docker Compose file
validate_docker_compose() {
  local compose_file="$1"
  local errors=0
  
  if [ ! -f "$compose_file" ]; then
    log_error "Docker Compose file not found: $compose_file"
    return 1
  fi
  
  log_info "Validating Docker Compose file: $compose_file"
  
  if command_exists docker; then
    if docker compose -f "$compose_file" config >/dev/null 2>&1; then
      log_success "Docker Compose file validation passed"
      return 0
    else
      log_error "Docker Compose file validation failed"
      docker compose -f "$compose_file" config
      return 1
    fi
  else
    log_warn "Docker not found, skipping Docker Compose validation"
    return 0
  fi
}

# Validate image registry configuration
validate_registry_config() {
  local registry="${1:-}"
  local tag="${2:-latest}"
  
  if [ -z "$registry" ]; then
    log_warn "No registry specified, using local images"
    return 0
  fi
  
  log_info "Validating registry configuration: $registry"
  
  # Check registry format
  if [[ ! "$registry" =~ ^([a-z0-9.-]+\.)?[a-z0-9.-]+\.[a-z]{2,}(/.*)?$ ]] && \
     [[ ! "$registry" =~ ^[0-9]{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com(/.*)?$ ]]; then
    log_error "Invalid registry format: $registry"
    return 1
  fi
  
  log_success "Registry configuration validation passed"
  return 0
}

# Main validation function
validate_all() {
  local env_file="${1:-}"
  local compose_file="${2:-}"
  local k8s_manifest_dir="${3:-}"
  local errors=0
  
  if [ -n "$env_file" ]; then
    validate_env_file "$env_file" || errors=$((errors + 1))
  fi
  
  if [ -n "$compose_file" ]; then
    validate_docker_compose "$compose_file" || errors=$((errors + 1))
  fi
  
  if [ -n "$k8s_manifest_dir" ]; then
    validate_k8s_manifests "$k8s_manifest_dir" || errors=$((errors + 1))
  fi
  
  if [ $errors -eq 0 ]; then
    log_success "All validations passed"
    return 0
  else
    log_error "Validation failed with $errors error(s)"
    return 1
  fi
}

# Run validation if script is executed directly
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
  validate_all "$@"
fi


