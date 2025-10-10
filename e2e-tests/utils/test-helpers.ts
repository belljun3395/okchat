import { Page, expect } from '@playwright/test';

/**
 * Test helper utilities for common operations
 */

/**
 * Wait for network to be idle
 */
export async function waitForNetworkIdle(page: Page, timeout = 5000): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout });
}

/**
 * Wait for element to be visible and enabled
 */
export async function waitForElement(
  page: Page,
  selector: string,
  options?: { timeout?: number }
): Promise<void> {
  await page.waitForSelector(selector, {
    state: 'visible',
    timeout: options?.timeout || 10000,
  });
}

/**
 * Type text with realistic typing speed
 */
export async function typeText(
  page: Page,
  selector: string,
  text: string,
  delay = 50
): Promise<void> {
  await page.locator(selector).fill('');
  await page.locator(selector).type(text, { delay });
}

/**
 * Take screenshot with timestamp
 */
export async function takeScreenshot(
  page: Page,
  name: string
): Promise<void> {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  await page.screenshot({
    path: `test-results/screenshots/${name}-${timestamp}.png`,
    fullPage: true,
  });
}

/**
 * Check if element contains text
 */
export async function hasText(
  page: Page,
  selector: string,
  text: string
): Promise<boolean> {
  const element = page.locator(selector);
  const content = await element.textContent();
  return content?.includes(text) || false;
}

/**
 * Wait for message to appear in chat
 */
export async function waitForChatMessage(
  page: Page,
  messageText: string,
  timeout = 30000
): Promise<void> {
  await page.waitForSelector(
    `.message:has-text("${messageText}")`,
    { timeout }
  );
}

/**
 * Get all chat messages
 */
export async function getChatMessages(page: Page): Promise<string[]> {
  const messages = await page.locator('.message .message-content').allTextContents();
  return messages;
}

/**
 * Clear local storage and cookies
 */
export async function clearBrowserData(page: Page): Promise<void> {
  await page.context().clearCookies();
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}

/**
 * Mock API response
 */
export async function mockApiResponse(
  page: Page,
  url: string,
  response: any
): Promise<void> {
  await page.route(url, (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(response),
    });
  });
}

/**
 * Wait for SSE (Server-Sent Events) stream to complete
 */
export async function waitForStreamComplete(
  page: Page,
  timeout = 60000
): Promise<void> {
  // Wait for typing indicator to disappear
  await page.waitForSelector('#typingIndicator', {
    state: 'hidden',
    timeout,
  });
  
  // Additional wait for any final rendering
  await page.waitForTimeout(500);
}

/**
 * Check console for errors
 */
export async function getConsoleErrors(page: Page): Promise<string[]> {
  const errors: string[] = [];
  
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      errors.push(msg.text());
    }
  });
  
  return errors;
}

/**
 * Assert no console errors
 */
export async function assertNoConsoleErrors(page: Page): Promise<void> {
  const errors = await getConsoleErrors(page);
  expect(errors).toHaveLength(0);
}
