# Contributing to E2E Tests

OKChat E2E 테스트에 기여해주셔서 감사합니다!

## 🚀 시작하기

1. **저장소 클론 및 설정**
   ```bash
   git clone <repository-url>
   cd e2e-tests
   ./setup.sh
   ```

2. **테스트 실행 확인**
   ```bash
   npm test
   ```

## 📝 테스트 작성 가이드라인

### 1. 테스트 구조

테스트는 다음과 같은 구조를 따라야 합니다:

```typescript
import { test, expect } from '@playwright/test';
import { PageObjectName } from '../pages/PageObjectName';

test.describe('Feature Name', () => {
  let page: PageObjectName;

  test.beforeEach(async ({ page: browserPage }) => {
    page = new PageObjectName(browserPage);
    await page.goto();
  });

  test('should do something specific', async ({ page }) => {
    // Arrange
    const testData = 'test';
    
    // Act
    await page.performAction(testData);
    
    // Assert
    await expect(page.element).toBeVisible();
  });
});
```

### 2. 네이밍 컨벤션

- **테스트 파일**: `feature-name.spec.ts`
- **Page Objects**: `FeaturePage.ts` (PascalCase)
- **Helper 함수**: `camelCase`
- **테스트 설명**: 명확하고 구체적으로 (한글 또는 영문)

```typescript
// ✅ Good
test('should send message and receive response', async ({ page }) => {

// ❌ Bad
test('test1', async ({ page }) => {
```

### 3. Page Object Model (POM) 사용

모든 페이지 상호작용은 Page Object를 통해 수행합니다:

```typescript
// pages/NewFeaturePage.ts
export class NewFeaturePage {
  readonly page: Page;
  readonly inputField: Locator;
  readonly submitButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.inputField = page.locator('#input');
    this.submitButton = page.locator('#submit');
  }

  async goto() {
    await this.page.goto('/new-feature');
  }

  async submitForm(data: string) {
    await this.inputField.fill(data);
    await this.submitButton.click();
  }
}
```

### 4. Assertion 가이드라인

- 명확하고 구체적인 assertion 사용
- 테스트 실패 시 원인을 쉽게 파악할 수 있도록 작성

```typescript
// ✅ Good
await expect(page.locator('.message')).toContainText('Expected text');
await expect(page.locator('.error')).toBeVisible();

// ❌ Bad
expect(await page.locator('.message').textContent()).toBe('Expected text');
```

### 5. 비동기 처리

- `await`를 빠뜨리지 않도록 주의
- 적절한 대기 메커니즘 사용

```typescript
// ✅ Good
await page.waitForSelector('.element');
await page.waitForLoadState('networkidle');

// ❌ Bad
await page.waitForTimeout(5000); // 고정 시간 대기는 피하기
```

### 6. 테스트 독립성

- 각 테스트는 독립적으로 실행 가능해야 함
- 테스트 간 의존성 금지
- `beforeEach`로 초기 상태 설정

```typescript
// ✅ Good
test.beforeEach(async ({ page }) => {
  await page.goto('/chat');
  await clearData(page);
});

// ❌ Bad
test('test 1', async ({ page }) => {
  // Sets up state for test 2
});

test('test 2', async ({ page }) => {
  // Depends on test 1
});
```

## 🎯 테스트 카테고리

테스트는 다음 카테고리로 분류됩니다:

1. **기능 테스트** (`*.spec.ts`): 핵심 기능 검증
2. **접근성 테스트** (`accessibility.spec.ts`): WCAG 준수 검증
3. **성능 테스트** (`performance.spec.ts`): 로딩 시간, 응답 속도 등
4. **시각적 회귀 테스트** (`visual-regression.spec.ts`): UI 변경 감지

## ✅ Pull Request 체크리스트

PR을 생성하기 전에 확인하세요:

- [ ] 모든 테스트가 통과함 (`npm test`)
- [ ] 새로운 기능에 대한 테스트를 추가함
- [ ] Page Object Model을 사용함
- [ ] 테스트 설명이 명확함
- [ ] 코드가 TypeScript 타입 체크를 통과함
- [ ] README가 필요한 경우 업데이트함
- [ ] 불필요한 콘솔 로그를 제거함

## 🐛 버그 리포트

버그를 발견하셨나요? 다음 정보를 포함해주세요:

1. **환경 정보**
   - OS: 
   - Node.js 버전:
   - Playwright 버전:
   - 브라우저:

2. **재현 단계**
   1. Step 1
   2. Step 2
   3. ...

3. **예상 동작**

4. **실제 동작**

5. **스크린샷/로그** (있는 경우)

## 💡 개선 제안

새로운 기능이나 개선 사항을 제안하고 싶으신가요?

1. Issue를 생성하여 아이디어를 공유해주세요
2. 가능하면 구체적인 사용 사례를 포함해주세요
3. 다른 사람의 의견을 기다린 후 구현을 시작하세요

## 🔍 코드 리뷰 프로세스

1. PR 생성
2. 자동화된 테스트 실행
3. 코드 리뷰어 배정
4. 리뷰 피드백 반영
5. 승인 후 머지

## 📚 추가 자료

- [Playwright 공식 문서](https://playwright.dev/)
- [TypeScript 가이드](https://www.typescriptlang.org/docs/)
- [Testing Best Practices](https://playwright.dev/docs/best-practices)

## 🙏 감사합니다!

여러분의 기여가 프로젝트를 더 좋게 만듭니다!
