# Observability 구현 TODO 리스트

> **작성일**: 2025-12-12
> **목적**: 단계별 구현 추적

---

## 진행 현황

| Phase | 전체 | 완료 | 진행중 | 진행률 |
|-------|------|------|--------|--------|
| Phase 1: 기초 | 15 | 0 | 0 | 0% |
| Phase 2: 고급 | 23 | 0 | 0 | 0% |
| Phase 3: 엔터프라이즈 | 16 | 0 | 0 | 0% |
| **전체** | **54** | **0** | **0** | **0%** |

---

## 🔴 Phase 1: 기초 강화 (1-2주)

### 1.1 GlobalExceptionHandler (3일)

**우선순위**: 🔴 Critical
**담당자**: [할당 필요]

- [ ] **ErrorCode enum 정의** (4h)
  - [ ] NOT_FOUND (E001, 404)
  - [ ] INVALID_INPUT (E002, 400)
  - [ ] UNAUTHORIZED (E003, 401)
  - [ ] FORBIDDEN (E004, 403)
  - [ ] RATE_LIMIT_EXCEEDED (E005, 429)
  - [ ] EXTERNAL_SERVICE_ERROR (E006, 503)
  - [ ] INTERNAL_SERVER_ERROR (E999, 500)

- [ ] **ErrorResponse DTO** (2h)
  - [ ] code, message, timestamp, path, traceId, details

- [ ] **GlobalExceptionHandler 구현** (1일)
  - [ ] @RestControllerAdvice
  - [ ] classifyError() 로직
  - [ ] logError() 로직
  - [ ] recordMetrics() 로직
  - [ ] buildErrorResponse()

- [ ] **커스텀 Exception 클래스** (4h)
  - [ ] NotFoundException
  - [ ] ValidationException
  - [ ] AuthenticationException
  - [ ] RateLimitException
  - [ ] ExternalApiException
  - [ ] DatabaseException

- [ ] **에러 메트릭 정의** (2h)
  - [ ] api.errors.total (counter)
  - [ ] Tags: error_code, error_type, path, method

- [ ] **단위 테스트** (4h)
  - [ ] NotFoundException 처리
  - [ ] ValidationException 처리
  - [ ] 일반 Exception 처리
  - [ ] 에러 응답 형식 검증
  - [ ] 메트릭 기록 검증
  - [ ] traceId 포함 검증

- [ ] **기존 컨트롤러 통합** (2h)
  - [ ] PromptController 로컬 핸들러 제거
  - [ ] 커스텀 Exception 사용
  - [ ] 통합 테스트

- [ ] **문서화** (1h)
  - [ ] 에러 코드 테이블
  - [ ] API 문서 업데이트

**완료 기준:**
- ✅ 모든 API가 표준 에러 응답 반환
- ✅ Prometheus에서 `api.errors.total` 확인
- ✅ 테스트 커버리지 80% 이상

---

### 1.2 민감정보 마스킹 (2일)

**우선순위**: 🔴 Critical
**담당자**: [할당 필요]

- [ ] **SensitiveDataMaskingConverter 구현** (1일)
  - [ ] MessageConverter 인터페이스
  - [ ] Regex 패턴 맵
  - [ ] 패턴별 마스킹 로직

- [ ] **마스킹 패턴 정의** (4h)
  - [ ] password 패턴
  - [ ] apiKey 패턴 (대소문자 무시)
  - [ ] email 패턴
  - [ ] creditCard 패턴
  - [ ] phone 패턴

- [ ] **마스킹 함수** (2h)
  - [ ] maskPassword(): "***MASKED***"
  - [ ] maskApiKey(): "***MASKED***"
  - [ ] maskEmail(): "us***@example.com"
  - [ ] maskCreditCard(): "**** **** **** 1234"

- [ ] **logback-spring.xml 업데이트** (1h)
  - [ ] JSON appender에 converter 추가
  - [ ] CONSOLE appender에 converter 추가
  - [ ] FILE appender에 converter 추가

- [ ] **마스킹 테스트** (4h)
  - [ ] 비밀번호 마스킹
  - [ ] API 키 마스킹 (다양한 형식)
  - [ ] 이메일 마스킹
  - [ ] 신용카드 마스킹
  - [ ] 여러 패턴 동시 존재
  - [ ] 성능 테스트

- [ ] **실제 로그 검증** (2h)
  - [ ] 개발 환경에서 민감정보 로그 생성
  - [ ] 마스킹 적용 확인
  - [ ] false positive 검증

