# 백엔드 API 문서

이 문서는 okchat 애플리케이션의 백엔드 API에 대한 포괄적인 개요를 제공합니다. 각 엔드포인트의 입력 파라미터, 출력 형식 및 기본 비즈니스 로직에 대한 세부 정보를 포함합니다.

## 1. 채팅 API (`ChatController`)

**기본 경로**: `/api/chat`

### 1.1 AI 채팅 (스트리밍)
- **엔드포인트**: `POST /api/chat`
- **설명**: SSE(Server-Sent Events)를 사용하여 실시간 스트리밍 응답으로 문서 기반 AI 채팅을 수행합니다. RAG(검색 증강 생성), 세션 관리 및 심층 분석 모드를 지원합니다.
- **입력 (`ChatRequest`)**:
    - `message` (String, 필수): 사용자의 채팅 메시지.
    - `keywords` (List<String>, 선택): 검색 범위를 필터링할 키워드 목록.
    - `sessionId` (String, 선택): 대화 컨텍스트를 유지하기 위한 세션 ID.
    - `isDeepThink` (Boolean, 기본값: false): true일 경우 더 깊은 분석(복잡한 RAG/처리)을 수행합니다.
    - `userEmail` (String, 선택): 권한 필터링 및 분석용 사용자 이메일.
- **출력**: `Flow<String>` (Server-Sent Events)
    - 초기 이벤트: `__REQUEST_ID__:<uuid>`
    - 후속 이벤트: AI 응답의 텍스트 청크.
- **비즈니스 로직**:
    1.  요청 ID(Request ID)를 생성하거나 가져옵니다.
    2.  `DocumentBaseChatService.chat()`에 위임합니다.
    3.  서비스 수행 내용:
        -   쿼리 임베딩 생성.
        -   관련 문서에 대한 벡터 검색/키워드 검색.
        -   권한 확인 (`userEmail` 사용).
        -   컨텍스트 조합.
        -   LLM 호출 (스트리밍).
    4.  클라이언트에 응답을 스트리밍합니다.

## 2. 채팅 분석 API (`ChatAnalyticsController`)

**기본 경로**: `/api/admin/chat/analytics`

### 2.1 일일 사용량 통계
- **엔드포인트**: `GET /usage/daily`
- **입력**:
    - `startDate` (DateTime, 필수): 기간 시작 (ISO-8601).
    - `endDate` (DateTime, 필수): 기간 종료 (ISO-8601).
- **출력 (`DailyUsageStats`)**:
    - `totalInteractions` (Long): 총 채팅 상호작용 수.
    - `averageResponseTime` (Double): 평균 응답 시간 (밀리초).
- **로직**: 분석 리포지토리에서 해당 날짜 범위의 사용량 데이터를 집계합니다.

### 2.2 품질 트렌드 통계
- **엔드포인트**: `GET /quality/trend`
- **입력**: `startDate`, `endDate` (위와 동일)
- **출력 (`QualityTrendStats`)**:
    - `averageRating` (Double): 평균 사용자 평점 (1-5).
    - `helpfulPercentage` (Double): "도움짐" 피드백 비율.
    - `totalInteractions` (Long): 기간 내 총 상호작용 수.
- **로직**: 사용자 피드백(평점/도움됨 여부)을 기반으로 품질 지표를 계산합니다.

### 2.3 성능 지표
- **엔드포인트**: `GET /performance`
- **입력**: `startDate`, `endDate`
- **출력 (`PerformanceMetrics`)**:
    - `averageResponseTimeMs` (Double): 평균 지연 시간.
    - `errorRate` (Double): 에러 발생 비율 (현재 에러 추적 비활성화로 0일 수 있음).
- **로직**: 시스템 성능 지표를 계산합니다.

### 2.4 쿼리 유형별 통계
- **엔드포인트**: `GET /query-types`
- **입력**: `startDate`, `endDate`
- **출력**: `List<QueryTypeStat>`
    - `queryType`: 쿼리 유형 (예: DOCUMENT\_SEARCH, KEYWORD, GENERAL).
    - `count`: 해당 유형의 쿼리 수.
    - `averageRating`: 해당 유형의 평균 평점.
    - `averageResponseTime`: 해당 유형의 평균 응답 시간.
- **로직**: 사용자 쿼리 유형별로 성능 및 사용량 데이터를 세분화합니다.

### 2.5 상호작용 시계열 데이터
- **엔드포인트**: `GET /timeseries/interactions`
- **입력**: `startDate`, `endDate`
- **출력 (`InteractionTimeSeries`)**:
    - `dataPoints` (List): 차트용 날짜 및 값 쌍.
    - `dateRange`: 요청된 범위.
- **로직**: 시간 경과에 따른 상호작용 볼륨을 플로팅하기 위한 데이터 포인트를 제공합니다.

