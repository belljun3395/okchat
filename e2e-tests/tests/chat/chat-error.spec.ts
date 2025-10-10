import { test, expect } from '../../src/fixtures/test-fixtures';
import { logStep } from '../../src/utils/test-helpers';

test.describe('Chat Error Handling', () => {
  test.beforeEach(async ({ chatPage }) => {
    await chatPage.goto();
    await expect(chatPage.chatContainer).toBeVisible();
  });

  test('should handle network errors gracefully', async ({ chatPage, context }) => {
    logStep('Simulate network failure');
    
    // Block API calls to simulate network error
    await context.route('**/api/chat', route => route.abort('failed'));
    
    // Try to send message
    await chatPage.sendMessage('네트워크 오류 테스트');
    
    // Should show error message
    const errorMessage = await chatPage.getLastBotMessage();
    expect(errorMessage).toBeTruthy();
    expect(errorMessage).toContain('오류');
    
    // Inputs should be re-enabled
    expect(await chatPage.isSendButtonEnabled()).toBe(true);
  });

  test('should handle server errors', async ({ chatPage, context }) => {
    logStep('Simulate server error');
    
    // Mock server error response
    await context.route('**/api/chat', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal Server Error' })
      });
    });
    
    await chatPage.sendMessage('서버 오류 테스트');
    
    // Should show appropriate error message
    const errorMessage = await chatPage.getLastBotMessage();
    expect(errorMessage).toBeTruthy();
    expect(errorMessage).toContain('서버');
  });

  test('should handle timeout errors', async ({ chatPage, context }) => {
    logStep('Simulate timeout');
    
    // Create a delayed response that will timeout
    await context.route('**/api/chat', async route => {
      await new Promise(resolve => setTimeout(resolve, 35000)); // Longer than timeout
      route.abort('timedout');
    });
    
    // Send message (this will timeout)
    await chatPage.sendMessage('타임아웃 테스트');
    
    // Should show timeout error
    const errorMessage = await chatPage.getLastBotMessage();
    expect(errorMessage).toBeTruthy();
    
    // Should be able to retry
    expect(await chatPage.isSendButtonEnabled()).toBe(true);
  });

  test('should handle malformed responses', async ({ chatPage, context }) => {
    logStep('Simulate malformed response');
    
    // Mock malformed response
    await context.route('**/api/chat', route => {
      route.fulfill({
        status: 200,
        contentType: 'text/plain',
        body: 'This is not a valid SSE response'
      });
    });
    
    await chatPage.sendMessage('잘못된 응답 테스트');
    
    // Should handle gracefully
    await chatPage.page.waitForTimeout(2000);
    
    // Check if any error message appeared or if it handled silently
    const messages = await chatPage.getMessages();
    expect(messages.length).toBeGreaterThan(1); // At least welcome + user message
  });

  test('should handle authentication errors', async ({ chatPage, context }) => {
    logStep('Simulate authentication error');
    
    // Mock 401 response
    await context.route('**/api/chat', route => {
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Unauthorized' })
      });
    });
    
    await chatPage.sendMessage('인증 오류 테스트');
    
    // Should show auth error message
    const errorMessage = await chatPage.getLastBotMessage();
    expect(errorMessage).toBeTruthy();
    expect(errorMessage).toContain('인증');
  });

  test('should handle rate limiting', async ({ chatPage, context }) => {
    logStep('Simulate rate limiting');
    
    // Mock 429 response
    await context.route('**/api/chat', route => {
      route.fulfill({
        status: 429,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Too Many Requests' })
      });
    });
    
    await chatPage.sendMessage('요청 제한 테스트');
    
    // Should show rate limit error
    const errorMessage = await chatPage.getLastBotMessage();
    expect(errorMessage).toBeTruthy();
    expect(errorMessage).toContain('너무 많습니다');
  });

  test('should recover from errors', async ({ chatPage, context }) => {
    logStep('Test error recovery');
    
    let requestCount = 0;
    
    // First request fails, second succeeds
    await context.route('**/api/chat', route => {
      requestCount++;
      
      if (requestCount === 1) {
        route.fulfill({
          status: 500,
          body: 'Server Error'
        });
      } else {
        // Simulate successful SSE response
        route.fulfill({
          status: 200,
          contentType: 'text/event-stream',
          body: 'data: 복구된 응답입니다.\ndata: [DONE]\n\n'
        });
      }
    });
    
    // First attempt - should fail
    await chatPage.sendMessage('첫 번째 시도');
    const errorMessage = await chatPage.getLastBotMessage();
    expect(errorMessage).toContain('서버');
    
    // Second attempt - should succeed
    await chatPage.sendMessage('두 번째 시도');
    await chatPage.waitForResponse();
    
    const successMessage = await chatPage.getLastBotMessage();
    expect(successMessage).toContain('복구된 응답');
  });

  test('should handle empty SSE data', async ({ chatPage, context }) => {
    logStep('Test empty SSE data handling');
    
    // Mock empty SSE response
    await context.route('**/api/chat', route => {
      route.fulfill({
        status: 200,
        contentType: 'text/event-stream',
        body: 'data: \ndata: [DONE]\n\n'
      });
    });
    
    await chatPage.sendMessage('빈 응답 테스트');
    
    // Should handle gracefully
    await chatPage.page.waitForTimeout(2000);
    
    // Should not crash or show error
    const messages = await chatPage.getMessages();
    expect(messages.length).toBeGreaterThanOrEqual(2); // Welcome + user message
  });
});