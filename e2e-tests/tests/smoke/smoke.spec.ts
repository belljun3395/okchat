import { test, expect } from '@playwright/test';
import { logStep } from '../../src/utils/test-helpers';

/**
 * Smoke tests - Quick tests to verify basic functionality
 * These tests should run quickly and catch major issues
 */
test.describe('Smoke Tests', () => {
  test('should load home page', async ({ page }) => {
    logStep('Verify home page loads');
    
    await page.goto('/');
    await expect(page).toHaveTitle(/OKChat/i);
  });

  test('should access chat interface', async ({ page }) => {
    logStep('Verify chat interface is accessible');
    
    await page.goto('/chat');
    await page.waitForLoadState('networkidle');
    
    // Check critical elements
    const chatContainer = page.locator('.chat-container');
    await expect(chatContainer).toBeVisible();
    
    const messageInput = page.locator('#messageInput');
    await expect(messageInput).toBeVisible();
    await expect(messageInput).toBeEnabled();
  });

  test('should access admin permissions', async ({ page }) => {
    logStep('Verify admin permissions page is accessible');
    
    await page.goto('/admin/permissions');
    await page.waitForLoadState('networkidle');
    
    // Check page loaded
    const container = page.locator('.container');
    await expect(container).toBeVisible();
    
    // Check for permission management header
    await expect(page.locator('h1')).toContainText('Permission Management');
  });

  test('should navigate between pages', async ({ page }) => {
    logStep('Test navigation flow');
    
    // Start at permissions page
    await page.goto('/admin/permissions');
    
    // Navigate to chat
    await page.click('a[href="/chat"]');
    await page.waitForURL('**/chat');
    expect(page.url()).toContain('/chat');
    
    // Navigate back to admin
    await page.click('.back-btn');
    await page.waitForURL('**/admin/permissions');
    expect(page.url()).toContain('/admin/permissions');
  });

  test('should have responsive design', async ({ page }) => {
    logStep('Test responsive behavior');
    
    // Test on different viewport sizes
    const viewports = [
      { width: 1920, height: 1080, name: 'Desktop' },
      { width: 768, height: 1024, name: 'Tablet' },
      { width: 375, height: 667, name: 'Mobile' }
    ];
    
    for (const viewport of viewports) {
      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      await page.goto('/chat');
      
      // Chat should be visible on all sizes
      const chatContainer = page.locator('.chat-container');
      await expect(chatContainer).toBeVisible();
      
      console.log(`✓ ${viewport.name} view works correctly`);
    }
  });

  test('should handle basic chat interaction', async ({ page }) => {
    logStep('Test basic chat functionality');
    
    await page.goto('/chat');
    
    // Type and send a message
    const messageInput = page.locator('#messageInput');
    const sendButton = page.locator('#sendButton');
    
    await messageInput.fill('Hello, this is a smoke test');
    await sendButton.click();
    
    // Verify message appears
    const userMessage = page.locator('.message.user').filter({ hasText: 'Hello, this is a smoke test' });
    await expect(userMessage).toBeVisible();
    
    // Wait for bot response (with timeout)
    const typingIndicator = page.locator('.typing-indicator');
    await typingIndicator.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
    
    // Inputs should be disabled while processing
    await expect(messageInput).toBeDisabled();
    await expect(sendButton).toBeDisabled();
  });

  test('should check API health', async ({ request }) => {
    logStep('Verify API endpoints are responsive');
    
    // Test chat API endpoint exists
    const chatResponse = await request.post('/api/chat', {
      data: {
        message: 'Health check',
        sessionId: 'smoke-test-session'
      },
      headers: {
        'Content-Type': 'application/json'
      },
      ignoreHTTPSErrors: true
    });
    
    // Should not return 404
    expect(chatResponse.status()).not.toBe(404);
    
    // Should be a valid HTTP response
    expect([200, 201, 400, 401, 403, 500]).toContain(chatResponse.status());
  });

  test('should verify static assets load', async ({ page }) => {
    logStep('Check static assets');
    
    await page.goto('/chat');
    
    // Check for console errors
    const consoleErrors: string[] = [];
    page.on('console', message => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text());
      }
    });
    
    // Wait for page to fully load
    await page.waitForLoadState('networkidle');
    
    // Check for network failures
    const failedRequests: string[] = [];
    page.on('requestfailed', request => {
      failedRequests.push(request.url());
    });
    
    await page.waitForTimeout(2000);
    
    // Verify no critical errors
    const criticalErrors = consoleErrors.filter(error => 
      !error.includes('favicon') && 
      !error.includes('DevTools')
    );
    
    expect(criticalErrors).toHaveLength(0);
    expect(failedRequests.filter(url => !url.includes('favicon'))).toHaveLength(0);
  });

  test('should verify permission manager loads', async ({ page }) => {
    logStep('Quick check of permission manager');
    
    await page.goto('/admin/permissions/manage');
    await page.waitForLoadState('networkidle');
    
    // Check both panels load
    const userPanel = page.locator('.user-panel');
    const treePanel = page.locator('.tree-panel');
    
    await expect(userPanel).toBeVisible();
    await expect(treePanel).toBeVisible();
    
    // Check critical controls
    await expect(page.locator('#userSearch')).toBeVisible();
    await expect(page.locator('#saveButton')).toBeVisible();
  });

  test('should complete basic user flow', async ({ page }) => {
    logStep('Test complete user flow');
    
    // 1. Start at admin page
    await page.goto('/admin/permissions');
    await expect(page.locator('h1')).toContainText('Permission Management');
    
    // 2. Go to chat
    await page.click('a[href="/chat"]');
    await page.waitForURL('**/chat');
    
    // 3. Send a message
    await page.locator('#messageInput').fill('Quick test message');
    await page.locator('#sendButton').click();
    
    // 4. Go back to admin
    await page.click('.back-btn');
    await page.waitForURL('**/admin/permissions');
    
    // 5. Go to permission manager
    await page.click('a[href="/admin/permissions/manage"]');
    await page.waitForURL('**/admin/permissions/manage');
    
    // Verify we completed the flow
    await expect(page.locator('.user-panel')).toBeVisible();
  });
});