import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Chat interface
 */
export class ChatPage extends BasePage {
  // Locators
  readonly chatContainer: Locator;
  readonly chatHeader: Locator;
  readonly chatMessages: Locator;
  readonly messageInput: Locator;
  readonly sendButton: Locator;
  readonly deepThinkCheckbox: Locator;
  readonly keywordsInput: Locator;
  readonly backButton: Locator;
  readonly sessionInfo: Locator;
  readonly messageCount: Locator;
  readonly typingIndicator: Locator;

  constructor(page: Page) {
    super(page);

    // Initialize locators
    this.chatContainer = page.locator('.chat-container');
    this.chatHeader = page.locator('.chat-header');
    this.chatMessages = page.locator('#chatMessages');
    this.messageInput = page.locator('#messageInput');
    this.sendButton = page.locator('#sendButton');
    this.deepThinkCheckbox = page.locator('#deepThinkCheckbox');
    this.keywordsInput = page.locator('#keywordsInput');
    this.backButton = page.locator('.back-btn');
    this.sessionInfo = page.locator('#sessionId');
    this.messageCount = page.locator('#messageCount');
    this.typingIndicator = page.locator('.typing-indicator');
  }

  /**
   * Navigate to chat page
   */
  async goto() {
    await super.goto('/chat');
  }

  /**
   * Check if the page is loaded
   */
  async isLoaded(): Promise<boolean> {
    try {
      await this.waitForElement(this.chatContainer);
      await this.waitForElement(this.messageInput);
      await this.waitForElement(this.sendButton);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Send a message in the chat
   */
  async sendMessage(message: string, options?: {
    deepThink?: boolean;
    keywords?: string[];
  }) {
    // Set deep think option if provided
    if (options?.deepThink !== undefined) {
      const isChecked = await this.deepThinkCheckbox.isChecked();
      if (isChecked !== options.deepThink) {
        await this.deepThinkCheckbox.click();
      }
    }

    // Set keywords if provided
    if (options?.keywords && options.keywords.length > 0) {
      await this.fillInput(this.keywordsInput, options.keywords.join(', '));
    }

    // Type and send message
    await this.fillInput(this.messageInput, message);
    await this.sendButton.click();

    // Wait for response
    await this.waitForResponse();
  }

  /**
   * Wait for bot response
   */
  async waitForResponse() {
    // Wait for typing indicator to appear
    await this.typingIndicator.waitFor({ state: 'visible', timeout: 5000 });
    
    // Wait for typing indicator to disappear (response received)
    await this.typingIndicator.waitFor({ state: 'hidden', timeout: 30000 });
  }

  /**
   * Get all messages in the chat
   */
  async getMessages(): Promise<Array<{ type: 'user' | 'bot', content: string, timestamp?: string }>> {
    const messages = await this.page.locator('.message').all();
    const result = [];

    for (const message of messages) {
      const isUser = await message.getAttribute('class').then(cls => cls?.includes('user'));
      const content = await message.locator('.message-content').textContent();
      const timestamp = await message.locator('.message-timestamp').textContent().catch(() => null);

      result.push({
        type: isUser ? 'user' : 'bot',
        content: content || '',
        timestamp: timestamp || undefined
      });
    }

    return result;
  }

  /**
   * Get the last message
   */
  async getLastMessage(): Promise<{ type: 'user' | 'bot', content: string, timestamp?: string } | null> {
    const messages = await this.getMessages();
    return messages.length > 0 ? messages[messages.length - 1] : null;
  }

  /**
   * Get bot's last response
   */
  async getLastBotMessage(): Promise<string | null> {
    const messages = await this.getMessages();
    const botMessages = messages.filter(m => m.type === 'bot');
    return botMessages.length > 0 ? botMessages[botMessages.length - 1].content : null;
  }

  /**
   * Clear the chat (reload page)
   */
  async clearChat() {
    await this.reload();
  }

  /**
   * Get session ID
   */
  async getSessionId(): Promise<string> {
    const text = await this.sessionInfo.textContent();
    return text?.replace('Session: ', '') || '';
  }

  /**
   * Get message count
   */
  async getMessageCount(): Promise<number> {
    const text = await this.messageCount.textContent();
    const match = text?.match(/(\d+) messages?/);
    return match ? parseInt(match[1]) : 0;
  }

  /**
   * Check if send button is enabled
   */
  async isSendButtonEnabled(): Promise<boolean> {
    return await this.sendButton.isEnabled();
  }

  /**
   * Check if deep think is checked
   */
  async isDeepThinkEnabled(): Promise<boolean> {
    return await this.deepThinkCheckbox.isChecked();
  }

  /**
   * Get keywords value
   */
  async getKeywords(): Promise<string> {
    return await this.keywordsInput.inputValue();
  }

  /**
   * Navigate back to admin
   */
  async goBack() {
    await this.backButton.click();
  }

  /**
   * Wait for code blocks in messages
   */
  async waitForCodeBlocks() {
    await this.page.locator('.code-block-wrapper').first().waitFor({ state: 'visible' });
  }

  /**
   * Copy code from a code block
   */
  async copyCodeBlock(index: number = 0) {
    const copyButton = this.page.locator('.code-copy-button').nth(index);
    await copyButton.click();
    
    // Wait for copy confirmation
    await expect(copyButton).toHaveText('✓ 복사됨');
  }

  /**
   * Check if a message contains specific text
   */
  async messageContains(text: string, messageIndex?: number): Promise<boolean> {
    const messages = await this.getMessages();
    
    if (messageIndex !== undefined) {
      return messages[messageIndex]?.content.includes(text) || false;
    }

    return messages.some(m => m.content.includes(text));
  }

  /**
   * Wait for specific text in bot response
   */
  async waitForBotResponse(expectedText: string, timeout: number = 30000) {
    await this.page.waitForFunction(
      (text) => {
        const messages = document.querySelectorAll('.message.bot .message-content');
        const lastMessage = messages[messages.length - 1];
        return lastMessage && lastMessage.textContent?.includes(text);
      },
      expectedText,
      { timeout }
    );
  }

  /**
   * Take a screenshot of the chat
   */
  async screenshotChat(name: string) {
    await this.chatContainer.screenshot({ 
      path: `test-results/screenshots/chat-${name}-${Date.now()}.png` 
    });
  }
}