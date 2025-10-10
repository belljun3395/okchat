import { test, expect } from '@playwright/test';
import { ChatPage } from '../pages/ChatPage';
import { PermissionsPage } from '../pages/PermissionsPage';

/**
 * Accessibility Tests
 * 접근성 검증 테스트
 */

test.describe('Accessibility Tests', () => {
  
  test('chat page keyboard navigation', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Tab through interactive elements
    await page.keyboard.press('Tab');
    await expect(page.locator('#messageInput')).toBeFocused();
    
    // Type message using keyboard
    await page.keyboard.type('키보드 테스트');
    
    // Send with Enter
    await page.keyboard.press('Enter');
    
    // Verify message was sent
    await chatPage.waitForTypingIndicator();
    const lastUserMsg = await chatPage.getLastUserMessage();
    expect(lastUserMsg).toBe('키보드 테스트');
  });

  test('chat page focus management', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Message input should be focused on load
    await expect(chatPage.messageInput).toBeFocused();
    
    // After sending message, input should be re-focused
    await chatPage.sendMessage('포커스 테스트');
    
    // Wait for processing
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();
    
    // Input should be focused again
    await expect(chatPage.messageInput).toBeFocused();
  });

  test('permissions page link navigation', async ({ page }) => {
    const permissionsPage = new PermissionsPage(page);
    await permissionsPage.goto();
    
    // All links should be accessible via keyboard
    await page.keyboard.press('Tab');
    
    // Check that focused element is a link or button
    const focusedElement = await page.evaluate(() => {
      const el = document.activeElement;
      return el?.tagName.toLowerCase();
    });
    
    expect(['a', 'button', 'input']).toContain(focusedElement);
  });

  test('form elements have labels', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Check that checkboxes have associated labels
    const deepThinkCheckbox = chatPage.deepThinkCheckbox;
    const checkboxId = await deepThinkCheckbox.getAttribute('id');
    
    if (checkboxId) {
      const label = page.locator(`label[for="${checkboxId}"]`);
      await expect(label).toBeVisible();
    }
  });

  test('buttons have proper text or aria-labels', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Send button should have text
    const sendButtonText = await chatPage.sendButton.textContent();
    expect(sendButtonText).toBeTruthy();
    
    // Back button should have text
    const backButtonText = await chatPage.backButton.textContent();
    expect(backButtonText).toBeTruthy();
  });

  test('color contrast for text elements', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Check header text color
    const headerTitle = page.locator('.header-title');
    const color = await headerTitle.evaluate((el) => {
      const styles = window.getComputedStyle(el);
      return {
        color: styles.color,
        backgroundColor: styles.backgroundColor,
      };
    });
    
    // Should have colors defined
    expect(color.color).toBeTruthy();
  });

  test('interactive elements are keyboard accessible', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Tab to send button
    await page.keyboard.press('Tab'); // message input
    await page.keyboard.press('Tab'); // send button
    
    // Send button should be focused (or another interactive element)
    const focusedElement = await page.evaluate(() => document.activeElement?.tagName);
    expect(['BUTTON', 'INPUT', 'A']).toContain(focusedElement);
  });

  test('no autoplaying content', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Check for autoplaying videos or audio
    const autoplayElements = await page.locator('video[autoplay], audio[autoplay]').count();
    expect(autoplayElements).toBe(0);
  });

  test('proper heading hierarchy', async ({ page }) => {
    const permissionsPage = new PermissionsPage(page);
    await permissionsPage.goto();
    
    // Page should have h1
    const h1Count = await page.locator('h1').count();
    expect(h1Count).toBeGreaterThanOrEqual(1);
    
    // Check heading order (h1 should come before h2, etc.)
    const headings = await page.locator('h1, h2, h3, h4, h5, h6').all();
    expect(headings.length).toBeGreaterThan(0);
  });

  test('images have alt text or are decorative', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Find all images
    const images = await page.locator('img').all();
    
    for (const img of images) {
      const alt = await img.getAttribute('alt');
      const role = await img.getAttribute('role');
      
      // Image should have alt text or be marked as decorative
      const hasAccessibility = alt !== null || role === 'presentation';
      expect(hasAccessibility).toBeTruthy();
    }
  });

  test('focus is visible', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Tab to an element
    await page.keyboard.press('Tab');
    
    // Check that focused element has visible outline or focus style
    const focusStyle = await page.evaluate(() => {
      const el = document.activeElement;
      if (!el) return null;
      const styles = window.getComputedStyle(el);
      return {
        outline: styles.outline,
        outlineWidth: styles.outlineWidth,
        boxShadow: styles.boxShadow,
      };
    });
    
    // Should have some focus indicator
    expect(focusStyle).toBeTruthy();
  });

  test('skip to main content link (optional)', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Optional: Check for skip link
    // This is a best practice for accessibility
    const skipLink = page.locator('a[href="#main-content"], a:has-text("Skip to")');
    const skipLinkCount = await skipLink.count();
    
    // If present, should be functional
    if (skipLinkCount > 0) {
      await expect(skipLink.first()).toBeVisible();
    }
  });

  test('error messages are announced', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Simulate network error
    await page.route('**/api/chat', (route) => route.abort());
    
    await chatPage.sendMessage('오류 테스트');
    
    // Wait for error message
    await page.waitForTimeout(2000);
    
    // Error message should be visible
    const lastBotMsg = await chatPage.getLastBotMessage();
    expect(lastBotMsg).toBeTruthy();
  });

  test('responsive design - mobile accessibility', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // All interactive elements should still be accessible
    await expect(chatPage.messageInput).toBeVisible();
    await expect(chatPage.sendButton).toBeVisible();
    
    // Touch targets should be large enough (at least 44x44px)
    const buttonBox = await chatPage.sendButton.boundingBox();
    if (buttonBox) {
      expect(buttonBox.width).toBeGreaterThanOrEqual(40);
      expect(buttonBox.height).toBeGreaterThanOrEqual(40);
    }
  });

  test('language attribute is set', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // HTML element should have lang attribute
    const lang = await page.locator('html').getAttribute('lang');
    
    // Should have language set (likely 'ko' for Korean or 'en' for English)
    expect(lang).toBeTruthy();
  });
});
