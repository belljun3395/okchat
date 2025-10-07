#!/bin/bash

# OkChat Build and Push to ECR Script
# This script builds the Docker image and pushes it to AWS ECR

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-}"
IMAGE_NAME="okchat"
VERSION="${1:-latest}"

echo "===================================="
echo "OkChat Build and Push to ECR"
echo "===================================="

# Check AWS_ACCOUNT_ID
if [ -z "$AWS_ACCOUNT_ID" ]; then
    echo -e "${YELLOW}Getting AWS Account ID...${NC}"
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
fi

echo -e "AWS Region: ${GREEN}${AWS_REGION}${NC}"
echo -e "AWS Account ID: ${GREEN}${AWS_ACCOUNT_ID}${NC}"
echo -e "Image Version: ${GREEN}${VERSION}${NC}"
echo ""

# Build Docker image
echo -e "${YELLOW}[1/4] Building Docker image...${NC}"
cd "$(dirname "$0")/../.."
docker build -t ${IMAGE_NAME}:${VERSION} .
echo -e "${GREEN}Docker image built successfully${NC}"

# Login to ECR
echo -e "\n${YELLOW}[2/4] Logging in to ECR...${NC}"
aws ecr get-login-password --region ${AWS_REGION} | \
  docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
echo -e "${GREEN}Logged in to ECR${NC}"

# Check if repository exists, create if not
echo -e "\n${YELLOW}[3/4] Checking ECR repository...${NC}"
if ! aws ecr describe-repositories --repository-names ${IMAGE_NAME} --region ${AWS_REGION} &> /dev/null; then
    echo -e "${YELLOW}Repository does not exist. Creating...${NC}"
    aws ecr create-repository \
        --repository-name ${IMAGE_NAME} \
        --region ${AWS_REGION} \
        --image-scanning-configuration scanOnPush=true \
        --encryption-configuration encryptionType=AES256
    echo -e "${GREEN}Repository created${NC}"
else
    echo -e "${GREEN}Repository exists${NC}"
fi

# Tag and push image
echo -e "\n${YELLOW}[4/4] Tagging and pushing image...${NC}"
ECR_IMAGE="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${IMAGE_NAME}:${VERSION}"
docker tag ${IMAGE_NAME}:${VERSION} ${ECR_IMAGE}
docker push ${ECR_IMAGE}

# Also tag and push as latest
if [ "$VERSION" != "latest" ]; then
    ECR_IMAGE_LATEST="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${IMAGE_NAME}:latest"
    docker tag ${IMAGE_NAME}:${VERSION} ${ECR_IMAGE_LATEST}
    docker push ${ECR_IMAGE_LATEST}
fi

echo -e "\n${GREEN}===================================="
echo "Build and Push Completed!"
echo -e "====================================${NC}"
echo -e "Image: ${GREEN}${ECR_IMAGE}${NC}"
echo ""
echo "To deploy to EKS:"
echo "  1. Update k8s/overlays/production/deployment-patch.yaml with the image URI"
echo "  2. Run: kubectl apply -k k8s/overlays/production"