- [ ] **문서화** (1h)
  - [ ] 마스킹 규칙 문서화
  - [ ] 새 패턴 추가 방법

**완료 기준:**
- ✅ 로그에 평문 비밀번호/API 키 0건
- ✅ 이메일 부분 마스킹 확인
- ✅ 로깅 성능 저하 < 5%

---

### 1.3 RED 메트릭 표준화 (2일)

**우선순위**: 🔴 Critical
**담당자**: [할당 필요]

- [ ] **MetricsWebFilter 구현** (1일)
  - [ ] WebFilter 인터페이스
  - [ ] MeterRegistry 주입
  - [ ] 요청 시작 시간 기록
  - [ ] 응답 후 메트릭 수집

- [ ] **Path Normalization** (4h)
  - [ ] UUID 패턴: /api/users/{uuid}
  - [ ] 숫자 ID: /api/users/{id}
  - [ ] 날짜 패턴: /api/logs/{date}

- [ ] **RED 메트릭 수집** (4h)
  - [ ] Rate: http_requests_total
  - [ ] Error: http_requests_errors_total
  - [ ] Duration: http_request_duration_seconds
  - [ ] Histogram buckets 설정

- [ ] **기존 메트릭 통합** (2h)
  - [ ] MetricAspect와 중복 제거
  - [ ] 네이밍 통일 (snake_case)
  - [ ] 불필요한 메트릭 제거

- [ ] **Prometheus 쿼리** (2h)
  - [ ] 에러율 쿼리
  - [ ] P50/P95/P99 지연 쿼리
  - [ ] RPS 쿼리

- [ ] **테스트** (2h)
  - [ ] 통합 테스트
  - [ ] Path normalization 테스트
  - [ ] 에러 발생 시 메트릭 확인

- [ ] **Grafana 패널** (2h)
  - [ ] RED Dashboard 생성
  - [ ] Rate 패널
  - [ ] Error 패널
  - [ ] Duration 패널

**완료 기준:**
- ✅ Prometheus에서 RED 메트릭 쿼리 가능
- ✅ Grafana에서 엔드포인트별 성능 시각화
- ✅ 동적 경로 올바르게 정규화

---

## 🟡 Phase 2: 고급 기능 (2-3주)

### 2.1 SLI/SLO 정의 (3일)

**우선순위**: 🟡 High
**담당자**: [할당 필요]

- [ ] **SLI 정의 문서화** (2h)
  - [ ] 가용성: 99.9%
  - [ ] 지연시간: P95 < 500ms
  - [ ] 품질: 만족도 > 80%

- [ ] **SloConfig 구현** (1일)
  - [ ] @Configuration 클래스
  - [ ] @Scheduled 메서드
  - [ ] calculateAvailabilitySlo()
  - [ ] calculateLatencySlo()
  - [ ] calculateQualitySlo()
  - [ ] calculateErrorBudget()

- [ ] **SLI 측정 로직** (4h)
  - [ ] 가용성 계산
  - [ ] P95 지연시간 추출
  - [ ] 사용자 만족도

- [ ] **에러 예산 계산** (2h)
  - [ ] 월간 예산 계산
  - [ ] 남은 예산 비율
  - [ ] 소진 속도 추정

- [ ] **Alert 조건** (2h)
  - [ ] checkAlertConditions()
  - [ ] 에러 예산 < 10% 경고
  - [ ] P95 > 500ms 경고
  - [ ] 만족도 < 80% 경고

- [ ] **Prometheus Alert Rules** (2h)
  - [ ] AvailabilitySLOBreach
  - [ ] ErrorBudgetLow
  - [ ] LatencySLOBreach
  - [ ] QualitySLOBreach

- [ ] **Grafana SLO Dashboard** (4h)
  - [ ] SLO Overview 패널
  - [ ] 가용성 Gauge
  - [ ] 에러 예산 Graph
  - [ ] P95 지연시간 Graph

- [ ] **단위 테스트** (2h)
  - [ ] calculateErrorBudget() 테스트
  - [ ] Alert 조건 테스트

- [ ] **문서화** (2h)
  - [ ] SLO 정의 및 근거
  - [ ] 배포 결정 프로세스

**완료 기준:**
- ✅ Grafana에서 SLI 실시간 확인
- ✅ 에러 예산 < 10% 시 알림
- ✅ SLO 문서 팀 공유

---

### 2.2 Circuit Breaker (3일)

**우선순위**: 🟡 High
**담당자**: [할당 필요]

