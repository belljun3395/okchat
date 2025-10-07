# ============================================================================
# VPC 및 네트워크 구성
# ============================================================================

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${local.name_prefix}-vpc"
  cidr = var.vpc_cidr

  azs              = var.availability_zones
  public_subnets   = local.public_subnet_cidrs
  private_subnets  = local.private_subnet_cidrs
  database_subnets = local.database_subnet_cidrs

  # NAT Gateway 설정 (비용 절감을 위해 단일 NAT Gateway 사용)
  enable_nat_gateway   = true
  single_nat_gateway   = true # 비용 절감: 단일 NAT Gateway
  enable_dns_hostnames = true
  enable_dns_support   = true

  # VPC 엔드포인트 설정 (NAT Gateway 비용 절감)
  enable_s3_endpoint = true

  # EKS 클러스터를 위한 태그
  public_subnet_tags = {
    "kubernetes.io/role/elb"                     = "1"
    "kubernetes.io/cluster/${local.name_prefix}" = "shared"
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb"            = "1"
    "kubernetes.io/cluster/${local.name_prefix}" = "shared"
  }

  database_subnet_tags = {
    "database" = "true"
  }

  tags = local.common_tags
}

# ============================================================================
# VPC 엔드포인트 (비용 절감 - ECR과 CloudWatch Logs용)
# ============================================================================

# ECR API 엔드포인트
resource "aws_vpc_endpoint" "ecr_api" {
  vpc_id              = module.vpc.vpc_id
  service_name        = "com.amazonaws.${var.aws_region}.ecr.api"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-ecr-api-endpoint"
    }
  )
}

# ECR Docker 엔드포인트
resource "aws_vpc_endpoint" "ecr_dkr" {
  vpc_id              = module.vpc.vpc_id
  service_name        = "com.amazonaws.${var.aws_region}.ecr.dkr"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-ecr-dkr-endpoint"
    }
  )
}

# CloudWatch Logs 엔드포인트
resource "aws_vpc_endpoint" "logs" {
  vpc_id              = module.vpc.vpc_id
  service_name        = "com.amazonaws.${var.aws_region}.logs"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-logs-endpoint"
    }
  )
}

# STS 엔드포인트 (EKS용)
resource "aws_vpc_endpoint" "sts" {
  vpc_id              = module.vpc.vpc_id
  service_name        = "com.amazonaws.${var.aws_region}.sts"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-sts-endpoint"
    }
  )
}