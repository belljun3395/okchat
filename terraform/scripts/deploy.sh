#!/bin/bash
# ============================================================================
# OkChat Terraform 배포 스크립트
# ============================================================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 함수 정의
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    print_info "사전 요구사항 확인 중..."
    
    # AWS CLI 확인
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI가 설치되어 있지 않습니다."
        exit 1
    fi
    
    # Terraform 확인
    if ! command -v terraform &> /dev/null; then
        print_error "Terraform이 설치되어 있지 않습니다."
        exit 1
    fi
    
    # kubectl 확인
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl이 설치되어 있지 않습니다."
        exit 1
    fi
    
    # AWS 자격 증명 확인
    if ! aws sts get-caller-identity &> /dev/null; then
        print_error "AWS 자격 증명이 구성되어 있지 않습니다."
        print_error "aws configure 명령을 실행하세요."
        exit 1
    fi
    
    print_info "모든 사전 요구사항이 충족되었습니다."
}

check_tfvars() {
    print_info "terraform.tfvars 파일 확인 중..."
    
    if [ ! -f "terraform.tfvars" ]; then
        print_error "terraform.tfvars 파일이 없습니다."
        print_info "terraform.tfvars.example을 복사하여 terraform.tfvars를 생성하세요."
        print_info "cp terraform.tfvars.example terraform.tfvars"
        exit 1
    fi
    
    # OpenAI API Key 확인
    if ! grep -q "openai_api_key.*=.*\"sk-" terraform.tfvars; then
        print_warn "OpenAI API Key가 설정되지 않았을 수 있습니다."
        print_warn "terraform.tfvars에서 openai_api_key를 확인하세요."
        read -p "계속하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    print_info "terraform.tfvars 파일이 확인되었습니다."
}

terraform_init() {
    print_info "Terraform 초기화 중..."
    terraform init
    print_info "Terraform 초기화가 완료되었습니다."
}

terraform_plan() {
    print_info "Terraform 계획 생성 중..."
    terraform plan -out=tfplan
    print_info "Terraform 계획이 생성되었습니다."
    
    print_warn "생성될 리소스를 확인하세요."
    read -p "배포를 계속하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "배포가 취소되었습니다."
        rm -f tfplan
        exit 0
    fi
}

terraform_apply() {
    print_info "Terraform 배포 중..."
    print_warn "이 작업은 20-30분 정도 소요될 수 있습니다."
    
    terraform apply tfplan
    
    rm -f tfplan
    print_info "Terraform 배포가 완료되었습니다."
}

configure_kubectl() {
    print_info "kubectl 설정 중..."
    
    CLUSTER_NAME=$(terraform output -raw eks_cluster_name)
    AWS_REGION=$(terraform output -json | jq -r '.configure_kubectl.value' | awk '{print $4}')
    
    aws eks update-kubeconfig --region ${AWS_REGION} --name ${CLUSTER_NAME}
    
    print_info "kubectl이 설정되었습니다."
    
    # 클러스터 확인
    print_info "클러스터 노드 확인 중..."
    kubectl get nodes
    
    print_info "네임스페이스 확인 중..."
    kubectl get namespaces
}

display_outputs() {
    print_info "배포 정보:"
    echo ""
    
    terraform output -json | jq -r 'to_entries[] | "\(.key): \(.value.value)"' | while IFS=: read -r key value; do
        if [[ "$key" != *"credentials"* ]] && [[ "$key" != *"password"* ]]; then
            echo "  $key:$value"
        fi
    done
    
    echo ""
    print_info "민감한 정보는 다음 명령으로 확인할 수 있습니다:"
    echo "  - RDS 자격 증명: terraform output -json database_credentials"
    echo "  - OpenSearch 자격 증명: terraform output -json opensearch_credentials"
    echo ""
}

next_steps() {
    print_info "다음 단계:"
    echo ""
    echo "1. Docker 이미지 빌드 및 푸시:"
    echo "   cd .."
    echo "   docker build -t okchat:latest ."
    echo "   ECR_URL=\$(cd terraform && terraform output -raw ecr_repository_url)"
    echo "   aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin \$ECR_URL"
    echo "   docker tag okchat:latest \$ECR_URL:latest"
    echo "   docker push \$ECR_URL:latest"
    echo ""
    echo "2. 애플리케이션 상태 확인:"
    echo "   kubectl get pods -n okchat"
    echo "   kubectl logs -f deployment/okchat-app -n okchat"
    echo ""
    echo "3. 애플리케이션 접속:"
    APP_URL=$(terraform output -raw application_url 2>/dev/null || echo "ALB URL 생성 중...")
    echo "   ${APP_URL}"
    echo ""
}

# 메인 실행
main() {
    print_info "OkChat AWS 개발 환경 배포를 시작합니다..."
    echo ""
    
    check_prerequisites
    check_tfvars
    terraform_init
    terraform_plan
    terraform_apply
    
    print_info "배포가 성공적으로 완료되었습니다!"
    echo ""
    
    # kubectl 설정 (실패해도 계속 진행)
    configure_kubectl || print_warn "kubectl 설정에 실패했습니다. 수동으로 설정하세요."
    
    echo ""
    display_outputs
    next_steps
    
    print_info "배포 스크립트가 완료되었습니다."
}

# 스크립트 실행
main "$@"