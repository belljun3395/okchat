# 백엔드 모듈화 아키텍처 정의서

## 1. 개요

본 문서는 `okchat` 백엔드 서비스의 **모듈형 아키텍처(Modular Architecture)** 구조와 현황을 정의합니다.
현재 프로젝트는 모놀리스 구조에서 멀티 모듈 구조로의 전환 작업을 완료하였으며, 각 도메인은 물리적으로 독립된 모듈로 격리되어 있습니다.

이 구조는 **도메인 완전 격리(Domain Isolation)**와 **유연한 확장성(Scalability)**을 핵심 가치로 하며, 향후 트래픽 증가나 비즈니스 확장에 따라 마이그레이션 비용 없이 즉시 마이크로서비스 아키텍처(MSA)로 전환할 수 있는 기반을 제공합니다.

## 2. 핵심 설계 원칙

본 프로젝트는 다음 4가지 원칙을 준수하여 구현되었습니다.

1.  **도메인 완전 격리 (Zero Compile Dependency)**
    *   도메인 모듈 간의 컴파일 타임 의존성을 완전히 제거하였습니다.
    *   한 도메인의 코드 변경이 다른 도메인의 빌드에 영향을 주지 않습니다.

2.  **명확한 통신 표준 (Strict Communication Standard)**
    *   **외부 API**: 프론트엔드 통신용 (`/api/v1/*`), `okchat-server` 계층에서 처리
    *   **내부 API**: 도메인 간 통신용 (`/internal/api/v1/*`), HTTP/Feign Client 사용

3.  **공통 의존성 최소화 (Minimize Shared Libs)**
    *   `common` 모듈의 비대화를 방지하고, 기술적 목적에 따라 라이브러리를 세분화했습니다.
    *   비즈니스 로직 공유를 금지하며, 필요한 경우 API 통신을 통해 해결합니다.

4.  **설정 기반 배포 유연성 (Configuration-Driven Deployment)**
    *   코드 수정 없이 설정 파일(`application.yml`) 변경만으로 모놀리스/분산 배포 모드 전환이 가능합니다.

---

## 3. 프로젝트 아키텍처 및 모듈 현황 (Project Status)

현재 코드베이스를 기준으로 구현이 완료된 모듈과 구조를 정의합니다.

### 3.1. 최상위 모듈 구성

| 모듈명 | 역할 | 상태 | 비고 |
|:---:|:---|:---:|:---|
| `okchat-root` | 빌드 루트 및 설정 관리 | **[완료]** | Gradle Kotlin DSL |
| `okchat-lib` | 기술적 인프라 및 유틸리티 | **[완료]** | 비즈니스 로직 없음 |
| `okchat-domain` | 핵심 비즈니스 로직 | **[완료]** | 도메인별 하위 모듈로 격리 |
| `okchat-server` | 실행 가능한 애플리케이션 | **[완료]** | API 서버, 배치 서버 |

---

### 3.2. 도메인 모듈 상세 현황

비즈니스 로직을 담당하는 핵심 모듈들의 구현 현황입니다.

#### [Module] okchat-domain-user (Level 0)
*다른 도메인에 의존하지 않는 최하위 도메인입니다.*

*   **진행 상태**: **[구현 완료]**
*   **주요 책임**: 사용자 식별, 시스템 알림, 멤버십 관리
*   **세부 패키지 구성**:
    *   `user`: 사용자 계정(User) 및 프로필 관리, 인증 데이터 제공
    *   `email`: 시스템 이메일 발송 처리 (구 Notification)
    *   `knowledge`: 지식베이스 멤버 관리 (`KnowledgeBaseUser` 등)
        *   *Note*: 지식베이스의 "권한 관리(누가)"를 담당하며, "컨텐츠 관리(무엇을)"와 분리됨
*   **인터페이스**:
    *   `UserInternalController`: 다른 도메인에 사용자 정보 제공

#### [Module] okchat-domain-docs (Level 1)
*User 도메인을 의존하여 문서와 지식베이스 컨텐츠를 관리합니다.*

*   **진행 상태**: **[구현 완료]**
*   **주요 책임**: 문서 데이터 관리, 검색 엔진 연동, 외부 소스 동기화
*   **세부 패키지 구성**:
    *   `search`: OpenSearch 연동, 벡터 검색, 하이브리드 검색 로직
    *   `permission`: 문서 단위 접근 제어(ACL) 로직
    *   `confluence`: Confluence API 연동 및 데이터 동기화
    *   `knowledge`: 지식베이스 메타데이터 및 문서 구조 관리
