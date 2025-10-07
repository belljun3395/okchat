# ============================================================================
# OkChat AWS 개발 환경 Terraform 구성
# ============================================================================
# 
# 이 Terraform 구성은 OkChat 애플리케이션을 위한 AWS 개발 환경을 생성합니다.
# 비용 최적화를 위해 다음과 같은 전략을 사용합니다:
#
# 1. 최소 인스턴스 크기 사용:
#    - RDS: db.t4g.micro
#    - ElastiCache: cache.t4g.micro
#    - OpenSearch: t3.small.search
#    - EKS 노드: t3.medium
#
# 2. 단일 AZ 배포 (RDS, Redis, OpenSearch)
# 3. 단일 NAT Gateway
# 4. 자동 백업 최소화
# 5. 불필요한 기능 비활성화 (Performance Insights, Auto-Tune 등)
#
# 환경 변수 매핑:
# - application.yaml의 모든 환경 변수를 ConfigMap/Secret으로 매핑
# - AWS Secrets Manager에 민감 정보 저장
# - Kubernetes Secret으로 Pod에 주입
#
# ============================================================================

# 이 파일은 메인 진입점입니다.
# 실제 리소스는 다음 파일들에서 정의됩니다:
#
# - providers.tf      : Terraform 및 Provider 설정
# - variables.tf      : 변수 정의
# - locals.tf         : 로컬 변수 및 환경 변수 매핑
# - vpc.tf            : VPC 및 네트워크 구성
# - security_groups.tf: Security Groups
# - iam.tf            : IAM 역할 및 정책
# - secrets.tf        : 비밀번호 생성 및 Secrets Manager
# - eks.tf            : EKS 클러스터
# - rds.tf            : RDS MySQL
# - elasticache.tf    : ElastiCache Redis
# - opensearch.tf     : OpenSearch
# - ecr.tf            : ECR 레포지토리
# - kubernetes.tf     : Kubernetes 리소스 (Namespace, ConfigMap, Secret, Deployment 등)
# - outputs.tf        : 출력 값
#
# ============================================================================