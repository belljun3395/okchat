locals {
  name_prefix = "${var.project_name}-${var.environment}"

  # 일반 태그
  common_tags = merge(
    var.common_tags,
    {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )

  # VPC 서브넷 설정
  public_subnet_cidrs   = [for idx, az in var.availability_zones : cidrsubnet(var.vpc_cidr, 8, idx)]
  private_subnet_cidrs  = [for idx, az in var.availability_zones : cidrsubnet(var.vpc_cidr, 8, idx + 10)]
  database_subnet_cidrs = [for idx, az in var.availability_zones : cidrsubnet(var.vpc_cidr, 8, idx + 20)]

  # 환경 변수 매핑 (K8s ConfigMap/Secret에서 사용)
  app_config = {
    SPRING_APPLICATION_NAME                              = "ok-chat"
    SPRING_DATASOURCE_URL                                = "jdbc:mysql://${module.rds.db_instance_endpoint}/okchat?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul"
    SPRING_DATASOURCE_DRIVER_CLASS_NAME                  = "com.mysql.cj.jdbc.Driver"
    SPRING_DATA_REDIS_HOST                               = aws_elasticache_cluster.redis.cache_nodes[0].address
    SPRING_DATA_REDIS_PORT                               = "6379"
    SPRING_AI_VECTORSTORE_OPENSEARCH_HOST                = aws_opensearch_domain.opensearch.endpoint
    SPRING_AI_VECTORSTORE_OPENSEARCH_PORT                = "443"
    SPRING_AI_VECTORSTORE_OPENSEARCH_SCHEME              = "https"
    SPRING_AI_VECTORSTORE_OPENSEARCH_INDEX_NAME          = "vector_store"
    SPRING_AI_VECTORSTORE_OPENSEARCH_EMBEDDING_DIMENSION = "1536"
    SPRING_AI_OPENAI_EMBEDDING_MODEL                     = "text-embedding-3-small"
    SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL                  = "gpt-4.1-mini"
    SPRING_CLOUD_TASK_ENABLED                            = "true"
    SPRING_CLOUD_TASK_INITIALIZE_ENABLED                 = "true"
    SPRING_CLOUD_TASK_TABLE_PREFIX                       = "TASK_"
    SPRING_CLOUD_TASK_SINGLE_INSTANCE_ENABLED            = "true"
    TASK_CONFLUENCE_SYNC_ENABLED                         = "false"
    TASK_EMAIL_POLLING_ENABLED                           = "false"
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE           = "10"
    SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE                = "2"
    SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT          = "30000"
    SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT                = "600000"
    SPRING_DATASOURCE_HIKARI_MAX_LIFETIME                = "1800000"
  }

  app_secrets = {
    SPRING_DATASOURCE_USERNAME                 = var.rds_master_username
    SPRING_DATASOURCE_PASSWORD                 = random_password.rds_password.result
    MYSQL_ROOT_PASSWORD                        = random_password.rds_password.result
    OPENAI_API_KEY                             = var.openai_api_key
    SPRING_AI_VECTORSTORE_OPENSEARCH_USERNAME  = var.opensearch_master_user
    SPRING_AI_VECTORSTORE_OPENSEARCH_PASSWORD  = random_password.opensearch_password.result
    CONFLUENCE_BASE_URL                        = var.confluence_base_url
    CONFLUENCE_EMAIL                           = var.confluence_email
    CONFLUENCE_API_TOKEN                       = var.confluence_api_token
    EMAIL_PROVIDERS_GMAIL_USERNAME             = var.gmail_username
    EMAIL_PROVIDERS_GMAIL_OAUTH2_CLIENT_ID     = var.gmail_oauth2_client_id
    EMAIL_PROVIDERS_GMAIL_OAUTH2_CLIENT_SECRET = var.gmail_oauth2_client_secret
  }
}