*   **의존성**:
    *   `okchat-domain-user`: 멤버십 확인 및 알림 발송 시 호출

#### [Module] okchat-domain-ai (Level 1)
*Docs 도메인의 데이터를 활용하여 AI 서비스를 제공합니다.*

*   **진행 상태**: **[구현 완료]**
*   **주요 책임**: LLM 대화 처리, 질의 분석, RAG 파이프라인 실행
*   **세부 패키지 구성**:
    *   `chat`: 채팅 파이프라인(Processing Pipeline) 및 대화 관리
    *   `classifier`: 사용자 질의 의도 분석 (`QueryClassifier`)
    *   `prompt`: 프롬프트 템플릿 관리 (`SystemPrompt`, `UserPrompt`)
    *   *Note*: `extraction`, `chunking` 전략 패턴은 `okchat-lib-ai` 라이브러리를 활용
*   **의존성**:
    *   `okchat-domain-docs`: 질문에 답변하기 위한 문서 검색 및 권한 검증

#### [Module] okchat-domain-task (Level 2)
*모든 도메인을 조율하여 비동기 배치 작업을 수행합니다.*

*   **진행 상태**: **[구현 완료]**
*   **주요 책임**: 주기적 데이터 동기화, 유지보수 작업
*   **세부 패키지 구성**:
    *   `task`: 배치 작업 정의 (Confluence Sync, Email Polling 등)
*   **특이사항**:
    *   `okchat-batch-server`를 통해 별도 프로세스로 실행 가능

---

### 3.3. 기술 및 서버 모듈 현황

#### [Lib] okchat-lib 계층
기술적인 복잡성을 추상화하여 도메인 모듈이 비즈니스에 집중하도록 돕습니다.

*   `okchat-lib-web`: **[완료]** 공통 MVC 설정, Exception Handler, API Response 규격
*   `okchat-lib-persistence`: **[완료]** JPA, QueryDSL 설정, Redis 설정
*   `okchat-lib-ai`: **[완료]**
    *   LLM Client 추상화
    *   `ExtractionService` (Keyword, Title, Location 추출)
    *   `ChunkingStrategy` (Semantic, Recursive 청킹)
    *   `Prompt` 모델 정의

#### [Server] okchat-server 계층
최종 사용자에게 기능을 제공하는 실행 진입점입니다.

*   `okchat-api-server`: **[완료]**
    *   프론트엔드와 통신하는 REST API 서버
    *   `/api/v1` 경로로 서비스 노출
    *   각 도메인의 UseCase를 조립하여 외부 요청 처리
*   `okchat-batch-server`: **[완료]**
    *   배치 작업 전용 서버
    *   별도 인스턴스로 분리 실행 가능

---

## 4. 향후 고도화 계획 (Future Roadmap)

현재 모듈화 구조는 완성되었으나, 운영 효율성과 성능 최적화를 위해 다음 단계의 작업들이 정의되어 있습니다.

### 4.1. [Pending] Internal API gRPC 전환
*   **현재**: HTTP (Feign Client) 기반 동기 통신
*   **목표**: 고성능 gRPC 프로토콜 도입
*   **대상**: 데이터 전송량이 많거나 레이턴시에 민감한 내부구간
    *   `Ai` → `Docs` (검색 결과 조회)
    *   `Docs` → `SearchEngine` (대량 인덱싱)
*   **기대 효과**: 직렬화 비용 감소 및 타입 안정성(Proto) 확보

### 4.2. [Optional] 물리적 서버 분리 (Physical Separation)
*   **현재**: 논리적으로 분리되었으나, 단일 프로세스(`api-server`)로 배포 중 (Monolithic Deployment)
*   **목표**: 도메인별 독립 배포 파이프라인 구축
*   **기준**:
    *   특정 도메인의 트래픽이 급증하여 독립 스케일링이 필요할 때
    *   배포 주기가 서로 달라 독립 배포가 유리할 때
*   **작업 내용**:
    *   `okchat-user-server`, `okchat-chat-server` 등 전용 런타임 모듈 생성
    *   CI/CD 파이프라인 분리 구축

### 4.3. [Ongoing] 모니터링 및 관측성(Observability) 강화
*   **현재**: Prometheus, Grafana, Tempo 연동 완료
*   **목표**: 분산 환경에서의 추적성 강화
*   **작업 내용**:
    *   도메인 경계를 넘나드는 트랙잭션에 대한 Distributed Tracing 커버리지 확대
    *   도메인별 비즈니스 메트릭 대시보드 세분화

---

## 5. MSA 비기능(운영) 검토: 관측·통신·회복탄력성

