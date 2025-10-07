# OkChat Kubernetes 문제 해결 가이드

이 문서는 OkChat을 Kubernetes에 배포할 때 발생할 수 있는 일반적인 문제와 해결 방법을 다룹니다.

## 📋 목차

- [빠른 진단](#빠른-진단)
- [일반적인 문제들](#일반적인-문제들)
- [로그 및 디버깅](#로그-및-디버깅)
- [리소스 문제](#리소스-문제)
- [네트워크 문제](#네트워크-문제)

## 🔍 빠른 진단

문제가 발생하면 다음 순서로 확인하세요:

```bash
# 1. Pod 상태
kubectl get pods -n okchat

# 2. 최근 이벤트
kubectl get events -n okchat --sort-by='.lastTimestamp' | tail -20

# 3. 애플리케이션 로그
kubectl logs -n okchat -l app=okchat-app --tail=100

# 4. 리소스 사용량
kubectl top pods -n okchat
kubectl top nodes

# 5. Pod 상세 정보
kubectl describe pod -n okchat <pod-name>
```

## 일반적인 문제들

### 1. Docker Desktop 메모리 부족

**증상:**
```
❌  Exiting due to MK_USAGE: Docker Desktop has only 7937MB memory but you specified 8192MB
```

**해결 방법 A: 더 적은 메모리로 실행 (권장)**

환경 변수로 리소스 설정을 조정:

```bash
# 4GB 메모리로 실행 (최소 권장)
MINIKUBE_MEMORY=4096 ./k8s/scripts/deploy-local.sh

# 커스텀 설정
MINIKUBE_CPUS=2 MINIKUBE_MEMORY=4096 MINIKUBE_DISK=30g ./k8s/scripts/deploy-local.sh
```

**해결 방법 B: Docker Desktop 메모리 증가**

1. Docker Desktop 열기
2. Settings (⚙️) → Resources → Memory
3. Memory를 8GB 이상으로 증가
4. Apply & Restart

### 2. Minikube 불완전한 상태

**증상:**
```
❌  Exiting due to MK_ADDON_ENABLE_PAUSED: enabled failed: get state: unknown state "minikube"
Error response from daemon: No such container: minikube
```

**해결 방법:**

```bash
# 자동 수정 스크립트 사용
./k8s/scripts/fix-minikube.sh

# 또는 수동으로
minikube delete
minikube start --cpus=4 --memory=6144 --disk-size=40g
```

### 3. ImagePullBackOff 에러

**증상:**
```
NAME                              READY   STATUS             RESTARTS   AGE
okchat-app-xxx                    0/1     ImagePullBackOff   0          2m
```

**원인:** Minikube가 로컬 Docker 이미지를 찾을 수 없음

**해결 방법:**

```bash
# Docker 환경을 Minikube로 전환
eval $(minikube docker-env)

# 이미지 다시 빌드
docker build -t okchat:latest .

# 이미지 확인
docker images | grep okchat

# Pod 재시작
kubectl rollout restart deployment/okchat-app -n okchat
```

### 4. Pod가 Pending 상태로 멈춤

**증상:**
```
NAME                              READY   STATUS    RESTARTS   AGE
okchat-opensearch-xxx             0/1     Pending   0          5m
```

**원인:** 리소스 부족 또는 PVC 문제

**해결 방법:**

```bash
# Pod 상세 정보 확인
kubectl describe pod -n okchat <pod-name>

# 노드 리소스 확인
kubectl top nodes

# 리소스가 부족한 경우
MINIKUBE_MEMORY=8192 minikube start --cpus=4
```

### 5. CrashLoopBackOff - OpenAI API Key 문제

**증상:**
```
NAME                              READY   STATUS             RESTARTS   AGE
okchat-app-xxx                    0/1     CrashLoopBackOff   5          10m
```

**로그:**
```
Error: Invalid OpenAI API key
```

**해결 방법:**

```bash
# Secret 확인
kubectl get secret okchat-secret -n okchat -o yaml

# Secret 업데이트
kubectl delete secret okchat-secret -n okchat
kubectl create secret generic okchat-secret \
  --from-literal=OPENAI_API_KEY='your-actual-api-key' \
  --from-literal=SPRING_DATASOURCE_USERNAME=root \
  --from-literal=SPRING_DATASOURCE_PASSWORD=root \
  --from-literal=MYSQL_ROOT_PASSWORD=root \
  --namespace=okchat

# Deployment 재시작
kubectl rollout restart deployment/okchat-app -n okchat
```

### 6. MySQL 연결 실패

**증상:**
```
Error: Communications link failure
```

**해결 방법:**

```bash
# MySQL Pod 상태 확인
kubectl get pods -n okchat -l app=okchat-mysql

# MySQL 로그 확인
kubectl logs -n okchat -l app=okchat-mysql

# MySQL이 Ready 상태가 될 때까지 대기
kubectl wait --for=condition=ready pod -l app=okchat-mysql -n okchat --timeout=300s

# 네트워크 연결 테스트
kubectl run tmp-shell --rm -i --tty --image busybox -n okchat -- sh
# 내부에서: nc -zv okchat-mysql 3306
```

### 7. OpenSearch vm.max_map_count 오류

**증상:**
```
bootstrap checks failed
max virtual memory areas vm.max_map_count [65530] is too low
```

**해결 방법:**

**Minikube:**
```bash
minikube ssh
sudo sysctl -w vm.max_map_count=262144
exit
```

**Docker Desktop (Mac):**
```bash
# Docker Desktop 4.6.0+ 에서는 자동으로 설정됨
# 이전 버전인 경우 Docker Desktop 업데이트 필요
```

### 8. Ingress에 접속 불가

**증상:**
- `http://okchat.local` 접속 시 연결 실패

**해결 방법:**

```bash
# 1. Ingress 상태 확인
kubectl get ingress -n okchat

# 2. Ingress Controller 확인
kubectl get pods -n ingress-nginx

# 3. /etc/hosts 확인
cat /etc/hosts | grep okchat.local

# 4. /etc/hosts에 추가 (없는 경우)
echo "$(minikube ip) okchat.local" | sudo tee -a /etc/hosts

# 5. 또는 포트 포워딩 사용
kubectl port-forward -n okchat svc/okchat-app 8080:8080
# 접속: http://localhost:8080
```

### 9. Disk Space 부족

**증상:**
```
❌  Exiting due to GUEST_PROVISION: Failed to start host
```

**해결 방법:**

```bash
# Minikube 이미지 정리
minikube ssh
docker system prune -a --volumes -f
exit

# 또는 Minikube 재생성
minikube delete
MINIKUBE_DISK=50g ./k8s/scripts/deploy-local.sh
```

### 10. Health Check 실패

**증상:**
```
Readiness probe failed: Get "http://10.244.0.5:8080/actuator/health/readiness": dial tcp 10.244.0.5:8080: connect: connection refused
```

**해결 방법:**

```bash
# 애플리케이션 로그 확인
kubectl logs -n okchat -l app=okchat-app --tail=100

# Actuator 엔드포인트 확인
kubectl exec -n okchat <pod-name> -- curl http://localhost:8080/actuator/health

# 시작 시간이 오래 걸리는 경우 initialDelaySeconds 증가
# k8s/overlays/local/deployment-patch.yaml 수정
```

### 11. 환경 변수가 적용되지 않음

**증상:**
- Secret에 값이 있지만 애플리케이션이 기본값을 사용
- `Could not resolve placeholder` 에러

**원인:**
- Deployment에서 환경 변수 매핑이 누락됨

**해결 방법:**

```bash
# Deployment의 환경 변수 확인
kubectl get deployment okchat-app -n okchat -o yaml | grep -A 2 "env:"

# Pod 내부에서 환경 변수 확인
kubectl exec -n okchat <pod-name> -- env | grep OPENAI

# 환경 변수가 없으면 Deployment 수정 필요
# k8s/base/okchat-deployment.yaml 확인
```

### 12. Unknown database 'okchat' 에러

**증상:**
```
java.sql.SQLSyntaxErrorException: Unknown database 'okchat'
```

**원인:**
- MySQL 데이터베이스가 생성되지 않음
- Init ConfigMap이 적용되지 않음

**해결 방법:**

```bash
# 수동으로 데이터베이스 생성
kubectl exec -n okchat <mysql-pod-name> -- mysql -uroot -proot -e \
  "CREATE DATABASE IF NOT EXISTS okchat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 데이터베이스 확인
kubectl exec -n okchat <mysql-pod-name> -- mysql -uroot -proot -e "SHOW DATABASES;"

# MySQL Pod 재시작 (init script 다시 실행)
kubectl delete pod -n okchat -l app=okchat-mysql

# 애플리케이션 Pod 재시작
kubectl delete pod -n okchat -l app=okchat-app
```

## 리소스 요구사항

### 최소 사양
- CPU: 2 cores
- Memory: 4GB
- Disk: 20GB

```bash
MINIKUBE_CPUS=2 MINIKUBE_MEMORY=4096 MINIKUBE_DISK=20g ./k8s/scripts/deploy-local.sh
```

### 권장 사양
- CPU: 4 cores
- Memory: 6GB
- Disk: 40GB

```bash
# 기본값 (권장 사양)
./k8s/scripts/deploy-local.sh
```

### 최적 사양
- CPU: 4+ cores
- Memory: 8GB+
- Disk: 50GB+

```bash
MINIKUBE_CPUS=6 MINIKUBE_MEMORY=8192 MINIKUBE_DISK=50g ./k8s/scripts/deploy-local.sh
```

## 디버깅 명령어 모음

### 상태 확인
```bash
# 모든 리소스 확인
kubectl get all -n okchat

# Pod 상세 정보
kubectl describe pod -n okchat <pod-name>

# 이벤트 확인
kubectl get events -n okchat --sort-by='.lastTimestamp'

# 리소스 사용량
kubectl top pods -n okchat
kubectl top nodes
```

### 로그 확인
```bash
# 실시간 로그
kubectl logs -n okchat -l app=okchat-app -f

# 이전 컨테이너 로그 (재시작한 경우)
kubectl logs -n okchat <pod-name> --previous

# 모든 Pod 로그
kubectl logs -n okchat --all-containers=true -l app=okchat-app
```

### 네트워크 디버깅
```bash
# 임시 디버그 Pod 실행
kubectl run tmp-shell --rm -i --tty --image nicolaka/netshoot -n okchat

# 내부에서 테스트
nslookup okchat-mysql
nc -zv okchat-mysql 3306
nc -zv okchat-redis 6379
nc -zv okchat-opensearch 9200
curl http://okchat-app:8080/actuator/health
```

### Pod 내부 접속
```bash
# Shell 접속
kubectl exec -it -n okchat <pod-name> -- sh

# 특정 명령 실행
kubectl exec -n okchat <pod-name> -- curl http://localhost:8080/actuator/health
```

## 완전히 새로 시작하기

모든 것을 초기화하고 새로 시작:

```bash
# 1. 리소스 삭제
kubectl delete namespace okchat

# 2. Minikube 삭제
minikube delete

# 3. 새로 배포
./k8s/scripts/deploy-local.sh
```

## 도움이 필요한 경우

1. **로그 수집**
   ```bash
   kubectl logs -n okchat -l app=okchat-app --tail=200 > app.log
   kubectl describe pod -n okchat <pod-name> > pod-describe.log
   kubectl get events -n okchat > events.log
   ```

2. **현재 상태 확인**
   ```bash
   kubectl get all -n okchat > resources.log
   minikube status > minikube.log
   docker info > docker.log
   ```

3. 수집한 로그와 함께 팀에 문의

## 유용한 링크

- [Minikube 공식 문서](https://minikube.sigs.k8s.io/docs/)
- [Kubernetes 트러블슈팅](https://kubernetes.io/docs/tasks/debug/)
- [Docker Desktop 설정](https://docs.docker.com/desktop/settings/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)