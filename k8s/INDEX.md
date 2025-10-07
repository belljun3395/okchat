# OkChat Kubernetes 문서 가이드

## 📚 문서 구조

OkChat Kubernetes 배포를 위한 문서는 3개의 핵심 문서로 구성되어 있습니다:

### 1. [README.md](./README.md) - 메인 문서 ⭐

**대상:** 모든 사용자  
**내용:**
- 프로젝트 개요
- 빠른 시작 (5분 안에 시작)
- 접속 방법
- 유용한 명령어 모음
- 빠른 문제 해결

**이 문서부터 시작하세요!**

### 2. [DEPLOYMENT.md](./DEPLOYMENT.md) - 상세 배포 가이드

**대상:** 배포 담당자, DevOps 엔지니어  
**내용:**
- 로컬 환경 (Minikube) 상세 배포
- AWS EKS 프로덕션 배포
- AWS 관리형 서비스 (RDS, ElastiCache, OpenSearch) 설정
- 베스트 프랙티스

**상세한 배포가 필요할 때 참조하세요.**

### 3. [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - 문제 해결 가이드

**대상:** 문제 해결이 필요한 모든 사용자  
**내용:**
- 일반적인 문제 12가지
- 빠른 진단 방법
- 단계별 해결 방법
- 디버깅 명령어

**문제가 발생했을 때 참조하세요.**

## 🚀 시작하기

### 처음 사용하는 경우

1. [README.md](./README.md)의 "빠른 시작" 섹션 읽기
2. Secret 설정 (OPENAI_API_KEY)
3. 자동 배포 스크립트 실행:
   ```bash
   ./k8s/scripts/deploy-local.sh
   ```

### 상세 설정이 필요한 경우

- [DEPLOYMENT.md](./DEPLOYMENT.md) 참조

### 문제가 발생한 경우

- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)의 "빠른 진단" 섹션 참조

## 📂 프로젝트 구조

```
k8s/
├── base/                    # 기본 Kubernetes 리소스
├── overlays/
│   ├── local/              # Minikube 설정
│   └── production/         # AWS EKS 설정
├── scripts/                # 자동화 스크립트
├── README.md               # 메인 문서
├── DEPLOYMENT.md           # 상세 배포 가이드
└── TROUBLESHOOTING.md      # 문제 해결 가이드
```

## 🔗 빠른 링크

### 자주 사용하는 명령어

```bash
# 상태 확인
kubectl get pods -n okchat

# 로그 보기
kubectl logs -n okchat -l app=okchat-app -f

# 접속 (Port Forward)
kubectl port-forward -n okchat svc/okchat-app 8080:8080

# 정리
./k8s/scripts/cleanup-local.sh
```

### 자주 찾는 섹션

- [빠른 시작](./README.md#빠른-시작)
- [접속 방법](./README.md#접속-방법)
- [AWS EKS 배포](./DEPLOYMENT.md#프로덕션-환경-aws-eks)
- [일반적인 문제들](./TROUBLESHOOTING.md#일반적인-문제들)

## 💡 팁

1. **빠르게 시작**: README.md부터 읽으세요
2. **자동화 활용**: scripts/ 디렉토리의 스크립트 사용
3. **문제 해결**: TROUBLESHOOTING.md의 빠른 진단 체크리스트 활용
4. **프로덕션 배포**: DEPLOYMENT.md의 AWS 관리형 서비스 섹션 참조

## 📞 지원

문제가 해결되지 않으면:
1. GitHub Issues 확인
2. 팀에 문의
3. Kubernetes 공식 문서 참조
