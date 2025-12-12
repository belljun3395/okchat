# Sentry 통합 가이드

> 중앙화된 에러 추적 및 실시간 알림

---

## 설정

### 1. 의존성 (build.gradle.kts)

```kotlin
implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.18.1")
implementation("io.sentry:sentry-logback:7.18.1")
```

### 2. Sentry 프로젝트 생성

**Self-Hosted (권장):**
```bash
docker run -d \
  --name sentry \
  -p 9000:9000 \
  -e SENTRY_SECRET_KEY=your-secret-key \
  sentry:latest
```

**Cloud (Sentry.io):**
1. https://sentry.io 가입
2. 새 프로젝트 생성 (Spring Boot)
3. DSN 복사

### 3. 환경 변수 설정

```bash
# .env 또는 Kubernetes Secret
export SENTRY_DSN="https://[public-key]@[your-instance]/[project-id]"
export SENTRY_ENVIRONMENT="production"
export SENTRY_TRACES_SAMPLE_RATE="0.1"  # 10% 트레이싱

# 선택사항: Release 추적용
export APP_VERSION="1.0.0"
export GIT_COMMIT=$(git rev-parse --short HEAD)
```

### 4. application.yaml 설정

자동으로 추가됨:

```yaml
sentry:
  dsn: ${SENTRY_DSN:}
  environment: ${SENTRY_ENVIRONMENT:local}
  traces-sample-rate: 1.0
  enable-tracing: true
  send-default-pii: false
  ignored-exceptions-for-type:
    - com.okestro.okchat.exception.NotFoundException
    - com.okestro.okchat.exception.ValidationException
```

---

## 자동 기능

### 1. 에러 자동 캡처

모든 예외가 자동으로 Sentry에 전송됩니다 (무시 목록 제외):

```kotlin
// 자동으로 Sentry에 전송됨
throw ExternalApiException("Confluence API failed", service = "Confluence")
```

### 2. Breadcrumb 기록

사용자 행동이 자동으로 기록됩니다:
- HTTP 요청
- Database 쿼리
- 로그 메시지 (INFO 이상)
- Spring Events

### 3. 트레이스 통합

OpenTelemetry Trace ID가 자동으로 연결됩니다.

---

## 수동 사용법

### 1. 명시적 에러 전송

```kotlin
import io.sentry.Sentry
import io.sentry.SentryLevel

try {
    riskyOperation()
} catch (e: Exception) {
    Sentry.captureException(e) { scope ->
        scope.setTag("operation", "riskyOperation")
        scope.setExtra("userId", userId)
        scope.setLevel(SentryLevel.ERROR)
    }
    // 에러 처리 계속...
}
```

### 2. 커스텀 메시지 전송

```kotlin
Sentry.captureMessage("Important event occurred") { scope ->
    scope.setTag("event_type", "business_critical")
    scope.setLevel(SentryLevel.WARNING)
}
```

### 3. User Context 설정

```kotlin
import io.sentry.protocol.User

Sentry.configureScope { scope ->
    val user = User().apply {
        id = userId
        email = "user@example.com"
        username = "john_doe"
        ipAddress = request.remoteAddress
    }
    scope.user = user
}
```

### 4. Breadcrumb 수동 추가

```kotlin
import io.sentry.Breadcrumb

val breadcrumb = Breadcrumb().apply {
    message = "User clicked button"
    category = "ui.click"
    level = SentryLevel.INFO
    setData("button_id", "submit")
}

Sentry.addBreadcrumb(breadcrumb)
```

### 5. Transaction 추적

```kotlin
import io.sentry.Sentry

val transaction = Sentry.startTransaction("chat.process", "task")

try {
    // 비즈니스 로직
    val span = transaction.startChild("database.query")
    try {
        queryDatabase()
    } finally {
        span.finish()
    }

    transaction.status = SpanStatus.OK
} catch (e: Exception) {
    transaction.throwable = e
    transaction.status = SpanStatus.INTERNAL_ERROR
    throw e
} finally {
    transaction.finish()
}
```

