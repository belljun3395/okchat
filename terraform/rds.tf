# ============================================================================
# RDS MySQL 구성 (비용 최적화)
# ============================================================================

module "rds" {
  source  = "terraform-aws-modules/rds/aws"
  version = "~> 6.0"

  identifier = "${local.name_prefix}-mysql"

  # 엔진 설정
  engine               = "mysql"
  engine_version       = "8.0"
  family               = "mysql8.0"
  major_engine_version = "8.0"
  instance_class       = var.rds_instance_class

  # 스토리지 설정 (비용 최적화)
  allocated_storage     = var.rds_allocated_storage
  max_allocated_storage = var.rds_max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true
  iops                  = 3000

  # 데이터베이스 설정
  db_name  = var.rds_database_name
  username = var.rds_master_username
  password = random_password.rds_password.result
  port     = 3306

  # 네트워크 설정
  db_subnet_group_name   = module.vpc.database_subnet_group_name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # Multi-AZ 설정 (비용 절감을 위해 개발 환경에서는 비활성화)
  multi_az = false

  # 백업 설정
  backup_retention_period          = var.rds_backup_retention_period
  backup_window                    = "03:00-04:00"
  maintenance_window               = "mon:04:00-mon:05:00"
  skip_final_snapshot              = var.rds_skip_final_snapshot
  final_snapshot_identifier_prefix = var.rds_skip_final_snapshot ? null : "${local.name_prefix}-final-snapshot"

  # 모니터링 설정
  enabled_cloudwatch_logs_exports        = ["error", "general", "slowquery"]
  create_cloudwatch_log_group            = true
  cloudwatch_log_group_retention_in_days = 7

  # Performance Insights (비용 절감을 위해 비활성화)
  performance_insights_enabled = false

  # 파라미터 그룹 설정
  parameters = [
    {
      name  = "character_set_server"
      value = "utf8mb4"
    },
    {
      name  = "collation_server"
      value = "utf8mb4_unicode_ci"
    },
    {
      name  = "lower_case_table_names"
      value = "1"
    },
    {
      name  = "max_connections"
      value = "2048"
    },
    {
      name  = "wait_timeout"
      value = "3600"
    },
    {
      name  = "interactive_timeout"
      value = "3600"
    },
    {
      name  = "max_allowed_packet"
      value = "67108864" # 64MB
    }
  ]

  # 옵션 그룹 설정
  options = []

  # 자동 마이너 버전 업그레이드
  auto_minor_version_upgrade = true

  # 삭제 방지 (개발 환경에서는 비활성화)
  deletion_protection = false

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-mysql"
    }
  )
}

# RDS 초기화 스크립트 실행을 위한 Lambda (선택사항)
# 실제 초기화는 애플리케이션에서 Spring Boot가 처리하므로 생략