- [ ] **의존성 추가** (30min)
  - [ ] resilience4j-spring-boot3
  - [ ] resilience4j-reactor
  - [ ] resilience4j-micrometer

- [ ] **ResilienceConfig** (2h)
  - [ ] CircuitBreakerRegistry Bean
  - [ ] failureRateThreshold: 50%
  - [ ] slowCallRateThreshold: 50%
  - [ ] waitDurationInOpenState: 30s
  - [ ] Micrometer 바인딩

- [ ] **Confluence API 적용** (4h)
  - [ ] CircuitBreaker 인스턴스 생성
  - [ ] getPages() 적용
  - [ ] getPageContent() 적용
  - [ ] Fallback: 캐시 반환

- [ ] **OpenAI API 적용** (4h)
  - [ ] CircuitBreaker 인스턴스
  - [ ] chat() 적용
  - [ ] embedding() 적용
  - [ ] Fallback: 기본 응답

- [ ] **Email Provider 적용** (2h)
  - [ ] CircuitBreaker 인스턴스
  - [ ] sendEmail() 적용
  - [ ] Fallback: 큐에 추가

- [ ] **Fallback 전략** (4h)
  - [ ] Confluence: Redis 캐시
  - [ ] OpenAI: 기본 응답
  - [ ] Email: 재시도 큐
  - [ ] Circuit Open 로깅

- [ ] **메트릭** (2h)
  - [ ] resilience4j_circuitbreaker_state
  - [ ] resilience4j_circuitbreaker_failure_rate
  - [ ] resilience4j_circuitbreaker_calls_total

- [ ] **통합 테스트** (4h)
  - [ ] Confluence API 장애 시뮬레이션
  - [ ] Circuit Open 확인
  - [ ] Fallback 동작 검증
  - [ ] Half-Open 전이 테스트

- [ ] **문서화** (2h)
  - [ ] Circuit Breaker 설정
  - [ ] Fallback 동작 가이드

**완료 기준:**
- ✅ Confluence API 10번 실패 시 Circuit Open
- ✅ Fallback 동작 확인
- ✅ Prometheus에서 Circuit 상태 확인

---

### 2.3 Sentry 통합 (2일)

**우선순위**: 🟡 High
**담당자**: [할당 필요]

- [ ] **Sentry 설정** (1h)
  - [ ] Sentry.io 계정
  - [ ] 프로젝트 생성
  - [ ] DSN 획득
  - [ ] 환경 변수 설정

- [ ] **의존성 추가** (30min)
  - [ ] sentry-spring-boot-starter-jakarta
  - [ ] sentry-logback

- [ ] **application.yaml** (1h)
  - [ ] sentry.dsn
  - [ ] sentry.environment
  - [ ] sentry.traces-sample-rate: 0.1
  - [ ] ignored-exceptions-for-type

- [ ] **GlobalExceptionHandler 통합** (2h)
  - [ ] IHub 주입
  - [ ] shouldReportToSentry()
  - [ ] captureException()
  - [ ] Scope 설정
  - [ ] 민감정보 필터링

- [ ] **에러 심각도 매핑** (1h)
  - [ ] ValidationException → INFO
  - [ ] ExternalApiException → WARNING
  - [ ] DatabaseException → ERROR
  - [ ] SecurityException → FATAL

- [ ] **Breadcrumbs** (2h)
  - [ ] HTTP 요청 자동 기록
  - [ ] Database 쿼리
  - [ ] 비즈니스 이벤트

- [ ] **Release Tracking** (1h)
  - [ ] Git commit SHA
  - [ ] Version 태그

- [ ] **Slack 알림** (1h)
  - [ ] Slack App 설치
  - [ ] 채널: #okchat-alerts
  - [ ] Alert Rules 설정

- [ ] **테스트** (2h)
  - [ ] 의도적 에러 발생
  - [ ] Sentry Dashboard 확인
  - [ ] Slack 알림 수신

- [ ] **문서화** (1h)
  - [ ] Sentry 사용 가이드

**완료 기준:**
- ✅ 에러 1분 내 Sentry 알림
- ✅ Slack 에러 상세 확인
- ✅ 민감정보 필터링 확인

---

### 2.4 수동 Span 생성 (2일)

**우선순위**: 🟡 High
**담당자**: [할당 필요]

- [ ] **Tracer 주입** (1h)
  - [ ] TracingConfig 업데이트
  - [ ] Tracer Bean 확인

