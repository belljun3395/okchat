# Terraform Cloud 설정 가이드

Terraform Cloud를 사용하여 상태 관리 및 협업을 개선하는 방법입니다.

## 🎯 Terraform Cloud를 사용하는 이유

### 장점

1. **원격 상태 관리**
   - `.tfstate` 파일을 안전하게 클라우드에 저장
   - 상태 파일 충돌 방지
   - 자동 백업

2. **협업**
   - 팀원들과 안전하게 협업
   - 상태 잠금 (State Locking)
   - 변경 이력 추적

3. **CI/CD 통합**
   - GitHub Actions와 자동 연동
   - PR에 Plan 결과 자동 표시
   - 승인 워크플로우

4. **보안**
   - 민감한 변수 암호화 저장
   - 접근 제어 (RBAC)
   - Audit 로그

5. **무료 티어**
   - 5명까지 무료
   - 무제한 워크스페이스
   - 월 500회 Run

## 🚀 초기 설정

### 1. Terraform Cloud 가입

```bash
# 1. 웹사이트 방문
https://app.terraform.io/signup

# 2. 계정 생성
- Email로 가입 또는
- GitHub 연동

# 3. 이메일 인증
```

### 2. Organization 생성

```bash
# 1. 로그인 후 "Create an organization" 클릭
# 2. Organization 이름 입력
#    예시: "your-company" 또는 "okestro"
# 3. Email 입력
```

### 3. Workspace 생성

#### 방법 1: UI에서 생성

```bash
# 1. "New workspace" 클릭
# 2. Workflow 선택: "API-driven workflow"
# 3. Workspace 이름: "okchat-dev"
# 4. Description: "OkChat Development Environment"
```

#### 방법 2: CLI로 생성

```bash
# 1. Terraform Cloud 로그인
terraform login

# 2. providers.tf의 organization 이름 수정
# 3. terraform init 실행하면 자동 생성
cd terraform
terraform init
```

### 4. API Token 생성

```bash
# 1. User Settings (우측 상단 아이콘)
# 2. Tokens 메뉴
# 3. "Create an API token"
# 4. Description: "GitHub Actions"
# 5. 생성된 토큰 복사 (다시 볼 수 없음!)
```

## 🔧 프로젝트 설정

### 1. providers.tf 수정

이미 적용되어 있습니다:

```hcl
terraform {
  cloud {
    organization = "your-org-name"  # 실제 조직명으로 변경!
    
    workspaces {
      name = "okchat-dev"
    }
  }
}
```

**수정 방법**:
```bash
cd terraform
vi providers.tf

# organization = "your-org-name" 을
# organization = "okestro" (실제 조직명)로 변경
```

### 2. Terraform 초기화

```bash
cd terraform

# Terraform Cloud 로그인
terraform login
# 브라우저에서 토큰 생성 및 입력

# 초기화
terraform init

# 기존 로컬 상태를 클라우드로 마이그레이션할지 물어봄
# yes 입력
```

### 3. Workspace Variables 설정

#### Terraform Variables (terraform.tfvars 내용)

```bash
# Terraform Cloud UI에서:
# Workspace > Variables > Terraform Variables

# 각 변수 추가:
```

| Variable | Value | Sensitive |
|----------|-------|-----------|
| `openai_api_key` | `sk-proj-xxxxx` | ✅ Yes |
| `aws_region` | `ap-northeast-2` | No |
| `project_name` | `okchat` | No |
| `environment` | `dev` | No |
| `eks_node_capacity_type` | `SPOT` | No |
| `eks_node_desired_size` | `2` | No |

#### Environment Variables (AWS 자격증명)

| Variable | Value | Sensitive |
|----------|-------|-----------|
| `AWS_ACCESS_KEY_ID` | `AKIAIOSFODNN7EXAMPLE` | ✅ Yes |
| `AWS_SECRET_ACCESS_KEY` | `wJalrXUtnFEMI/K7MDENG...` | ✅ Yes |

**중요**: 
- Sensitive 체크한 변수는 암호화되어 저장됨
- UI에서 다시 볼 수 없음 (보안)

### 4. Execution Mode 설정

```bash
# Workspace > Settings > General

Execution Mode: Remote
Auto Apply: Off  # PR 승인 후 수동 Apply 권장
Terraform Version: 1.9.0 (Latest)
```

## 📋 GitHub Actions 통합

### 1. GitHub Secrets 설정

```bash
# Repository > Settings > Secrets and variables > Actions
# New repository secret 클릭
```

**필수 Secret**:
- Name: `TF_API_TOKEN`
- Value: (위에서 생성한 Terraform Cloud API Token)

### 2. 워크플로우 파일 확인

`.github/workflows/terraform.yml` - 이미 생성됨 ✅

**주요 기능**:
- PR에 `terraform plan` 결과 자동 표시
- Main 브랜치 머지 시 `terraform apply` 자동 실행
- 포맷 검사 및 검증

### 3. 동작 확인

```bash
# 1. 테스트 PR 생성
git checkout -b test/terraform-cloud
git commit --allow-empty -m "Test Terraform Cloud"
git push origin test/terraform-cloud

# 2. GitHub에서 PR 생성

# 3. Actions 탭에서 "Terraform CI/CD" 워크플로우 확인
# - terraform-check: 포맷 및 검증
# - terraform-plan: Plan 결과가 PR 코멘트로 표시됨

# 4. PR 머지
# - terraform-apply: 자동 배포 실행
```

