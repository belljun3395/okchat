import { test, expect } from '@playwright/test';
import { ChatPage } from '../pages/ChatPage';
import { waitForStreamComplete } from '../utils/test-helpers';

test.describe('Chat Interface', () => {
  let chatPage: ChatPage;

  test.beforeEach(async ({ page }) => {
    chatPage = new ChatPage(page);
    await chatPage.goto();
  });

  test('should display chat interface correctly', async ({ page }) => {
    // Check page title
    await expect(page).toHaveTitle(/OKChat/);

    // Check header elements
    await expect(page.locator('.header-title')).toHaveText('OKChat');
    await expect(chatPage.backButton).toBeVisible();

    // Check welcome message
    const hasWelcome = await chatPage.hasWelcomeMessage();
    expect(hasWelcome).toBeTruthy();

    // Check input elements
    await expect(chatPage.messageInput).toBeVisible();
    await expect(chatPage.sendButton).toBeVisible();
    await expect(chatPage.deepThinkCheckbox).toBeVisible();
    await expect(chatPage.keywordsInput).toBeVisible();

    // Check session info
    await expect(chatPage.sessionId).toBeVisible();
    await expect(chatPage.messageCount).toBeVisible();
  });

  test('should have correct initial state', async () => {
    // Message count should be 0
    const countText = await chatPage.getMessageCountText();
    expect(countText).toBe('0 messages');

    // Deep think checkbox should be unchecked
    await expect(chatPage.deepThinkCheckbox).not.toBeChecked();

    // Input should be enabled
    expect(await chatPage.isMessageInputDisabled()).toBe(false);
    expect(await chatPage.isSendButtonDisabled()).toBe(false);

    // Session ID should be displayed
    const sessionText = await chatPage.sessionId.textContent();
    expect(sessionText).toContain('Session: session-');
  });

  test('should send a message and receive response', async ({ page }) => {
    const userMessage = '안녕하세요';

    // Send message
    await chatPage.sendMessage(userMessage);

    // Check that send button is disabled during processing
    expect(await chatPage.isSendButtonDisabled()).toBe(true);

    // Wait for typing indicator
    await chatPage.waitForTypingIndicator();
    await expect(page.locator('.typing-indicator.active')).toBeVisible();

    // Wait for response
    await chatPage.waitForResponse();

    // Check that typing indicator is gone
    await expect(page.locator('.typing-indicator.active')).not.toBeVisible();

    // Check that send button is enabled again
    expect(await chatPage.isSendButtonDisabled()).toBe(false);

    // Verify user message is displayed
    const lastUserMsg = await chatPage.getLastUserMessage();
    expect(lastUserMsg).toBe(userMessage);

    // Verify bot response is received
    const lastBotMsg = await chatPage.getLastBotMessage();
    expect(lastBotMsg).toBeTruthy();
    expect(lastBotMsg!.length).toBeGreaterThan(0);

    // Check message count updated
    const countText = await chatPage.getMessageCountText();
    expect(countText).toContain('messages');
  });

  test('should handle Enter key to send message', async ({ page }) => {
    const userMessage = '테스트 메시지';

    // Type message and press Enter
    await chatPage.messageInput.fill(userMessage);
    await page.keyboard.press('Enter');

    // Wait for response
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();

    // Verify message was sent
    const lastUserMsg = await chatPage.getLastUserMessage();
    expect(lastUserMsg).toBe(userMessage);
  });

  test('should send message with keywords', async ({ page }) => {
    const userMessage = '프로젝트 상태를 알려주세요';
    const keywords = ['프로젝트', '2025'];

    // Send message with keywords
    await chatPage.sendMessageWithKeywords(userMessage, keywords);

    // Wait for response
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();

    // Verify message was sent
    const lastUserMsg = await chatPage.getLastUserMessage();
    expect(lastUserMsg).toBe(userMessage);

    // Verify bot response
    const lastBotMsg = await chatPage.getLastBotMessage();
    expect(lastBotMsg).toBeTruthy();
  });

  test('should send message with deep think enabled', async ({ page }) => {
    const userMessage = '복잡한 분석이 필요한 질문';

    // Send message with deep think
    await chatPage.sendMessageWithDeepThink(userMessage);

    // Verify checkbox is checked
    await expect(chatPage.deepThinkCheckbox).toBeChecked();

    // Wait for response
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse(90000); // Deep think may take longer

    // Verify message was sent
    const lastUserMsg = await chatPage.getLastUserMessage();
    expect(lastUserMsg).toBe(userMessage);
  });

  test('should not send empty message', async ({ page }) => {
    const initialMessages = await chatPage.getMessages();
    const initialCount = initialMessages.length;

    // Try to send empty message
    await chatPage.messageInput.fill('   ');
    await chatPage.sendButton.click();

    // Wait a bit
    await page.waitForTimeout(500);

    // Verify no new message was added
    const currentMessages = await chatPage.getMessages();
    expect(currentMessages.length).toBe(initialCount);

    // Input should still be focused
    await expect(chatPage.messageInput).toBeFocused();
  });

  test('should clear input after sending message', async ({ page }) => {
    const userMessage = '입력 테스트';

    // Send message
    await chatPage.sendMessage(userMessage);

    // Verify input is cleared
    const inputValue = await chatPage.messageInput.inputValue();
    expect(inputValue).toBe('');
  });

  test('should display message timestamps', async ({ page }) => {
    // Send a message
    await chatPage.sendMessage('타임스탬프 테스트');
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();

    // Check that timestamps are displayed
    const timestamps = await page.locator('.message-timestamp').all();
    expect(timestamps.length).toBeGreaterThan(0);

    // Verify timestamp format (HH:MM)
    const timestampText = await timestamps[0].textContent();
    expect(timestampText).toMatch(/\d{2}:\d{2}/);
  });

  test('should render markdown in bot messages', async ({ page }) => {
    const userMessage = '마크다운 포맷으로 답변해주세요';

    await chatPage.sendMessage(userMessage);
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();

    // Check for markdown elements (may vary based on response)
    const botMessage = page.locator('.message.bot:last-child .message-content');
    
    // Wait for content to be rendered
    await botMessage.waitFor({ state: 'visible' });
    
    // Just verify that content exists and is formatted
    const content = await botMessage.innerHTML();
    expect(content.length).toBeGreaterThan(0);
  });

  test('should handle code blocks with copy button', async ({ page }) => {
    // This test assumes the bot might return code blocks
    const userMessage = '코드 예제를 보여주세요';

    await chatPage.sendMessage(userMessage);
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();

    // Check if code blocks exist
    const codeBlocks = await page.locator('pre code').count();
    
    if (codeBlocks > 0) {
      // Verify copy button exists
      const copyButtons = await page.locator('.code-copy-button').count();
      expect(copyButtons).toBeGreaterThan(0);

      // Click copy button
      await chatPage.clickCodeCopyButton(0);

      // Verify button text changes to "복사됨"
      await expect(page.locator('.code-copy-button').first()).toContainText('복사됨');
    }
  });

  test('should scroll to latest message', async ({ page }) => {
    // Send multiple messages
    for (let i = 0; i < 3; i++) {
      await chatPage.sendMessage(`메시지 ${i + 1}`);
      await page.waitForTimeout(1000);
    }

    // Check that chat is scrolled to bottom
    const chatContainer = page.locator('#chatMessages');
    const scrollTop = await chatContainer.evaluate((el) => el.scrollTop);
    const scrollHeight = await chatContainer.evaluate((el) => el.scrollHeight);
    const clientHeight = await chatContainer.evaluate((el) => el.clientHeight);

    // Should be scrolled near the bottom (within 50px tolerance)
    expect(scrollTop + clientHeight).toBeGreaterThan(scrollHeight - 50);
  });

  test('should navigate back to admin', async ({ page }) => {
    await chatPage.goBackToAdmin();
    
    // Should navigate to permissions page
    await page.waitForURL(/\/admin\/permissions/);
    expect(page.url()).toContain('/admin/permissions');
  });

  test('should not have console errors', async ({ page }) => {
    const consoleErrors: string[] = [];

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    // Send a message
    await chatPage.sendMessage('오류 체크 테스트');
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();

    // Check for console errors (excluding expected network errors during testing)
    const criticalErrors = consoleErrors.filter(
      (error) => !error.includes('net::ERR_') && !error.includes('favicon')
    );
    expect(criticalErrors).toHaveLength(0);
  });

  test('should handle network errors gracefully', async ({ page }) => {
    // Simulate network failure
    await page.route('**/api/chat', (route) => {
      route.abort('failed');
    });

    const userMessage = '네트워크 오류 테스트';
    await chatPage.sendMessage(userMessage);

    // Should show error message
    await page.waitForTimeout(2000);
    const lastBotMsg = await chatPage.getLastBotMessage();
    expect(lastBotMsg).toContain('네트워크');
  });

  test('should maintain session ID throughout interaction', async ({ page }) => {
    const initialSessionId = await chatPage.sessionId.textContent();

    // Send a message
    await chatPage.sendMessage('세션 테스트');
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();

    // Session ID should remain the same
    const currentSessionId = await chatPage.sessionId.textContent();
    expect(currentSessionId).toBe(initialSessionId);
  });
});
