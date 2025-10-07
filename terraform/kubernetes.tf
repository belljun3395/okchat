# ============================================================================
# Kubernetes 리소스 구성 (환경 변수 매핑)
# ============================================================================

# Namespace 생성
resource "kubernetes_namespace" "okchat" {
  metadata {
    name = "okchat"
    labels = {
      name        = "okchat"
      environment = var.environment
    }
  }

  depends_on = [module.eks]
}

# ConfigMap 생성 (환경 변수 매핑)
resource "kubernetes_config_map" "okchat" {
  metadata {
    name      = "okchat-config"
    namespace = kubernetes_namespace.okchat.metadata[0].name
  }

  data = local.app_config

  depends_on = [kubernetes_namespace.okchat]
}

# Secret 생성 (민감한 정보)
resource "kubernetes_secret" "okchat" {
  metadata {
    name      = "okchat-secret"
    namespace = kubernetes_namespace.okchat.metadata[0].name
  }

  data = {
    for k, v in local.app_secrets : k => tostring(v)
  }

  type = "Opaque"

  depends_on = [kubernetes_namespace.okchat]
}

# MySQL 초기화 스크립트 ConfigMap (선택사항)
resource "kubernetes_config_map" "mysql_init" {
  count = fileexists("${path.module}/../resources/mysql-init.d/init.sql") ? 1 : 0

  metadata {
    name      = "mysql-init-scripts"
    namespace = kubernetes_namespace.okchat.metadata[0].name
  }

  data = {
    "init.sql" = file("${path.module}/../resources/mysql-init.d/init.sql")
  }

  depends_on = [kubernetes_namespace.okchat]
}

# Deployment 생성
resource "kubernetes_deployment" "okchat" {
  metadata {
    name      = "okchat-app"
    namespace = kubernetes_namespace.okchat.metadata[0].name
    labels = {
      app       = "okchat-app"
      component = "application"
    }
  }

  spec {
    replicas = var.app_replica_count

    selector {
      match_labels = {
        app = "okchat-app"
      }
    }

    template {
      metadata {
        labels = {
          app       = "okchat-app"
          component = "application"
        }
        annotations = {
          "prometheus.io/scrape" = "true"
          "prometheus.io/port"   = "8080"
          "prometheus.io/path"   = "/actuator/prometheus"
        }
      }

      spec {
        # Init Container는 AWS Managed Services를 사용하므로 필요 없음

        container {
          name              = "okchat"
          image             = "${aws_ecr_repository.okchat.repository_url}:latest"
          image_pull_policy = "Always"

          port {
            container_port = 8080
            name           = "http"
            protocol       = "TCP"
          }

          # 환경 변수 - ConfigMap에서 가져오기
          dynamic "env" {
            for_each = local.app_config
            content {
              name = env.key
              value_from {
                config_map_key_ref {
                  name = kubernetes_config_map.okchat.metadata[0].name
                  key  = env.key
                }
              }
            }
          }

          # 환경 변수 - Secret에서 가져오기
          dynamic "env" {
            for_each = local.app_secrets
            content {
              name = env.key
              value_from {
                secret_key_ref {
                  name = kubernetes_secret.okchat.metadata[0].name
                  key  = env.key
                }
              }
            }
          }

          # Spring Profile
          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "prod"
          }

          # Java Options
          env {
            name  = "JAVA_OPTS"
            value = "-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
          }

          # Liveness Probe
          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = 8080
            }
            initial_delay_seconds = 120
            period_seconds        = 10
            timeout_seconds       = 5
            failure_threshold     = 3
          }

          # Readiness Probe
          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = 8080
            }
            initial_delay_seconds = 60
            period_seconds        = 5
            timeout_seconds       = 3
            failure_threshold     = 3
          }

          # Resources
          resources {
            requests = {
              memory = "1Gi"
              cpu    = "500m"
            }
            limits = {
              memory = "3Gi"
              cpu    = "2000m"
            }
          }

          # Volume Mounts
          volume_mount {
            name       = "logs"
            mount_path = "/app/logs"
          }
        }

        # Volumes
        volume {
          name = "logs"
          empty_dir {}
        }
      }
    }
  }

  depends_on = [
    kubernetes_config_map.okchat,
    kubernetes_secret.okchat,
    module.rds,
    aws_elasticache_cluster.redis,
    aws_opensearch_domain.opensearch
  ]
}

# Service 생성
resource "kubernetes_service" "okchat" {
  metadata {
    name      = "okchat-app"
    namespace = kubernetes_namespace.okchat.metadata[0].name
    labels = {
      app       = "okchat-app"
      component = "application"
    }
  }

  spec {
    type = "ClusterIP"

    port {
      port        = 8080
      target_port = 8080
      protocol    = "TCP"
      name        = "http"
    }

    selector = {
      app = "okchat-app"
    }
  }

  depends_on = [kubernetes_deployment.okchat]
}

# Headless Service 생성
resource "kubernetes_service" "okchat_headless" {
  metadata {
    name      = "okchat-app-headless"
    namespace = kubernetes_namespace.okchat.metadata[0].name
    labels = {
      app       = "okchat-app"
      component = "application"
    }
  }

  spec {
    type       = "ClusterIP"
    cluster_ip = "None"

    port {
      port        = 8080
      target_port = 8080
      protocol    = "TCP"
      name        = "http"
    }

    selector = {
      app = "okchat-app"
    }
  }

  depends_on = [kubernetes_deployment.okchat]
}

# Ingress 생성 (ALB)
resource "kubernetes_ingress_v1" "okchat" {
  metadata {
    name      = "okchat-ingress"
    namespace = kubernetes_namespace.okchat.metadata[0].name
    annotations = {
      "kubernetes.io/ingress.class"                = "alb"
      "alb.ingress.kubernetes.io/scheme"           = "internet-facing"
      "alb.ingress.kubernetes.io/target-type"      = "ip"
      "alb.ingress.kubernetes.io/listen-ports"     = jsonencode([{ HTTP = 80 }])
      "alb.ingress.kubernetes.io/healthcheck-path" = "/actuator/health"
      "alb.ingress.kubernetes.io/tags"             = "Environment=${var.environment},Project=${var.project_name}"
    }
  }

  spec {
    rule {
      http {
        path {
          path      = "/*"
          path_type = "ImplementationSpecific"

          backend {
            service {
              name = kubernetes_service.okchat.metadata[0].name
              port {
                number = 8080
              }
            }
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_service.okchat,
    helm_release.aws_load_balancer_controller
  ]
}

# HorizontalPodAutoscaler 생성
resource "kubernetes_horizontal_pod_autoscaler_v2" "okchat" {
  metadata {
    name      = "okchat-app-hpa"
    namespace = kubernetes_namespace.okchat.metadata[0].name
  }

  spec {
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment.okchat.metadata[0].name
    }

    min_replicas = var.hpa_min_replicas
    max_replicas = var.hpa_max_replicas

    metric {
      type = "Resource"
      resource {
        name = "cpu"
        target {
          type                = "Utilization"
          average_utilization = 70
        }
      }
    }

    metric {
      type = "Resource"
      resource {
        name = "memory"
        target {
          type                = "Utilization"
          average_utilization = 80
        }
      }
    }
  }

  depends_on = [
    kubernetes_deployment.okchat,
    helm_release.metrics_server
  ]
}