---

## 민감정보 보호

### 자동 필터링

**SentryConfig**에서 자동으로 필터링됩니다:

1. **HTTP Headers**
   - `Authorization`, `Cookie`, `X-API-Key` 등

2. **Query Parameters**
   - `apiKey`, `token`, `password`, `secret`

3. **User 정보**
   - Email: `jo***@example.com`
   - IP: `192.168.1.***`

4. **데이터 패턴**
   - 신용카드 번호
   - JWT 토큰
   - API 키 (32자 이상)

### 수동 필터링

```kotlin
Sentry.configureScope { scope ->
    scope.setContexts("request", mapOf(
        "url" to request.url,
        "method" to request.method,
        "apiKey" to "***FILTERED***"  // 수동 마스킹
    ))
}
```

---

## Slack 알림 연동

### 1. Sentry Slack App 설치

1. Sentry Dashboard → Settings → Integrations
2. Slack 연동
3. 채널 선택: `#okchat-alerts`

### 2. Alert Rules 설정

**Rule 1: 새 에러 발생**
```yaml
Name: New Error Alert
Conditions:
  - An event is first seen
Actions:
  - Send notification to Slack (#okchat-alerts)
  - Assign to: On-call Engineer
```

**Rule 2: 에러 빈도 급증**
```yaml
Name: High Error Frequency
Conditions:
  - An event is seen more than 10 times in 1 hour
Actions:
  - Send notification to Slack (#okchat-critical)
  - Create Jira ticket
```

**Rule 3: 새 릴리즈 에러**
```yaml
Name: New Release Errors
Conditions:
  - An event's release is newer than 1 hour
  - An event is first seen in release
Actions:
  - Send notification to Slack (#okchat-releases)
  - Tag: regression
```

---

## 대시보드 활용

### Sentry Dashboard 주요 메뉴

#### 1. Issues
- 에러 그룹별 조회
- 영향받은 사용자 수
- 발생 빈도 추이

#### 2. Performance
- Transaction 지연시간
- Slow 쿼리
- API 호출 성능

#### 3. Releases
- 버전별 에러 비교
- 릴리즈 건강도 (Crash-free %)
- 배포 전후 비교

#### 4. Alerts
- Alert 규칙 관리
- 알림 이력
- 무음 처리 (Snooze)

---

## 운영 시나리오

### 시나리오 1: 에러 발생 시

1. **Slack 알림 수신**
   ```
   [Sentry] New Error: ExternalApiException
   Message: Confluence API timeout
   URL: https://sentry.io/organizations/okestro/issues/12345/
   Environment: production
   Release: okchat@abc1234
   ```

2. **Sentry Dashboard 확인**
   - Stack trace 분석
   - Breadcrumbs로 사용자 행동 추적
   - 영향받은 사용자 수 확인

3. **Trace ID로 로그 연결**
   ```bash
   # Sentry에서 Trace ID 복사
   grep "abc-123-def-456" logs/okchat.log
   ```

4. **수정 및 배포**
   - 버그 수정
   - Sentry Issue에 커밋 연결
   ```bash
   git commit -m "Fix Confluence timeout (Fixes SENT-12345)"
   ```

5. **Release 생성**
   ```bash
   sentry-cli releases new okchat@1.0.1
   sentry-cli releases set-commits okchat@1.0.1 --auto
   sentry-cli releases finalize okchat@1.0.1
   ```

### 시나리오 2: 성능 저하 감지

1. **Performance 탭에서 느린 Transaction 확인**
2. **Span 분석으로 병목 구간 식별**
3. **Trace ID로 상세 로그 확인**
4. **최적화 후 성능 비교**

---

## 환경별 설정

### Local (개발)
```yaml
sentry:
  dsn: ""  # 비활성화
  traces-sample-rate: 0
```

### Staging
```yaml
sentry:
  dsn: ${SENTRY_DSN}
  environment: staging
  traces-sample-rate: 0.5  # 50% 샘플링
```

