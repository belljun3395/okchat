# ============================================================================
# ECR (Elastic Container Registry)
# ============================================================================

resource "aws_ecr_repository" "okchat" {
  name                 = "${local.name_prefix}-app"
  image_tag_mutability = "MUTABLE"

  # 이미지 스캔 설정
  image_scanning_configuration {
    scan_on_push = true
  }

  # 암호화 설정
  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-ecr"
    }
  )
}

# ECR 라이프사이클 정책 (비용 절감을 위해 오래된 이미지 자동 삭제)
resource "aws_ecr_lifecycle_policy" "okchat" {
  repository = aws_ecr_repository.okchat.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 images"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["v"]
          countType     = "imageCountMoreThan"
          countNumber   = 10
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Remove untagged images after 7 days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 7
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# ECR 레포지토리 정책 (EKS 노드에서 접근 허용)
resource "aws_ecr_repository_policy" "okchat" {
  repository = aws_ecr_repository.okchat.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowPullFromEKS"
        Effect = "Allow"
        Principal = {
          AWS = data.aws_iam_role.eks_node_group.arn
        }
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer"
        ]
      }
    ]
  })

  depends_on = [data.aws_iam_role.eks_node_group]
}