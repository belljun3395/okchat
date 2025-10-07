#!/bin/bash

# Minikube Troubleshooting and Fix Script
# This script helps resolve common Minikube issues

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "===================================="
echo "Minikube Troubleshooting Tool"
echo "===================================="

# Check Minikube status
echo -e "\n${YELLOW}Checking Minikube status...${NC}"
MINIKUBE_STATUS=$(minikube status --format='{{.Host}}' 2>/dev/null || echo "NotFound")
echo "Current status: $MINIKUBE_STATUS"

# Check Docker
echo -e "\n${YELLOW}Checking Docker...${NC}"
if ! docker info &> /dev/null; then
    echo -e "${RED}ERROR: Docker is not running!${NC}"
    echo "Please start Docker Desktop and try again."
    exit 1
else
    echo -e "${GREEN}Docker is running${NC}"
fi

# Check if minikube container exists
echo -e "\n${YELLOW}Checking Minikube container...${NC}"
if docker ps -a --format '{{.Names}}' | grep -q "^minikube$"; then
    CONTAINER_STATE=$(docker inspect minikube --format='{{.State.Status}}' 2>/dev/null || echo "unknown")
    echo "Container state: $CONTAINER_STATE"
    
    if [ "$CONTAINER_STATE" != "running" ]; then
        echo -e "${YELLOW}Container exists but not running${NC}"
    fi
else
    echo -e "${YELLOW}No minikube container found${NC}"
fi

# Provide options
echo -e "\n${YELLOW}===================================="
echo "Available Options"
echo -e "====================================${NC}"
echo "1. Delete and recreate Minikube (recommended)"
echo "2. Try to restart existing Minikube"
echo "3. Just delete Minikube (manual restart needed)"
echo "4. Exit"
echo ""
read -p "Select option (1-4): " -n 1 -r
echo

case $REPLY in
    1)
        echo -e "\n${YELLOW}Deleting existing Minikube...${NC}"
        minikube delete || true
        
        # Configure resource limits (can be overridden with environment variables)
        MINIKUBE_CPUS="${MINIKUBE_CPUS:-4}"
        MINIKUBE_MEMORY="${MINIKUBE_MEMORY:-6144}"  # 6GB default
        MINIKUBE_DISK="${MINIKUBE_DISK:-40g}"
        
        echo -e "\n${YELLOW}Starting fresh Minikube instance...${NC}"
        echo "Using: ${MINIKUBE_CPUS} CPUs, ${MINIKUBE_MEMORY}MB RAM, ${MINIKUBE_DISK} disk"
        minikube start --cpus=$MINIKUBE_CPUS --memory=$MINIKUBE_MEMORY --disk-size=$MINIKUBE_DISK
        
        echo -e "\n${GREEN}Minikube has been reset successfully!${NC}"
        echo -e "\n${YELLOW}Enabling Ingress addon...${NC}"
        minikube addons enable ingress
        
        echo -e "\n${GREEN}===================================="
        echo "Minikube is ready!"
        echo -e "====================================${NC}"
        echo "You can now run: ./k8s/scripts/deploy-local.sh"
        ;;
    2)
        echo -e "\n${YELLOW}Attempting to start Minikube...${NC}"
        if minikube start; then
            echo -e "\n${GREEN}Minikube started successfully!${NC}"
            minikube addons enable ingress
        else
            echo -e "\n${RED}Failed to start Minikube${NC}"
            echo "Please try option 1 (delete and recreate)"
        fi
        ;;
    3)
        echo -e "\n${YELLOW}Deleting Minikube...${NC}"
        minikube delete
        echo -e "\n${GREEN}Minikube deleted${NC}"
        echo "Run 'minikube start' when you're ready"
        ;;
    4)
        echo "Exiting..."
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid option${NC}"
        exit 1
        ;;
esac