### 2.6 피드백 제출
- **엔드포인트**: `POST /feedback`
- **입력 (`FeedbackRequest`)**:
    - `requestId` (String, 필수): 리뷰 대상 채팅 요청 ID.
    - `rating` (Int, 선택): 1-5 평점.
    - `wasHelpful` (Boolean, 선택): 도움됨 여부 플래그.
    - `feedback` (String, 선택): 텍스트 코멘트.
- **출력**: 200 OK (비동기)
- **로직**:
    1.  피드백을 로깅합니다.
    2.  `ChatEventBus`에 `FeedbackSubmittedEvent`를 게시합니다.
    3.  이벤트를 비동기적으로 처리하여 분석 데이터를 업데이트합니다.

## 3. 프롬프트 API (`PromptController`)

**기본 경로**: `/api/prompts`

### 3.1 프롬프트 조회
- **엔드포인트**: `GET /{type}`
- **입력**:
    - `type` (Path, 필수): 프롬프트 유형 (예: "system").
    - `version` (Query, 선택): 특정 버전 번호.
- **출력 (`PromptContentResponse`)**: 프롬프트의 내용 및 메타데이터.
- **로직**: 지정된 버전을 조회하거나 지정되지 않은 경우 최신 버전을 조회합니다 (`GetPromptUseCase` 또는 `GetLatestPromptVersionUseCase` 사용).

### 3.2 모든 버전 조회
- **엔드포인트**: `GET /{type}/versions`
- **입력**: `type` (Path)
- **출력**: `List<PromptResponse>` (모든 버전 목록)
- **로직**: `GetAllPromptVersionsUseCase`를 통해 과거 버전을 조회합니다.

### 3.3 프롬프트 생성
- **엔드포인트**: `POST /`
- **입력 (`CreatePromptRequest`)**:
    - `type` (String)
    - `content` (String)
- **출력 (`PromptResponse`)**: 생성된 프롬프트 상세 정보 (v1).
- **로직**: `CreatePromptUseCase`를 통해 새 프롬프트 항목을 생성합니다.

### 3.4 프롬프트 업데이트
- **엔드포인트**: `PUT /{type}`
- **입력 (`UpdatePromptRequest`)**:
    - `content` (String)
- **출력 (`PromptResponse`)**: 새 프롬프트 버전 상세 정보.
- **로직**: `UpdatePromptUseCase`를 통해 기존 프롬프트 유형의 *새 버전*을 생성합니다.

### 3.5 프롬프트 비활성화
- **엔드포인트**: `DELETE /{type}/versions/{version}`
- **입력**: `type`, `version` (Path)
- **출력**: 204 No Content
- **로직**: `DeactivatePromptUseCase`를 통해 특정 프롬프트 버전을 비활성 상태로 표시합니다.

## 4. 작업 실행 API (`TaskExecutionController`)

**기본 경로**: `/api/tasks`

### 4.1 최근 실행 조회
- **엔드포인트**: `GET /`
- **출력**: `Flux<TaskExecutionDto>` (실행 기록 스트림)
- **로직**: `TaskExecutionRepository`에서 최근 50개의 작업 실행을 가져옵니다.

### 4.2 실행 상세 조회
- **엔드포인트**: `GET /{id}`
- **입력**: `id` (Path)
- **출력**: `TaskExecutionDto`
- **로직**: 특정 작업 실행에 대한 세부 정보를 조회합니다.

### 4.3 실행 통계 조회
- **엔드포인트**: `GET /stats`
- **출력 (`TaskStatsDto`)**:
    - `total`, `success`, `failure`: 카운트.
    - `lastExecution`: 마지막 실행 타임스탬프.
- **로직**: 작업 실행 성공/실패 횟수를 집계합니다.

### 4.4 실행 파라미터 조회
- **엔드포인트**: `GET /{id}/params`
- **입력**: `id` (Path)
- **출력**: `Flux<String>`
- **로직**: 작업 실행에 사용된 시작 파라미터를 조회합니다.

## 5. 권한 API (`PermissionController`)

**기본 경로**: `/api/admin/permissions`

### 5.1 모든 경로 조회
- **엔드포인트**: `GET /paths`
- **출력**: `List<String>`
- **로직**: 시스템의 모든 고유 문서 경로를 반환합니다.

### 5.2 경로 상세 조회
- **엔드포인트**: `GET /path/detail`
- **입력**: `path` (Query)
- **출력 (`PathDetailResponse`)**:
    - 해당 경로의 문서들.
    - 해당 경로에 접근 가능한 사용자들.
    - 통계 (총 문서/사용자 수).
- **로직**: 특정 경로에 대한 문서 및 사용자 권한 데이터를 집계합니다.

### 5.3 사용자 권한 통계 조회
- **엔드포인트**: `GET /users`
- **출력**: `List<UserPermissionStat>`
- **로직**: 모든 사용자와 그들의 권한 개수 목록을 반환합니다.

