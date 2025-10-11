import { test, expect } from '@playwright/test';
import { ChatPage } from '../pages/ChatPage';
import { PermissionsPage } from '../pages/PermissionsPage';

/**
 * Visual Regression Tests
 * 화면의 시각적 변화를 감지하는 테스트
 */

test.describe('Visual Regression Tests', () => {
  
  test('chat page visual snapshot', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Wait for page to be fully loaded
    await page.waitForLoadState('networkidle');
    
    // Take snapshot of the entire page
    await expect(page).toHaveScreenshot('chat-page.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('permissions page visual snapshot', async ({ page }) => {
    const permissionsPage = new PermissionsPage(page);
    await permissionsPage.goto();
    
    await page.waitForLoadState('networkidle');
    
    await expect(page).toHaveScreenshot('permissions-page.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('chat message rendering snapshot', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Send a test message
    await chatPage.sendMessage('테스트 메시지');
    
    // Wait for response
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();
    
    // Take snapshot of chat messages
    const messagesContainer = page.locator('#chatMessages');
    await expect(messagesContainer).toHaveScreenshot('chat-messages.png', {
      animations: 'disabled',
    });
  });

  test('mobile chat view snapshot', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    await page.waitForLoadState('networkidle');
    
    await expect(page).toHaveScreenshot('chat-page-mobile.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('tablet permissions view snapshot', async ({ page }) => {
    // Set tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 });
    
    const permissionsPage = new PermissionsPage(page);
    await permissionsPage.goto();
    await page.waitForLoadState('networkidle');
    
    await expect(page).toHaveScreenshot('permissions-page-tablet.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });
});