모듈 분리는 **조직/코드/빌드 단위의 경계**를 만드는 작업이고, 물리적 분리(MSA)는 여기에 **네트워크/운영/장애/보안**이라는 새로운 축이 추가되는 것을 의미합니다.
본 장에서는 서비스를 물리적으로 분리했을 때 필연적으로 발생하는 트레이드오프(관측 복잡도 증가, 호출 비용 발생, 장애 전파 위험)를 최소화하기 위한 구체적인 운영 전략을 정의합니다.

### 5.1. 관측성(Observability) 확보 전략

현재 `okchat`은 물리적 분리를 대비하여 다음과 같은 관측성 스택을 이미 확보하고 있습니다.

*   **Metrics**: Spring Boot Actuator + Micrometer Prometheus (`/actuator/prometheus`)
    *   설정 파일: `okchat-server/okchat-api-server/src/main/resources/application/management.yaml`
*   **Tracing**: Micrometer Tracing(OTel bridge) + OTLP exporter → Tempo
    *   Tempo 설정: `compose.yaml`, `grafana/tempo-config.yaml`
*   **Logging**: JSON 로그(프로덕션) + traceId/spanId MDC 자동 주입
    *   Logback 설정: `okchat-server/okchat-api-server/src/main/resources/logback-spring.xml`

#### 5.1.1. 서비스 공통 관측 규약 (Standardization)

서비스가 분산되면 "각 서비스가 무엇을 같은 의미로 측정하고 기록하느냐"가 전체 시스템의 가시성을 결정합니다. 다음 규약을 준수해야 합니다.

*   **서비스 식별자 통일**: `spring.application.name`을 서비스별로 유일하게 유지해야 합니다. (예: `okchat-api`, `okchat-ai`)
*   **고조도(Low-Cardinality) 태그 사용**: 메트릭 태그에는 `application`, `env`, `version` 등 유한한 값만 사용합니다. `userId`나 `sessionId`와 같은 고카디널리티 데이터는 메트릭이 아닌 **Trace**나 **Log**에만 남겨야 합니다.
*   **Trace-Log 상관관계 유지**: 모든 로그에는 `traceId`와 `spanId`가 포함되어야 하며, Grafana에서 로그를 통해 트레이스로 점프(Exemplar)할 수 있어야 합니다.

#### 5.1.2. 프로메테우스 스크레이핑 확장 전략

서비스 분리 시 Prometheus가 수집해야 할 타겟이 늘어납니다. 환경별로 다음 전략을 권장합니다.

*   **Docker Compose / VM**: `prometheus.yml`의 `static_configs`에 각 서비스의 호스트:포트를 명시합니다.
*   **Kubernetes**: `ServiceMonitor` 또는 `PodMonitor` 리소스를 사용하여 동적으로 파드를 디스커버리하도록 설정합니다. (Prometheus Operator 활용 권장)

### 5.2. API 통신 및 프로토콜 최적화

물리적 분리 이후 "모듈 간 메서드 호출"은 "네트워크 패킷 전송"으로 바뀝니다. 이에 따른 레이턴시와 장애 전파를 막기 위해 다음 전략을 사용합니다.

#### 5.2.1. 동기 vs 비동기 선택 기준

| 통신 방식 | 장점 | 단점 | 추천 사용처 (okchat) |
| :--- | :--- | :--- | :--- |
| **동기 (HTTP/gRPC)** | 구현 단순, 즉시 응답 확인 | 강한 결합, 장애 전파, 레이턴시 누적 | 사용자 로그인, 필수 권한 체크, 실시간 채팅 응답 |
| **비동기 (Event/MQ)** | 느슨한 결합, 트래픽 유량 제어 | 데이터 최종 일관성, 디버깅 복잡 | 문서 인덱싱, 알림 발송, 장기 실행 배치 작업 |

특히 채팅 요청(`Chat Pipeline`)은 **사용자 입력 → 문서검색 → 권한체크 → LLM 호출**로 이어지는 팬아웃(Fan-out) 구조를 가집니다. 이를 최적화하기 위해 다음 기법을 적용합니다.

1.  **Coarse-Grained API**: `multiSearch`와 같이 한 번의 호출로 필요한 모든 데이터를 가져오도록 API를 설계하여 RTT(Round-Trip Time)를 줄입니다.
2.  **병렬 처리 (Parallel Processing)**: 상호 의존성이 없는 권한 체크와 문서 검색은 `async/await` (Kotlin Coroutines)를 사용하여 병렬로 호출합니다.
3.  **적극적 캐싱**: 변경 빈도가 낮은 '문서 권한 정보'나 '지식베이스 메타데이터'는 각 도메인 내부에 TTL 캐시를 둡니다.

