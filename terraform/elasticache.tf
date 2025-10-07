# ============================================================================
# ElastiCache Redis 구성 (비용 최적화)
# ============================================================================

# 모듈 대신 직접 리소스 생성 (더 많은 제어 가능)
resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "${local.name_prefix}-redis"
  engine               = "redis"
  engine_version       = var.redis_engine_version
  node_type            = var.redis_node_type
  num_cache_nodes      = var.redis_num_cache_nodes
  parameter_group_name = aws_elasticache_parameter_group.redis.name
  subnet_group_name    = aws_elasticache_subnet_group.redis.name
  security_group_ids   = [aws_security_group.redis.id]
  port                 = 6379
  az_mode              = "single-az"

  # 백업 설정
  snapshot_retention_limit = 1
  snapshot_window          = "03:00-04:00"
  maintenance_window       = "mon:04:00-mon:05:00"

  # 자동 마이너 버전 업그레이드
  auto_minor_version_upgrade = true

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-redis"
    }
  )
}

# ElastiCache 서브넷 그룹
resource "aws_elasticache_subnet_group" "redis" {
  name       = "${local.name_prefix}-redis-subnet-group"
  subnet_ids = module.vpc.database_subnets

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-redis-subnet-group"
    }
  )
}

# ElastiCache 파라미터 그룹
resource "aws_elasticache_parameter_group" "redis" {
  name   = "${local.name_prefix}-redis-params"
  family = var.redis_parameter_group_family

  # Redis 설정 (애플리케이션 요구사항에 맞춤)
  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }

  parameter {
    name  = "timeout"
    value = "300"
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-redis-params"
    }
  )
}