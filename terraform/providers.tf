terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.33"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.15"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Terraform Cloud 백엔드 (권장)
  cloud {
    organization = "your-org-name"  # Terraform Cloud 조직명으로 변경하세요
    
    workspaces {
      name = "okchat-dev"  # 워크스페이스 이름
    }
  }

  # 또는 S3 백엔드 (대안)
  # backend "s3" {
  #   bucket         = "your-terraform-state-bucket"
  #   key            = "okchat/dev/terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   dynamodb_table = "terraform-lock"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)

  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args = [
      "eks",
      "get-token",
      "--cluster-name",
      module.eks.cluster_name,
      "--region",
      var.aws_region,
    ]
  }
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)

    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args = [
        "eks",
        "get-token",
        "--cluster-name",
        module.eks.cluster_name,
        "--region",
        var.aws_region,
      ]
    }
  }
}