#### 5.2.2. gRPC 도입 및 스트리밍

현재 `Future Roadmap`에 있는 gRPC 도입 시 다음 이점을 얻을 수 있습니다.

*   **채팅 스트리밍 최적화**: 현재 `Flux<String>` (SSE) 기반의 스트리밍 응답을 gRPC Server Streaming으로 전환하면, 버퍼링 없는 실시간 토큰 전송이 보장됩니다.
*   **타입 안정성**: Protobuf를 통해 서비스 간 데이터 계약(Contract)을 엄격하게 관리할 수 있습니다.

### 5.3. 회복탄력성 (Resilience) 강화

네트워크는 언제든 실패할 수 있다는 가정 하에 설계해야 합니다. `Circuit Breaker`는 그 시작일 뿐입니다.

#### 5.3.1. 방어적 코드 작성 원칙

1.  **Timeout 필수 적용**: 모든 외부/내부 호출(FeignClient, WebClient)에는 반드시 **Connect Timeout**과 **Read Timeout**을 명시합니다. 타임아웃 없는 호출은 시스템 전체를 멈추게 하는 "느린 장애"의 주범입니다.
2.  **Circuit Breaker 세분화**: 다운스트림 서비스별로 서킷 브레이커를 분리합니다. (예: `docs` 서비스가 느려졌다고 해서 `user` 서비스 호출까지 차단되어서는 안 됩니다.)
3.  **Fallback 전략**:
    *   **조회 실패 시**: 가능하다면 기본값(Default)이나 캐시된 구버전 데이터를 반환합니다.
    *   **권한 체크 실패 시**: 보안을 위해 반드시 **차단(Deny)** 처리합니다.

#### 5.3.2. 현재 적용 현황

*   `okchat-api-server`에 `Resilience4j`가 설정되어 있으며, AI 채팅 서비스(`DocumentBaseChatService`)에 서킷 브레이커와 타임 리미터가 적용되어 있습니다.
*   Grafana 대시보드(`ok-chat-ai.json`)를 통해 서킷의 상태(Closed/Open/Half-Open)를 실시간으로 모니터링할 수 있습니다.

### 5.4. 배포 및 보안 고려사항

*   **Graceful Shutdown**: 채팅 스트리밍 중인 연결을 강제로 끊지 않도록, 배포 시 `SIGTERM` 시그널을 처리하고 일정 시간(Drain Period) 동안 기존 연결 종료를 기다려야 합니다.
*   **Internal API 보안**: `/internal/api/**` 경로는 외부 로드밸런서(Ingress/ALB) 단에서 **반드시 차단**해야 합니다. Kubernetes 환경에서는 `NetworkPolicy`를 통해 특정 파드 간 통신만 허용하는 화이트리스트 정책을 권장합니다.

---

## 6. 결론

본 문서를 통해 정의된 `okchat`의 모듈형 아키텍처는 단순한 코드 분리를 넘어, 미래의 마이크로서비스 확장을 위한 구체적인 청사진을 제시합니다.

### 6.1. 완전한 도메인 격리 (Domain Isolation)
*   각 도메인(`User`, `Docs`, `Ai`, `Task`)은 컴파일 타임에 서로를 전혀 알지 못합니다.
*   오직 정의된 인터페이스(API, Event)를 통해서만 소통하므로, 특정 도메인의 복잡도가 시스템 전체로 전파되지 않습니다.

### 6.2. 유연한 인프라 (Flexible Infrastructure)
*   **설정 중심(Configuration-Driven)**: 코드 변경 없이 설정(`application.yml`)만으로 모놀리스와 분산 환경을 오갈 수 있습니다.
*   **클라우드 네이티브(Cloud-Native)**: Docker 컨테이너 및 Kubernetes 환경에 즉시 배포 가능한 구조와 관측성(Observability)을 갖추고 있습니다.

### 6.3. 단계적 진화 (Evolutionary Architecture)
*   **Phase 1 (완료)**: 모듈 분리 및 논리적 격리, HTTP 통신 기반 구축
*   **Phase 2 (Roadmap)**: 트래픽 임계치 도달 시 물리적 서버 분리 및 배포 파이프라인 독립
*   **Phase 3 (Roadmap)**: 고성능 gRPC 전환 및 서비스 메시 도입

이러한 아키텍처는 기술 변경에 유연하게 대응하면서도 비즈니스 로직의 견고함을 유지할 수 있는 최적의 기반이 될 것입니다. `okchat` 팀은 이 정의서를 기준으로 시스템을 진화시켜 나갈 것입니다.

