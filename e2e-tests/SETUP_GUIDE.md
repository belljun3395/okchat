# Playwright E2E 테스트 설정 가이드

## 🎯 개요

이 문서는 OKChat 프로젝트의 Playwright E2E 테스트 시스템을 처음 설정하는 방법을 안내합니다.

## 📋 사전 요구사항

시작하기 전에 다음이 설치되어 있는지 확인하세요:

- **Node.js** 18 이상 ([다운로드](https://nodejs.org/))
- **npm** (Node.js와 함께 설치됨)
- **Java** 21 이상 (Spring Boot 애플리케이션 실행용)

## 🚀 빠른 시작

### 1단계: 의존성 설치

프로젝트 루트에서 E2E 테스트 디렉토리로 이동:

```bash
cd e2e-tests
```

자동 설정 스크립트 실행:

```bash
./setup.sh
```

또는 수동으로 설치:

```bash
# npm 패키지 설치
npm install

# Playwright 브라우저 설치
npx playwright install

# Linux의 경우 시스템 의존성도 설치
npx playwright install-deps
```

### 2단계: 애플리케이션 실행

**옵션 A: 별도 터미널에서 실행**

```bash
# 프로젝트 루트로 돌아가기
cd ..

# Spring Boot 애플리케이션 실행
./gradlew bootRun
```

**옵션 B: Playwright가 자동으로 실행**

Playwright 설정에 `webServer` 옵션이 포함되어 있어 테스트 실행 시 자동으로 애플리케이션을 시작합니다.

### 3단계: 테스트 실행

```bash
cd e2e-tests

# 모든 테스트 실행
npm test

# UI 모드로 실행 (권장)
npm run test:ui

# 브라우저 창을 보며 실행
npm run test:headed
```

## 📁 프로젝트 구조

```
e2e-tests/
├── tests/                      # 테스트 파일
│   ├── chat.spec.ts           # 채팅 기능 테스트
│   ├── permissions.spec.ts    # 권한 관리 테스트
│   ├── accessibility.spec.ts  # 접근성 테스트
│   ├── performance.spec.ts    # 성능 테스트
│   └── visual-regression.spec.ts # 시각적 회귀 테스트
├── pages/                      # Page Object Models
│   ├── ChatPage.ts
│   └── PermissionsPage.ts
├── utils/                      # 헬퍼 유틸리티
│   └── test-helpers.ts
├── playwright.config.ts        # Playwright 설정
├── package.json               # 패키지 정보
├── tsconfig.json              # TypeScript 설정
├── README.md                  # 사용 가이드
├── CONTRIBUTING.md            # 기여 가이드
└── setup.sh                   # 설정 스크립트
```

## 🧪 테스트 실행 명령어

### 기본 실행

```bash
# 모든 테스트 (헤드리스)
npm test

# UI 모드 (디버깅에 최적)
npm run test:ui

# 헤드 모드 (브라우저 보기)
npm run test:headed

# 디버그 모드
npm run test:debug
```

### 특정 테스트 실행

```bash
# 채팅 테스트만
npm run test:chat

# 권한 관리 테스트만
npm run test:permissions

# 특정 파일 실행
npx playwright test tests/chat.spec.ts

# 특정 테스트 케이스 실행
npx playwright test -g "should send message"
```

### 브라우저별 실행

```bash
# Chrome에서만
npm run test:chrome

# Firefox에서만
npm run test:firefox

# Safari(WebKit)에서만
npm run test:safari

# 모바일 Chrome
npm run test:mobile
```

### 리포트 및 결과

```bash
# HTML 리포트 보기
npm run report

# 리포트 자동 열기
npx playwright show-report
```

## 🔧 설정 커스터마이징

### 환경 변수 설정

`.env.example` 파일을 `.env`로 복사하여 수정:

```bash
cp .env.example .env
```

`.env` 파일 내용:

```bash
# 애플리케이션 URL
BASE_URL=http://localhost:8080

# 브라우저 헤드리스 모드
HEADLESS=false

# 테스트 타임아웃
TEST_TIMEOUT=30000
```

### Playwright 설정 수정

`playwright.config.ts` 파일을 편집하여 설정을 변경할 수 있습니다:

```typescript
export default defineConfig({
  // 타임아웃 조정
  timeout: 60000,
  
  // 재시도 횟수
  retries: 2,
  
  // 워커 수
  workers: 4,
  
  // 베이스 URL
  use: {
    baseURL: 'http://localhost:8080',
  },
});
```

## 🎥 디버깅

### UI 모드 사용

UI 모드는 테스트를 시각적으로 디버깅할 수 있는 최고의 방법입니다:

```bash
npm run test:ui
```

기능:
- ✅ 각 단계를 시각적으로 확인
- ✅ 타임라인 보기
- ✅ DOM 스냅샷
- ✅ 네트워크 요청 확인
- ✅ 콘솔 로그 확인

### 디버그 모드

특정 테스트를 한 줄씩 실행:

```bash
npx playwright test tests/chat.spec.ts --debug
```

### 트레이스 뷰어

실패한 테스트의 트레이스 확인:

```bash
npx playwright show-trace test-results/trace.zip
```

### 코드 생성기

자동으로 테스트 코드 생성:

```bash
npm run codegen
# 또는
npx playwright codegen http://localhost:8080
```

## 📸 스크린샷 및 비디오

### 스크린샷

테스트 실패 시 자동으로 스크린샷이 저장됩니다:

```
test-results/
  screenshots/
    test-name-chromium.png
```

수동으로 스크린샷 찍기:

```typescript
await page.screenshot({ path: 'screenshot.png' });
```

### 비디오

실패한 테스트의 비디오가 자동으로 저장됩니다:

```
test-results/
  videos/
    test-name-chromium.webm
```

## 🔍 일반적인 문제 해결

### 1. 브라우저 실행 오류

**문제**: `browserType.launch: Executable doesn't exist`

**해결**:
```bash
npx playwright install chromium
# 또는 모든 브라우저 설치
npx playwright install
```

### 2. 시스템 의존성 오류 (Linux)

**문제**: `Host system is missing dependencies`

**해결**:
```bash
sudo npx playwright install-deps
```

### 3. 타임아웃 오류

**문제**: `Test timeout of 30000ms exceeded`

**해결**:
- `playwright.config.ts`에서 타임아웃 증가
- 또는 개별 테스트에서:

```typescript
test('slow test', async ({ page }) => {
  test.setTimeout(60000);
  // ...
});
```

### 4. 포트 충돌

**문제**: 애플리케이션이 이미 8080 포트에서 실행 중

**해결**:
```bash
# 다른 포트로 애플리케이션 실행
./gradlew bootRun --args='--server.port=8081'

# .env 파일에서 BASE_URL 수정
BASE_URL=http://localhost:8081
```

### 5. Node.js 버전 오류

**문제**: `Node.js 18 or higher is required`

**해결**:
```bash
# nvm 사용 시
nvm install 20
nvm use 20

# 또는 공식 사이트에서 다운로드
# https://nodejs.org/
```

## 🌐 CI/CD 환경에서 실행

### GitHub Actions

`.github/workflows/e2e-tests.yml`:

```yaml
name: E2E Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: Install dependencies
        working-directory: e2e-tests
        run: |
          npm ci
          npx playwright install --with-deps
      
      - name: Run tests
        working-directory: e2e-tests
        run: npm test
        env:
          CI: true
      
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: e2e-tests/playwright-report/
```

### Docker

```bash
# Playwright Docker 이미지 사용
docker run -it --rm \
  -v $(pwd):/work \
  -w /work/e2e-tests \
  mcr.microsoft.com/playwright:v1.48.0-jammy \
  npm test
```

## 📊 테스트 커버리지

현재 구현된 테스트:

| 카테고리 | 테스트 수 | 설명 |
|---------|----------|------|
| 채팅 기능 | 16개 | 메시지 전송, 응답 수신 등 |
| 권한 관리 | 17개 | 사용자/경로 관리 |
| 접근성 | 14개 | WCAG 준수 확인 |
| 성능 | 11개 | 로딩 시간, 메모리 사용 |
| 시각적 회귀 | 5개 | UI 변경 감지 |

**총계**: 63개 테스트

## 📚 추가 학습 자료

- [Playwright 공식 문서](https://playwright.dev/)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Page Object Model 패턴](https://playwright.dev/docs/pom)
- [Debugging Tests](https://playwright.dev/docs/debug)
- [CI/CD Integration](https://playwright.dev/docs/ci)

## 💡 팁과 트릭

### 1. 빠른 피드백을 위한 단일 브라우저 테스트

개발 중에는 한 브라우저만 사용:

```bash
npx playwright test --project=chromium
```

### 2. 실패한 테스트만 재실행

```bash
npx playwright test --last-failed
```

### 3. 테스트 필터링

```bash
# 이름으로 필터링
npx playwright test -g "chat"

# 파일로 필터링
npx playwright test chat
```

### 4. 병렬 실행 조절

```bash
# 워커 수 지정
npx playwright test --workers=2
```

### 5. 헤드 모드 + 슬로우 모션

```bash
npx playwright test --headed --slow-mo=1000
```

## 🤝 도움 받기

문제가 발생하면:

1. **문서 확인**: README.md, CONTRIBUTING.md
2. **Issue 검색**: GitHub Issues
3. **새 Issue 생성**: 상세한 재현 단계 포함
4. **팀에 문의**: Slack, 이메일 등

## ✅ 다음 단계

테스트 시스템이 설정되었으면:

1. 📖 [README.md](./README.md)에서 전체 기능 확인
2. 🧪 기존 테스트 실행해보기
3. 📝 [CONTRIBUTING.md](./CONTRIBUTING.md)에서 새 테스트 작성법 학습
4. 🚀 팀과 함께 테스트 커버리지 확대

Happy Testing! 🎉
