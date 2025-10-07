# OkChat Terraform 빠른 시작 가이드

## 5분 만에 시작하기

### 1. 사전 준비 ✅

```bash
# AWS CLI 설치 확인
aws --version

# Terraform 설치 확인
terraform version

# AWS 자격 증명 설정
aws configure
```

### 2. 변수 파일 생성 📝

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

**terraform.tfvars 수정 (필수!):**

```hcl
openai_api_key = "sk-your-actual-api-key-here"  # 반드시 변경하세요!
```

### 3. 배포 실행 🚀

**자동 배포 (권장):**

```bash
./scripts/deploy.sh
```

**수동 배포:**

```bash
terraform init
terraform plan
terraform apply
```

### 4. kubectl 설정 ⚙️

```bash
aws eks update-kubeconfig --region ap-northeast-2 --name okchat-dev
kubectl get nodes
```

### 5. Docker 이미지 빌드 및 배포 🐳

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin $(terraform output -raw ecr_repository_url)

# 이미지 빌드 (프로젝트 루트에서)
cd ..
docker build -t okchat:latest .

# 이미지 푸시
docker tag okchat:latest $(cd terraform && terraform output -raw ecr_repository_url):latest
docker push $(cd terraform && terraform output -raw ecr_repository_url):latest

# 배포 확인
kubectl get pods -n okchat
kubectl logs -f deployment/okchat-app -n okchat
```

### 6. 애플리케이션 접속 🌐

```bash
# ALB URL 확인
terraform output application_url

# 브라우저에서 접속
# http://<alb-url>
```

---

## 주요 명령어

### 리소스 확인

```bash
# 모든 출력 값 보기
terraform output

# 특정 출력 값 보기
terraform output ecr_repository_url
terraform output application_url

# 민감한 정보 보기
terraform output -json database_credentials
terraform output -json opensearch_credentials
```

### Kubernetes 관리

```bash
# Pod 상태 확인
kubectl get pods -n okchat

# 로그 확인
kubectl logs -f deployment/okchat-app -n okchat

# Deployment 재시작
kubectl rollout restart deployment/okchat-app -n okchat

# HPA 확인
kubectl get hpa -n okchat
```

### 리소스 정리

```bash
# 자동 정리 (권장)
./scripts/destroy.sh

# 수동 정리
terraform destroy
```

---

## 비용 절감 팁 💰

### 1. 업무 시간 외 노드 축소

```bash
# 노드 수를 0으로 (야간/주말)
aws eks update-nodegroup-config \
  --cluster-name okchat-dev \
  --nodegroup-name okchat-dev-node-group \
  --scaling-config minSize=0,maxSize=3,desiredSize=0 \
  --region ap-northeast-2
```

### 2. 개발 완료 후 즉시 삭제

```bash
./scripts/destroy.sh
```

### 3. Spot 인스턴스 사용 (선택사항)

`terraform.tfvars`에서 변경:

```hcl
# eks_node_capacity_type = "SPOT"  # 비용 절감, 안정성 감소
```

---

## 문제 해결 🔧

### Pod가 시작되지 않음

```bash
# Pod 상세 정보 확인
kubectl describe pod <pod-name> -n okchat

# 이벤트 확인
kubectl get events -n okchat --sort-by='.lastTimestamp'
```

### 이미지를 가져오지 못함 (ImagePullBackOff)

```bash
# ECR에 이미지가 있는지 확인
aws ecr describe-images --repository-name okchat-dev-app --region ap-northeast-2

# 없다면 이미지 빌드 및 푸시 (위의 5번 참조)
```

### RDS 연결 실패

```bash
# ConfigMap 확인
kubectl get configmap okchat-config -n okchat -o yaml | grep DATASOURCE

# Secret 확인
kubectl get secret okchat-secret -n okchat -o jsonpath='{.data.SPRING_DATASOURCE_PASSWORD}' | base64 -d
```

---

## 필수 권한 목록

Terraform 배포에 필요한 AWS 권한:

- ✅ VPC (VPC, 서브넷, NAT Gateway, IGW)
- ✅ EC2 (Security Groups, ENI)
- ✅ EKS (클러스터, 노드 그룹)
- ✅ RDS (MySQL 인스턴스)
- ✅ ElastiCache (Redis 클러스터)
- ✅ OpenSearch (도메인)
- ✅ ECR (레포지토리)
- ✅ IAM (역할, 정책)
- ✅ Secrets Manager (시크릿)
- ✅ CloudWatch (로그 그룹)
- ✅ ELB (Application Load Balancer)

**권장**: `AdministratorAccess` 또는 PowerUserAccess + IAM 권한

---

## 월간 예상 비용

| 항목                           | 비용 (USD) |
|------------------------------|----------|
| EKS 클러스터                     | $73      |
| EKS 노드 (t3.medium x2)        | $30      |
| RDS (db.t4g.micro)           | $10      |
| Redis (cache.t4g.micro)      | $10      |
| OpenSearch (t3.small.search) | $25      |
| NAT Gateway                  | $32      |
| 기타 (데이터 전송, EBS)             | $15      |
| **총 예상 비용**                  | **$195** |

> 💡 **팁**: 미사용 시간에 EKS 노드를 축소하면 월 $30-50 절감 가능

---

## 다음 단계

1. ✅ Terraform으로 인프라 배포
2. ✅ Docker 이미지 빌드 및 푸시
3. ✅ 애플리케이션 동작 확인
4. 📊 모니터링 설정 (선택사항)
5. 🔐 도메인 및 HTTPS 설정 (선택사항)
6. 🎯 성능 튜닝 및 최적화

---

**문의**: 자세한 내용은 [README.md](./README.md) 참조