- [ ] **DocumentBaseChatService** (4h)
  - [ ] chat.process Span
  - [ ] chat.search Span
  - [ ] chat.ai_call Span
  - [ ] Span attributes
  - [ ] Exception 기록
  - [ ] Events 추가

- [ ] **Span Attributes 표준** (2h)
  - [ ] chat.* attributes
  - [ ] search.* attributes
  - [ ] ai.* attributes
  - [ ] AttributeKey 상수

- [ ] **주요 서비스에 Span 추가** (6h)
  - [ ] ConfluenceService
  - [ ] EmailService
  - [ ] VectorStoreService
  - [ ] QueryClassifier

- [ ] **에러 Span 마킹** (2h)
  - [ ] recordException()
  - [ ] setStatus(ERROR)
  - [ ] 에러 메시지 포함

- [ ] **Jaeger 설정** (2h)
  - [ ] Docker Compose에 Jaeger
  - [ ] OTLP endpoint 확인
  - [ ] Jaeger UI 접속

- [ ] **테스트** (2h)
  - [ ] Chat 요청 실행
  - [ ] Jaeger에서 Trace 확인
  - [ ] Span 계층 구조 검증

- [ ] **문서화** (1h)
  - [ ] Span 네이밍 컨벤션
  - [ ] Attributes 표준

**완료 기준:**
- ✅ Jaeger에서 전체 실행 경로 확인
- ✅ 단계별 지연시간 측정
- ✅ 에러 위치 정확 식별

---

## 🟢 Phase 3: 엔터프라이즈 (3-4주)

### 3.1 보안 감사 로그 (2일)

**우선순위**: 🟢 Medium
**담당자**: [할당 필요]

- [ ] **SecurityAuditLogger** (4h)
  - [ ] @Component
  - [ ] AUDIT Logger
  - [ ] logAuthentication()
  - [ ] logDataAccess()
  - [ ] logSecurityViolation()
  - [ ] logPermissionChange()

- [ ] **AUDIT Appender** (2h)
  - [ ] AUDIT appender 생성
  - [ ] RollingFileAppender
  - [ ] 365일 보존
  - [ ] LogstashEncoder

- [ ] **인증/인가 로깅** (4h)
  - [ ] 로그인 성공/실패
  - [ ] 로그아웃
  - [ ] 토큰 갱신
  - [ ] 권한 부족

- [ ] **데이터 접근 로깅** (4h)
  - [ ] Chat API 접근
  - [ ] Document 조회
  - [ ] User 정보 조회

- [ ] **보안 위반 로깅** (2h)
  - [ ] Rate Limit 초과
  - [ ] 잘못된 토큰
  - [ ] 공격 시도 감지

- [ ] **메트릭** (2h)
  - [ ] security.authentication
  - [ ] security.violations
  - [ ] security.data_access

- [ ] **테스트** (2h)
  - [ ] 인증 실패 로그
  - [ ] 데이터 접근 로그
  - [ ] 로그 파일 로테이션

- [ ] **문서화** (1h)
  - [ ] 감사 로그 조회
  - [ ] 컴플라이언스 리포트

**완료 기준:**
- ✅ logs/audit.log 생성
- ✅ JSON 형식 기록
- ✅ 365일 보존 설정

---

### 3.2 Grafana 대시보드 (3일)

**우선순위**: 🟢 Medium
**담당자**: [할당 필요]

- [ ] **Grafana 설치** (2h)
  - [ ] Docker Compose에 추가
  - [ ] Admin 계정
  - [ ] Prometheus 데이터 소스

- [ ] **SLO Overview** (4h)
  - [ ] Availability Gauge
  - [ ] Error Budget Graph
  - [ ] P95 Latency Graph
  - [ ] User Satisfaction Gauge

- [ ] **RED Metrics** (4h)
  - [ ] Rate Panel
  - [ ] Error Rate Panel
  - [ ] Duration Panel

- [ ] **Business Metrics** (4h)
  - [ ] 시간당 대화 수
  - [ ] 활성 사용자
  - [ ] 평균 평점
  - [ ] AI 토큰 사용량

- [ ] **Infrastructure** (4h)
  - [ ] JVM Memory
  - [ ] DB Connection Pool
  - [ ] Redis 연결
  - [ ] Circuit Breaker 상태

- [ ] **Alert Rules** (4h)
  - [ ] High Error Rate
  - [ ] High Latency
  - [ ] SLO Breach
  - [ ] Error Budget Low

