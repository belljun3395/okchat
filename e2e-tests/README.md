# OKChat E2E Test Suite

Playwright 기반의 프론트엔드 E2E(End-to-End) 테스트 시스템입니다.

## 📋 목차

- [개요](#개요)
- [설치](#설치)
- [테스트 실행](#테스트-실행)
- [테스트 구조](#테스트-구조)
- [작성된 테스트](#작성된-테스트)
- [테스트 작성 가이드](#테스트-작성-가이드)
- [CI/CD 통합](#cicd-통합)
- [문제 해결](#문제-해결)

## 🎯 개요

이 테스트 시스템은 OKChat 애플리케이션의 프론트엔드 기능을 자동으로 검증합니다.

### 주요 기능

- ✅ **크로스 브라우저 테스팅**: Chromium, Firefox, WebKit 지원
- 📱 **모바일 테스팅**: 모바일 Chrome, Safari 뷰포트 지원
- 🎥 **비디오 녹화**: 실패한 테스트의 비디오 자동 저장
- 📸 **스크린샷**: 실패 시 자동 스크린샷 캡처
- 📊 **상세한 리포트**: HTML 리포트 생성
- 🔄 **재시도 메커니즘**: CI 환경에서 자동 재시도

## 🔧 설치

### 사전 요구사항

- Node.js 18 이상
- npm 또는 yarn

### 설치 방법

```bash
cd e2e-tests

# 의존성 설치
npm install

# Playwright 브라우저 설치
npx playwright install

# Playwright 브라우저와 시스템 의존성 모두 설치 (Linux)
npx playwright install --with-deps
```

## 🚀 테스트 실행

### 기본 실행

```bash
# 모든 테스트 실행
npm test

# UI 모드로 실행 (디버깅에 유용)
npm run test:ui

# 헤드 모드로 실행 (브라우저 창 보이기)
npm run test:headed

# 디버그 모드로 실행
npm run test:debug
```

### 특정 테스트 실행

```bash
# 채팅 인터페이스 테스트만 실행
npm run test:chat

# 권한 관리 테스트만 실행
npm run test:permissions
```

### 브라우저별 실행

```bash
# Chrome에서만 실행
npm run test:chrome

# Firefox에서만 실행
npm run test:firefox

# Safari(WebKit)에서만 실행
npm run test:safari

# 모바일 Chrome에서 실행
npm run test:mobile
```

### 리포트 보기

```bash
# HTML 리포트 열기
npm run report
```

## 📁 테스트 구조

```
e2e-tests/
├── tests/                    # 테스트 파일
│   ├── chat.spec.ts         # 채팅 인터페이스 테스트
│   └── permissions.spec.ts  # 권한 관리 테스트
├── pages/                    # Page Object Models
│   ├── ChatPage.ts          # 채팅 페이지 POM
│   └── PermissionsPage.ts   # 권한 페이지 POM
├── utils/                    # 유틸리티 함수
│   └── test-helpers.ts      # 공통 헬퍼 함수
├── playwright.config.ts      # Playwright 설정
├── package.json             # 패키지 설정
└── tsconfig.json            # TypeScript 설정
```

## ✅ 작성된 테스트

### 채팅 인터페이스 (`tests/chat.spec.ts`)

| 테스트 | 설명 |
|--------|------|
| UI 표시 확인 | 채팅 인터페이스의 모든 요소가 올바르게 표시되는지 확인 |
| 초기 상태 확인 | 페이지 로드 시 초기 상태가 올바른지 확인 |
| 메시지 전송 | 사용자 메시지 전송 및 봇 응답 수신 확인 |
| Enter 키 처리 | Enter 키로 메시지 전송 확인 |
| 키워드 입력 | 키워드와 함께 메시지 전송 확인 |
| 심층 분석 모드 | Deep Think 모드로 메시지 전송 확인 |
| 빈 메시지 방지 | 빈 메시지는 전송되지 않는지 확인 |
| 입력 필드 초기화 | 메시지 전송 후 입력 필드가 비워지는지 확인 |
| 타임스탬프 표시 | 메시지에 타임스탬프가 표시되는지 확인 |
| 마크다운 렌더링 | 봇 응답의 마크다운이 올바르게 렌더링되는지 확인 |
| 코드 복사 버튼 | 코드 블록의 복사 버튼 동작 확인 |
| 자동 스크롤 | 최신 메시지로 자동 스크롤 확인 |
| 뒤로가기 | 관리자 페이지로 돌아가기 확인 |
| 콘솔 오류 없음 | JavaScript 콘솔 오류가 없는지 확인 |
| 네트워크 오류 처리 | 네트워크 오류 시 적절한 처리 확인 |
| 세션 유지 | 세션 ID가 유지되는지 확인 |

### 권한 관리 인터페이스 (`tests/permissions.spec.ts`)

| 테스트 | 설명 |
|--------|------|
| UI 표시 확인 | 권한 관리 페이지의 모든 요소가 올바르게 표시되는지 확인 |
| 통계 표시 | 사용자 수와 경로 수 통계가 올바르게 표시되는지 확인 |
| 빠른 액션 링크 | 모든 빠른 액션 링크가 동작하는지 확인 |
| 테이블 컬럼 | 사용자 테이블의 컬럼이 올바르게 표시되는지 확인 |
| 사용자 행 표시 | 사용자 정보가 올바르게 표시되는지 확인 |
| 상태 배지 | 사용자 상태 배지가 올바르게 표시되는지 확인 |
| 채팅으로 이동 | 채팅 인터페이스로 이동 확인 |
| 권한 관리자로 이동 | 권한 관리자 페이지로 이동 확인 |
| 사용자 관리로 이동 | 사용자 관리 페이지로 이동 확인 |
| 경로 탐색으로 이동 | 경로 탐색 페이지로 이동 확인 |
| 사용자 상세 보기 | 사용자 상세 페이지로 이동 확인 |
| 호버 효과 | 빠른 링크 호버 효과 확인 |
| 반응형 레이아웃 | 통계 카드의 반응형 레이아웃 확인 |
| 스타일링 | 색상과 스타일이 올바르게 적용되는지 확인 |
| 콘솔 오류 없음 | JavaScript 콘솔 오류가 없는지 확인 |
| 접근성 | 적절한 접근성 속성이 있는지 확인 |
| 사용자 수 | 테이블의 사용자 수가 통계와 일치하는지 확인 |
| 이메일 형식 | 사용자 이메일 형식이 올바른지 확인 |

## 📝 테스트 작성 가이드

### Page Object Model 사용

```typescript
import { ChatPage } from '../pages/ChatPage';

test('example test', async ({ page }) => {
  const chatPage = new ChatPage(page);
  await chatPage.goto();
  await chatPage.sendMessage('Hello');
  // ...
});
```

### 헬퍼 함수 활용

```typescript
import { waitForStreamComplete, typeText } from '../utils/test-helpers';

test('example with helpers', async ({ page }) => {
  await typeText(page, '#input', 'Hello', 100);
  await waitForStreamComplete(page);
  // ...
});
```

### 새로운 테스트 파일 추가

1. `tests/` 디렉토리에 `*.spec.ts` 파일 생성
2. 필요한 경우 `pages/`에 Page Object 추가
3. `package.json`에 새로운 테스트 스크립트 추가 (선택사항)

```typescript
// tests/new-feature.spec.ts
import { test, expect } from '@playwright/test';

test.describe('New Feature', () => {
  test('should work correctly', async ({ page }) => {
    await page.goto('/new-feature');
    // 테스트 작성
  });
});
```

## 🔄 CI/CD 통합

### GitHub Actions 예제

`.github/workflows/e2e-tests.yml` 파일을 생성:

```yaml
name: E2E Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
    
    - name: Install dependencies
      working-directory: e2e-tests
      run: |
        npm ci
        npx playwright install --with-deps
    
    - name: Start application
      run: |
        ./gradlew bootRun &
        sleep 30
    
    - name: Run tests
      working-directory: e2e-tests
      run: npm test
      env:
        CI: true
    
    - name: Upload test results
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: playwright-report
        path: e2e-tests/playwright-report/
        retention-days: 30
```

### GitLab CI 예제

`.gitlab-ci.yml`:

```yaml
e2e-tests:
  stage: test
  image: mcr.microsoft.com/playwright:v1.48.0-jammy
  
  services:
    - mysql:8.0
  
  variables:
    MYSQL_ROOT_PASSWORD: password
    MYSQL_DATABASE: okchat
  
  before_script:
    - cd e2e-tests
    - npm ci
  
  script:
    - npx playwright test
  
  artifacts:
    when: always
    paths:
      - e2e-tests/playwright-report/
      - e2e-tests/test-results/
    expire_in: 1 week
```

## 🐛 문제 해결

### 브라우저 실행 오류

```bash
# 브라우저 재설치
npx playwright install --force

# 시스템 의존성 설치
npx playwright install-deps
```

### 타임아웃 오류

`playwright.config.ts`에서 타임아웃 증가:

```typescript
use: {
  actionTimeout: 30000,      // 30초
  navigationTimeout: 60000,   // 60초
}
```

### 네트워크 연결 오류

애플리케이션이 실행 중인지 확인:

```bash
# 다른 터미널에서 애플리케이션 시작
cd ..
./gradlew bootRun

# 테스트 실행
cd e2e-tests
npm test
```

### 디버깅

```bash
# UI 모드 사용
npm run test:ui

# 디버그 모드 사용
npm run test:debug

# 특정 테스트만 디버깅
npx playwright test tests/chat.spec.ts --debug
```

### 테스트 코드 생성

Playwright의 코드 생성기 사용:

```bash
npm run codegen
```

## 📚 추가 자료

- [Playwright 공식 문서](https://playwright.dev/)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Page Object Model 가이드](https://playwright.dev/docs/pom)

## 🤝 기여하기

1. 새로운 테스트 추가 시 의미 있는 테스트 이름 사용
2. Page Object Model 패턴 준수
3. 공통 로직은 헬퍼 함수로 분리
4. 테스트 간 독립성 유지
5. 적절한 assertion 사용

## 📄 라이선스

MIT
