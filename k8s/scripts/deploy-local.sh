#!/bin/bash

# OkChat Local Deployment Script for Minikube
# This script automates the deployment process for local development

set -e

echo "===================================="
echo "OkChat Local Deployment to Minikube"
echo "===================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if minikube is running
echo -e "\n${YELLOW}[1/7] Checking Minikube status...${NC}"
MINIKUBE_STATUS=$(minikube status --format='{{.Host}}' 2>/dev/null || echo "NotFound")

# Configure resource limits (can be overridden with environment variables)
MINIKUBE_CPUS="${MINIKUBE_CPUS:-4}"
MINIKUBE_MEMORY="${MINIKUBE_MEMORY:-6144}"  # 6GB default (reduced from 8GB)
MINIKUBE_DISK="${MINIKUBE_DISK:-40g}"       # 40GB default

echo -e "${YELLOW}Resource configuration:${NC}"
echo "  CPUs: $MINIKUBE_CPUS"
echo "  Memory: ${MINIKUBE_MEMORY}MB"
echo "  Disk: $MINIKUBE_DISK"
echo ""

if [ "$MINIKUBE_STATUS" != "Running" ]; then
    if [ "$MINIKUBE_STATUS" == "NotFound" ] || [ "$MINIKUBE_STATUS" == "" ]; then
        echo -e "${YELLOW}Minikube is not running. Starting Minikube...${NC}"
        minikube start --cpus=$MINIKUBE_CPUS --memory=$MINIKUBE_MEMORY --disk-size=$MINIKUBE_DISK
    else
        echo -e "${RED}Minikube is in an inconsistent state. Cleaning up...${NC}"
        minikube delete || true
        echo -e "${YELLOW}Starting fresh Minikube instance...${NC}"
        minikube start --cpus=$MINIKUBE_CPUS --memory=$MINIKUBE_MEMORY --disk-size=$MINIKUBE_DISK
    fi
else
    echo -e "${GREEN}Minikube is already running${NC}"
fi

# Enable ingress addon
echo -e "\n${YELLOW}[2/7] Enabling Ingress addon...${NC}"
minikube addons enable ingress
echo -e "${GREEN}Ingress addon enabled${NC}"

# Switch to minikube docker environment
echo -e "\n${YELLOW}[3/7] Switching to Minikube Docker environment...${NC}"
eval $(minikube docker-env)
echo -e "${GREEN}Docker environment configured${NC}"

# Build Docker image
echo -e "\n${YELLOW}[4/7] Building Docker image...${NC}"
cd "$(dirname "$0")/../.."
docker build -t okchat:latest .
echo -e "${GREEN}Docker image built successfully${NC}"

# Check if secret file needs to be updated
echo -e "\n${YELLOW}[5/7] Checking secrets...${NC}"
if grep -q "your-openai-api-key-here" k8s/base/secret.yaml; then
    echo -e "${RED}WARNING: Please update k8s/base/secret.yaml with actual values!${NC}"
    echo -e "${YELLOW}Continuing with default values (application may not work properly)${NC}"
    read -p "Press Enter to continue or Ctrl+C to abort..."
fi

# Deploy to Kubernetes
echo -e "\n${YELLOW}[6/7] Deploying to Kubernetes...${NC}"
kubectl apply -k k8s/overlays/local
echo -e "${GREEN}Deployment completed${NC}"

# Wait for pods to be ready
echo -e "\n${YELLOW}[7/7] Waiting for pods to be ready...${NC}"
echo "This may take a few minutes..."
kubectl wait --for=condition=ready pod -l app=okchat-mysql -n okchat --timeout=300s || echo "MySQL pod timeout"
kubectl wait --for=condition=ready pod -l app=okchat-redis -n okchat --timeout=300s || echo "Redis pod timeout"
kubectl wait --for=condition=ready pod -l app=okchat-opensearch -n okchat --timeout=300s || echo "OpenSearch pod timeout"
kubectl wait --for=condition=ready pod -l app=okchat-app -n okchat --timeout=300s || echo "OkChat app timeout"

# Get status
echo -e "\n${GREEN}===================================="
echo "Deployment Status"
echo -e "====================================${NC}"
kubectl get pods -n okchat

# Setup /etc/hosts
echo -e "\n${YELLOW}Setting up /etc/hosts...${NC}"
MINIKUBE_IP=$(minikube ip)
if grep -q "okchat.local" /etc/hosts; then
    echo -e "${GREEN}/etc/hosts already configured${NC}"
else
    echo -e "${YELLOW}Adding okchat.local to /etc/hosts (requires sudo)${NC}"
    echo "$MINIKUBE_IP okchat.local" | sudo tee -a /etc/hosts
fi

# Display access information
echo -e "\n${GREEN}===================================="
echo "Deployment Successful!"
echo -e "====================================${NC}"
echo -e "Application URL: ${GREEN}http://okchat.local${NC}"
echo -e "Health Check: ${GREEN}http://okchat.local/actuator/health${NC}"
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n okchat          # View all pods"
echo "  kubectl logs -n okchat -l app=okchat-app -f  # View logs"
echo "  kubectl port-forward -n okchat svc/okchat-app 8080:8080  # Port forwarding"
echo "  ./k8s/scripts/cleanup-local.sh      # Clean up deployment"
echo -e "${GREEN}====================================${NC}"