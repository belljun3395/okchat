import { Page, Locator } from '@playwright/test';

/**
 * Page Object Model for Permissions Management Interface
 */
export class PermissionsPage {
  readonly page: Page;
  readonly chatLink: Locator;
  readonly permissionManagerLink: Locator;
  readonly manageUsersLink: Locator;
  readonly browsePathsLink: Locator;
  readonly totalUsers: Locator;
  readonly totalPaths: Locator;
  readonly usersTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.chatLink = page.locator('a[href="/chat"]');
    this.permissionManagerLink = page.locator('a[href="/admin/permissions/manage"]');
    this.manageUsersLink = page.locator('a[href="/admin/permissions/users"]');
    this.browsePathsLink = page.locator('a[href="/admin/permissions/paths"]');
    this.totalUsers = page.locator('.stat-card:has-text("Total Users") .stat-number');
    this.totalPaths = page.locator('.stat-card:has-text("Document Paths") .stat-number');
    this.usersTable = page.locator('table tbody');
  }

  /**
   * Navigate to permissions page
   */
  async goto() {
    await this.page.goto('/admin/permissions');
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Get total users count
   */
  async getTotalUsersCount() {
    const text = await this.totalUsers.textContent();
    return parseInt(text || '0', 10);
  }

  /**
   * Get total paths count
   */
  async getTotalPathsCount() {
    const text = await this.totalPaths.textContent();
    return parseInt(text || '0', 10);
  }

  /**
   * Navigate to chat
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
   * Navigate to users management
   */
  async goToManageUsers() {
    await this.manageUsersLink.click();
  }

  /**
   * Navigate to paths browser
   */
  async goToBrowsePaths() {
    await this.browsePathsLink.click();
  }

  /**
   * Get all user rows from the table
   */
  async getUserRows() {
    return await this.usersTable.locator('tr').all();
  }

  /**
   * Get user by email
   */
  async getUserByEmail(email: string) {
    return this.usersTable.locator(`tr:has-text("${email}")`);
  }

  /**
   * Click view details for a user
   */
  async viewUserDetails(email: string) {
    const userRow = await this.getUserByEmail(email);
    await userRow.locator('.btn-primary').click();
  }

  /**
   * Check if user is active
   */
  async isUserActive(email: string) {
    const userRow = await this.getUserByEmail(email);
    const statusBadge = await userRow.locator('span').textContent();
    return statusBadge?.includes('Active') || false;
  }

  /**
   * Get all active users
   */
  async getActiveUsers() {
    const rows = await this.getUserRows();
    const activeUsers: string[] = [];
    
    for (const row of rows) {
      const statusBadge = await row.locator('span').textContent();
      if (statusBadge?.includes('Active')) {
        const email = await row.locator('td:first-child').textContent();
        if (email) activeUsers.push(email);
      }
    }
    
    return activeUsers;
  }
}
