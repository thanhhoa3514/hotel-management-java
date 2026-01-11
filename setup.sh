#!/bin/bash

#======================================================================
# Hotel Management System - Setup Script (Linux/macOS)
#======================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_header() {
    echo ""
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

check_command() {
    if command -v "$1" &> /dev/null; then
        print_success "$1 is installed"
        return 0
    else
        print_error "$1 is not installed"
        return 1
    fi
}

wait_for_service() {
    local service=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=1

    echo "Waiting for $service to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            print_success "$service is ready!"
            return 0
        fi
        echo "  Attempt $attempt/$max_attempts - $service not ready yet..."
        sleep 2
        ((attempt++))
    done
    print_error "$service failed to start within expected time"
    return 1
}

#======================================================================
# Main Script
#======================================================================

print_header "Hotel Management System Setup"

echo "This script will:"
echo "  1. Check prerequisites"
echo "  2. Start Docker services (PostgreSQL, Redis, Keycloak)"
echo "  3. Wait for services to be healthy"
echo "  4. Build the application"
echo "  5. Optionally run the application"
echo ""

#----------------------------------------------------------------------
# Step 1: Check Prerequisites
#----------------------------------------------------------------------
print_header "Step 1: Checking Prerequisites"

MISSING_DEPS=0

# Check Java
if check_command java; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        print_success "Java version $JAVA_VERSION is compatible"
    else
        print_warning "Java 21 or higher is recommended (found version $JAVA_VERSION)"
    fi
else
    MISSING_DEPS=1
fi

# Check Maven
if ! check_command mvn; then
    MISSING_DEPS=1
fi

# Check Docker
if ! check_command docker; then
    MISSING_DEPS=1
fi

# Check Docker Compose
if docker compose version &> /dev/null; then
    print_success "docker compose is installed"
elif check_command docker-compose; then
    print_success "docker-compose is installed"
else
    MISSING_DEPS=1
fi

# Check curl
if ! check_command curl; then
    MISSING_DEPS=1
fi

if [ $MISSING_DEPS -eq 1 ]; then
    print_error "Missing dependencies. Please install them and try again."
    exit 1
fi

#----------------------------------------------------------------------
# Step 2: Start Docker Services
#----------------------------------------------------------------------
print_header "Step 2: Starting Docker Services"

# Check if Docker daemon is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker daemon is not running. Please start Docker and try again."
    exit 1
fi

# Stop existing containers if any
echo "Stopping any existing containers..."
docker compose down 2>/dev/null || docker-compose down 2>/dev/null || true

# Ask user if they want to reset data
read -p "Do you want to reset all data (remove volumes)? [y/N]: " RESET_DATA
if [[ "$RESET_DATA" =~ ^[Yy]$ ]]; then
    echo "Removing existing volumes..."
    docker compose down -v 2>/dev/null || docker-compose down -v 2>/dev/null || true
    print_success "Volumes removed"
fi

# Start services
echo "Starting Docker services..."
if docker compose up -d 2>/dev/null; then
    print_success "Docker services started with 'docker compose'"
elif docker-compose up -d 2>/dev/null; then
    print_success "Docker services started with 'docker-compose'"
else
    print_error "Failed to start Docker services"
    exit 1
fi

#----------------------------------------------------------------------
# Step 3: Wait for Services
#----------------------------------------------------------------------
print_header "Step 3: Waiting for Services to be Healthy"

# Wait for PostgreSQL
echo "Checking PostgreSQL..."
POSTGRES_READY=0
for i in {1..30}; do
    if docker exec hotel_postgres pg_isready -U hoteluser -d hotelmanagement > /dev/null 2>&1; then
        POSTGRES_READY=1
        break
    fi
    echo "  Attempt $i/30 - PostgreSQL not ready..."
    sleep 2
done

if [ $POSTGRES_READY -eq 1 ]; then
    print_success "PostgreSQL is ready!"
else
    print_error "PostgreSQL failed to start"
    exit 1
fi

# Wait for Redis
echo "Checking Redis..."
REDIS_READY=0
for i in {1..20}; do
    if docker exec hotel_redis redis-cli -a redis123 ping 2>/dev/null | grep -q "PONG"; then
        REDIS_READY=1
        break
    fi
    echo "  Attempt $i/20 - Redis not ready..."
    sleep 2
done

if [ $REDIS_READY -eq 1 ]; then
    print_success "Redis is ready!"
else
    print_error "Redis failed to start"
    exit 1
fi

# Wait for Keycloak (takes longer)
echo "Checking Keycloak (this may take a minute)..."
if wait_for_service "Keycloak" "http://localhost:8180/health/ready" 60; then
    print_success "Keycloak is ready!"
else
    print_warning "Keycloak may still be starting. You can check manually at http://localhost:8180"
fi

#----------------------------------------------------------------------
# Step 4: Build Application
#----------------------------------------------------------------------
print_header "Step 4: Building Application"

echo "Running Maven build..."
if mvn clean install -DskipTests -q; then
    print_success "Application built successfully!"
else
    print_error "Build failed. Check the error messages above."
    exit 1
fi

#----------------------------------------------------------------------
# Step 5: Run Application (Optional)
#----------------------------------------------------------------------
print_header "Setup Complete!"

echo ""
echo "Service URLs:"
echo "  - API:      http://localhost:8080"
echo "  - Keycloak: http://localhost:8180 (admin/admin123)"
echo "  - Postgres: localhost:5432 (hoteluser/hotelpass123)"
echo "  - Redis:    localhost:6379 (password: redis123)"
echo ""

read -p "Do you want to start the application now? [Y/n]: " START_APP
if [[ ! "$START_APP" =~ ^[Nn]$ ]]; then
    print_header "Starting Application"
    echo "Starting Spring Boot application..."
    echo "Press Ctrl+C to stop the application"
    echo ""
    mvn spring-boot:run
else
    echo ""
    echo "To start the application later, run:"
    echo "  mvn spring-boot:run"
    echo ""
    echo "Or run directly with Java:"
    echo "  java -jar target/quanlikhachsan-0.0.1-SNAPSHOT.jar"
    echo ""
fi

print_success "Done!"
