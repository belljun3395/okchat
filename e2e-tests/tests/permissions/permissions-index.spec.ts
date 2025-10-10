import { test, expect } from '../../src/fixtures/test-fixtures';
import { logStep } from '../../src/utils/test-helpers';

test.describe('Permissions Index Page', () => {
  test.beforeEach(async ({ permissionsPage }) => {
    await permissionsPage.goto();
    await expect(permissionsPage.container).toBeVisible();
  });

  test('should display permissions dashboard', async ({ permissionsPage }) => {
    logStep('Verify dashboard components');
    
    // Check header
    await expect(permissionsPage.header).toBeVisible();
    const headerText = await permissionsPage.header.textContent();
    expect(headerText).toContain('Permission Management');
    
    // Check statistics cards
    await expect(permissionsPage.totalUsersCard).toBeVisible();
    await expect(permissionsPage.totalPathsCard).toBeVisible();
    
    // Verify stats are numbers
    const totalUsers = await permissionsPage.getTotalUsers();
    const totalPaths = await permissionsPage.getTotalPaths();
    
    expect(totalUsers).toBeGreaterThanOrEqual(0);
    expect(totalPaths).toBeGreaterThanOrEqual(0);
  });

  test('should display quick action links', async ({ permissionsPage }) => {
    logStep('Verify quick action links');
    
    // Check all quick links are visible
    await expect(permissionsPage.chatLink).toBeVisible();
    await expect(permissionsPage.permissionManagerLink).toBeVisible();
    await expect(permissionsPage.manageUsersLink).toBeVisible();
    await expect(permissionsPage.browsePathsLink).toBeVisible();
    
    // Verify link texts
    await expect(permissionsPage.chatLink).toContainText('Chat Interface');
    await expect(permissionsPage.permissionManagerLink).toContainText('Permission Manager');
    await expect(permissionsPage.manageUsersLink).toContainText('Manage Users');
    await expect(permissionsPage.browsePathsLink).toContainText('Browse Paths');
  });

  test('should navigate to chat interface', async ({ permissionsPage, page }) => {
    logStep('Test navigation to chat');
    
    await permissionsPage.goToChat();
    await page.waitForURL('**/chat');
    
    expect(page.url()).toContain('/chat');
  });

  test('should navigate to permission manager', async ({ permissionsPage, page }) => {
    logStep('Test navigation to permission manager');
    
    await permissionsPage.goToPermissionManager();
    await page.waitForURL('**/admin/permissions/manage');
    
    expect(page.url()).toContain('/admin/permissions/manage');
  });

  test('should navigate to manage users', async ({ permissionsPage, page }) => {
    logStep('Test navigation to manage users');
    
    await permissionsPage.goToManageUsers();
    await page.waitForURL('**/admin/permissions/users');
    
    expect(page.url()).toContain('/admin/permissions/users');
  });

  test('should navigate to browse paths', async ({ permissionsPage, page }) => {
    logStep('Test navigation to browse paths');
    
    await permissionsPage.goToBrowsePaths();
    await page.waitForURL('**/admin/permissions/paths');
    
    expect(page.url()).toContain('/admin/permissions/paths');
  });

  test('should display recent users table', async ({ permissionsPage }) => {
    logStep('Verify recent users table');
    
    await expect(permissionsPage.recentUsersTable).toBeVisible();
    
    // Check table headers
    const headers = await permissionsPage.recentUsersTable.locator('th').allTextContents();
    expect(headers).toContain('Email');
    expect(headers).toContain('Name');
    expect(headers).toContain('Status');
    expect(headers).toContain('Actions');
    
    // Get users list
    const users = await permissionsPage.getRecentUsers();
    
    // Verify user data structure
    if (users.length > 0) {
      const firstUser = users[0];
      expect(firstUser).toHaveProperty('email');
      expect(firstUser).toHaveProperty('name');
      expect(firstUser).toHaveProperty('status');
      expect(['Active', 'Inactive']).toContain(firstUser.status);
    }
  });

  test('should view user details', async ({ permissionsPage, page }) => {
    logStep('Test view user details');
    
    const users = await permissionsPage.getRecentUsers();
    
    if (users.length > 0) {
      const firstUser = users[0];
      await permissionsPage.viewUserDetails(firstUser.email);
      
      // Should navigate to user detail page
      await page.waitForURL(`**/admin/permissions/user/${encodeURIComponent(firstUser.email)}`);
      expect(page.url()).toContain('/admin/permissions/user/');
    } else {
      test.skip();
    }
  });

  test('should handle empty user list', async ({ permissionsPage, context }) => {
    logStep('Test empty user list');
    
    // Mock empty user response
    await context.route('**/admin/permissions', route => {
      route.fulfill({
        status: 200,
        contentType: 'text/html',
        body: `
          <!DOCTYPE html>
          <html>
          <head><title>Permission Management</title></head>
          <body>
            <div class="container">
              <div class="header">
                <h1>🔐 Permission Management</h1>
              </div>
              <div class="stats">
                <div class="stat-card">
                  <div class="stat-number">0</div>
                  <div class="stat-label">Total Users</div>
                </div>
                <div class="stat-card">
                  <div class="stat-number">0</div>
                  <div class="stat-label">Document Paths</div>
                </div>
              </div>
              <div class="section">
                <table>
                  <thead>
                    <tr>
                      <th>Email</th>
                      <th>Name</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody></tbody>
                </table>
              </div>
            </div>
          </body>
          </html>
        `
      });
    });
    
    await permissionsPage.goto();
    
    const users = await permissionsPage.getRecentUsers();
    expect(users).toHaveLength(0);
    
    const totalUsers = await permissionsPage.getTotalUsers();
    expect(totalUsers).toBe(0);
  });

  test('should have responsive layout', async ({ permissionsPage, page }) => {
    logStep('Test responsive layout');
    
    // Test desktop view
    await page.setViewportSize({ width: 1920, height: 1080 });
    await expect(permissionsPage.container).toBeVisible();
    
    // Test tablet view
    await page.setViewportSize({ width: 768, height: 1024 });
    await expect(permissionsPage.container).toBeVisible();
    
    // Test mobile view
    await page.setViewportSize({ width: 375, height: 667 });
    await expect(permissionsPage.container).toBeVisible();
    
    // Quick links should still be accessible
    await expect(permissionsPage.chatLink).toBeVisible();
  });
});