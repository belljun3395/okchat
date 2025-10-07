# ============================================================================
# Terraform Outputs
# ============================================================================

# VPC
output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "vpc_cidr" {
  description = "VPC CIDR 블록"
  value       = module.vpc.vpc_cidr_block
}

output "private_subnets" {
  description = "프라이빗 서브넷 ID 리스트"
  value       = module.vpc.private_subnets
}

output "database_subnets" {
  description = "데이터베이스 서브넷 ID 리스트"
  value       = module.vpc.database_subnets
}

# EKS
output "eks_cluster_name" {
  description = "EKS 클러스터 이름"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS 클러스터 엔드포인트"
  value       = module.eks.cluster_endpoint
}

output "eks_cluster_version" {
  description = "EKS 클러스터 버전"
  value       = module.eks.cluster_version
}

output "eks_oidc_provider_arn" {
  description = "EKS OIDC Provider ARN"
  value       = module.eks.oidc_provider_arn
}

output "eks_node_group_iam_role_name" {
  description = "EKS Node Group IAM Role Name"
  value       = try(module.eks.eks_managed_node_groups["main"].iam_role_name, "")
}

output "eks_node_group_iam_role_arn" {
  description = "EKS Node Group IAM Role ARN"
  value       = try(data.aws_iam_role.eks_node_group.arn, "")
}

output "configure_kubectl" {
  description = "kubectl 설정 명령어"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

# RDS
output "rds_endpoint" {
  description = "RDS 엔드포인트"
  value       = module.rds.db_instance_endpoint
  sensitive   = true
}

output "rds_database_name" {
  description = "RDS 데이터베이스 이름"
  value       = module.rds.db_instance_name
}

output "rds_port" {
  description = "RDS 포트"
  value       = module.rds.db_instance_port
}

# Redis
output "redis_endpoint" {
  description = "Redis 엔드포인트"
  value       = aws_elasticache_cluster.redis.cache_nodes[0].address
  sensitive   = true
}

output "redis_port" {
  description = "Redis 포트"
  value       = 6379
}

# OpenSearch
output "opensearch_endpoint" {
  description = "OpenSearch 엔드포인트"
  value       = aws_opensearch_domain.opensearch.endpoint
  sensitive   = true
}

output "opensearch_dashboard_endpoint" {
  description = "OpenSearch 대시보드 엔드포인트"
  value       = aws_opensearch_domain.opensearch.dashboard_endpoint
  sensitive   = true
}

# ECR
output "ecr_repository_url" {
  description = "ECR 레포지토리 URL"
  value       = aws_ecr_repository.okchat.repository_url
}

output "ecr_repository_arn" {
  description = "ECR 레포지토리 ARN"
  value       = aws_ecr_repository.okchat.arn
}

# Secrets
output "secrets_manager_secret_arn" {
  description = "Secrets Manager Secret ARN"
  value       = aws_secretsmanager_secret.okchat_secrets.arn
  sensitive   = true
}

# Application
output "application_url" {
  description = "애플리케이션 URL (ALB)"
  value       = "http://${kubernetes_ingress_v1.okchat.status[0].load_balancer[0].ingress[0].hostname}"
}

# Kubernetes Namespace
output "kubernetes_namespace" {
  description = "Kubernetes 네임스페이스"
  value       = kubernetes_namespace.okchat.metadata[0].name
}

# ============================================================================
# 중요 정보 (민감 정보)
# ============================================================================

output "database_credentials" {
  description = "데이터베이스 자격 증명 (민감 정보)"
  value = {
    username = var.rds_master_username
    password = random_password.rds_password.result
  }
  sensitive = true
}

output "opensearch_credentials" {
  description = "OpenSearch 자격 증명 (민감 정보)"
  value = {
    username = var.opensearch_master_user
    password = random_password.opensearch_password.result
  }
  sensitive = true
}

# ============================================================================
# 유용한 명령어
# ============================================================================

output "useful_commands" {
  description = "유용한 명령어 모음"
  value = {
    kubectl_config          = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
    ecr_login               = "aws ecr get-login-password --region ${var.aws_region} | docker login --username AWS --password-stdin ${aws_ecr_repository.okchat.repository_url}"
    get_rds_password        = "terraform output -raw database_credentials"
    get_opensearch_password = "terraform output -raw opensearch_credentials"
    view_secrets            = "aws secretsmanager get-secret-value --secret-id ${aws_secretsmanager_secret.okchat_secrets.name} --region ${var.aws_region}"
  }
}

# ============================================================================
# 비용 정보 (추정치)
# ============================================================================

output "estimated_monthly_cost" {
  description = "월간 예상 비용 (USD) - 대략적인 추정치"
  value = {
    eks_cluster     = "73.00"  # EKS 클러스터: $0.10/hour * 730 hours
    eks_nodes       = "30.00"  # t3.medium * 2: $0.0416/hour * 2 * 730 hours
    rds             = "10.00"  # db.t4g.micro: $0.014/hour * 730 hours
    redis           = "10.00"  # cache.t4g.micro: $0.014/hour * 730 hours
    opensearch      = "25.00"  # t3.small.search: $0.036/hour * 730 hours
    nat_gateway     = "32.00"  # NAT Gateway: $0.045/hour * 730 hours
    data_transfer   = "10.00"  # 예상 데이터 전송 비용
    ebs_storage     = "5.00"   # EBS 스토리지
    total_estimated = "195.00" # 총 예상 비용
    note            = "이 비용은 대략적인 추정치이며 실제 사용량에 따라 달라질 수 있습니다. 개발 환경에서 미사용 시간에는 EKS 노드 수를 0으로 조정하여 비용을 절감할 수 있습니다."
  }
}