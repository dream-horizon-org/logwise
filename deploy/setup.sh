#!/usr/bin/env bash
set -euo pipefail

# Color and formatting functions
reset="\033[0m"
bold="\033[1m"
dim="\033[2m"
red="\033[31m"
green="\033[32m"
yellow="\033[33m"
blue="\033[34m"
magenta="\033[35m"
cyan="\033[36m"
white="\033[37m"

# Output functions
print_bold() { printf "${bold}%s${reset}\n" "$*"; }
print_success() { printf "${green}✓${reset} %s\n" "$*"; }
print_error() { printf "${red}✗${reset} %s\n" "$*"; }
print_warn() { printf "${yellow}⚠${reset}  %s\n" "$*"; }
print_info() { printf "${cyan}ℹ${reset}  %s\n" "$*"; }
print_step() { printf "\n${bold}${blue}▶${reset} ${bold}%s${reset}\n" "$*"; }
print_substep() { printf "${dim}  └─${reset} %s\n" "$*"; }

# Visual separators
print_separator() {
  printf "${dim}%s${reset}\n" "$(printf '─%.0s' {1..60})"
}

print_header() {
  local text="🚀 LogWise One-Click Setup"
  local box_width=59
  local text_len=${#text}
  local padding=$(( (box_width - text_len) / 2 ))
  local left_pad=$padding
  local right_pad=$(( box_width - text_len - left_pad - 1 ))
  
  echo ""
  printf "${bold}${cyan}"
  printf "╔═══════════════════════════════════════════════════════════╗\n"
  printf "║                                                           ║\n"
  printf "║%*s${white}%s${cyan}%*s║\n" $left_pad "" "$text" $right_pad ""
  printf "║                                                           ║\n"
  printf "╚═══════════════════════════════════════════════════════════╝\n"
  printf "${reset}\n"
}

print_footer() {
  local text="✅ Setup Complete!"
  local box_width=59
  local text_len=${#text}
  local padding=$(( (box_width - text_len) / 2 ))
  local left_pad=$padding
  local right_pad=$(( box_width - text_len - left_pad - 1 ))
  
  echo ""
  printf "${bold}${green}"
  printf "╔═══════════════════════════════════════════════════════════╗\n"
  printf "║                                                           ║\n"
  printf "║%*s${white}%s${green}%*s║\n" $left_pad "" "$text" $right_pad ""
  printf "║                                                           ║\n"
  printf "╚═══════════════════════════════════════════════════════════╝\n"
  printf "${reset}\n"
}

# Progress spinner
spinner() {
  local pid=$1
  local delay=0.1
  local spinstr='|/-\'
  while [ "$(ps a | awk '{print $1}' | grep $pid)" ]; do
    local temp=${spinstr#?}
    printf " [%c]  " "$spinstr"
    local spinstr=$temp${spinstr%"$temp"}
    sleep $delay
    printf "\b\b\b\b\b\b"
  done
  printf "    \b\b\b\b"
}

# Wait with spinner
wait_with_spinner() {
  local message=$1
  local check_command=$2
  local max_attempts=${3:-40}
  local attempt=0
  
  printf "${dim}  ${message}${reset}"
  while [ $attempt -lt $max_attempts ]; do
    if eval "$check_command" >/dev/null 2>&1; then
      printf "\r${green}✓${reset} ${message} ${green}ready${reset}\n"
      return 0
    fi
    attempt=$((attempt + 1))
    local spinstr='|/-\'
    local temp=${spinstr#?}
    local spinstr=$temp${spinstr%"$temp"}
    printf "\r${dim}  ${spinstr:0:1}${reset} ${message}${reset}"
    sleep 1
  done
  printf "\r${yellow}⚠${reset}  ${message} ${yellow}did not become healthy${reset}\n"
  return 1
}

# Check if a port is available
check_port_available() {
  local port=$1
  local service_name=$2
  
  if command -v lsof >/dev/null 2>&1; then
    if lsof -Pi :"$port" -sTCP:LISTEN -t >/dev/null 2>&1; then
      return 1  # Port is in use
    fi
  elif command -v netstat >/dev/null 2>&1; then
    if netstat -an 2>/dev/null | grep -q ":$port.*LISTEN"; then
      return 1  # Port is in use
    fi
  elif command -v ss >/dev/null 2>&1; then
    if ss -lnt 2>/dev/null | grep -q ":$port "; then
      return 1  # Port is in use
    fi
  else
    # Fallback: try to bind to the port
    if timeout 1 bash -c "echo >/dev/tcp/localhost/$port" 2>/dev/null; then
      return 1  # Port is in use
    fi
  fi
  return 0  # Port is available
}

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

main() {
  print_header
  
  # Step 1: Bootstrap prerequisites
  print_step "Step 1: Checking Prerequisites"
  print_substep "Verifying required tools..."
  
  if [ ! -f "./start.sh" ]; then
    print_error "start.sh not found. Are you in the deploy directory?"
    exit 1
  fi
  
  # Run bootstrap if not already done
  if ! command -v docker >/dev/null 2>&1 || ! command -v make >/dev/null 2>&1; then
    print_info "Installing prerequisites (this may take a few minutes)..."
    chmod +x ./start.sh || true
    ./start.sh
    print_success "Prerequisites installed"
  else
    print_success "Prerequisites already installed (Docker, Make found)"
  fi
  
  print_separator
  
  # Step 2: Create .env file if it doesn't exist
  print_step "Step 2: Environment Configuration"
  
  if [ ! -f ".env" ]; then
    if [ ! -f ".env.example" ]; then
      print_error ".env.example not found. Cannot create .env file."
      exit 1
    fi
    
    print_substep "Creating .env file from template..."
    cp .env.example .env
    print_success ".env file created"
    
    echo ""
    print_warn "IMPORTANT: Please edit .env and fill in your AWS credentials"
    echo ""
    printf "${dim}  Required variables:${reset}\n"
    printf "${dim}    • AWS_ACCESS_KEY_ID${reset}\n"
    printf "${dim}    • AWS_SECRET_ACCESS_KEY${reset}\n"
    printf "${dim}    • S3_BUCKET${reset}\n"
    printf "${dim}    • S3_ATHENA_OUTPUT${reset}\n"
    printf "${dim}    • ATHENA_WORKGROUP${reset}\n"
    printf "${dim}    • ATHENA_DATABASE${reset}\n"
    echo ""
    read -p "$(printf "${yellow}Press Enter after updating .env, or Ctrl+C to cancel...${reset}") "
    echo ""
  else
    print_success ".env file already exists, skipping creation"
  fi
  
  print_separator
  
  # Step 3: Verify .env has required values
  print_step "Step 3: Verifying Configuration"
  print_substep "Checking for placeholder values..."
  
  if grep -q "your-access-key-here" .env 2>/dev/null || \
     grep -q "your-bucket-name" .env 2>/dev/null; then
    print_warn ".env file still contains placeholder values"
    print_warn "Some services may not work correctly until you update AWS credentials"
    echo ""
    read -p "$(printf "${yellow}Continue anyway? (y/N)${reset} ") " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      print_info "Setup cancelled. Please update .env and run setup again."
      exit 0
    fi
  else
    print_success "Configuration verified"
  fi
  
  # Source .env to make variables available for health checks
  set -a
  source .env 2>/dev/null || true
  set +a
  
  print_separator
  
  # Step 3.5: Check port availability
  print_step "Step 3.5: Checking Port Availability"
  print_substep "Verifying required ports are available..."
  
  GRAFANA_PORT=${GRAFANA_PORT:-3000}
  ORCH_PORT=${ORCH_PORT:-8080}
  
  PORT_CONFLICTS=()
  
  if ! check_port_available "$GRAFANA_PORT" "Grafana"; then
    PORT_CONFLICTS+=("Grafana:${GRAFANA_PORT}")
    print_error "Port $GRAFANA_PORT is already in use (required for Grafana)"
    print_info "You can set GRAFANA_PORT in .env to use a different port"
  else
    print_success "Port $GRAFANA_PORT is available (Grafana)"
  fi
  
  if ! check_port_available "$ORCH_PORT" "Orchestrator"; then
    PORT_CONFLICTS+=("Orchestrator:${ORCH_PORT}")
    print_error "Port $ORCH_PORT is already in use (required for Orchestrator)"
    print_info "You can set ORCH_PORT in .env to use a different port"
  else
    print_success "Port $ORCH_PORT is available (Orchestrator)"
  fi
  
  if [ ${#PORT_CONFLICTS[@]} -gt 0 ]; then
    echo ""
    print_error "Port conflicts detected. Please resolve them before continuing."
    echo ""
    print_info "Options:"
    echo ""
    printf "  ${dim}1.${reset} Stop the service(s) using the conflicting port(s)\n"
    printf "  ${dim}2.${reset} Set different ports in .env:\n"
    printf "     ${dim}GRAFANA_PORT=3001${reset}\n"
    printf "     ${dim}ORCH_PORT=8081${reset}\n"
    echo ""
    print_info "To find what's using a port:"
    printf "  ${dim}lsof -i :$GRAFANA_PORT${reset}  (for Grafana)\n"
    printf "  ${dim}lsof -i :$ORCH_PORT${reset}  (for Orchestrator)\n"
    echo ""
    exit 1
  fi
  
  print_separator
  
  # Step 4: Start all services
  print_step "Step 4: Starting Services"
  print_substep "Building Docker images and starting containers..."
  print_info "This may take a few minutes on first run..."
  echo ""
  
  printf "${dim}  Starting services...${reset}"
  
  # Capture output to a temp file so we can show it on failure
  TEMP_LOG=$(mktemp)
  if make up >"$TEMP_LOG" 2>&1; then
    printf "\r${green}✓${reset} All services started successfully\n"
    rm -f "$TEMP_LOG"
  else
    # Even if make up returned non-zero, check actual service status
    # Docker Compose might return non-zero for warnings but services may still be starting
    printf "\r${yellow}⚠${reset}  Docker Compose reported issues, checking service status...\n"
    echo ""
    
    # Wait a moment for services to stabilize
    sleep 2
    
    # Get list of all services from docker-compose
    ALL_SERVICES=$(docker compose config --services 2>/dev/null || echo "")
    
    if [ -n "$ALL_SERVICES" ]; then
      FAILED_SERVICES=()
      
      # Show all service statuses first
      print_info "Service Status:"
      docker compose ps 2>/dev/null || true
      echo ""
      
      for service in $ALL_SERVICES; do
        # Get container name - try multiple methods
        CONTAINER_NAME=$(docker compose ps "$service" --format "{{.Name}}" 2>/dev/null | head -1)
        
        if [ -z "$CONTAINER_NAME" ]; then
          # Try with -a flag to include stopped containers
          CONTAINER_NAME=$(docker compose ps -a "$service" --format "{{.Name}}" 2>/dev/null | head -1)
        fi
        
        if [ -z "$CONTAINER_NAME" ]; then
          # Fallback: construct container name from service name (matches docker-compose naming)
          CONTAINER_NAME="logwise_${service}"
          
          # Verify container actually exists with this name
          if ! docker ps -a --format "{{.Names}}" 2>/dev/null | grep -q "^${CONTAINER_NAME}$"; then
            # Container doesn't exist - definitely failed
            FAILED_SERVICES+=("$service")
            echo ""
            printf "  ${red}✗${reset} ${bold}${service}${reset} - Status: ${red}container not found${reset}\n"
            printf "    ${dim}This service may have failed to start or build${reset}\n"
            continue
          fi
        fi
        
        # Get container status using docker inspect (most reliable)
        STATUS=$(docker inspect "$CONTAINER_NAME" --format='{{.State.Status}}' 2>/dev/null || echo "unknown")
        # Get health status - returns empty if no health check is defined
        HEALTH=$(docker inspect "$CONTAINER_NAME" --format='{{if .State.Health}}{{.State.Health.Status}}{{end}}' 2>/dev/null || echo "")
        EXIT_CODE=$(docker inspect "$CONTAINER_NAME" --format='{{.State.ExitCode}}' 2>/dev/null || echo "0")
        
        # Check if service actually failed
        # Only mark as failed if:
        # 1. Status is "exited" with non-zero exit code, or "dead", or "removing"
        # 2. Status is "running" but health check exists and is "unhealthy" (not "starting" - that's transitional)
        IS_FAILED=false
        
        case "$STATUS" in
          "exited")
            # Only failed if exit code is non-zero
            if [ "$EXIT_CODE" != "0" ] && [ "$EXIT_CODE" != "" ]; then
              IS_FAILED=true
              STATUS="${STATUS} (exit code: ${EXIT_CODE})"
            fi
            ;;
          "dead"|"removing")
            IS_FAILED=true
            ;;
          "running")
            # If running, only fail if health check exists and is unhealthy (not starting)
            if [ -n "$HEALTH" ] && [ "$HEALTH" = "unhealthy" ]; then
              IS_FAILED=true
              STATUS="${STATUS} (${HEALTH})"
            fi
            ;;
          "restarting")
            # Restarting is a transitional state, not a failure
            # Only fail if it's been restarting for a very long time (we'll check logs)
            # For now, don't mark as failed
            ;;
          "created"|"paused")
            # These are not running but not necessarily failed
            # Don't mark as failed, but note the state
            ;;
          "unknown")
            # Couldn't inspect container - might not exist or be accessible
            # Check if container actually exists
            if ! docker ps -a --format "{{.Names}}" 2>/dev/null | grep -q "^${CONTAINER_NAME}$"; then
              IS_FAILED=true
            fi
            ;;
        esac
        
        if [ "$IS_FAILED" = true ]; then
          FAILED_SERVICES+=("$service")
          
          echo ""
          printf "  ${red}✗${reset} ${bold}${service}${reset} - Status: ${red}${STATUS}${reset}\n"
          
          # Show recent logs for failed service
          printf "    ${dim}Recent logs (last 15 lines):${reset}\n"
          docker compose logs --tail=15 "$service" 2>/dev/null | sed 's/^/      /' || printf "      ${dim}No logs available${reset}\n"
        fi
      done
      
      if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
        echo ""
        print_error "Docker Compose output:"
        echo ""
        cat "$TEMP_LOG" | sed 's/^/  /'
        echo ""
        print_error "The following services failed to start:"
        for service in "${FAILED_SERVICES[@]}"; do
          printf "  ${red}•${reset} ${bold}${service}${reset}\n"
        done
        echo ""
        print_info "To view logs for a specific service:"
        printf "  ${dim}docker compose logs <service-name>${reset}\n"
        echo ""
        print_info "To view all logs:"
        printf "  ${dim}make logs${reset}\n"
        echo ""
        print_info "To check service status:"
        printf "  ${dim}make ps${reset}\n"
        echo ""
        rm -f "$TEMP_LOG"
        exit 1
      else
        # No services actually failed, just warnings or transient issues
        printf "\r${green}✓${reset} All services started successfully (warnings in output are non-critical)\n"
        rm -f "$TEMP_LOG"
      fi
    else
      # Couldn't get service list, show the error output
      print_error "Docker Compose output:"
      echo ""
      cat "$TEMP_LOG" | sed 's/^/  /'
      rm -f "$TEMP_LOG"
      echo ""
      print_warn "Could not retrieve service list. Check Docker Compose configuration."
      echo ""
      exit 1
    fi
  fi
  print_separator
  
  # Step 5: Wait for services to be healthy
  print_step "Step 5: Health Checks"
  print_info "Waiting for services to become healthy..."
  echo ""
  
  # Track unhealthy services
  UNHEALTHY_SERVICES=()
  
  # Wait for Kafka
  if ! wait_with_spinner "Kafka" \
    'docker compose ps kafka 2>/dev/null | grep -q "healthy"' \
    40; then
    UNHEALTHY_SERVICES+=("kafka:Kafka")
  fi
  
  # Wait for MySQL
  if ! wait_with_spinner "MySQL Database" \
    "docker compose exec -T db mysqladmin ping -h localhost -uroot -p\"${MYSQL_ROOT_PASSWORD:-root_pass}\" --silent" \
    40; then
    UNHEALTHY_SERVICES+=("db:MySQL Database")
  fi
  
  # Wait for Orchestrator
  if ! wait_with_spinner "Orchestrator Service" \
    "curl -fsS http://localhost:${ORCH_PORT:-8080}/healthcheck 2>/dev/null | grep -q \"UP\"" \
    40; then
    UNHEALTHY_SERVICES+=("orchestrator:Orchestrator Service")
  fi
  
  # Wait for Grafana (check from host to ensure port mapping works)
  GRAFANA_PORT=${GRAFANA_PORT:-3000}
  if ! wait_with_spinner "Grafana Dashboard" \
    "curl -fsS -o /dev/null -w '%{http_code}' http://localhost:${GRAFANA_PORT}/api/health 2>/dev/null | grep -q '200'" \
    40; then
    UNHEALTHY_SERVICES+=("grafana:Grafana Dashboard")
    # Additional check: verify container is running even if port mapping failed
    if docker compose ps grafana 2>/dev/null | grep -q "running"; then
      print_warn "Grafana container is running but not accessible on port ${GRAFANA_PORT}"
      print_warn "This may indicate a port binding issue. Check if port ${GRAFANA_PORT} is available."
    fi
  fi
  
  # Show logs for unhealthy services
  if [ ${#UNHEALTHY_SERVICES[@]} -gt 0 ]; then
    echo ""
    print_error "The following services did not become healthy:"
    echo ""
    
    for service_info in "${UNHEALTHY_SERVICES[@]}"; do
      IFS=':' read -r service_name display_name <<< "$service_info"
      printf "  ${red}✗${reset} ${bold}${display_name}${reset} (${service_name})\n"
      printf "    ${dim}Status:${reset}\n"
      docker compose ps "$service_name" 2>/dev/null | grep -v "NAME" | grep "$service_name" | sed 's/^/      /' || printf "      ${dim}Not found${reset}\n"
      printf "    ${dim}Recent logs (last 20 lines):${reset}\n"
      docker compose logs --tail=20 "$service_name" 2>/dev/null | sed 's/^/      /' || printf "      ${dim}No logs available${reset}\n"
      echo ""
    done
    
    print_separator
    echo ""
    print_error "Setup incomplete: Some services failed to become healthy"
    echo ""
    print_info "Troubleshooting steps:"
    echo ""
    printf "  ${dim}1.${reset} Check service logs: ${bold}docker compose logs <service-name>${reset}\n"
    printf "  ${dim}2.${reset} Check service status: ${bold}docker compose ps${reset}\n"
    printf "  ${dim}3.${reset} Verify .env configuration is correct\n"
    printf "  ${dim}4.${reset} Check Docker resources (memory/CPU limits)\n"
    printf "  ${dim}5.${reset} Try restarting services: ${bold}make down && make up${reset}\n"
    echo ""
    exit 1
  fi
  
  print_separator
  
  # Final success message
  print_footer
  
  print_info "Your LogWise stack is now running!"
  echo ""
  
  printf "${bold}${cyan}Access Your Services:${reset}\n"
  echo ""
  GRAFANA_PORT=${GRAFANA_PORT:-3000}
  printf "  ${green}📊${reset} ${bold}Grafana Dashboard${reset}\n"
  printf "     ${dim}http://localhost:${GRAFANA_PORT}${reset}\n"
  printf "     ${dim}Login: admin / admin${reset}\n"
  echo ""
  printf "  ${green}⚡${reset} ${bold}Spark Master UI${reset}\n"
  printf "     ${dim}http://localhost:18080${reset}\n"
  echo ""
  printf "  ${green}🔧${reset} ${bold}Orchestrator API${reset}\n"
  printf "     ${dim}http://localhost:${ORCH_PORT:-8080}${reset}\n"
  printf "     ${dim}Health: http://localhost:${ORCH_PORT:-8080}/healthcheck${reset}\n"
  echo ""
  printf "  ${green}📡${reset} ${bold}Vector API${reset}\n"
  printf "     ${dim}API: http://localhost:8686${reset}\n"
  printf "     ${dim}OTLP gRPC: localhost:4317${reset}\n"
  printf "     ${dim}OTLP HTTP: http://localhost:4318${reset}\n"
  echo ""
  
  printf "${bold}${cyan}Useful Commands:${reset}\n"
  echo ""
  printf "  ${dim}•${reset} View logs:        ${bold}make logs${reset}\n"
  printf "  ${dim}•${reset} Check status:      ${bold}make ps${reset}\n"
  printf "  ${dim}•${reset} Stop services:     ${bold}make down${reset}\n"
  printf "  ${dim}•${reset} Restart services:  ${bold}make up${reset}\n"
  echo ""
  
  printf "${bold}${cyan}Next Steps:${reset}\n"
  echo ""
  printf "  ${dim}1.${reset} Verify your AWS credentials in .env are correct\n"
  printf "  ${dim}2.${reset} Monitor Spark jobs: ${bold}http://localhost:18080${reset}\n"
  printf "  ${dim}3.${reset} Access Grafana and configure your dashboards\n"
  echo ""
  
  printf "${bold}${green}Happy Logging! 🚀${reset}\n"
  echo ""
}

main "$@"
