# ⚡ Quick Start Guide

빠르게 E2E 테스트를 시작하세요!

## 1️⃣ 설치 (최초 1회)

```bash
cd e2e-tests
./setup.sh
```

## 2️⃣ 테스트 실행

### 가장 쉬운 방법 (프로젝트 루트에서)

```bash
./run-e2e-tests.sh
```

메뉴가 나타나면 원하는 옵션을 선택하세요!

### 직접 실행

```bash
cd e2e-tests

# UI 모드 (추천! 👍)
npm run test:ui

# 모든 테스트
npm test

# 브라우저 보기
npm run test:headed
```

## 3️⃣ 결과 확인

```bash
# HTML 리포트 열기
npm run report
```

## 📋 자주 사용하는 명령어

| 명령어 | 설명 |
|--------|------|
| `npm run test:ui` | 🎨 UI 모드 (최고의 디버깅 경험) |
| `npm test` | 🤖 모든 테스트 실행 |
| `npm run test:chat` | 💬 채팅 테스트만 |
| `npm run test:permissions` | 🔐 권한 테스트만 |
| `npm run test:headed` | 👀 브라우저 보며 실행 |
| `npm run report` | 📊 리포트 보기 |
| `npm run codegen` | ✨ 테스트 코드 생성 |

## 🐛 디버깅

```bash
# 특정 테스트 디버깅
npx playwright test tests/chat.spec.ts --debug

# UI 모드로 디버깅 (가장 쉬움)
npm run test:ui
```

## 📚 더 자세한 내용

- **[README.md](./README.md)** - 전체 가이드
- **[SETUP_GUIDE.md](./SETUP_GUIDE.md)** - 상세 설정
- **[CONTRIBUTING.md](./CONTRIBUTING.md)** - 테스트 작성법

## 💡 팁

1. **첫 실행은 UI 모드로!**
   ```bash
   npm run test:ui
   ```

2. **특정 브라우저만 빠르게 테스트**
   ```bash
   npm run test:chrome
   ```

3. **실패한 테스트만 다시 실행**
   ```bash
   npx playwright test --last-failed
   ```

4. **테스트 이름으로 필터링**
   ```bash
   npx playwright test -g "chat"
   ```

## ⚠️ 문제 발생?

### 브라우저가 없다는 오류
```bash
npx playwright install chromium
```

### 포트 충돌 (8080)
애플리케이션이 이미 실행 중인지 확인:
```bash
curl http://localhost:8080
```

### 타임아웃 오류
테스트 시간이 더 필요할 수 있습니다. `playwright.config.ts`에서 타임아웃을 늘려보세요.

---

**Happy Testing! 🎉**

더 궁금한 점이 있다면 팀에 문의하세요!