### 5.4 사용자 권한 조회
- **엔드포인트**: `GET /user/{email}`
- **입력**: `email` (Path)
- **출력 (`UserPermissionsResponse`)**: 사용자 상세 및 경로 권한 목록.
- **로직**: 특정 사용자에게 부여된 모든 경로 권한을 조회합니다.

### 5.5 사용자 전체 권한 취소
- **엔드포인트**: `DELETE /user/{email}`
- **입력**: `email` (Path)
- **출력**: `PermissionResponse`
- **로직**: 지정된 사용자의 *모든* 권한을 취소합니다.

### 5.6 권한 일괄 부여
- **엔드포인트**: `POST /path/bulk`
- **입력 (`BulkGrantPathPermissionRequest`)**:
    - `userEmail`: 대상 사용자.
    - `documentPaths`: 접근 권한을 부여할 경로 목록.
    - `spaceKey`: 선택적 Confluence 스페이스 키.
- **출력**: `BulkPermissionResponse`
- **로직**: 경로를 순회하며 사용자에게 "허용(ALLOW)" 권한을 부여합니다.

### 5.7 권한 일괄 취소
- **엔드포인트**: `DELETE /path/bulk`
- **입력 (`RevokeBulkPathPermissionRequest`)**:
    - `userEmail`
    - `documentPaths`
- **출력**: `PermissionResponse`
- **로직**: 사용자에 대해 지정된 경로의 권한을 취소합니다.

### 5.8 권한 일괄 거부 (Deny)
- **엔드포인트**: `POST /path/bulk/deny`
- **입력**: 일괄 부여와 동일
- **출력**: `BulkPermissionResponse`
- **로직**: 지정된 경로에 대해 명시적으로 "거부(DENY)" 권한을 부여합니다 (접근 차단).

## 6. 이메일 회신 대기 API (`PendingEmailReplyController`)

**기본 경로**: `/api/email/pending`

### 6.1 대기 중인 회신 조회
- **엔드포인트**: `GET /`
- **입력**: `page`, `size` (Query)
- **출력**: `Page<PendingEmailReply>`
- **로직**: 검토 대기 중인 이메일의 페이지네이션 목록입니다.

### 6.2 상태별 회신 조회
- **엔드포인트**: `GET /status/{status}`
- **입력**: `status` (Path) (예: PENDING, APPROVED, REJECTED)
- **출력**: `List<PendingEmailReply>`
- **로직**: 검토 상태별로 회신을 필터링합니다.

### 6.3 ID로 회신 조회
- **엔드포인트**: `GET /{id}`
- **입력**: `id` (Path)
- **출력**: `PendingEmailReply`
- **로직**: 단일 회신 상세 정보를 가져옵니다.

### 6.4 카운트 조회
- **엔드포인트**: `GET /count`
- **출력**: `Map<String, Long>` (상태 -> 카운트)
- **로직**: 각 상태(pending, approved, rejected, sent, failed)별 이메일 수를 반환합니다.

### 6.5 승인 및 발송
- **엔드포인트**: `POST /{id}/approve`
- **입력**: `ReviewRequest` (Body) -> `reviewedBy`
- **출력**: `EmailApiResponse`
- **로직**:
    1.  상태를 APPROVED로 업데이트합니다.
    2.  이메일 발송을 트리거합니다.
    3.  성공 시 SENT 상태로 이동합니다.

### 6.6 회신 거부
- **엔드포인트**: `POST /{id}/reject`
- **입력**: `ReviewRequest` (Body) -> `reviewedBy`, `rejectionReason`
- **출력**: `EmailApiResponse`
- **로직**: 상태를 REJECTED로 업데이트하고 사유를 기록합니다.

### 6.7 회신 삭제
- **엔드포인트**: `DELETE /{id}`
- **입력**: `id` (Path)
- **출력**: `EmailApiResponse`
- **로직**: 대기 중인 회신 레코드를 영구 삭제합니다.

## 7. 이메일 OAuth2 API (`OAuth2AuthController`)

**기본 경로**: `/api/email/oauth2`

### 7.1 인증 시작
- **엔드포인트**: `GET /authenticate`
- **입력**: `username` (Query)
- **출력**: 302 Redirect
- **로직**: OAuth2 인증 URL을 생성하고 사용자를 제공자(예: Google/Microsoft)로 리다이렉트합니다.

### 7.2 콜백
- **엔드포인트**: `/oauth2/callback`
- **입력**: `code`, `state` (Query)
- **출력**: JSON 상태
- **로직**: 인증 코드를 액세스 토큰으로 교환하고 저장합니다.

### 7.3 토큰 확인
- **엔드포인트**: `GET /token`
- **입력**: `username` (Query)
- **출력**: 상태 JSON (`hasToken`: true/false)
- **로직**: 사용자에 대한 유효한 토큰이 존재하는지 확인합니다.

### 7.4 토큰 삭제
- **엔드포인트**: `GET /clear`
- **입력**: `username` (Query)
- **출력**: 상태 JSON
- **로직**: 사용자의 저장된 토큰을 제거합니다.
