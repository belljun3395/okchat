#!/bin/bash

# OkChat Local Access Script
# Provides easy access to the application running in Minikube

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}===================================="
echo "OkChat Local Access"
echo -e "====================================${NC}"

# Check if application is running
echo -e "\n${YELLOW}Checking application status...${NC}"
POD_STATUS=$(kubectl get pods -n okchat -l app=okchat-app -o jsonpath='{.items[0].status.phase}' 2>/dev/null || echo "NotFound")

if [ "$POD_STATUS" != "Running" ]; then
    echo -e "${RED}Error: Application is not running!${NC}"
    echo "Please deploy the application first:"
    echo "  ./k8s/scripts/deploy-local.sh"
    exit 1
fi

echo -e "${GREEN}✓ Application is running${NC}"

# Show access options
echo -e "\n${BLUE}===================================="
echo "Access Options"
echo -e "====================================${NC}"

echo -e "\n${YELLOW}Option 1: Port Forwarding (Recommended)${NC}"
echo "Run this command in a separate terminal:"
echo -e "${GREEN}kubectl port-forward -n okchat svc/okchat-app 8080:8080${NC}"
echo ""
echo "Then access:"
echo "  - Application: http://localhost:8080"
echo "  - Health Check: http://localhost:8080/actuator/health"
echo "  - Metrics: http://localhost:8080/actuator/metrics"
echo "  - Prometheus: http://localhost:8080/actuator/prometheus"

echo -e "\n${YELLOW}Option 2: Minikube Service (Direct Access)${NC}"
echo "Run this command:"
echo -e "${GREEN}minikube service okchat-app -n okchat${NC}"
echo "(This will automatically open your browser)"

echo -e "\n${YELLOW}Option 3: Ingress (Requires tunnel)${NC}"
echo "In a separate terminal with sudo access, run:"
echo -e "${GREEN}sudo minikube tunnel${NC}"
echo ""
echo "Then access:"
echo "  - Application: http://okchat.local"

# Auto-start port forward option
echo -e "\n${BLUE}===================================="
echo "Quick Start"
echo -e "====================================${NC}"
read -p "Do you want to start port-forwarding now? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "\n${GREEN}Starting port-forward on http://localhost:8080${NC}"
    echo -e "${YELLOW}Press Ctrl+C to stop${NC}\n"
    kubectl port-forward -n okchat svc/okchat-app 8080:8080
fi