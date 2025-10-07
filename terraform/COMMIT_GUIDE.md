# Terraform 커밋 가이드

## ✅ 커밋 준비 완료

다음 파일들이 Git에 추가되었습니다:

### 커밋될 파일 (22개)

#### 설정 파일
- ✅ `.gitignore` - Git 제외 규칙
- ✅ `.terraform.lock.hcl` - Provider 버전 잠금

#### 문서 (3개)
- ✅ `README.md` - 메인 문서
- ✅ `QUICK_START.md` - 빠른 시작 가이드
- ✅ `IMPLEMENTATION.md` - 구현 상세

#### Terraform 코드 (14개 .tf)
- ✅ `main.tf` - 메인 엔트리
- ✅ `providers.tf` - Provider 설정
- ✅ `variables.tf` - 변수 정의
- ✅ `locals.tf` - 로컬 변수 (환경 변수 매핑)
- ✅ `outputs.tf` - 출력 값
- ✅ `vpc.tf` - VPC 및 네트워크
- ✅ `security_groups.tf` - 보안 그룹
- ✅ `iam.tf` - IAM 역할
- ✅ `secrets.tf` - Secrets Manager
- ✅ `eks.tf` - EKS 클러스터
- ✅ `rds.tf` - RDS MySQL
- ✅ `elasticache.tf` - Redis
- ✅ `opensearch.tf` - OpenSearch
- ✅ `ecr.tf` - ECR
- ✅ `kubernetes.tf` - K8s 리소스

#### 스크립트 (2개)
- ✅ `scripts/deploy.sh` - 자동 배포
- ✅ `scripts/destroy.sh` - 자동 정리

## ❌ 커밋되지 않는 파일 (보안)

`.gitignore`에 의해 자동으로 제외되는 파일들:

### 절대 커밋 금지 ⛔
- `*.tfvars` - **실제 API 키와 비밀번호 포함!**
- `*.tfstate*` - 인프라 상태 (민감 정보 포함)
- `.terraform/` - Provider 플러그인
- `*.pem`, `*.key` - SSH 키
- `.env*` - 환경 변수

### 기타 제외
- `tfplan` - Terraform 실행 계획
- `crash.log` - 충돌 로그
- `kubeconfig*` - Kubernetes 설정
- IDE 설정 파일

## 🚀 커밋 방법

### 1. 현재 상태 확인

```bash
git status terraform/
```

### 2. 커밋 메시지 작성

```bash
git commit -m "Add Terraform AWS infrastructure

- EKS cluster with Spot instances (cost optimized)
- RDS MySQL, ElastiCache Redis, OpenSearch
- VPC with NAT Gateway and VPC Endpoints
- Kubernetes resources with auto-scaling
- Comprehensive documentation

Estimated cost: $174/month (Scenario 3)
"
```

또는 더 상세하게:

```bash
git commit -m "Add Terraform AWS infrastructure" -m "
Infrastructure:
- EKS 1.31 with t3.medium Spot instances (2 nodes)
- RDS MySQL 8.0 (db.t4g.micro, ARM Graviton2)
- ElastiCache Redis 7.1 (cache.t4g.micro)
- OpenSearch 2.11 (t3.small.search)
- ECR for container images

Cost Optimization:
- Spot instances: 70-90% savings
- ARM Graviton2: 20% additional savings
- Single AZ deployment
- Single NAT Gateway
- VPC Endpoints for data transfer savings

Features:
- Auto-scaling (HPA)
- Load Balancer (ALB)
- Secrets management (AWS Secrets Manager)
- IRSA for Pod IAM roles
- Complete environment variable mapping

Estimated monthly cost: \$174 (dev environment)
"
```

### 3. 커밋 실행

```bash
git commit
```

## ⚠️ 커밋 전 체크리스트

### 필수 확인 ✓

- [ ] `terraform.tfvars` 파일이 `.gitignore`에 포함되어 있는가?
- [ ] 실제 API 키나 비밀번호가 코드에 하드코딩되지 않았는가?
- [ ] `.terraform/` 디렉토리가 제외되었는가?
- [ ] `*.tfstate` 파일이 제외되었는가?
- [ ] 문서가 최신 상태인가?

### 확인 명령어

```bash
# 민감한 정보가 포함된 파일이 staged되지 않았는지 확인
git diff --staged --name-only | grep -E "\\.tfvars$|\\.tfstate$|\\.pem$|\\.key$"

# 아무것도 출력되지 않으면 OK!
```

## 📝 권장 커밋 메시지 템플릿

```
Add/Update/Fix Terraform <component>

<간단한 설명>

Changes:
- <변경사항 1>
- <변경사항 2>

<추가 컨텍스트>
```

### 예시

```
Add Terraform AWS infrastructure for OkChat

비용 최적화된 AWS 개발 환경 구성

Changes:
- Add EKS cluster with Spot instances
- Add RDS MySQL with ARM Graviton2
- Add ElastiCache Redis and OpenSearch
- Add complete Kubernetes resources
- Add deployment automation scripts

Cost: $174/month (Scenario 3)
Features: Auto-scaling, ALB, Secrets Manager, IRSA
```

## 🔐 보안 주의사항

### 절대 커밋하면 안 되는 것

1. **terraform.tfvars**
   ```hcl
   openai_api_key = "sk-proj-xxxxx"  # 절대 커밋 금지!
   ```

2. **terraform.tfstate**
   - 모든 리소스 ID, IP, 비밀번호 포함

3. **SSH 키, 인증서**
   - *.pem, *.key, *.crt

4. **환경 변수 파일**
   - .env, .env.local

### 만약 실수로 커밋했다면

```bash
# 마지막 커밋 취소 (아직 push 안 함)
git reset HEAD~1

# 이미 push 했다면
# 1. 즉시 API 키 재발급
# 2. Git history 정리 (복잡함)
# 3. 새 레포지토리 생성 권장
```

## 🎯 다음 단계

### 커밋 후

1. **Push 전 확인**
   ```bash
   git log --oneline -1
   git show HEAD --stat
   ```

2. **Push**
   ```bash
   git push origin main
   ```

3. **문서 확인**
   - GitHub에서 README.md가 제대로 렌더링되는지 확인
   - 링크가 작동하는지 확인

### 팀원 온보딩

다른 팀원이 사용할 때:

```bash
# 1. Clone
git clone <repository>
cd okchat/terraform

# 2. 설정 파일 생성
cp terraform.tfvars.example terraform.tfvars
vi terraform.tfvars  # API 키 등 실제 값 입력

# 3. Terraform 초기화
terraform init

# 4. 배포
./scripts/deploy.sh
```

## 📚 추가 참고

- [README.md](./README.md) - 전체 가이드
- [QUICK_START.md](./QUICK_START.md) - 빠른 시작
- [IMPLEMENTATION.md](./IMPLEMENTATION.md) - 구현 상세

---

**중요**: 커밋 후에는 절대 `terraform.tfvars`에 실제 값을 입력하지 마세요!  
항상 `terraform.tfvars.example`을 복사해서 사용하세요.

**작성**: DevOps Team  
**업데이트**: 2025-10-07