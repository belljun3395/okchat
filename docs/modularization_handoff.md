# okchat 모듈화 진행 기록 (세션 핸드오프)

이 문서는 `okchat` 백엔드 모듈화 작업(Phase1 ~ Phase2 진행 중)의 의사결정과 변경 내용을 **세션 간 이어서 작업**할 수 있도록 요약한 기록입니다.

---

## 작업 배경

- 목표 문서: `modularization_strategy.md`
- 핵심 원칙:
  - Domain 간 **컴파일 의존성 제거**(HTTP/Internal API로 통신)
  - `okchat-lib-*`는 비즈니스 로직 없는 기술 레이어
  - External API(`/api/*`)는 server/root에, Internal API(`/internal/api/*`)는 domain 모듈에

---

## Phase 1 (기반 환경 구축) 완료 요약

### 1) 멀티 모듈/버전 관리

- `okchat-lib` 하위 모듈 생성 및 기술 코드 분리
  - `okchat-lib-web`
  - `okchat-lib-persistence`
  - `okchat-lib-ai`
- 버전 중앙 관리: `gradle.properties`
- Docker 멀티모듈 빌드 대응:
  - `Dockerfile`
  - `Dockerfile.native`

### 2) 설정 파일 분리 (모듈별 구성)

- `src/main/resources/application.yaml`은 `spring.config.import` 중심으로 단순화
- 실제 설정은 `src/main/resources/application/*.yaml`로 분리
  - `core.yaml`, `persistence.yaml`, `task.yaml`, `ai.yaml`, `search.yaml`, `confluence.yaml`, `email.yaml`, `management.yaml`, `resilience.yaml`

### 3) Prompt 설정 도메인 노출 개선

- `PromptCacheConfig` 같은 “특정 도메인(prompt)”이 노출된 설정 파일/구성을 정리
- 프리픽스 등은 `PromptCacheService` 내부 상수로 이동하여 도메인 노출을 줄임

### 4) ktlint 오류 수정

- 파일명/클래스명 불일치로 인한 ktlint 실패 수정
  - `src/main/kotlin/com/okestro/okchat/prompt/application/dto/GetAllPromptTypesDto.kt`
  - → `GetAllPromptTypesUseCaseOut.kt`로 정리

---

## Phase 2-1 (Level 0: user 도메인) 완료 요약

### 1) okchat-domain-user 모듈 생성

- `settings.gradle.kts`에 include:
  - `:okchat-domain:okchat-domain-user`
- 루트 앱 의존성 추가:
  - `build.gradle.kts`에 `implementation(project(":okchat-domain:okchat-domain-user"))`

### 2) user 도메인 코드 이관

- 외부 컨트롤러는 루트 유지:
  - `src/main/kotlin/com/okestro/okchat/user/controller/UserAdminController.kt`
- 비즈니스 코드/테스트는 domain 모듈로 이동:
  - `okchat-domain/okchat-domain-user/src/main/kotlin/com/okestro/okchat/user/**`
  - `okchat-domain/okchat-domain-user/src/test/kotlin/com/okestro/okchat/user/**`

### 3) knowledge-member (KB 멤버십)도 user 도메인으로 이동

- membership 관련 Entity/Repository/UseCase가 `okchat-domain-user`로 이관됨
- DTO는 루트 `KnowledgeBaseUseCaseDtos`에서 분리하여 도메인 모듈에 위치
- Internal API 추가:
  - User: `GET /internal/api/v1/users/{id}`, `GET /internal/api/v1/users/by-email?email=...`
  - Knowledge-member: 멤버십 조회용 내부 API 추가

### 4) EmailProperties의 공용화

- `EmailProperties`를 루트에서 `okchat-lib-web`로 이동하여 공용 참조 가능하게 정리

### 5) 모듈 경계로 인한 Kotlin smart cast 이슈 수정

- 다른 모듈로 이동하면서 `User.id`(nullable) 사용 시 smart cast가 불가해진 부분을 `requireNotNull` 패턴으로 정리

### 6) 신규 테스트

- `GetUserByIdUseCaseTest` 추가

---

## 현재 의사결정: Phase2-2 우선순위

### 결론

- **Phase2-2(`okchat-domain-docs`)를 먼저 진행**하기로 결정.

### 이유

- `docs`는 이후 `ai(chat/pipeline)` 도메인 모듈화의 선행조건이라 “크리티컬 패스”를 앞당김
- `email/notification`은 `EmailChatService → DocumentBaseChatService`로 이미 chat 의존이 강함
  - 지금 당장 분리하려면 포트/클라이언트 추상화 설계가 선행되어야 해서 비용이 큼

### email/notification 분리(후순위) 방향 메모

- email polling/오케스트레이션: `task` 도메인
- email 코어(provider/oauth2/pending-reply): `user(notification)` 도메인
- AI 답변 생성: `ai` 도메인(Internal API/Client로 연결)

---

## Phase2-2 (okchat-domain-docs) TODO

> 아래 내용은 Phase2-2 진행 후 업데이트됨.

### 완료(구현/이관)

- `okchat-domain/okchat-domain-docs` 모듈 생성 및 wiring
  - `settings.gradle.kts` include
  - 루트 `build.gradle.kts` 의존성 추가
  - `Dockerfile`, `Dockerfile.native`에 멀티모듈 COPY 반영
- 공용 AI chunking 유틸을 `okchat-lib-ai`로 이동
  - `com.okestro.okchat.ai.service.chunking.*`
  - `com.okestro.okchat.ai.support.MathUtils`
