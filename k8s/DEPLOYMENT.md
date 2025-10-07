# OkChat 상세 배포 가이드

이 문서는 OkChat을 로컬 환경(Minikube)과 프로덕션 환경(AWS EKS)에 배포하는 상세한 가이드입니다.

## 목차

- [로컬 개발 환경 (Minikube)](#로컬-개발-환경-minikube)
- [프로덕션 환경 (AWS EKS)](#프로덕션-환경-aws-eks)
- [AWS 관리형 서비스 사용](#aws-관리형-서비스-사용)

---

## 로컬 개발 환경 (Minikube)

### 사전 요구사항

#### 필수 도구
- **Docker Desktop**: 최신 버전
- **kubectl**: Kubernetes CLI 도구
- **Minikube**: 로컬 Kubernetes 클러스터

설치 확인:
```bash
docker version
kubectl version --client
minikube version
```

#### 시스템 요구사항

| 사양 | CPU | Memory | Disk | 용도 |
|------|-----|--------|------|------|
| 최소 | 2 cores | 4GB | 20GB | 기본 테스트 |
| 권장 | 4 cores | 6GB | 40GB | 개발 환경 |
| 최적 | 4+ cores | 8GB+ | 50GB+ | 풀 스택 개발 |

### 단계별 배포

#### 1. Minikube 시작

```bash
# 권장 사양으로 시작
minikube start --cpus=4 --memory=6144 --disk-size=40g

# 리소스가 부족한 경우
minikube start --cpus=2 --memory=4096 --disk-size=20g

# Ingress 애드온 활성화
minikube addons enable ingress
```

#### 2. Docker 환경 설정

```bash
# Minikube Docker 환경 사용
eval $(minikube docker-env)

# 확인
docker ps
```

#### 3. Secret 설정

**중요**: 실제 값으로 업데이트 필수!

```bash
# Secret 파일 수정
vim k8s/base/secret.yaml
```

최소한 다음 값들을 변경해야 합니다:
- `OPENAI_API_KEY`: 실제 OpenAI API 키

선택적 (기능 활성화 시):
- `CONFLUENCE_EMAIL`: Confluence 이메일
- `CONFLUENCE_API_TOKEN`: Confluence API 토큰
- `EMAIL_PROVIDERS_GMAIL_*`: Gmail OAuth2 설정

#### 4. Docker 이미지 빌드

```bash
# 프로젝트 루트에서
cd /path/to/okchat

# 이미지 빌드
docker build -t okchat:latest .

# 빌드 확인
docker images | grep okchat
```

#### 5. Kubernetes 리소스 배포

```bash
# Kustomize로 배포
kubectl apply -k k8s/overlays/local

# 또는 자동화 스크립트 사용
./k8s/scripts/deploy-local.sh
```

#### 6. 배포 상태 확인

```bash
# Pod 상태 확인
kubectl get pods -n okchat

# 모든 Pod가 Running 상태가 될 때까지 대기
kubectl wait --for=condition=ready pod --all -n okchat --timeout=300s

# 상세 상태
kubectl get all -n okchat
```

예상 출력:
```
NAME                                 READY   STATUS    RESTARTS   AGE
okchat-app-xxxxxxxxx-xxxxx          1/1     Running   0          2m
okchat-mysql-xxxxxxxxx-xxxxx        1/1     Running   0          2m
okchat-opensearch-xxxxxxxxx-xxxxx   1/1     Running   0          2m
okchat-redis-xxxxxxxxx-xxxxx        1/1     Running   0          2m
```

#### 7. 애플리케이션 접속

**방법 A: Port Forward (권장)**
```bash
kubectl port-forward -n okchat svc/okchat-app 8080:8080
```
접속: http://localhost:8080/actuator/health

**방법 B: Minikube Service**
```bash
minikube service okchat-app -n okchat
```

**방법 C: Ingress (선택)**
```bash
# 별도 터미널에서
sudo minikube tunnel

# /etc/hosts 설정 확인
cat /etc/hosts | grep okchat.local
# 없으면 추가:
echo "$(minikube ip) okchat.local" | sudo tee -a /etc/hosts

# 접속
curl http://okchat.local/actuator/health
```

### 로컬 환경 설정 세부사항

#### 리소스 제한

로컬 환경에서는 리소스가 최소화되어 있습니다:

| 컴포넌트 | CPU Request | CPU Limit | Memory Request | Memory Limit |
|----------|-------------|-----------|----------------|--------------|
| okchat-app | 250m | 1000m | 512Mi | 1Gi |
| mysql | 100m | 500m | 256Mi | 512Mi |
| redis | 50m | 250m | 128Mi | 256Mi |
| opensearch | 250m | 1000m | 1Gi | 2Gi |

#### 스토리지

- **StorageClass**: `standard` (Minikube 기본)
- MySQL: 5Gi
- Redis: 2Gi
- OpenSearch: 10Gi

#### 환경 변수 커스터마이징

```bash
# 메모리 조정
MINIKUBE_MEMORY=8192 ./k8s/scripts/deploy-local.sh

# CPU 조정
MINIKUBE_CPUS=6 ./k8s/scripts/deploy-local.sh

# 디스크 조정
MINIKUBE_DISK=50g ./k8s/scripts/deploy-local.sh

# 모두 조정
MINIKUBE_CPUS=6 MINIKUBE_MEMORY=8192 MINIKUBE_DISK=50g ./k8s/scripts/deploy-local.sh
```

---

## 프로덕션 환경 (AWS EKS)

### 사전 요구사항

#### 필수 도구
- **AWS CLI 2.x**
- **eksctl**: EKS 클러스터 관리 도구
- **kubectl**: Kubernetes CLI
- **Helm 3.x**: 패키지 관리자

#### AWS 계정 요구사항
- 적절한 IAM 권한
- VPC 및 서브넷 설정
- EC2, EKS, RDS 등 서비스 접근 권한

### 1. EKS 클러스터 생성

#### eksctl을 사용한 클러스터 생성

```bash
# 기본 클러스터 생성
eksctl create cluster \
  --name okchat-prod \
  --region ap-northeast-2 \
  --node-type t3.xlarge \
  --nodes 3 \
  --nodes-min 3 \
  --nodes-max 10 \
  --managed \
  --with-oidc \
  --ssh-access \
  --ssh-public-key your-key-name

# 클러스터 확인
kubectl get nodes
kubectl config current-context
```

#### 또는 기존 클러스터 사용

```bash
# kubeconfig 업데이트
aws eks update-kubeconfig --region ap-northeast-2 --name okchat-prod

# 연결 확인
kubectl get svc
```

### 2. 필수 구성요소 설치

#### A. AWS Load Balancer Controller

```bash
# IAM 정책 다운로드 및 생성
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.0/docs/install/iam_policy.json

aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam_policy.json

# ACCOUNT_ID 가져오기
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# IRSA (IAM Roles for Service Accounts) 생성
eksctl create iamserviceaccount \
  --cluster=okchat-prod \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --role-name AmazonEKSLoadBalancerControllerRole \
  --attach-policy-arn=arn:aws:iam::${ACCOUNT_ID}:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve

# Helm으로 Controller 설치
helm repo add eks https://aws.github.io/eks-charts
helm repo update

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=okchat-prod \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller

# 확인
kubectl get deployment -n kube-system aws-load-balancer-controller
```

#### B. EBS CSI Driver

```bash
# IRSA 생성
eksctl create iamserviceaccount \
  --name ebs-csi-controller-sa \
  --namespace kube-system \
  --cluster okchat-prod \
  --role-name AmazonEKS_EBS_CSI_DriverRole \
  --role-only \
  --attach-policy-arn arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy \
  --approve

# EBS CSI 드라이버 애드온 설치
eksctl create addon \
  --name aws-ebs-csi-driver \
  --cluster okchat-prod \
  --service-account-role-arn arn:aws:iam::${ACCOUNT_ID}:role/AmazonEKS_EBS_CSI_DriverRole \
  --force

# gp3 StorageClass 생성
kubectl apply -f - <<EOF
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3
  annotations:
    storageclass.kubernetes.io/is-default-class: "true"
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  encrypted: "true"
  iops: "3000"
  throughput: "125"
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
EOF
```

### 3. ECR 리포지토리 및 이미지

#### ECR 리포지토리 생성

```bash
# 리포지토리 생성
aws ecr create-repository \
    --repository-name okchat \
    --region ap-northeast-2 \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256

# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com
```

#### 이미지 빌드 및 푸시

```bash
# 프로젝트 루트에서
docker build -t okchat:latest .

# 태그
docker tag okchat:latest ${ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/okchat:latest
docker tag okchat:latest ${ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/okchat:v1.0.0

# 푸시
docker push ${ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/okchat:latest
docker push ${ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com/okchat:v1.0.0

# 또는 스크립트 사용
./k8s/scripts/build-and-push.sh v1.0.0
```

### 4. Secret 설정

#### 방법 A: Kubernetes Secret (간단)

```bash
kubectl create secret generic okchat-secret \
  --from-literal=SPRING_DATASOURCE_USERNAME=admin \
  --from-literal=SPRING_DATASOURCE_PASSWORD='YOUR_STRONG_PASSWORD' \
  --from-literal=MYSQL_ROOT_PASSWORD='YOUR_STRONG_PASSWORD' \
  --from-literal=OPENAI_API_KEY='YOUR_OPENAI_KEY' \
  --from-literal=CONFLUENCE_BASE_URL='https://your-confluence.atlassian.net/wiki/api/v2' \
  --from-literal=CONFLUENCE_EMAIL='your-email@example.com' \
  --from-literal=CONFLUENCE_API_TOKEN='YOUR_CONFLUENCE_TOKEN' \
  --namespace=okchat
```

#### 방법 B: AWS Secrets Manager (권장)

```bash
# Secret 생성
aws secretsmanager create-secret \
    --name okchat/prod/db \
    --secret-string '{"username":"admin","password":"YOUR_STRONG_PASSWORD"}' \
    --region ap-northeast-2

aws secretsmanager create-secret \
    --name okchat/prod/openai \
    --secret-string '{"api_key":"YOUR_OPENAI_KEY"}' \
    --region ap-northeast-2

# External Secrets Operator 설치 (Helm)
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace

# SecretStore 및 ExternalSecret 구성 필요 (별도 문서 참조)
```

### 5. 프로덕션 설정 업데이트

```bash
# Deployment의 이미지 URL 업데이트
sed -i '' "s/<AWS_ACCOUNT_ID>/${ACCOUNT_ID}/g" k8s/overlays/production/deployment-patch.yaml
sed -i '' "s/<AWS_REGION>/ap-northeast-2/g" k8s/overlays/production/deployment-patch.yaml

# Ingress 설정 업데이트
vim k8s/overlays/production/ingress-patch.yaml
```

**Ingress 설정 시 업데이트 필요:**
- `alb.ingress.kubernetes.io/certificate-arn`: ACM 인증서 ARN
- `alb.ingress.kubernetes.io/security-groups`: Security Group ID
- `spec.rules[0].host`: 실제 도메인 이름

### 6. 배포

```bash
# 프로덕션 환경 배포
kubectl apply -k k8s/overlays/production

# 배포 상태 확인
kubectl get pods -n okchat -w

# 모든 리소스 확인
kubectl get all -n okchat
```

### 7. ALB 및 DNS 설정

```bash
# ALB 주소 확인
kubectl get ingress -n okchat

# 출력 예시:
# NAME              CLASS   HOSTS                    ADDRESS                                    PORTS   AGE
# okchat-ingress    alb     okchat.yourdomain.com    k8s-okchat-xxx.elb.ap-northeast-2.amazonaws.com   80, 443   5m
```

**Route53에 레코드 추가:**
1. Route53 콘솔로 이동
2. Hosted Zone 선택
3. "Create record" 클릭
4. Record name: `okchat` (또는 원하는 서브도메인)
5. Record type: `A` - Alias
6. Route traffic to: Alias to Application and Classic Load Balancer
7. Region: `ap-northeast-2`
8. ALB 선택
9. Create record

### 8. 모니터링 설정 (선택)

#### Prometheus & Grafana 설치

```bash
# Prometheus Operator 설치
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false

# Grafana 접속
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80

# 기본 계정: admin / prom-operator
```

### 프로덕션 환경 설정 세부사항

#### 리소스 설정

| 컴포넌트 | Replicas | CPU Request | CPU Limit | Memory Request | Memory Limit |
|----------|----------|-------------|-----------|----------------|--------------|
| okchat-app | 3 | 1000m | 2000m | 2Gi | 4Gi |
| mysql* | 1 | 500m | 1000m | 1Gi | 2Gi |
| redis* | 1 | 250m | 500m | 512Mi | 1Gi |
| opensearch* | 1 | 1000m | 2000m | 4Gi | 6Gi |

*권장: AWS 관리형 서비스 사용 (RDS, ElastiCache, OpenSearch Service)

#### Auto-scaling 설정

HPA (Horizontal Pod Autoscaler) 설정:
- 최소 replicas: 3
- 최대 replicas: 10
- CPU 목표: 70%
- Memory 목표: 80%

```bash
# HPA 상태 확인
kubectl get hpa -n okchat
kubectl describe hpa okchat-app-hpa -n okchat
```

#### 스토리지

- **StorageClass**: `gp3` (AWS EBS)
- MySQL: 50Gi
- Redis: 20Gi
- OpenSearch: 100Gi

---

## AWS 관리형 서비스 사용

프로덕션 환경에서는 AWS 관리형 서비스 사용을 강력히 권장합니다.

### 장점

- ✅ 고가용성 (Multi-AZ)
- ✅ 자동 백업 및 복구
- ✅ 자동 패치 및 업데이트
- ✅ 모니터링 및 알림
- ✅ 스케일링 용이
- ✅ 보안 강화
- ✅ 운영 부담 감소

### 1. Amazon RDS for MySQL

#### RDS 인스턴스 생성

```bash
aws rds create-db-instance \
  --db-instance-identifier okchat-db \
  --db-instance-class db.t3.medium \
  --engine mysql \
  --engine-version 8.0.35 \
  --master-username admin \
  --master-user-password YOUR_STRONG_PASSWORD \
  --allocated-storage 100 \
  --storage-type gp3 \
  --vpc-security-group-ids sg-xxxxxxxx \
  --db-subnet-group-name my-db-subnet-group \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --multi-az \
  --storage-encrypted \
  --enable-performance-insights \
  --publicly-accessible false \
  --region ap-northeast-2
```

#### ConfigMap 업데이트

```yaml
SPRING_DATASOURCE_URL: "jdbc:mysql://okchat-db.xxxxx.ap-northeast-2.rds.amazonaws.com:3306/okchat?useSSL=true&serverTimezone=Asia/Seoul"
```

#### Deployment에서 MySQL 제거

```yaml
# k8s/overlays/production/kustomization.yaml
resources:
  - ../../base/namespace.yaml
  - ../../base/configmap.yaml
  - ../../base/secret.yaml
  # - ../../base/mysql-deployment.yaml    # 주석 처리
  - ../../base/redis-deployment.yaml
  - ../../base/opensearch-deployment.yaml
  - ../../base/okchat-deployment.yaml
  - ../../base/ingress.yaml
```

### 2. Amazon ElastiCache for Redis

#### ElastiCache 클러스터 생성

```bash
aws elasticache create-replication-group \
  --replication-group-id okchat-redis \
  --replication-group-description "OkChat Redis Cache" \
  --engine redis \
  --engine-version 7.0 \
  --cache-node-type cache.t3.medium \
  --num-cache-clusters 2 \
  --automatic-failover-enabled \
  --cache-subnet-group-name my-cache-subnet-group \
  --security-group-ids sg-xxxxxxxx \
  --at-rest-encryption-enabled \
  --transit-encryption-enabled \
  --snapshot-retention-limit 5 \
  --snapshot-window "03:00-05:00" \
  --region ap-northeast-2
```

#### ConfigMap 업데이트

```yaml
SPRING_DATA_REDIS_HOST: "okchat-redis.xxxxx.cache.amazonaws.com"
SPRING_DATA_REDIS_PORT: "6379"
```

### 3. Amazon OpenSearch Service

#### OpenSearch 도메인 생성

AWS Console 사용 권장:

1. AWS Console > OpenSearch Service
2. "Create domain" 클릭
3. 설정:
   - Domain name: `okchat-opensearch`
   - Deployment type: Production
   - Version: 2.11
   - Data nodes: 3 x r6g.xlarge.search
   - Storage: 100 GiB EBS gp3 per node
   - Network: VPC access
   - Fine-grained access control: Enabled
   - Encryption: At rest and in transit

#### ConfigMap 업데이트

```yaml
SPRING_AI_VECTORSTORE_OPENSEARCH_HOST: "search-okchat-xxxxx.ap-northeast-2.es.amazonaws.com"
SPRING_AI_VECTORSTORE_OPENSEARCH_PORT: "443"
SPRING_AI_VECTORSTORE_OPENSEARCH_SCHEME: "https"
```

#### Secret 추가

```bash
kubectl create secret generic okchat-opensearch-secret \
  --from-literal=SPRING_AI_VECTORSTORE_OPENSEARCH_USERNAME=admin \
  --from-literal=SPRING_AI_VECTORSTORE_OPENSEARCH_PASSWORD=YOUR_PASSWORD \
  --namespace=okchat
```

### 네트워크 설정

AWS 관리형 서비스 사용 시 네트워크 연결:

1. **VPC Peering** 또는 **AWS Transit Gateway** 설정
2. **Security Groups** 설정:
   - RDS: 3306 포트를 EKS 워커 노드에서 접근 허용
   - ElastiCache: 6379 포트 허용
   - OpenSearch: 443 포트 허용
3. **Subnet** 설정: Private subnet 사용
4. **IAM Roles**: 필요한 권한 부여

---

## 베스트 프랙티스

### 보안

1. **모든 통신 암호화** (TLS/SSL)
2. **Secret은 AWS Secrets Manager 사용**
3. **Network Policy 적용**
4. **최소 권한 원칙** (RBAC)
5. **정기적인 보안 스캔**

### 성능

1. **HPA 활용** (Auto-scaling)
2. **리소스 제한 설정** (Requests/Limits)
3. **Connection Pool 최적화**
4. **캐시 전략 수립**

### 모니터링

1. **Prometheus + Grafana**
2. **CloudWatch 로그 통합**
3. **알림 설정** (PagerDuty, Slack 등)
4. **정기적인 Health Check**

### 백업 및 복구

1. **자동 백업 설정** (RDS, ElastiCache)
2. **PVC 스냅샷** (EBS)
3. **재해 복구 계획** (DR Plan)
4. **정기적인 복구 테스트**

---

## 다음 단계

- [메인 문서로 돌아가기](./README.md)
- [문제 해결 가이드](./TROUBLESHOOTING.md)
- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [AWS EKS 사용자 가이드](https://docs.aws.amazon.com/eks/)