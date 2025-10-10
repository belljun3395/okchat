# OKChat E2E Tests

Playwright 기반의 End-to-End 테스트 시스템입니다. 화면의 기능들을 자동으로 검증합니다.

## 📋 목차

- [설치](#설치)
- [테스트 실행](#테스트-실행)
- [테스트 구조](#테스트-구조)
- [Page Objects](#page-objects)
- [CI/CD 통합](#cicd-통합)
- [문제 해결](#문제-해결)

## 🚀 설치

### 사전 요구사항

- Node.js 18.0.0 이상
- Java 17 (Spring Boot 애플리케이션 실행용)
- npm 또는 yarn

### 설치 단계

```bash
# e2e-tests 디렉토리로 이동
cd e2e-tests

# 의존성 설치
npm install

# Playwright 브라우저 설치
npx playwright install --with-deps
```

### 환경 설정

1. `.env` 파일 생성:
```bash
cp .env.example .env
```

2. 필요에 따라 `.env` 파일 수정

## 🧪 테스트 실행

### Spring Boot 애플리케이션 시작

테스트를 실행하기 전에 Spring Boot 애플리케이션이 실행 중이어야 합니다:

```bash
# 프로젝트 루트에서
./gradlew bootRun
```

### 테스트 명령어

```bash
# 모든 테스트 실행
npm test

# 특정 브라우저에서 테스트
npm run test:chrome
npm run test:firefox
npm run test:webkit

# UI 모드로 테스트 실행 (시각적 디버깅)
npm run test:ui

# 디버그 모드로 테스트 실행
npm run test:debug

# 특정 테스트 파일/폴더 실행
npm test tests/chat
npm test tests/permissions

# Smoke 테스트만 실행
npm test tests/smoke

# 테스트 리포트 확인
npm run test:report
```

### 스크립트를 통한 실행

편의를 위한 쉘 스크립트도 제공됩니다:

```bash
# 기본 실행 (Chromium)
./scripts/run-tests.sh

# Firefox에서 헤드 모드로 실행
./scripts/run-tests.sh -b firefox -h

# 채팅 테스트만 디버그 모드로 실행
./scripts/run-tests.sh -t chat -d

# Smoke 테스트 실행
./scripts/run-tests.sh -s
```

## 📁 테스트 구조

```
e2e-tests/
├── src/
│   ├── pages/          # Page Object 클래스들
│   │   ├── BasePage.ts
│   │   ├── ChatPage.ts
│   │   ├── PermissionsPage.ts
│   │   └── PermissionManagerPage.ts
│   ├── fixtures/       # 테스트 픽스처
│   │   └── test-fixtures.ts
│   ├── utils/          # 유틸리티 함수
│   │   └── test-helpers.ts
│   └── types/          # TypeScript 타입 정의
├── tests/
│   ├── chat/           # 채팅 관련 테스트
│   │   ├── chat.spec.ts
│   │   └── chat-error.spec.ts
│   ├── permissions/    # 권한 관리 테스트
│   │   ├── permissions-index.spec.ts
│   │   └── permission-manager.spec.ts
│   └── smoke/          # Smoke 테스트
│       └── smoke.spec.ts
├── playwright.config.ts # Playwright 설정
└── package.json
```

## 📄 Page Objects

Page Object Pattern을 사용하여 테스트 코드의 유지보수성을 높였습니다.

### BasePage

모든 Page Object의 기본 클래스:
- 공통 기능 (navigation, wait, screenshot 등)
- 재사용 가능한 메서드

### ChatPage

채팅 화면 관련 기능:
- 메시지 전송/수신
- 심층 분석 모드
- 키워드 검색
- 마크다운 렌더링 검증

### PermissionsPage

권한 관리 대시보드:
- 통계 확인
- 사용자 목록
- 빠른 링크 네비게이션

### PermissionManagerPage

권한 상세 관리:
- 사용자 검색/선택
- 트리 구조 권한 관리
- 권한 저장

## 🔧 CI/CD 통합

GitHub Actions를 통한 자동화된 테스트가 구성되어 있습니다.

### 실행 조건

- `main`, `develop` 브랜치 push
- Pull Request
- 매일 오전 2시 (스케줄)
- 수동 실행

### 테스트 매트릭스

- 브라우저: Chromium, Firefox, WebKit
- 병렬 실행으로 빠른 피드백

### 아티팩트

테스트 실패 시:
- 스크린샷
- 비디오 녹화
- 트레이스 파일
- JUnit 리포트

## 🐛 문제 해결

### 일반적인 문제

1. **테스트 시작 실패**
   - Spring Boot 애플리케이션이 실행 중인지 확인
   - `http://localhost:8080` 접속 가능한지 확인

2. **브라우저 실행 오류**
   ```bash
   # 브라우저 재설치
   npx playwright install --force
   ```

3. **타임아웃 오류**
   - 네트워크 상태 확인
   - `playwright.config.ts`에서 타임아웃 값 조정

4. **권한 오류 (Linux/Mac)**
   ```bash
   chmod +x scripts/run-tests.sh
   ```

### 디버깅 팁

1. **UI 모드 사용**
   ```bash
   npm run test:ui
   ```
   시각적으로 테스트 실행 과정을 확인

2. **트레이스 확인**
   ```bash
   npx playwright show-trace trace.zip
   ```
   실패한 테스트의 상세 정보 확인

3. **스크린샷 확인**
   `test-results/screenshots/` 디렉토리 확인

4. **로그 레벨 조정**
   ```bash
   DEBUG=pw:api npm test
   ```

## 📊 테스트 커버리지

### 현재 테스트 범위

- ✅ 채팅 인터페이스
  - 메시지 송수신
  - 마크다운 렌더링
  - 코드 블록 복사
  - 에러 처리
  
- ✅ 권한 관리
  - 대시보드 표시
  - 사용자 관리
  - 권한 트리 조작
  - 저장 기능

- ✅ Smoke 테스트
  - 페이지 로딩
  - 기본 네비게이션
  - API 헬스 체크

## 🤝 기여하기

새로운 테스트 추가 시:

1. 적절한 디렉토리에 `.spec.ts` 파일 생성
2. Page Object가 필요한 경우 `src/pages/`에 추가
3. 공통 유틸리티는 `src/utils/`에 추가
4. 테스트 실행 및 검증
5. PR 생성

## 📞 지원

문제가 있거나 질문이 있으면 이슈를 생성해주세요.