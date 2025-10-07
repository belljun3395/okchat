# ============================================================================
# AWS Secrets Manager (선택적 사용)
# ============================================================================

# 랜덤 패스워드 생성
resource "random_password" "rds_password" {
  length  = 16
  special = true
  # MySQL에서 문제될 수 있는 특수문자 제외
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "random_password" "opensearch_password" {
  length           = 16
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

# Secrets Manager에 시크릿 저장
resource "aws_secretsmanager_secret" "okchat_secrets" {
  name_prefix             = "${local.name_prefix}-secrets-"
  description             = "OkChat application secrets"
  recovery_window_in_days = var.environment == "prod" ? 30 : 0

  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "okchat_secrets" {
  secret_id = aws_secretsmanager_secret.okchat_secrets.id
  secret_string = jsonencode({
    # Database
    SPRING_DATASOURCE_USERNAME = var.rds_master_username
    SPRING_DATASOURCE_PASSWORD = random_password.rds_password.result
    MYSQL_ROOT_PASSWORD        = random_password.rds_password.result

    # OpenAI
    OPENAI_API_KEY = var.openai_api_key

    # OpenSearch
    SPRING_AI_VECTORSTORE_OPENSEARCH_USERNAME = var.opensearch_master_user
    SPRING_AI_VECTORSTORE_OPENSEARCH_PASSWORD = random_password.opensearch_password.result

    # Confluence (optional)
    CONFLUENCE_BASE_URL  = var.confluence_base_url
    CONFLUENCE_EMAIL     = var.confluence_email
    CONFLUENCE_API_TOKEN = var.confluence_api_token

    # Gmail (optional)
    EMAIL_PROVIDERS_GMAIL_USERNAME             = var.gmail_username
    EMAIL_PROVIDERS_GMAIL_OAUTH2_CLIENT_ID     = var.gmail_oauth2_client_id
    EMAIL_PROVIDERS_GMAIL_OAUTH2_CLIENT_SECRET = var.gmail_oauth2_client_secret
  })
}