- [ ] **Alert 통합** (2h)
  - [ ] Slack 웹훅
  - [ ] Email 알림
  - [ ] 심각도별 라우팅

- [ ] **대시보드 Export** (1h)
  - [ ] JSON Export
  - [ ] docs/grafana-dashboards/

- [ ] **문서화** (2h)
  - [ ] 대시보드 사용 가이드
  - [ ] Alert 대응 가이드

**완료 기준:**
- ✅ 4개 대시보드 생성
- ✅ SLO 실시간 모니터링
- ✅ Slack 알림 수신

---

### 3.3 동적 로그 레벨 (2일)

**우선순위**: 🟢 Medium
**담당자**: [할당 필요]

- [ ] **LoggerController** (4h)
  - [ ] @RestController
  - [ ] LoggerContext 접근
  - [ ] getAllLoggers()
  - [ ] getLogger()
  - [ ] setLogLevel()

- [ ] **자동 복원** (4h)
  - [ ] LogLevelAutoRestore
  - [ ] ScheduledExecutorService
  - [ ] setTemporaryLevel()
  - [ ] 기본 5분 복원

- [ ] **보안 설정** (2h)
  - [ ] ADMIN 권한 필요
  - [ ] CSRF 설정

- [ ] **API 문서화** (2h)
  - [ ] Swagger 어노테이션
  - [ ] 요청/응답 예시

- [ ] **통합 테스트** (3h)
  - [ ] 로그 레벨 조회
  - [ ] 로그 레벨 변경
  - [ ] 자동 복원
  - [ ] 권한 검증

- [ ] **CLI 스크립트** (1h)
  - [ ] change-log-level.sh
  - [ ] 사용법 출력

- [ ] **문서화** (2h)
  - [ ] API 사용 가이드
  - [ ] 프로덕션 디버깅 절차

**완료 기준:**
- ✅ POST /actuator/loggers 성공
- ✅ 5분 후 자동 복원
- ✅ 감사 로그에 변경 이력

---

### 3.4 Adaptive Sampling (2일)

**우선순위**: 🔵 Low
**담당자**: [할당 필요]

- [ ] **AdaptiveSampler** (4h)
  - [ ] Sampler 인터페이스
  - [ ] shouldSample() 로직
  - [ ] 에러 100% 샘플링
  - [ ] 느린 요청 50%
  - [ ] 일반 10%

- [ ] **경로별 샘플링** (2h)
  - [ ] /api/chat → 30%
  - [ ] /api/admin → 100%
  - [ ] 설정 파일화

- [ ] **TracingSamplerConfig** (2h)
  - [ ] AdaptiveSampler Bean
  - [ ] application.yaml 바인딩
  - [ ] 환경별 전략

- [ ] **샘플링 메트릭** (2h)
  - [ ] tracing.sampling.decisions
  - [ ] tracing.sampling.rate

- [ ] **테스트** (4h)
  - [ ] 에러 요청 샘플링
  - [ ] 느린 요청 샘플링
  - [ ] 일반 요청 샘플링
  - [ ] 메트릭 수집

- [ ] **비용 분석** (2h)
  - [ ] 현재 볼륨 측정
  - [ ] 예상 볼륨
  - [ ] 비용 절감액

- [ ] **Grafana 패널** (2h)
  - [ ] 샘플링 비율 그래프
  - [ ] 샘플링 이유별 분포

**완료 기준:**
- ✅ 에러 요청 100% 샘플링
- ✅ 트레이스 볼륨 80% 감소
- ✅ Grafana에서 샘플링 모니터링

---

## Git Commit Convention

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: 새 기능
- `fix`: 버그 수정
- `refactor`: 리팩토링
- `test`: 테스트
- `docs`: 문서
- `chore`: 빌드/설정

**예시:**
```
feat(observability): add GlobalExceptionHandler

- Implement ErrorCode enum with 8 types
- Add ErrorResponse DTO with traceId
- Auto-collect error metrics

Closes #123
```

---

## 마일스톤

- **M1: 기초 강화** (2025-12-19)
  - GlobalExceptionHandler
  - 민감정보 마스킹
  - RED 메트릭

- **M2: 안정성 강화** (2025-12-31)
  - SLI/SLO
  - Circuit Breaker
  - Sentry
  - 수동 Span

- **M3: 엔터프라이즈** (2026-01-15)
  - 감사 로그
  - Grafana
  - 동적 로그 레벨
  - Adaptive Sampling

---

**문서 버전**: 1.0
**최종 수정**: 2025-12-12
