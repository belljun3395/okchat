#!/bin/bash
# ============================================================================
# OkChat Terraform 리소스 정리 스크립트
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

confirm_destroy() {
    print_warn "===================== 경고 ====================="
    print_warn "이 작업은 모든 AWS 리소스를 삭제합니다!"
    print_warn "다음 리소스가 삭제됩니다:"
    echo "  - EKS 클러스터 및 노드 그룹"
    echo "  - RDS MySQL 데이터베이스"
    echo "  - ElastiCache Redis 클러스터"
    echo "  - OpenSearch 도메인"
    echo "  - VPC 및 네트워크 리소스"
    echo "  - ECR 레포지토리"
    echo "  - IAM 역할 및 정책"
    echo "  - Secrets Manager 시크릿"
    print_warn "=============================================\n"
    
    print_error "삭제하기 전에 중요한 데이터를 백업했는지 확인하세요!"
    echo ""
    
    read -p "정말로 모든 리소스를 삭제하시겠습니까? 'yes'를 입력하세요: " -r
    echo
    if [[ $REPLY != "yes" ]]; then
        print_info "리소스 삭제가 취소되었습니다."
        exit 0
    fi
    
    print_warn "마지막 확인: 프로젝트 이름을 입력하세요 (okchat): "
    read -r PROJECT_NAME
    if [[ $PROJECT_NAME != "okchat" ]]; then
        print_error "프로젝트 이름이 일치하지 않습니다. 삭제가 취소되었습니다."
        exit 1
    fi
}

backup_terraform_state() {
    print_info "Terraform State 백업 중..."
    
    if [ -f "terraform.tfstate" ]; then
        BACKUP_FILE="terraform.tfstate.backup.$(date +%Y%m%d_%H%M%S)"
        cp terraform.tfstate "$BACKUP_FILE"
        print_info "State 파일이 백업되었습니다: $BACKUP_FILE"
    fi
}

cleanup_kubernetes_resources() {
    print_info "Kubernetes 리소스 정리 중..."
    
    # kubectl이 설정되어 있는지 확인
    if kubectl cluster-info &> /dev/null; then
        print_info "LoadBalancer 서비스 삭제 중..."
        kubectl delete svc --all -n okchat --ignore-not-found=true
        
        print_info "PVC 삭제 중..."
        kubectl delete pvc --all -n okchat --ignore-not-found=true
        
        # ALB가 완전히 삭제될 때까지 대기
        print_info "ALB 삭제 대기 중..."
        sleep 30
    else
        print_warn "kubectl 설정이 없습니다. Kubernetes 리소스 정리를 건너뜁니다."
    fi
}

terraform_destroy() {
    print_info "Terraform 리소스 삭제 중..."
    print_warn "이 작업은 15-20분 정도 소요될 수 있습니다."
    
    # 먼저 Kubernetes 리소스 삭제
    terraform destroy -target=kubernetes_ingress_v1.okchat -auto-approve || true
    terraform destroy -target=kubernetes_service.okchat -auto-approve || true
    terraform destroy -target=kubernetes_deployment.okchat -auto-approve || true
    
    # 나머지 리소스 삭제
    terraform destroy -auto-approve
    
    print_info "Terraform 리소스 삭제가 완료되었습니다."
}

manual_cleanup_check() {
    print_info "수동 정리가 필요할 수 있는 리소스 확인 중..."
    
    AWS_REGION=$(grep 'aws_region' terraform.tfvars 2>/dev/null | awk -F'"' '{print $2}' || echo "ap-northeast-2")
    PROJECT_NAME=$(grep 'project_name' terraform.tfvars 2>/dev/null | awk -F'"' '{print $2}' || echo "okchat")
    
    echo ""
    print_warn "다음 리소스는 수동으로 확인하세요:"
    echo ""
    
    # CloudWatch 로그 그룹 확인
    print_info "1. CloudWatch 로그 그룹:"
    aws logs describe-log-groups --log-group-name-prefix "/aws/eks/${PROJECT_NAME}" --region ${AWS_REGION} 2>/dev/null || true
    
    # EBS 볼륨 확인
    print_info "2. EBS 볼륨 및 스냅샷:"
    aws ec2 describe-volumes --filters "Name=tag:Project,Values=${PROJECT_NAME}" --region ${AWS_REGION} --query 'Volumes[*].[VolumeId,State]' --output table 2>/dev/null || true
    
    # ENI 확인
    print_info "3. 네트워크 인터페이스 (ENI):"
    aws ec2 describe-network-interfaces --filters "Name=tag:Project,Values=${PROJECT_NAME}" --region ${AWS_REGION} --query 'NetworkInterfaces[*].[NetworkInterfaceId,Status]' --output table 2>/dev/null || true
    
    echo ""
    print_info "위 리소스가 있다면 다음 명령으로 삭제할 수 있습니다:"
    echo "  aws logs delete-log-group --log-group-name <log-group-name> --region ${AWS_REGION}"
    echo "  aws ec2 delete-volume --volume-id <volume-id> --region ${AWS_REGION}"
    echo ""
}

final_message() {
    print_info "=============================================\n"
    print_info "리소스 정리가 완료되었습니다!"
    print_info "=============================================\n"
    
    echo "다음 사항을 확인하세요:"
    echo "  1. AWS Console에서 모든 리소스가 삭제되었는지 확인"
    echo "  2. CloudWatch 로그 그룹 수동 삭제"
    echo "  3. EBS 스냅샷 수동 삭제"
    echo "  4. Secrets Manager 시크릿 확인 (복구 대기 중일 수 있음)"
    echo ""
    echo "Terraform State 백업 파일을 삭제하려면:"
    echo "  rm -f terraform.tfstate.backup.*"
    echo ""
}

# 메인 실행
main() {
    print_info "OkChat AWS 리소스 정리를 시작합니다..."
    echo ""
    
    # 확인
    confirm_destroy
    
    # State 백업
    backup_terraform_state
    
    # Kubernetes 리소스 정리
    cleanup_kubernetes_resources
    
    # Terraform 리소스 삭제
    terraform_destroy
    
    # 수동 정리 확인
    manual_cleanup_check
    
    # 최종 메시지
    final_message
}

# 스크립트 실행
main "$@"