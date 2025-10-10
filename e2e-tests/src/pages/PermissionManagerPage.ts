import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Permission Manager page
 */
export class PermissionManagerPage extends BasePage {
  // Locators
  readonly container: Locator;
  readonly header: Locator;
  readonly backButton: Locator;
  
  // User panel
  readonly userPanel: Locator;
  readonly userSearch: Locator;
  readonly userList: Locator;
  
  // Tree panel
  readonly treePanel: Locator;
  readonly treeContainer: Locator;
  readonly selectedUserLabel: Locator;
  readonly saveButton: Locator;
  readonly expandAllButton: Locator;
  readonly collapseAllButton: Locator;
  
  // Action buttons
  readonly grantAllButton: Locator;
  readonly revokeAllButton: Locator;
  readonly toggleDescendantsButton: Locator;
  
  // Splitter
  readonly splitter: Locator;

  constructor(page: Page) {
    super(page);

    // Initialize locators
    this.container = page.locator('.container');
    this.header = page.locator('.header');
    this.backButton = page.locator('.back-btn');
    
    // User panel
    this.userPanel = page.locator('.user-panel');
    this.userSearch = page.locator('#userSearch');
    this.userList = page.locator('#userList');
    
    // Tree panel
    this.treePanel = page.locator('.tree-panel');
    this.treeContainer = page.locator('#treeContainer');
    this.selectedUserLabel = page.locator('#selectedUser');
    this.saveButton = page.locator('#saveButton');
    this.expandAllButton = page.locator('#expandAll');
    this.collapseAllButton = page.locator('#collapseAll');
    
    // Action buttons
    this.grantAllButton = page.locator('#grantAll');
    this.revokeAllButton = page.locator('#revokeAll');
    this.toggleDescendantsButton = page.locator('#toggleDescendants');
    
    // Splitter
    this.splitter = page.locator('.splitter');
  }

  /**
   * Navigate to permission manager page
   */
  async goto() {
    await super.goto('/admin/permissions/manage');
  }

  /**
   * Check if the page is loaded
   */
  async isLoaded(): Promise<boolean> {
    try {
      await this.waitForElement(this.container);
      await this.waitForElement(this.userPanel);
      await this.waitForElement(this.treePanel);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Search for a user
   */
  async searchUser(query: string) {
    await this.fillInput(this.userSearch, query);
    // Wait for search results to update
    await this.page.waitForTimeout(300);
  }

  /**
   * Select a user from the list
   */
  async selectUser(email: string) {
    const userItem = this.userList.locator('.user-item').filter({ hasText: email });
    await userItem.click();
    
    // Wait for tree to load
    await this.page.waitForFunction(
      () => document.querySelector('#treeContainer .tree-node') !== null,
      { timeout: 5000 }
    );
  }

  /**
   * Get selected user email
   */
  async getSelectedUser(): Promise<string | null> {
    const text = await this.selectedUserLabel.textContent();
    return text?.replace('Selected: ', '') || null;
  }

  /**
   * Expand all nodes in the tree
   */
  async expandAll() {
    await this.expandAllButton.click();
    await this.page.waitForTimeout(500);
  }

  /**
   * Collapse all nodes in the tree
   */
  async collapseAll() {
    await this.collapseAllButton.click();
    await this.page.waitForTimeout(500);
  }

  /**
   * Toggle a tree node by path
   */
  async toggleNode(path: string) {
    const node = this.treeContainer.locator('.tree-node').filter({ hasText: path });
    const toggle = node.locator('.tree-toggle').first();
    await toggle.click();
    await this.page.waitForTimeout(200);
  }

  /**
   * Check/uncheck a permission checkbox
   */
  async togglePermission(path: string) {
    const node = this.treeContainer.locator('.tree-node').filter({ hasText: path });
    const checkbox = node.locator('input[type="checkbox"]').first();
    await checkbox.click();
  }

  /**
   * Check if a path has permission
   */
  async hasPermission(path: string): Promise<boolean> {
    const node = this.treeContainer.locator('.tree-node').filter({ hasText: path });
    const checkbox = node.locator('input[type="checkbox"]').first();
    return await checkbox.isChecked();
  }

  /**
   * Grant all permissions
   */
  async grantAll() {
    await this.grantAllButton.click();
    await this.page.waitForTimeout(300);
  }

  /**
   * Revoke all permissions
   */
  async revokeAll() {
    await this.revokeAllButton.click();
    await this.page.waitForTimeout(300);
  }

  /**
   * Toggle descendants mode
   */
  async toggleDescendantsMode() {
    await this.toggleDescendantsButton.click();
  }

  /**
   * Check if descendants mode is active
   */
  async isDescendantsModeActive(): Promise<boolean> {
    const classes = await this.toggleDescendantsButton.getAttribute('class');
    return classes?.includes('active') || false;
  }

  /**
   * Save permissions
   */
  async savePermissions() {
    await this.saveButton.click();
    
    // Wait for save to complete
    await this.page.waitForFunction(
      () => {
        const button = document.querySelector('#saveButton');
        return button && !button.textContent?.includes('Saving...');
      },
      { timeout: 10000 }
    );
  }

  /**
   * Get all visible paths in the tree
   */
  async getVisiblePaths(): Promise<string[]> {
    const nodes = await this.treeContainer.locator('.tree-label').all();
    const paths = [];
    
    for (const node of nodes) {
      const text = await node.textContent();
      if (text) paths.push(text.trim());
    }
    
    return paths;
  }

  /**
   * Get user list
   */
  async getUserList(): Promise<Array<{ email: string; name: string }>> {
    const items = await this.userList.locator('.user-item').all();
    const users = [];
    
    for (const item of items) {
      const email = await item.locator('.user-email').textContent();
      const name = await item.locator('.user-name').textContent();
      
      if (email && name) {
        users.push({ email, name });
      }
    }
    
    return users;
  }

  /**
   * Resize the panels using the splitter
   */
  async resizePanels(deltaX: number) {
    const splitterBox = await this.splitter.boundingBox();
    if (!splitterBox) return;
    
    await this.page.mouse.move(splitterBox.x + splitterBox.width / 2, splitterBox.y + splitterBox.height / 2);
    await this.page.mouse.down();
    await this.page.mouse.move(splitterBox.x + splitterBox.width / 2 + deltaX, splitterBox.y + splitterBox.height / 2);
    await this.page.mouse.up();
  }

  /**
   * Navigate back
   */
  async goBack() {
    await this.backButton.click();
  }

  /**
   * Wait for tree to load
   */
  async waitForTreeToLoad() {
    await this.treeContainer.locator('.tree-node').first().waitFor({ state: 'visible' });
  }

  /**
   * Get the state of the save button
   */
  async getSaveButtonState(): Promise<{ enabled: boolean; text: string }> {
    const enabled = await this.saveButton.isEnabled();
    const text = await this.saveButton.textContent() || '';
    return { enabled, text };
  }
}