## 🔄 워크플로우

### 일반적인 사용 흐름

```mermaid
graph LR
    A[로컬 변경] --> B[Git Push]
    B --> C[GitHub PR]
    C --> D[Terraform Plan]
    D --> E[리뷰]
    E --> F{승인?}
    F -->|Yes| G[Merge to Main]
    F -->|No| A
    G --> H[Terraform Apply]
    H --> I[배포 완료]
```

### PR 워크플로우

```bash
# 1. Feature 브랜치 생성
git checkout -b feature/add-rds-replica

# 2. Terraform 코드 수정
cd terraform
vi rds.tf

# 3. 로컬 테스트 (선택)
terraform fmt
terraform validate
terraform plan  # Terraform Cloud에서 실행됨

# 4. 커밋 및 푸시
git add .
git commit -m "Add RDS read replica"
git push origin feature/add-rds-replica

# 5. GitHub PR 생성
# - GitHub Actions가 자동으로 terraform plan 실행
# - Plan 결과가 PR 코멘트에 표시됨

# 6. 코드 리뷰
# - Plan 결과 확인
# - 예상 비용 검토
# - 보안 검토

# 7. PR 승인 및 Merge
# - Main 브랜치로 자동 머지
# - Terraform Apply 자동 실행
```

## 🔐 보안 모범 사례

### 1. 접근 제어

```bash
# Workspace > Settings > Team Access

팀 권한 레벨:
- Read: 상태 읽기만
- Plan: Plan 실행 가능
- Write: Apply 실행 가능
- Admin: 모든 권한
```

### 2. Run Triggers

```bash
# Workspace > Settings > Run Triggers

# 다른 워크스페이스의 변경사항이 이 워크스페이스에 영향을 줄 때 자동 실행
# 예: VPC 워크스페이스 → EKS 워크스페이스
```

### 3. Notifications

```bash
# Workspace > Settings > Notifications

알림 채널 설정:
- Slack
- Microsoft Teams
- Email
- Webhook

알림 이벤트:
- Run started
- Plan finished
- Apply finished
- Apply errored
```

### 4. Policy as Code (Sentinel) - Paid

```hcl
# 비용 제한 정책 예시
import "tfrun"

# 월 $300 이상의 변경 차단
policy "cost-limit" {
  source = "./cost-limit.sentinel"
  enforcement_level = "hard-mandatory"
}
```

## 💰 비용 관리

### 무료 티어 제한

- ✅ 5명까지 무료
- ✅ 무제한 워크스페이스
- ✅ 월 500회 Run
- ✅ Private Registry
- ❌ Sentinel (Policy as Code)
- ❌ SSO
- ❌ Audit Logging

### Run 횟수 최적화

```bash
# 1. 로컬에서 테스트 먼저
terraform fmt
terraform validate

# 2. Draft PR 사용
# - Draft PR은 Plan을 자동 실행하지 않음
# - Ready for Review로 변경 시 실행

# 3. Auto-apply 비활성화
# - Manual Approve로 불필요한 Apply 방지
```

## 🛠 문제 해결

### 마이그레이션 오류

```bash
# 오류: "Backend initialization required"

# 해결:
rm -rf .terraform
rm .terraform.lock.hcl
terraform init
```

### State Lock 오류

```bash
# 오류: "Error acquiring the state lock"

# 해결:
# Terraform Cloud UI에서:
# Workspace > Settings > General > Force Unlock
```

### API Token 만료

```bash
# 오류: "401 Unauthorized"

# 해결:
# 1. 새 토큰 생성
# 2. GitHub Secrets 업데이트
# 3. 로컬: terraform login 재실행
```

### Plan이 너무 느림

```bash
# 최적화:
# 1. Workspace > Settings > General
# 2. "Execution Mode" → Local로 변경 (상태만 원격 저장)
# 3. 또는 모듈 분리로 scope 줄이기
```

## 📊 모니터링

### Run History 확인

```bash
# Workspace > Runs

확인 가능 정보:
- Plan/Apply 이력
- 실행 시간
- 변경된 리소스 수
- 실행자 (User/GitHub Actions)
```

### State History

```bash
# Workspace > States

기능:
- 모든 상태 버전 이력
- 특정 버전으로 롤백
- 상태 다운로드
- Diff 보기
```

### Cost Estimation (Paid)

```bash
# Plan 실행 시 자동으로 비용 예측
# AWS, Azure, GCP 지원
```

## 🔄 로컬로 다시 전환

```bash
# Terraform Cloud가 마음에 들지 않으면:

# 1. providers.tf 수정
terraform {
  # cloud 블록 제거 또는 주석 처리
  
  backend "local" {
    path = "terraform.tfstate"
  }
}

# 2. 상태 다운로드
# Terraform Cloud UI에서 최신 상태 다운로드

# 3. 로컬로 복사
mv ~/Downloads/terraform.tfstate ./terraform.tfstate

# 4. 재초기화
terraform init -migrate-state
```

## 📚 추가 리소스

- [Terraform Cloud 문서](https://developer.hashicorp.com/terraform/cloud-docs)
- [GitHub Actions 통합](https://developer.hashicorp.com/terraform/cloud-docs/vcs/github-actions)
- [API 문서](https://developer.hashicorp.com/terraform/cloud-docs/api-docs)

---

**업데이트**: 2025-10-07  
**작성**: DevOps Team