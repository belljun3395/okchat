import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Page Object for the Permissions Index page
 */
export class PermissionsPage extends BasePage {
  // Locators
  readonly container: Locator;
  readonly header: Locator;
  readonly totalUsersCard: Locator;
  readonly totalPathsCard: Locator;
  readonly chatLink: Locator;
  readonly permissionManagerLink: Locator;
  readonly manageUsersLink: Locator;
  readonly browsePathsLink: Locator;
  readonly recentUsersTable: Locator;

  constructor(page: Page) {
    super(page);

    // Initialize locators
    this.container = page.locator('.container');
    this.header = page.locator('.header');
    this.totalUsersCard = page.locator('.stat-card').filter({ hasText: 'Total Users' });
    this.totalPathsCard = page.locator('.stat-card').filter({ hasText: 'Document Paths' });
    
    // Quick action links
    this.chatLink = page.locator('a[href="/chat"]');
    this.permissionManagerLink = page.locator('a[href="/admin/permissions/manage"]');
    this.manageUsersLink = page.locator('a[href="/admin/permissions/users"]');
    this.browsePathsLink = page.locator('a[href="/admin/permissions/paths"]');
    
    this.recentUsersTable = page.locator('table').filter({ has: page.locator('th:has-text("Email")') });
  }

  /**
   * Navigate to permissions page
   */
  async goto() {
    await super.goto('/admin/permissions');
  }

  /**
   * Check if the page is loaded
   */
  async isLoaded(): Promise<boolean> {
    try {
      await this.waitForElement(this.container);
      await this.waitForElement(this.header);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get total users count
   */
  async getTotalUsers(): Promise<number> {
    const text = await this.totalUsersCard.locator('.stat-number').textContent();
    return parseInt(text || '0');
  }

  /**
   * Get total paths count
   */
  async getTotalPaths(): Promise<number> {
    const text = await this.totalPathsCard.locator('.stat-number').textContent();
    return parseInt(text || '0');
  }

  /**
   * Navigate to chat interface
   */
  async goToChat() {
    await this.chatLink.click();
  }

  /**
   * Navigate to permission manager
   */
  async goToPermissionManager() {
    await this.permissionManagerLink.click();
  }

  /**
   * Navigate to manage users
   */
  async goToManageUsers() {
    await this.manageUsersLink.click();
  }

  /**
   * Navigate to browse paths
   */
  async goToBrowsePaths() {
    await this.browsePathsLink.click();
  }

  /**
   * Get recent users from table
   */
  async getRecentUsers(): Promise<Array<{
    email: string;
    name: string;
    status: 'Active' | 'Inactive';
  }>> {
    const rows = await this.recentUsersTable.locator('tbody tr').all();
    const users = [];

    for (const row of rows) {
      const email = await row.locator('td').nth(0).textContent();
      const name = await row.locator('td').nth(1).textContent();
      const statusElement = await row.locator('td').nth(2).locator('span').textContent();
      
      users.push({
        email: email || '',
        name: name || '',
        status: statusElement === 'Active' ? 'Active' : 'Inactive'
      });
    }

    return users;
  }

  /**
   * Click view details for a specific user
   */
  async viewUserDetails(email: string) {
    const row = this.recentUsersTable.locator('tbody tr').filter({ hasText: email });
    await row.locator('a:has-text("View Details")').click();
  }

  /**
   * Check if a user exists in the recent users table
   */
  async userExists(email: string): Promise<boolean> {
    const users = await this.getRecentUsers();
    return users.some(user => user.email === email);
  }
}