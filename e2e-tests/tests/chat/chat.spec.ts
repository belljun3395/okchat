import { test, expect } from '../../src/fixtures/test-fixtures';
import { logStep, delay, generateTestId } from '../../src/utils/test-helpers';

test.describe('Chat Interface', () => {
  test.beforeEach(async ({ chatPage }) => {
    await chatPage.goto();
    await expect(chatPage.chatContainer).toBeVisible();
  });

  test('should load chat interface correctly', async ({ chatPage }) => {
    logStep('Verify chat interface components');
    
    // Check all main components are visible
    await expect(chatPage.chatHeader).toBeVisible();
    await expect(chatPage.messageInput).toBeVisible();
    await expect(chatPage.sendButton).toBeVisible();
    await expect(chatPage.deepThinkCheckbox).toBeVisible();
    await expect(chatPage.keywordsInput).toBeVisible();
    
    // Check initial state
    await expect(chatPage.messageInput).toHaveValue('');
    await expect(chatPage.keywordsInput).toHaveValue('');
    await expect(chatPage.deepThinkCheckbox).not.toBeChecked();
    
    // Verify welcome message
    const messages = await chatPage.getMessages();
    expect(messages.length).toBeGreaterThan(0);
    expect(messages[0].type).toBe('bot');
    expect(messages[0].content).toContain('안녕하세요');
  });

  test('should send and receive messages', async ({ chatPage }) => {
    logStep('Send a simple message');
    
    const testMessage = '테스트 메시지입니다';
    await chatPage.sendMessage(testMessage);
    
    // Verify user message appears
    const messages = await chatPage.getMessages();
    const userMessage = messages.find(m => m.type === 'user' && m.content === testMessage);
    expect(userMessage).toBeTruthy();
    
    // Wait for bot response
    await chatPage.waitForResponse();
    
    // Verify bot responded
    const lastMessage = await chatPage.getLastBotMessage();
    expect(lastMessage).toBeTruthy();
    expect(lastMessage!.length).toBeGreaterThan(0);
    
    // Check message count updated
    const messageCount = await chatPage.getMessageCount();
    expect(messageCount).toBeGreaterThan(0);
  });

  test('should handle deep think mode', async ({ chatPage }) => {
    logStep('Test deep think functionality');
    
    const testMessage = '복잡한 질문입니다. 자세히 설명해주세요.';
    
    // Send message with deep think enabled
    await chatPage.sendMessage(testMessage, { deepThink: true });
    
    // Verify deep think was enabled
    expect(await chatPage.isDeepThinkEnabled()).toBe(true);
    
    // Wait for response
    await chatPage.waitForResponse();
    
    // Deep think responses should be longer/more detailed
    const response = await chatPage.getLastBotMessage();
    expect(response).toBeTruthy();
    expect(response!.length).toBeGreaterThan(100);
  });

  test('should handle keywords', async ({ chatPage }) => {
    logStep('Test keyword functionality');
    
    const keywords = ['테스트', '문서', '검색'];
    const testMessage = '키워드와 관련된 정보를 찾아주세요';
    
    await chatPage.sendMessage(testMessage, { keywords });
    
    // Verify keywords were set
    const keywordValue = await chatPage.getKeywords();
    expect(keywordValue).toBe(keywords.join(', '));
    
    // Wait for response
    await chatPage.waitForResponse();
    
    const response = await chatPage.getLastBotMessage();
    expect(response).toBeTruthy();
  });

  test('should show typing indicator during response', async ({ chatPage }) => {
    logStep('Verify typing indicator');
    
    // Send message
    const messagePromise = chatPage.sendMessage('테스트 질문');
    
    // Check typing indicator appears
    await chatPage.typingIndicator.waitFor({ state: 'visible', timeout: 5000 });
    expect(await chatPage.typingIndicator.isVisible()).toBe(true);
    
    // Wait for response to complete
    await messagePromise;
    
    // Typing indicator should be hidden
    expect(await chatPage.typingIndicator.isVisible()).toBe(false);
  });

  test('should handle markdown formatting', async ({ chatPage }) => {
    logStep('Test markdown rendering');
    
    const testMessage = '마크다운 형식으로 답변해주세요. 제목, 목록, 코드 블록을 포함해서.';
    await chatPage.sendMessage(testMessage);
    await chatPage.waitForResponse();
    
    // Check for markdown elements
    const lastMessageElement = chatPage.page.locator('.message.bot').last();
    
    // Check for headers
    const headers = await lastMessageElement.locator('h1, h2, h3, h4, h5, h6').count();
    expect(headers).toBeGreaterThan(0);
    
    // Check for lists (might not always be present)
    const lists = await lastMessageElement.locator('ul, ol').count();
    
    // At least one formatting element should be present
    expect(headers + lists).toBeGreaterThan(0);
  });

  test('should handle code blocks with copy functionality', async ({ chatPage }) => {
    logStep('Test code block functionality');
    
    const testMessage = '간단한 JavaScript 코드 예제를 보여주세요';
    await chatPage.sendMessage(testMessage);
    await chatPage.waitForResponse();
    
    // Look for code blocks
    const codeBlocks = chatPage.page.locator('.code-block-wrapper');
    const codeBlockCount = await codeBlocks.count();
    
    if (codeBlockCount > 0) {
      // Test copy functionality
      await chatPage.copyCodeBlock(0);
      
      // Verify copy button shows success
      const copyButton = chatPage.page.locator('.code-copy-button').first();
      await expect(copyButton).toHaveText('✓ 복사됨');
      
      // Wait for button to reset
      await delay(2500);
      await expect(copyButton).toHaveText('복사');
    }
  });

  test('should maintain session', async ({ chatPage }) => {
    logStep('Test session persistence');
    
    // Get initial session ID
    const sessionId = await chatPage.getSessionId();
    expect(sessionId).toBeTruthy();
    expect(sessionId).toMatch(/^session-\d+-\w+$/);
    
    // Send multiple messages
    await chatPage.sendMessage('첫 번째 메시지');
    await chatPage.waitForResponse();
    
    await chatPage.sendMessage('두 번째 메시지');
    await chatPage.waitForResponse();
    
    // Session ID should remain the same
    const currentSessionId = await chatPage.getSessionId();
    expect(currentSessionId).toBe(sessionId);
    
    // Message count should increase
    const messageCount = await chatPage.getMessageCount();
    expect(messageCount).toBe(2);
  });

  test('should handle empty message', async ({ chatPage }) => {
    logStep('Test empty message handling');
    
    // Try to send empty message
    await chatPage.messageInput.fill('');
    await chatPage.sendButton.click();
    
    // Should not send message
    const messages = await chatPage.getMessages();
    const initialCount = messages.length;
    
    await delay(1000);
    
    const updatedMessages = await chatPage.getMessages();
    expect(updatedMessages.length).toBe(initialCount);
  });

  test('should disable inputs while processing', async ({ chatPage }) => {
    logStep('Test input state during processing');
    
    // Send a message
    await chatPage.fillInput(chatPage.messageInput, '처리 중 테스트');
    const sendPromise = chatPage.sendButton.click();
    
    // Check inputs are disabled
    await delay(100); // Small delay to ensure processing started
    expect(await chatPage.sendButton.isDisabled()).toBe(true);
    expect(await chatPage.messageInput.isDisabled()).toBe(true);
    
    // Wait for completion
    await sendPromise;
    await chatPage.waitForResponse();
    
    // Inputs should be enabled again
    expect(await chatPage.sendButton.isEnabled()).toBe(true);
    expect(await chatPage.messageInput.isEnabled()).toBe(true);
  });

  test('should handle navigation', async ({ chatPage, page }) => {
    logStep('Test navigation functionality');
    
    // Click back button
    await chatPage.goBack();
    
    // Should navigate to admin permissions page
    await page.waitForURL('**/admin/permissions');
    expect(page.url()).toContain('/admin/permissions');
  });

  test('should handle long messages', async ({ chatPage }) => {
    logStep('Test long message handling');
    
    const longMessage = '긴 메시지 테스트: ' + 'A'.repeat(500);
    await chatPage.sendMessage(longMessage);
    
    // Verify message was sent
    const messages = await chatPage.getMessages();
    const userMessage = messages.find(m => m.type === 'user' && m.content.includes('긴 메시지 테스트'));
    expect(userMessage).toBeTruthy();
    
    // Wait for response
    await chatPage.waitForResponse();
    
    const response = await chatPage.getLastBotMessage();
    expect(response).toBeTruthy();
  });

  test('should show timestamps', async ({ chatPage }) => {
    logStep('Test message timestamps');
    
    await chatPage.sendMessage('타임스탬프 테스트');
    await chatPage.waitForResponse();
    
    const messages = await chatPage.getMessages();
    
    // All messages should have timestamps
    for (const message of messages) {
      if (message.timestamp) {
        expect(message.timestamp).toMatch(/^\d{2}:\d{2}$/);
      }
    }
  });

  test('should handle enter key', async ({ chatPage, page }) => {
    logStep('Test enter key functionality');
    
    await chatPage.fillInput(chatPage.messageInput, '엔터 키 테스트');
    await page.keyboard.press('Enter');
    
    // Message should be sent
    await chatPage.waitForResponse();
    
    const messages = await chatPage.getMessages();
    const testMessage = messages.find(m => m.content === '엔터 키 테스트');
    expect(testMessage).toBeTruthy();
  });
});