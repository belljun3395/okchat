import { Page } from '@playwright/test';

/**
 * Test helper utilities
 */

/**
 * Generate a unique test ID
 */
export function generateTestId(prefix: string = 'test'): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
}

/**
 * Generate a unique email for testing
 */
export function generateTestEmail(): string {
  return `test-${Date.now()}@example.com`;
}

/**
 * Wait for network to be idle
 */
export async function waitForNetworkIdle(page: Page, timeout: number = 5000) {
  await page.waitForLoadState('networkidle', { timeout });
}

/**
 * Retry a function with exponential backoff
 */
export async function retry<T>(
  fn: () => Promise<T>,
  options: {
    retries?: number;
    delay?: number;
    backoff?: number;
  } = {}
): Promise<T> {
  const { retries = 3, delay = 1000, backoff = 2 } = options;
  
  let lastError: Error;
  
  for (let i = 0; i < retries; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      
      if (i < retries - 1) {
        const waitTime = delay * Math.pow(backoff, i);
        await new Promise(resolve => setTimeout(resolve, waitTime));
      }
    }
  }
  
  throw lastError!;
}

/**
 * Take a screenshot with metadata
 */
export async function takeScreenshot(
  page: Page,
  name: string,
  metadata?: Record<string, any>
) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filename = `${name}-${timestamp}.png`;
  
  await page.screenshot({
    path: `test-results/screenshots/${filename}`,
    fullPage: true
  });
  
  // Log metadata if provided
  if (metadata) {
    console.log(`Screenshot ${filename} metadata:`, JSON.stringify(metadata, null, 2));
  }
}

/**
 * Mock date/time for consistent testing
 */
export async function mockDateTime(page: Page, date: Date) {
  await page.addInitScript((dateStr) => {
    const mockDate = new Date(dateStr);
    
    // Override Date constructor
    // @ts-ignore
    Date = class extends Date {
      constructor(...args: any[]) {
        if (args.length === 0) {
          super(mockDate);
        } else {
          // @ts-ignore
          super(...args);
        }
      }
      
      static now() {
        return mockDate.getTime();
      }
    };
  }, date.toISOString());
}

/**
 * Clear all cookies and local storage
 */
export async function clearBrowserData(page: Page) {
  await page.context().clearCookies();
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}

/**
 * Wait for element to be stable (not moving)
 */
export async function waitForElementStable(
  page: Page,
  selector: string,
  timeout: number = 5000
) {
  const element = page.locator(selector);
  await element.waitFor({ state: 'visible' });
  
  let previousBox = await element.boundingBox();
  const startTime = Date.now();
  
  while (Date.now() - startTime < timeout) {
    await page.waitForTimeout(100);
    const currentBox = await element.boundingBox();
    
    if (
      previousBox &&
      currentBox &&
      previousBox.x === currentBox.x &&
      previousBox.y === currentBox.y &&
      previousBox.width === currentBox.width &&
      previousBox.height === currentBox.height
    ) {
      return;
    }
    
    previousBox = currentBox;
  }
  
  throw new Error(`Element ${selector} did not stabilize within ${timeout}ms`);
}

/**
 * Measure performance of an action
 */
export async function measurePerformance<T>(
  name: string,
  action: () => Promise<T>
): Promise<{ result: T; duration: number }> {
  const startTime = performance.now();
  const result = await action();
  const duration = performance.now() - startTime;
  
  console.log(`Performance: ${name} took ${duration.toFixed(2)}ms`);
  
  return { result, duration };
}

/**
 * Create a test user object
 */
export function createTestUser(overrides?: Partial<TestUser>): TestUser {
  return {
    email: generateTestEmail(),
    name: `Test User ${Date.now()}`,
    password: 'Test123!@#',
    ...overrides
  };
}

export interface TestUser {
  email: string;
  name: string;
  password: string;
}

/**
 * Format date for display
 */
export function formatDate(date: Date): string {
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
}

/**
 * Parse Korean number text to number
 */
export function parseKoreanNumber(text: string): number {
  // Remove commas and parse
  const cleaned = text.replace(/,/g, '').replace(/[^0-9.-]/g, '');
  return parseInt(cleaned) || 0;
}

/**
 * Check if running in CI environment
 */
export function isCI(): boolean {
  return process.env.CI === 'true';
}

/**
 * Get environment variable with fallback
 */
export function getEnvVar(name: string, fallback: string = ''): string {
  return process.env[name] || fallback;
}

/**
 * Log test step
 */
export function logStep(step: string) {
  console.log(`\n📌 ${step}\n`);
}

/**
 * Create a delay promise
 */
export function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}