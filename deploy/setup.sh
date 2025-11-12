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
  
  # Step 4: Start all services
  print_step "Step 4: Starting Services"
  print_substep "Building Docker images and starting containers..."
  print_info "This may take a few minutes on first run..."
  echo ""
  
  printf "${dim}  Starting services...${reset}"
  if make up >/dev/null 2>&1; then
    printf "\r${green}✓${reset} All services started successfully\n"
  else
    printf "\r${red}✗${reset} Failed to start services\n"
    print_error "Check the logs with: make logs"
    exit 1
  fi
  print_separator
  
  # Step 5: Wait for services to be healthy
  print_step "Step 5: Health Checks"
  print_info "Waiting for services to become healthy..."
  echo ""
  
  # Wait for Kafka
  wait_with_spinner "Kafka" \
    'docker compose ps kafka 2>/dev/null | grep -q "healthy"' \
    40
  
  # Wait for MySQL
  wait_with_spinner "MySQL Database" \
    "docker compose exec -T db mysqladmin ping -h localhost -uroot -p\"${MYSQL_ROOT_PASSWORD:-root_pass}\" --silent" \
    40
  
  # Wait for Orchestrator
  wait_with_spinner "Orchestrator Service" \
    "curl -fsS http://localhost:${ORCH_PORT:-8080}/healthcheck 2>/dev/null | grep -q \"UP\"" \
    40
  
  print_separator
  
  # Final success message
  print_footer
  
  print_info "Your LogWise stack is now running!"
  echo ""
  
  printf "${bold}${cyan}Access Your Services:${reset}\n"
  echo ""
  printf "  ${green}📊${reset} ${bold}Grafana Dashboard${reset}\n"
  printf "     ${dim}http://localhost:3000${reset}\n"
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
  printf "     ${dim}Internal API: http://vector:8686${reset}\n"
  printf "     ${dim}OTLP gRPC: vector:4317${reset}\n"
  printf "     ${dim}OTLP HTTP: http://vector:4318${reset}\n"
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
  printf "  ${dim}2.${reset} Check Spark job: ${bold}docker compose logs spark-client${reset}\n"
  printf "  ${dim}3.${reset} Access Grafana and configure your dashboards\n"
  echo ""
  
  printf "${bold}${green}Happy Logging! 🚀${reset}\n"
  echo ""
}

main "$@"
