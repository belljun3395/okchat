# ============================================================================
# OpenSearch 구성 (비용 최적화)
# ============================================================================

# 모듈 대신 직접 리소스 생성
resource "aws_opensearch_domain" "opensearch" {
  domain_name    = local.name_prefix
  engine_version = var.opensearch_engine_version

  # 클러스터 설정 (비용 최적화)
  cluster_config {
    instance_type            = var.opensearch_instance_type
    instance_count           = var.opensearch_instance_count
    dedicated_master_enabled = false
    zone_awareness_enabled   = false
  }

  # EBS 설정
  ebs_options {
    ebs_enabled = true
    volume_size = var.opensearch_ebs_volume_size
    volume_type = "gp3"
    iops        = 3000
    throughput  = 125
  }

  # 네트워크 설정
  vpc_options {
    subnet_ids         = [module.vpc.database_subnets[0]]
    security_group_ids = [aws_security_group.opensearch.id]
  }

  # 암호화 설정
  encrypt_at_rest {
    enabled = true
  }

  node_to_node_encryption {
    enabled = true
  }

  # 도메인 엔드포인트 설정
  domain_endpoint_options {
    enforce_https       = true
    tls_security_policy = "Policy-Min-TLS-1-2-2019-07"
  }

  # Fine-grained access control
  advanced_security_options {
    enabled                        = true
    anonymous_auth_enabled         = false
    internal_user_database_enabled = true
    master_user_options {
      master_user_name     = var.opensearch_master_user
      master_user_password = random_password.opensearch_password.result
    }
  }

  # 고급 옵션
  advanced_options = {
    "rest.action.multi.allow_explicit_index" = "true"
    "override_main_response_version"         = "false"
  }

  # 로그 전송 설정
  log_publishing_options {
    log_type                 = "ES_APPLICATION_LOGS"
    cloudwatch_log_group_arn = aws_cloudwatch_log_group.opensearch_app_logs.arn
  }

  # 자동 스냅샷 설정
  snapshot_options {
    automated_snapshot_start_hour = 3
  }

  # 업데이트 설정
  auto_tune_options {
    desired_state = "DISABLED"
  }

  depends_on = [aws_cloudwatch_log_resource_policy.opensearch_logs]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-opensearch"
    }
  )
}

# CloudWatch 로그 그룹 (OpenSearch 로그용)
resource "aws_cloudwatch_log_group" "opensearch_app_logs" {
  name              = "/aws/opensearch/${local.name_prefix}/application"
  retention_in_days = 7

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-opensearch-logs"
    }
  )
}

# CloudWatch 로그 그룹 리소스 정책
resource "aws_cloudwatch_log_resource_policy" "opensearch_logs" {
  policy_name = "${local.name_prefix}-opensearch-logs"

  policy_document = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "es.amazonaws.com"
        }
        Action = [
          "logs:PutLogEvents",
          "logs:CreateLogStream"
        ]
        Resource = "${aws_cloudwatch_log_group.opensearch_app_logs.arn}:*"
      }
    ]
  })
}