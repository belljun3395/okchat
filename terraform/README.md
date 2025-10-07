# OkChat AWS Terraform 구성

비용 최적화된 AWS 개발 환경을 위한 Terraform 구성입니다.

## 📋 목차

- [빠른 시작](#빠른-시작)
- [아키텍처](#아키텍처)
- [비용 정보](#비용-정보)
- [구성 요소](#구성-요소)
- [배포 가이드](#배포-가이드)
- [시나리오별 설정](#시나리오별-설정)
- [문제 해결](#문제-해결)

## 🚀 빠른 시작

```bash
# 1. 설정 파일 생성
cd terraform
cp terraform.tfvars.example terraform.tfvars
vi terraform.tfvars  # OpenAI API Key 설정 필수!

# 2. 배포
./scripts/deploy.sh

# 3. kubectl 설정
aws eks update-kubeconfig --region ap-northeast-2 --name okchat-dev

# 4. 확인
kubectl get pods -n okchat
```

> 💡 더 상세한 가이드는 [QUICK_START.md](./QUICK_START.md) 참조

## 🏗 아키텍처

```
┌─────────────────────────────────────────────┐
│                 Internet                     │
└──────────────────┬──────────────────────────┘
                   │
            ┌──────▼──────┐
            │     ALB     │
            └──────┬──────┘
                   │
┌──────────────────┼──────────────────────────┐
│              VPC │                           │
│  ┌───────────────▼─────────────────────┐   │
│  │      Private Subnet (2 AZ)          │   │
│  │  ┌────────────────────────────┐     │   │
│  │  │       EKS Cluster          │     │   │
│  │  │  ┌──────────┐ ┌──────────┐│     │   │
│  │  │  │ OkChat   │ │ OkChat   ││     │   │
│  │  │  │ Pod (1-2)│ │ Pod (1-2)││     │   │
│  │  │  └──────────┘ └──────────┘│     │   │
│  │  └────────────────────────────┘     │   │
│  └──────────────────────────────────────┘  │
│                   │                          │
│  ┌────────────────▼───────────────────────┐│
│  │       Database Subnet (2 AZ)           ││
│  │  ┌───────┐ ┌───────┐ ┌──────────┐    ││
│  │  │  RDS  │ │ Redis │ │OpenSearch│    ││
│  │  │ MySQL │ │       │ │          │    ││
│  │  └───────┘ └───────┘ └──────────┘    ││
│  └────────────────────────────────────────┘│
└─────────────────────────────────────────────┘
```

## 💰 비용 정보

### 시나리오별 월간 비용

| 시나리오 | 구성 | 월 비용 | 용도 |
|---------|------|---------|------|
| **기본** | ON_DEMAND x2 | $200 | 프로덕션 준비 |
| **시나리오 3** ⭐ | **SPOT x2** | **$174** | **개발/테스트 (권장)** |
| **시나리오 4** | SPOT x1 | $171 | 개인 학습 |
| **업무시간만** | SPOT x2 (10h/일) | $150 | 파트타임 |

### 비용 구성 (시나리오 3 기준)

| 항목 | 스펙 | 월 비용 |
|------|------|---------|
| EKS 클러스터 | - | $73 |
| **EKS 노드 (Spot)** | **t3.medium x2** | **$6-9** |
| RDS MySQL | db.t4g.micro | $10 |
| ElastiCache Redis | cache.t4g.micro | $10 |
| OpenSearch | t3.small.search | $26 |
| NAT Gateway | 단일 | $33 |
| ALB + 기타 | - | $18 |
| **총계** | | **$176-179** |

### 비용 절감 전략

1. **Spot 인스턴스**: 70-90% 절감 (적용됨 ✅)
2. **ARM Graviton2**: 20% 추가 절감 (적용됨 ✅)
3. **단일 AZ**: Multi-AZ 비용 제거 (적용됨 ✅)
4. **단일 NAT Gateway**: 절반 비용 (적용됨 ✅)
5. **업무 시간만 운영**: 추가 $20-30/월 절감 가능
6. **미사용 시 삭제**: 전체 비용 절감

## 📦 구성 요소

### 인프라 리소스

#### 네트워크 (vpc.tf, security_groups.tf)
- VPC: 10.0.0.0/16
- Public/Private/Database 서브넷 (2 AZ)
- 단일 NAT Gateway (비용 절감)
- VPC Endpoints: ECR, S3, CloudWatch, STS

#### 컴퓨팅 (eks.tf)
- EKS 1.31 클러스터
- **Spot 인스턴스**: t3.medium x2 (70-90% 절감)
- Auto Scaling: 1-3 노드
- AWS Load Balancer Controller
- Metrics Server (HPA)

#### 데이터베이스 (rds.tf)
- RDS MySQL 8.0
- **db.t4g.micro** (ARM Graviton2)
- 20GB gp3 스토리지
- 단일 AZ (비용 절감)
- 자동 백업 3일

#### 캐시 (elasticache.tf)
- Redis 7.1
- **cache.t4g.micro** (ARM Graviton2)
- 단일 노드
- 단일 AZ

#### 검색 (opensearch.tf)
- OpenSearch 2.11
- **t3.small.search**
- 20GB gp3 EBS
- Fine-grained access control
- HTTPS 강제

#### 컨테이너 레지스트리 (ecr.tf)
- ECR 프라이빗 레포지토리
- 이미지 스캔 활성화
- 라이프사이클 정책 (10개 유지)

#### 보안 (secrets.tf, iam.tf)
- AWS Secrets Manager
- 자동 비밀번호 생성
- IRSA (IAM Roles for Service Accounts)
- 최소 권한 원칙

### Kubernetes 리소스 (kubernetes.tf)

- **Namespace**: okchat
- **ConfigMap**: 환경 변수 (비민감 정보)
- **Secret**: 민감 정보 (RDS, Redis, OpenSearch, OpenAI API Key)
- **Deployment**: 애플리케이션 (Replicas: 1-2)
- **Service**: ClusterIP + Headless
- **Ingress**: ALB (internet-facing)
- **HPA**: CPU/메모리 기반 자동 스케일링

### 환경 변수 자동 매핑 (locals.tf)

모든 환경 변수가 AWS 리소스에서 자동으로 매핑됩니다:

```hcl
# 예시
SPRING_DATASOURCE_URL = "jdbc:mysql://${RDS_ENDPOINT}/okchat?..."
SPRING_DATA_REDIS_HOST = ${REDIS_ENDPOINT}
SPRING_AI_VECTORSTORE_OPENSEARCH_HOST = ${OPENSEARCH_ENDPOINT}
```

## 📝 배포 가이드

### 사전 요구사항

1. **필수 도구**
   - AWS CLI v2
   - Terraform >= 1.9
   - kubectl >= 1.31
   - Docker (이미지 빌드용)

2. **AWS 권한**
   - AdministratorAccess 또는
   - VPC, EC2, EKS, RDS, ElastiCache, OpenSearch, ECR, IAM, Secrets Manager 권한

3. **필수 정보**
   - OpenAI API Key (필수)
   - Confluence 자격증명 (선택)
   - Gmail OAuth2 자격증명 (선택)

### 배포 단계

#### 1. 설정 파일 생성

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
vi terraform.tfvars
```

**필수 수정**:
```hcl
openai_api_key = "sk-proj-your-actual-api-key-here"  # 필수!
```

#### 2. Terraform 초기화

```bash
terraform init
```

#### 3. 배포 계획 확인

```bash
terraform plan
```

생성될 리소스:
- VPC 및 네트워크 (약 20개)
- EKS 클러스터 및 노드 그룹
- RDS, Redis, OpenSearch
- ECR 레포지토리
- IAM 역할 및 정책
- Kubernetes 리소스

#### 4. 배포 실행

```bash
# 자동 배포 (권장)
./scripts/deploy.sh

# 또는 수동
terraform apply
```

**소요 시간**: 20-30분

#### 5. kubectl 설정

```bash
aws eks update-kubeconfig --region ap-northeast-2 --name okchat-dev
kubectl get nodes
kubectl get pods -n okchat
```

#### 6. Docker 이미지 빌드 및 푸시

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin $(terraform output -raw ecr_repository_url)

# 이미지 빌드
cd ..
docker build -t okchat:latest .

# 이미지 태그 및 푸시
ECR_URL=$(cd terraform && terraform output -raw ecr_repository_url)
docker tag okchat:latest $ECR_URL:latest
docker push $ECR_URL:latest
```

#### 7. 애플리케이션 확인

```bash
# Pod 상태
kubectl get pods -n okchat

# 로그 확인
kubectl logs -f deployment/okchat-app -n okchat

# ALB URL
terraform output application_url
```

## 🎯 시나리오별 설정

### 시나리오 3: Spot 인스턴스 (권장) ⭐

**비용**: $174/월  
**용도**: 개발/테스트 환경

```hcl
# terraform.tfvars
eks_node_capacity_type = "SPOT"
eks_node_desired_size  = 2
app_replica_count      = 2
hpa_min_replicas       = 2
hpa_max_replicas       = 5
```

**특징**:
- ✅ 70-90% 비용 절감
- ✅ 고가용성 유지
- ✅ 무중단 배포 가능
- ⚠️ 인스턴스 중단 가능 (2분 전 알림)

### 시나리오 4: 단일 Replica (최소 비용)

**비용**: $171/월 (-$3)  
**용도**: 개인 학습/개발

```bash
# 시나리오 4 설정 사용
cp scenarios/scenario-4-single-replica.tfvars terraform.tfvars
vi terraform.tfvars  # OpenAI API Key 설정
terraform apply
```

```hcl
# 또는 수동 설정
eks_node_desired_size = 1
eks_node_max_size     = 2
app_replica_count     = 1
hpa_min_replicas      = 1
hpa_max_replicas      = 3
```

**특징**:
- ✅ 최소 비용
- ⚠️ 배포 시 다운타임 (30-60초)
- ⚠️ 단일 장애점
- ❌ 롤링 업데이트 불가

### 업무 시간만 운영

**비용**: $150/월 (추가 $24 절감)

```bash
# 저녁 7시 - 노드 축소
aws eks update-nodegroup-config \
  --cluster-name okchat-dev \
  --nodegroup-name okchat-dev-node-group \
  --scaling-config minSize=0,desiredSize=0 \
  --region ap-northeast-2

# 아침 9시 - 노드 확대
aws eks update-nodegroup-config \
  --cluster-name okchat-dev \
  --nodegroup-name okchat-dev-node-group \
  --scaling-config minSize=1,desiredSize=2 \
  --region ap-northeast-2
```

> 💡 Lambda + EventBridge로 자동화 가능

## 🔧 문제 해결

### Pod가 ImagePullBackOff 상태

**원인**: ECR에 이미지가 없음

**해결**:
```bash
# 이미지 확인
aws ecr describe-images --repository-name okchat-dev-app --region ap-northeast-2

# 이미지 빌드 및 푸시 (위의 6단계 참조)
```

### RDS 연결 실패

**원인**: Security Group 또는 엔드포인트 오류

**해결**:
```bash
# ConfigMap 확인
kubectl get configmap okchat-config -n okchat -o yaml | grep DATASOURCE

# Secret 확인
kubectl get secret okchat-secret -n okchat -o jsonpath='{.data.SPRING_DATASOURCE_PASSWORD}' | base64 -d

# RDS 엔드포인트 확인
terraform output rds_endpoint
```

### OpenSearch 연결 실패

**원인**: 보안 그룹 또는 자격 증명 오류

**해결**:
```bash
# OpenSearch 엔드포인트 확인
terraform output opensearch_endpoint

# 자격 증명 확인
terraform output -json opensearch_credentials

# 보안 그룹 확인
aws opensearch describe-domain --domain-name okchat-dev
```

### Spot 인스턴스 중단

**현상**: 노드가 갑자기 종료됨

**대응**:
1. Kubernetes가 자동으로 새 노드에 Pod 재배포
2. 2분 전 알림 확인: `kubectl get events`
3. 중요 작업 전 ON_DEMAND로 전환:
   ```hcl
   eks_node_capacity_type = "ON_DEMAND"
   terraform apply
   ```

### Terraform Apply 실패

**원인**: 리소스 의존성 또는 권한 문제

**해결**:
```bash
# State 확인
terraform show

# 특정 리소스 재생성
terraform taint <resource>
terraform apply

# 최악의 경우
terraform state rm <resource>
terraform apply
```

## 🗑️ 리소스 정리

### 전체 삭제

```bash
# 자동 정리 (권장)
./scripts/destroy.sh

# 수동 정리
terraform destroy
```

**소요 시간**: 15-20분

### 수동 정리 필요 항목

1. **CloudWatch 로그 그룹**
   ```bash
   aws logs describe-log-groups --log-group-name-prefix /aws/eks/okchat-dev
   aws logs delete-log-group --log-group-name <log-group-name>
   ```

2. **EBS 스냅샷**
   ```bash
   aws ec2 describe-snapshots --owner-ids self --filters "Name=tag:Project,Values=okchat"
   ```

3. **Secrets Manager 시크릿** (복구 기간 있음)
   ```bash
   aws secretsmanager delete-secret --secret-id <secret-arn> --force-delete-without-recovery
   ```

## 📚 추가 문서

- [QUICK_START.md](./QUICK_START.md) - 5분 빠른 시작
- [scenarios/README.md](./scenarios/README.md) - 시나리오 상세 가이드
- [k8s/README.md](../k8s/README.md) - Kubernetes 배포 가이드

## 🔐 보안 권장사항

1. **terraform.tfvars 보호**
   ```bash
   # .gitignore에 포함됨
   chmod 600 terraform.tfvars
   ```

2. **AWS Secrets Manager 사용**
   - 민감 정보는 Secrets Manager에 저장
   - External Secrets Operator로 자동 동기화 (선택)

3. **IAM 최소 권한**
   - IRSA로 Pod별 권한 분리
   - 불필요한 권한 제거

4. **네트워크 보안**
   - Private 서브넷에 워크로드 배포
   - Security Group으로 트래픽 제한
   - HTTPS 강제

## 💡 유용한 명령어

```bash
# Terraform 출력 확인
terraform output
terraform output -raw ecr_repository_url
terraform output -json database_credentials

# kubectl 상태 확인
kubectl get all -n okchat
kubectl describe pod <pod-name> -n okchat
kubectl logs -f deployment/okchat-app -n okchat

# HPA 확인
kubectl get hpa -n okchat
kubectl describe hpa okchat-app-hpa -n okchat

# 노드 확인
kubectl get nodes
kubectl describe node <node-name>

# 이벤트 확인
kubectl get events -n okchat --sort-by='.lastTimestamp'

# 리소스 사용량
kubectl top nodes
kubectl top pods -n okchat
```

## 📞 지원

문제가 발생하거나 질문이 있으면:
1. 이 README의 문제 해결 섹션 확인
2. [QUICK_START.md](./QUICK_START.md) 참조
3. Terraform 로그 확인: `TF_LOG=DEBUG terraform apply`
4. AWS Console에서 리소스 상태 확인

---

**버전**: 1.3.0  
**업데이트**: 2025-10-07  
**작성**: DevOps Team