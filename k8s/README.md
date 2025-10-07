# OkChat Kubernetes 배포 가이드

OkChat을 Kubernetes 환경에 배포하기 위한 완전한 가이드입니다.

## 📋 목차

- [개요](#개요)
- [빠른 시작](#빠른-시작)
- [프로젝트 구조](#프로젝트-구조)
- [상세 배포 가이드](#상세-배포-가이드)
- [접속 방법](#접속-방법)
- [유용한 명령어](#유용한-명령어)
- [문제 해결](#문제-해결)

## 🎯 개요

### 애플리케이션 스택

- **Application**: Spring Boot 3.5.6 (Kotlin + WebFlux)
- **Database**: MySQL 8.0
- **Cache**: Redis 7
- **Vector Store**: OpenSearch 2.18
- **AI**: Spring AI + OpenAI

### 지원 환경

- **로컬 개발**: Minikube
- **프로덕션**: AWS EKS

### 주요 기능

- ✅ 완전한 환경 변수 관리
- ✅ MySQL 데이터베이스 자동 초기화
- ✅ Health checks & Probes
- ✅ Horizontal Pod Autoscaling (HPA)
- ✅ Persistent Volumes
- ✅ Ingress 설정
- ✅ 로컬/프로덕션 환경 분리 (Kustomize)

## 🚀 빠른 시작

### 사전 요구사항

**공통:**
- Docker Desktop
- kubectl CLI

**로컬 개발:**
- Minikube
- **최소**: 4GB RAM, 2 CPU, 20GB 디스크
- **권장**: 6GB RAM, 4 CPU, 40GB 디스크

### 5분 안에 시작하기

#### 1. Secret 설정 (필수!)

```bash
# OpenAI API 키를 실제 값으로 변경
vim k8s/base/secret.yaml
```

**중요**: `OPENAI_API_KEY`를 실제 키로 반드시 변경하세요!

#### 2. 자동 배포

```bash
# 모든 것을 자동으로 설정하고 배포
./k8s/scripts/deploy-local.sh
```

이 스크립트는 자동으로:
- Minikube 상태 확인 및 시작
- Ingress 활성화
- Docker 이미지 빌드
- Kubernetes 리소스 배포
- /etc/hosts 설정

#### 3. 접속

배포가 완료되면 (약 2-3분 소요):

```bash
# 방법 1: Port Forward (권장)
kubectl port-forward -n okchat svc/okchat-app 8080:8080

# 그 다음 브라우저에서:
# http://localhost:8080/actuator/health
```

```bash
# 방법 2: 접속 도우미 스크립트
./k8s/scripts/access-local.sh
```

### 리소스 조정

메모리가 부족한 경우:

```bash
# 4GB 메모리로 실행
MINIKUBE_MEMORY=4096 ./k8s/scripts/deploy-local.sh

# 최소 사양
MINIKUBE_CPUS=2 MINIKUBE_MEMORY=4096 MINIKUBE_DISK=20g ./k8s/scripts/deploy-local.sh
```

## 📁 프로젝트 구조

```
k8s/
├── base/                          # 기본 Kubernetes 리소스
│   ├── namespace.yaml            # okchat 네임스페이스
│   ├── configmap.yaml            # 애플리케이션 설정
│   ├── secret.yaml               # 민감한 정보 (API 키 등)
│   ├── mysql-init-configmap.yaml # MySQL 자동 초기화 스크립트
│   ├── pvc.yaml                  # 영구 볼륨 클레임
│   ├── mysql-deployment.yaml     # MySQL 8.0
│   ├── redis-deployment.yaml     # Redis 7
│   ├── opensearch-deployment.yaml # OpenSearch 2.18
│   ├── okchat-deployment.yaml    # Spring Boot 애플리케이션
│   ├── ingress.yaml              # Ingress 설정
│   └── kustomization.yaml        # Kustomize 기본 설정
│
├── overlays/
│   ├── local/                    # Minikube용 로컬 설정
│   │   ├── kustomization.yaml
│   │   ├── deployment-patch.yaml # 리소스 최소화
│   │   ├── pvc-patch.yaml        # standard storageClass
│   │   └── ingress-patch.yaml
│   │
│   └── production/               # AWS EKS용 프로덕션 설정
│       ├── kustomization.yaml
│       ├── deployment-patch.yaml  # 3 replicas, ECR 이미지
│       ├── pvc-patch.yaml        # gp3 storageClass
│       ├── ingress-patch.yaml    # ALB 설정
│       ├── hpa.yaml              # Auto-scaling
│       └── aws-services.yaml     # AWS 관리형 서비스 가이드
│
├── scripts/
│   ├── deploy-local.sh           # 로컬 자동 배포
│   ├── cleanup-local.sh          # 리소스 정리
│   ├── access-local.sh           # 접속 도우미
│   ├── fix-minikube.sh           # Minikube 문제 해결
│   └── build-and-push.sh         # ECR 이미지 푸시
│
├── README.md                     # 이 문서
├── DEPLOYMENT.md                 # 상세 배포 가이드
└── TROUBLESHOOTING.md            # 문제 해결 가이드
```

### 주요 리소스 설명

| 리소스 | 설명 | 포트 |
|--------|------|------|
| okchat-app | Spring Boot 애플리케이션 | 8080 |
| okchat-mysql | MySQL 데이터베이스 | 3306 |
| okchat-redis | Redis 캐시 | 6379 |
| okchat-opensearch | OpenSearch 벡터 스토어 | 9200, 9600 |

## 📚 상세 배포 가이드

로컬 및 AWS EKS 상세 배포 가이드는 별도 문서를 참조하세요:

👉 **[DEPLOYMENT.md](./DEPLOYMENT.md)** - 상세 배포 가이드

주요 내용:
- 로컬 개발 환경 (Minikube) 상세 설정
- AWS EKS 프로덕션 배포 완전 가이드
- AWS 관리형 서비스 (RDS, ElastiCache, OpenSearch) 설정
- 모니터링 및 로깅 설정

## 🔌 접속 방법

### 방법 1: Port Forwarding (권장) ⭐

가장 간단하고 안정적:

```bash
kubectl port-forward -n okchat svc/okchat-app 8080:8080
```

**접속:**
- Application: http://localhost:8080
- Health Check: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Prometheus: http://localhost:8080/actuator/prometheus

### 방법 2: Minikube Service

자동으로 브라우저 열기:

```bash
minikube service okchat-app -n okchat
```

### 방법 3: Ingress + Tunnel

실제 도메인 이름으로 접속:

```bash
# 별도 터미널에서 (sudo 필요)
sudo minikube tunnel

# 접속
# http://okchat.local
```

### 방법 4: 자동화 스크립트

```bash
./k8s/scripts/access-local.sh
```

대화형으로 접속 방법을 선택할 수 있습니다.

## 💻 유용한 명령어

### 상태 확인

```bash
# 모든 리소스 확인
kubectl get all -n okchat

# Pod 상태
kubectl get pods -n okchat

# Pod 상세 정보
kubectl describe pod -n okchat <pod-name>

# 리소스 사용량
kubectl top pods -n okchat
kubectl top nodes
```

### 로그 확인

```bash
# 실시간 로그
kubectl logs -n okchat -l app=okchat-app -f

# 최근 100줄
kubectl logs -n okchat -l app=okchat-app --tail=100

# 특정 Pod
kubectl logs -n okchat <pod-name>

# 이전 컨테이너 로그 (재시작된 경우)
kubectl logs -n okchat <pod-name> --previous
```

### 디버깅

```bash
# Pod 내부 접속
kubectl exec -it -n okchat <pod-name> -- sh

# 명령 실행
kubectl exec -n okchat <pod-name> -- curl http://localhost:8080/actuator/health

# 임시 디버그 Pod
kubectl run tmp-shell --rm -i --tty --image busybox -n okchat -- sh
```

### 스케일링

```bash
# 수동 스케일링
kubectl scale deployment/okchat-app -n okchat --replicas=3

# HPA 상태 확인 (프로덕션)
kubectl get hpa -n okchat
kubectl describe hpa okchat-app-hpa -n okchat
```

### 업데이트 및 롤백

```bash
# 새 이미지로 업데이트
kubectl set image deployment/okchat-app okchat=okchat:v1.1.0 -n okchat

# 롤아웃 상태 확인
kubectl rollout status deployment/okchat-app -n okchat

# 롤아웃 히스토리
kubectl rollout history deployment/okchat-app -n okchat

# 롤백
kubectl rollout undo deployment/okchat-app -n okchat

# 특정 리비전으로 롤백
kubectl rollout undo deployment/okchat-app --to-revision=2 -n okchat

# 재시작
kubectl rollout restart deployment/okchat-app -n okchat
```

### 데이터베이스 작업

```bash
# MySQL 접속
kubectl exec -it -n okchat <mysql-pod-name> -- mysql -uroot -proot okchat

# 데이터베이스 백업
kubectl exec -n okchat <mysql-pod-name> -- mysqldump -uroot -proot okchat > backup.sql

# 데이터베이스 복구
kubectl exec -i -n okchat <mysql-pod-name> -- mysql -uroot -proot okchat < backup.sql
```

## 🔧 문제 해결

### 빠른 체크리스트

```bash
# 1. Pod 상태 확인
kubectl get pods -n okchat

# 2. 애플리케이션 로그 확인
kubectl logs -n okchat -l app=okchat-app --tail=50

# 3. 이벤트 확인
kubectl get events -n okchat --sort-by='.lastTimestamp'

# 4. 리소스 사용량 확인
kubectl top pods -n okchat
```

### 일반적인 문제들

#### 1. Pod가 CrashLoopBackOff

```bash
# 로그 확인
kubectl logs -n okchat <pod-name>
kubectl logs -n okchat <pod-name> --previous

# 일반적인 원인:
# - Secret이 제대로 설정되지 않음
# - 데이터베이스 연결 실패
# - 메모리 부족
```

#### 2. ImagePullBackOff

```bash
# Minikube Docker 환경 확인
eval $(minikube docker-env)
docker images | grep okchat

# 이미지가 없으면 다시 빌드
docker build -t okchat:latest .
```

#### 3. Unknown database 'okchat'

데이터베이스가 자동 생성되지 않은 경우:

```bash
# 수동으로 생성
kubectl exec -n okchat <mysql-pod-name> -- mysql -uroot -proot -e \
  "CREATE DATABASE IF NOT EXISTS okchat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# MySQL Pod 재시작 (init script 실행)
kubectl delete pod -n okchat -l app=okchat-mysql
```

#### 4. Minikube 불완전한 상태

```bash
# 자동 수정 스크립트 사용
./k8s/scripts/fix-minikube.sh

# 또는 수동으로
minikube delete
minikube start --cpus=4 --memory=6144
```

#### 5. 메모리 부족

```bash
# Minikube 재시작 with more memory
minikube stop
MINIKUBE_MEMORY=8192 ./k8s/scripts/deploy-local.sh
```

자세한 문제 해결 가이드는 👉 **[TROUBLESHOOTING.md](./TROUBLESHOOTING.md)**

## 🔄 정리 및 재배포

### 리소스 정리

```bash
# 자동 정리
./k8s/scripts/cleanup-local.sh

# 또는 수동으로
kubectl delete -k k8s/overlays/local

# 완전히 초기화
minikube delete
```

### 재배포

```bash
# 전체 재배포
./k8s/scripts/deploy-local.sh

# 애플리케이션만 재배포
kubectl rollout restart deployment/okchat-app -n okchat

# 이미지 다시 빌드 후 재배포
eval $(minikube docker-env)
docker build -t okchat:latest .
kubectl delete pod -n okchat -l app=okchat-app
```

## 🏭 프로덕션 배포 (AWS EKS)

프로덕션 배포에 대한 상세한 가이드는:

👉 **[DEPLOYMENT.md](./DEPLOYMENT.md)** - AWS EKS 섹션 참조

**주요 단계:**
1. EKS 클러스터 생성
2. AWS Load Balancer Controller 설치
3. EBS CSI Driver 설치
4. ECR에 이미지 푸시
5. Secret 및 설정 업데이트
6. 프로덕션 환경 배포

**프로덕션 권장사항:**
- AWS RDS for MySQL (대신 자체 호스팅)
- Amazon ElastiCache for Redis
- Amazon OpenSearch Service
- ALB Ingress with SSL/TLS
- HPA 활성화 (3-10 pods)

## 📊 모니터링

### Health Checks

```bash
# Liveness
curl http://localhost:8080/actuator/health/liveness

# Readiness
curl http://localhost:8080/actuator/health/readiness

# 전체 Health
curl http://localhost:8080/actuator/health
```

### 메트릭

```bash
# Prometheus 메트릭
curl http://localhost:8080/actuator/prometheus

# 애플리케이션 메트릭
curl http://localhost:8080/actuator/metrics

# JVM 메모리
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### 리소스 모니터링

```bash
# Pod 리소스 사용량
kubectl top pods -n okchat

# 노드 리소스
kubectl top nodes

# HPA 상태
kubectl get hpa -n okchat
```

## 🔒 보안 권장사항

1. **Secret 관리**
   - Git에 실제 Secret 커밋 금지
   - AWS Secrets Manager 또는 External Secrets Operator 사용
   - 정기적인 키 로테이션

2. **네트워크 보안**
   - Network Policy 적용
   - TLS/SSL 인증서 사용
   - 불필요한 포트 차단

3. **이미지 보안**
   - 최신 베이스 이미지 사용
   - 정기적인 취약점 스캔
   - Non-root 사용자로 실행

4. **RBAC**
   - 최소 권한 원칙
   - ServiceAccount 분리
   - Role 및 RoleBinding 명확히 정의

## 📖 추가 리소스

### 문서
- [상세 배포 가이드](./DEPLOYMENT.md)
- [문제 해결 가이드](./TROUBLESHOOTING.md)

### 외부 링크
- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [Minikube 문서](https://minikube.sigs.k8s.io/docs/)
- [AWS EKS 사용자 가이드](https://docs.aws.amazon.com/eks/)
- [Kustomize 문서](https://kustomize.io/)
- [Spring Boot Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)

## 🤝 지원

문제가 발생하면:
1. [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) 확인
2. Pod 로그 및 이벤트 확인
3. 리소스 상태 확인
4. 팀에 문의

## 📝 변경 이력

- **2025-10-07**: 초기 Kubernetes 구성 완료
  - 로컬 (Minikube) 및 프로덕션 (AWS EKS) 지원
  - 자동 배포 스크립트 추가
  - MySQL 자동 초기화
  - 환경 변수 완전 매핑
  - Health checks 및 Probes 설정