### Production
```yaml
sentry:
  dsn: ${SENTRY_DSN}
  environment: production
  traces-sample-rate: 0.1  # 10% 샘플링 (비용 절약)
```

---

## Sentry CLI 사용

### 설치
```bash
npm install -g @sentry/cli
# 또는
brew install getsentry/tools/sentry-cli
```

### 인증
```bash
sentry-cli login
```

### Release 생성
```bash
# 1. 새 Release 생성
sentry-cli releases new okchat@1.0.0

# 2. Commit 연결
sentry-cli releases set-commits okchat@1.0.0 --auto

# 3. Source Map 업로드 (Frontend용)
sentry-cli releases files okchat@1.0.0 upload-sourcemaps ./dist

# 4. Release 완료
sentry-cli releases finalize okchat@1.0.0

# 5. 배포 기록
sentry-cli releases deploys okchat@1.0.0 new -e production
```

---

## 모니터링 메트릭

### Prometheus 메트릭

Sentry SDK가 자동으로 노출:

```promql
# Sentry로 전송된 에러 수
sentry_events_sent_total

# Sentry 전송 실패 수
sentry_events_dropped_total

# Sentry 전송 지연
sentry_events_send_duration_seconds
```

### Grafana 패널

```json
{
  "title": "Sentry Integration Health",
  "targets": [
    {
      "expr": "rate(sentry_events_sent_total[5m])",
      "legendFormat": "Events Sent/sec"
    },
    {
      "expr": "rate(sentry_events_dropped_total[5m])",
      "legendFormat": "Events Dropped/sec"
    }
  ]
}
```

---

## Best Practices

### 1. 무시할 에러 설정
```kotlin
// application.yaml
sentry:
  ignored-exceptions-for-type:
    - NotFoundException  # 404는 정상
    - ValidationException  # 사용자 입력 오류
```

### 2. 샘플링 전략
- 개발: 100% (모든 에러 캡처)
- 스테이징: 50% (충분한 데이터)
- 프로덕션: 10% (비용 절약)

### 3. Release 추적
- 모든 배포에 Release 생성
- Git commit 자동 연결
- 릴리즈별 에러 추이 모니터링

### 4. Alert 노이즈 방지
- 빈도 임계값 설정 (10회/시간)
- 새 에러만 알림
- 무음 처리 활용

### 5. 개인정보 보호
- `send-default-pii: false` 유지
- 커스텀 필터링 적극 활용
- 정기적인 감사

---

## 비용 최적화

### Sentry Cloud 요금제
- **Free**: 5,000 events/월
- **Team**: $26/월 (50,000 events)
- **Business**: $80/월 (100,000 events)

### 비용 절감 팁
1. **샘플링 활용**: 프로덕션 10%
2. **무시 목록 관리**: 불필요한 에러 제외
3. **Before Send Hook**: 필터링 강화
4. **Self-Hosted**: 무제한 (인프라 비용만)

### Self-Hosted 예상 비용
- Small (< 100K events/월): $50/월 (서버)
- Medium (< 1M events/월): $200/월 (서버 + DB)

---

## 문제 해결

### 에러가 Sentry에 전송되지 않음
```bash
# 1. DSN 확인
echo $SENTRY_DSN

# 2. 네트워크 테스트
curl -X POST \
  "https://[your-instance]/api/[project-id]/store/" \
  -H "X-Sentry-Auth: Sentry sentry_key=[your-key]" \
  -d '{}'

# 3. 로그 확인
# application.yaml에 debug: true 설정
```

### 너무 많은 에러 전송
```yaml
# Before Send Hook에서 필터링 강화
# SentryConfig.kt 참조
```

### 민감정보 유출
```kotlin
// SentryConfig의 filterSensitiveData() 함수 강화
// 정규식 패턴 추가
```

---

**작성일**: 2025-12-12
**작성자**: DevOps Team
**버전**: 1.0