- docs 도메인 코드 이관
  - `search`, `confluence`, `permission(application/model/repository/service)`, `knowledge(application/model/repository)`
  - External controller는 루트 유지:
    - `src/main/kotlin/com/okestro/okchat/permission/controller/PermissionController.kt`
    - `src/main/kotlin/com/okestro/okchat/knowledge/controller/KnowledgeBaseAdminController.kt`
  - Tool 계층은 root `ai.tools` 의존 때문에 루트 유지:
    - `src/main/kotlin/com/okestro/okchat/search/tools`
    - `src/main/kotlin/com/okestro/okchat/confluence/tools`
- docs → user 컴파일 의존 제거
  - docs 모듈에 Internal API 호출용 Client(WebClient) 추가:
    - `UserClient`, `KnowledgeMemberClient`, `KnowledgeBaseEmailClient`
    - 위치/패키지: `okchat-domain/okchat-domain-docs/src/main/kotlin/com/okestro/okchat/docs/client/user/*` (`com.okestro.okchat.docs.client.user`)
  - permission/knowledge usecase들이 repository/usecase 의존을 client 기반으로 전환
  - KB create/update/detail usecase는 `suspend fun execute(...)`로 변경
- user 도메인 Internal API 보강
  - `POST /internal/api/v1/knowledge-bases/{kbId}/members`
  - `GET/PUT /internal/api/v1/knowledge-bases/{kbId}/email-providers`
- 모듈 경계 smart cast 컴파일 오류 일부 수정
  - confluence tool 일부, `ConfluenceSyncTask` 등에서 local val/`requireNotNull` 패턴 적용

### 테스트/검증

- 테스트 수정(새 ctor/client/suspend 반영) 후 `./gradlew test` 통과
- docs 도메인 테스트를 루트(`src/test/kotlin`)에서 `okchat-domain/okchat-domain-docs/src/test/kotlin`로 이동하여 모듈 경계에 맞게 정리
- `./gradlew ktlintMainSourceSetCheck` 통과

### Phase 2-2 마무리 (Completed)

- runtime 설정 정리: `internal.services.user.url`을 `application/core.yaml`에 추가
- Docs 도메인 Internal API 구현 완료
  - `SearchInternalController` (MultiSearch)
  - `PermissionInternalController` (GetAllowedPaths)
  - DTOs: `InternalMultiSearchRequest/Response`, `InternalGetAllowedPathsResponse` etc.

---

## Phase 2-3 (okchat-domain-ai) 계획

### 목표
- `ai` 도메인(chat, prompt, classifier, extraction, chunking)을 독립 모듈로 분리
- `docs` 도메인과의 컴파일 의존성 제거 (Internal API 사용)

### 완료 (Completed/Verification)
- `okchat-domain-ai` 모듈 생성 및 설정 완료
  - `build.gradle.kts`에 필요 의존성(Spring AI, Coroutines, Reactor, Resilience4j, Kotlin Logging etc) 및 테스트 의존성 추가
- 코드 이관 및 리팩토링
  - `ChatController`는 Root 모듈로 이동 (`src/main/kotlin/com/okestro/okchat/chat/controller`)
  - AI Pipeline Steps (`DocumentSearchStep`, `PermissionFilterStep`)가 `DocsClient`를 사용하여 `docs` 도메인과 통신하도록 변경
  - `ChatContext.copy` -> `ChatContext.copyContext`로 리네이밍하여 충돌 방지
  - `SearchResult` 로컬 모델 정의 및 `Double` 점수 체계 적용
- 테스트 마이그레이션
  - `okchat-domain-ai` 내 테스트 파일들이 `DocsClient` Mocking 및 로컬 모델을 사용하도록 수정
  - `PermissionFilterStepTest`, `DocumentChatPipelineTest` 등 주요 로직 검증 완료
  - `test` 태스크 및 `ktlintFormat` 통과
- `okchat-domain-task` 모듈 `build.gradle.kts` 생성 (의존성 문제 해결)

---

## Phase 2-4 (okchat-domain-task) 계획 (Strategy Alignment)
### 목표
- **Docs, AI, User 도메인에 모두 의존**하는 `task` 도메인의 마이그레이션 (가장 마지막 순서)
- 배치 작업(`ConfluenceSyncTask`, `EmailPollingTask` 등)의 완전한 모듈 분리
- Root 모듈 의존성 제거

### 주요 작업 예정
1.  **코드 이관** (`src/main/kotlin/com/okestro/okchat/task` -> `okchat-domain/okchat-domain-task`)
    - `ConfluenceSyncTask.kt`
    - `EmailPollingTask.kt`
    - `MetricsUpdateTask.kt`
    - 관련 Repository 및 Entity
2.  **Feign/WebClient 구현**
    - `AiClient` (AI 도메인 호출)
    - `DocsClient` (Search/Knowledge 호출)
    - `UserClient` (Notification/EmailProvider 호출)
3.  **의존성 정리**
    - 컴파일 의존성(`implementation project(...)`) 제거 후 `Client`로 대체
4.  **`okchat-domain-task` 빌드 및 테스트 구성**

---

## Phase 3 (마무리 및 안정화) 계획
### 목표
- 전체 모듈 통합 빌드 및 E2E 테스트 검증
- **Legacy Cleaning**: Root 모듈에 남아있는 이관된 코드(빈 패키지, 미사용 유틸) 삭제
- **External Server Split (Optional)**: `okchat-server` (API Gateway 역할)와 도메인 서비스의 물리적 분리 준비 (Strategy Phase 4)
