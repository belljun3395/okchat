import { Page, Locator } from '@playwright/test';

/**
 * Page Object Model for Chat Interface
 */
export class ChatPage {
  readonly page: Page;
  readonly messageInput: Locator;
  readonly sendButton: Locator;
  readonly chatMessages: Locator;
  readonly deepThinkCheckbox: Locator;
  readonly keywordsInput: Locator;
  readonly sessionId: Locator;
  readonly messageCount: Locator;
  readonly backButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.messageInput = page.locator('#messageInput');
    this.sendButton = page.locator('#sendButton');
    this.chatMessages = page.locator('#chatMessages');
    this.deepThinkCheckbox = page.locator('#deepThinkCheckbox');
    this.keywordsInput = page.locator('#keywordsInput');
    this.sessionId = page.locator('#sessionId');
    this.messageCount = page.locator('#messageCount');
    this.backButton = page.locator('.back-btn');
  }

  /**
   * Navigate to chat page
   */
  async goto() {
    await this.page.goto('/chat');
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Send a message in the chat
   */
  async sendMessage(message: string) {
    await this.messageInput.fill(message);
    await this.sendButton.click();
  }

  /**
   * Send a message with keywords
   */
  async sendMessageWithKeywords(message: string, keywords: string[]) {
    await this.keywordsInput.fill(keywords.join(', '));
    await this.messageInput.fill(message);
    await this.sendButton.click();
  }

  /**
   * Send a message with deep think enabled
   */
  async sendMessageWithDeepThink(message: string) {
    await this.deepThinkCheckbox.check();
    await this.messageInput.fill(message);
    await this.sendButton.click();
  }

  /**
   * Wait for typing indicator to appear
   */
  async waitForTypingIndicator() {
    await this.page.waitForSelector('.typing-indicator.active', {
      state: 'visible',
    });
  }

  /**
   * Wait for typing indicator to disappear
   */
  async waitForResponse(timeout = 60000) {
    await this.page.waitForSelector('.typing-indicator.active', {
      state: 'hidden',
      timeout,
    });
  }

  /**
   * Get all messages
   */
  async getMessages() {
    return await this.chatMessages.locator('.message').all();
  }

  /**
   * Get message count
   */
  async getMessageCountText() {
    return await this.messageCount.textContent();
  }

  /**
   * Get last bot message
   */
  async getLastBotMessage() {
    const messages = await this.chatMessages
      .locator('.message.bot:last-child .message-content')
      .textContent();
    return messages;
  }

  /**
   * Get last user message
   */
  async getLastUserMessage() {
    const messages = await this.chatMessages
      .locator('.message.user:last-child .message-content')
      .textContent();
    return messages;
  }

  /**
   * Check if send button is disabled
   */
  async isSendButtonDisabled() {
    return await this.sendButton.isDisabled();
  }

  /**
   * Check if message input is disabled
   */
  async isMessageInputDisabled() {
    return await this.messageInput.isDisabled();
  }

  /**
   * Click on a code copy button
   */
  async clickCodeCopyButton(index = 0) {
    const copyButtons = this.page.locator('.code-copy-button');
    await copyButtons.nth(index).click();
  }

  /**
   * Check if welcome message is displayed
   */
  async hasWelcomeMessage() {
    const content = await this.chatMessages.locator('.message.bot:first-child').textContent();
    return content?.includes('안녕하세요') || false;
  }

  /**
   * Navigate back to admin
   */
  async goBackToAdmin() {
    await this.backButton.click();
  }
}
