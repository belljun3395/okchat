# ============================================================================
# 프로젝트 기본 설정
# ============================================================================
variable "project_name" {
  description = "프로젝트 이름"
  type        = string
  default     = "okchat"
}

variable "environment" {
  description = "환경 (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

# ============================================================================
# VPC 설정
# ============================================================================
variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "사용할 가용 영역 (비용 절감을 위해 2개만 사용)"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2c"]
}

# ============================================================================
# EKS 설정
# ============================================================================
variable "eks_cluster_version" {
  description = "EKS 클러스터 버전"
  type        = string
  default     = "1.31"
}

variable "eks_node_instance_types" {
  description = "EKS 노드 인스턴스 타입 (비용 절감을 위해 t3.medium 사용)"
  type        = list(string)
  default     = ["t3.medium"]
}

variable "eks_node_capacity_type" {
  description = "EKS 노드 용량 타입 (ON_DEMAND 또는 SPOT)"
  type        = string
  default     = "SPOT"
}

variable "eks_node_desired_size" {
  description = "EKS 노드 desired 개수"
  type        = number
  default     = 2
}

variable "eks_node_min_size" {
  description = "EKS 노드 최소 개수"
  type        = number
  default     = 1
}

variable "eks_node_max_size" {
  description = "EKS 노드 최대 개수"
  type        = number
  default     = 3
}

variable "eks_node_disk_size" {
  description = "EKS 노드 디스크 크기 (GB)"
  type        = number
  default     = 30
}

# ============================================================================
# 애플리케이션 설정
# ============================================================================
variable "app_replica_count" {
  description = "애플리케이션 Replica 개수"
  type        = number
  default     = 2
}

variable "hpa_min_replicas" {
  description = "HPA 최소 Replica 개수"
  type        = number
  default     = 2
}

variable "hpa_max_replicas" {
  description = "HPA 최대 Replica 개수"
  type        = number
  default     = 5
}

# ============================================================================
# RDS MySQL 설정
# ============================================================================
variable "rds_instance_class" {
  description = "RDS 인스턴스 타입 (비용 절감을 위해 t4g.micro 사용)"
  type        = string
  default     = "db.t4g.micro"
}

variable "rds_allocated_storage" {
  description = "RDS 스토리지 크기 (GB)"
  type        = number
  default     = 20
}

variable "rds_max_allocated_storage" {
  description = "RDS 자동 스케일링 최대 스토리지 (GB)"
  type        = number
  default     = 50
}

variable "rds_database_name" {
  description = "RDS 데이터베이스 이름"
  type        = string
  default     = "okchat"
}

variable "rds_master_username" {
  description = "RDS 마스터 사용자명"
  type        = string
  default     = "admin"
  sensitive   = true
}

variable "rds_backup_retention_period" {
  description = "RDS 백업 보관 기간 (일)"
  type        = number
  default     = 3
}

variable "rds_skip_final_snapshot" {
  description = "RDS 삭제 시 최종 스냅샷 생략 (개발 환경에서만 true)"
  type        = bool
  default     = true
}

# ============================================================================
# ElastiCache Redis 설정
# ============================================================================
variable "redis_node_type" {
  description = "Redis 노드 타입 (비용 절감을 위해 t4g.micro 사용)"
  type        = string
  default     = "cache.t4g.micro"
}

variable "redis_num_cache_nodes" {
  description = "Redis 노드 개수 (개발 환경이므로 1개)"
  type        = number
  default     = 1
}

variable "redis_parameter_group_family" {
  description = "Redis 파라미터 그룹 패밀리"
  type        = string
  default     = "redis7"
}

variable "redis_engine_version" {
  description = "Redis 엔진 버전"
  type        = string
  default     = "7.1"
}

# ============================================================================
# OpenSearch 설정
# ============================================================================
variable "opensearch_instance_type" {
  description = "OpenSearch 인스턴스 타입 (비용 절감을 위해 t3.small.search 사용)"
  type        = string
  default     = "t3.small.search"
}

variable "opensearch_instance_count" {
  description = "OpenSearch 인스턴스 개수"
  type        = number
  default     = 1
}

variable "opensearch_ebs_volume_size" {
  description = "OpenSearch EBS 볼륨 크기 (GB)"
  type        = number
  default     = 20
}

variable "opensearch_engine_version" {
  description = "OpenSearch 엔진 버전"
  type        = string
  default     = "OpenSearch_2.11"
}

variable "opensearch_master_user" {
  description = "OpenSearch 마스터 사용자명"
  type        = string
  default     = "admin"
  sensitive   = true
}

# ============================================================================
# 시크릿 설정
# ============================================================================
variable "openai_api_key" {
  description = "OpenAI API Key"
  type        = string
  sensitive   = true
}

variable "confluence_base_url" {
  description = "Confluence Base URL (선택사항)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "confluence_email" {
  description = "Confluence Email (선택사항)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "confluence_api_token" {
  description = "Confluence API Token (선택사항)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "gmail_username" {
  description = "Gmail Username (선택사항)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "gmail_oauth2_client_id" {
  description = "Gmail OAuth2 Client ID (선택사항)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "gmail_oauth2_client_secret" {
  description = "Gmail OAuth2 Client Secret (선택사항)"
  type        = string
  default     = ""
  sensitive   = true
}

# ============================================================================
# 태그
# ============================================================================
variable "common_tags" {
  description = "모든 리소스에 적용할 공통 태그"
  type        = map(string)
  default = {
    Owner = "DevOps Team"
  }
}