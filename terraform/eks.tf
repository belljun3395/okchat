# ============================================================================
# EKS 클러스터 구성 (비용 최적화)
# ============================================================================

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = local.name_prefix
  cluster_version = var.eks_cluster_version

  # 클러스터 엔드포인트 접근 설정
  cluster_endpoint_public_access  = true
  cluster_endpoint_private_access = true

  # 비용 절감: 클러스터 로깅은 필요한 것만 활성화
  cluster_enabled_log_types = ["audit", "api"]

  # VPC 설정
  vpc_id                   = module.vpc.vpc_id
  subnet_ids               = module.vpc.private_subnets
  control_plane_subnet_ids = module.vpc.private_subnets

  # 클러스터 애드온
  cluster_addons = {
    coredns = {
      most_recent = true
    }
    kube-proxy = {
      most_recent = true
    }
    vpc-cni = {
      most_recent = true
    }
    aws-ebs-csi-driver = {
      most_recent              = true
      service_account_role_arn = module.ebs_csi_irsa_role.iam_role_arn
    }
  }

  # EKS Managed Node Group (비용 최적화)
  eks_managed_node_groups = {
    main = {
      name = "${local.name_prefix}-node-group"

      instance_types = var.eks_node_instance_types
      capacity_type  = "SPOT" # 시나리오 3: 비용 절감을 위해 SPOT 인스턴스 사용

      min_size     = var.eks_node_min_size
      max_size     = var.eks_node_max_size
      desired_size = var.eks_node_desired_size

      # 디스크 설정
      block_device_mappings = {
        xvda = {
          device_name = "/dev/xvda"
          ebs = {
            volume_size           = var.eks_node_disk_size
            volume_type           = "gp3"
            iops                  = 3000
            throughput            = 125
            encrypted             = true
            delete_on_termination = true
          }
        }
      }

      # 노드 라벨
      labels = {
        Environment = var.environment
        NodeGroup   = "main"
      }

      # 노드 태그
      tags = merge(
        local.common_tags,
        {
          Name = "${local.name_prefix}-node"
        }
      )
    }
  }

  # 노드 보안 그룹 추가 규칙
  node_security_group_additional_rules = {
    ingress_self_all = {
      description = "Node to node all ports/protocols"
      protocol    = "-1"
      from_port   = 0
      to_port     = 0
      type        = "ingress"
      self        = true
    }
    ingress_cluster_all = {
      description                   = "Cluster to node all ports/protocols"
      protocol                      = "-1"
      from_port                     = 0
      to_port                       = 0
      type                          = "ingress"
      source_cluster_security_group = true
    }
    egress_all = {
      description      = "Node all egress"
      protocol         = "-1"
      from_port        = 0
      to_port          = 0
      type             = "egress"
      cidr_blocks      = ["0.0.0.0/0"]
      ipv6_cidr_blocks = ["::/0"]
    }
  }

  tags = local.common_tags
}

# ============================================================================
# EBS CSI Driver StorageClass
# ============================================================================

resource "kubernetes_storage_class_v1" "ebs_gp3" {
  metadata {
    name = "ebs-gp3"
    annotations = {
      "storageclass.kubernetes.io/is-default-class" = "true"
    }
  }

  storage_provisioner    = "ebs.csi.aws.com"
  allow_volume_expansion = true
  volume_binding_mode    = "WaitForFirstConsumer"

  parameters = {
    type       = "gp3"
    encrypted  = "true"
    iops       = "3000"
    throughput = "125"
  }

  depends_on = [module.eks]
}

# ============================================================================
# AWS Load Balancer Controller (Helm)
# ============================================================================

resource "helm_release" "aws_load_balancer_controller" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  namespace  = "kube-system"
  version    = "1.8.0"

  set {
    name  = "clusterName"
    value = module.eks.cluster_name
  }

  set {
    name  = "serviceAccount.create"
    value = "true"
  }

  set {
    name  = "serviceAccount.name"
    value = "aws-load-balancer-controller"
  }

  set {
    name  = "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = module.lb_controller_irsa_role.iam_role_arn
  }

  set {
    name  = "region"
    value = var.aws_region
  }

  set {
    name  = "vpcId"
    value = module.vpc.vpc_id
  }

  depends_on = [module.eks]
}

# ============================================================================
# Metrics Server (Helm) - HPA를 위해 필요
# ============================================================================

resource "helm_release" "metrics_server" {
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  namespace  = "kube-system"
  version    = "3.12.0"

  set {
    name  = "args[0]"
    value = "--kubelet-insecure-tls"
  }

  depends_on = [module.eks]
}