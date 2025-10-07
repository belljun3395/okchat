#!/bin/bash

# OkChat Local Cleanup Script
# This script removes all OkChat resources from Minikube

set -e

echo "===================================="
echo "OkChat Local Cleanup"
echo "===================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Confirm deletion
echo -e "${RED}WARNING: This will delete all OkChat resources from Minikube${NC}"
read -p "Are you sure? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cleanup cancelled"
    exit 1
fi

# Delete resources
echo -e "\n${YELLOW}Deleting OkChat resources...${NC}"
kubectl delete -k k8s/overlays/local

# Wait for namespace deletion
echo -e "\n${YELLOW}Waiting for namespace deletion...${NC}"
kubectl wait --for=delete namespace/okchat --timeout=60s || echo "Namespace deletion timeout"

echo -e "\n${GREEN}===================================="
echo "Cleanup completed successfully!"
echo -e "====================================${NC}"
echo ""
echo "To remove /etc/hosts entry (optional):"
echo "  sudo sed -i '' '/okchat.local/d' /etc/hosts"
echo ""
echo "To stop Minikube:"
echo "  minikube stop"
echo ""
echo "To delete Minikube (removes all data):"
echo "  minikube delete"