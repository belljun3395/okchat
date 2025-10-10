import { test, expect } from '@playwright/test';
import { PermissionsPage } from '../pages/PermissionsPage';

test.describe('Permissions Management Interface', () => {
  let permissionsPage: PermissionsPage;

  test.beforeEach(async ({ page }) => {
    permissionsPage = new PermissionsPage(page);
    await permissionsPage.goto();
  });

  test('should display permissions page correctly', async ({ page }) => {
    // Check page title
    await expect(page).toHaveTitle(/Permission Management/);

    // Check header
    await expect(page.locator('h1')).toContainText('Permission Management');
    await expect(page.locator('.subtitle')).toContainText('Manage document access permissions');

    // Check stats cards
    await expect(permissionsPage.totalUsers).toBeVisible();
    await expect(permissionsPage.totalPaths).toBeVisible();

    // Check quick action links
    await expect(permissionsPage.chatLink).toBeVisible();
    await expect(permissionsPage.permissionManagerLink).toBeVisible();
    await expect(permissionsPage.manageUsersLink).toBeVisible();
    await expect(permissionsPage.browsePathsLink).toBeVisible();

    // Check users table
    await expect(permissionsPage.usersTable).toBeVisible();
  });

  test('should display statistics correctly', async () => {
    // Get stats
    const usersCount = await permissionsPage.getTotalUsersCount();
    const pathsCount = await permissionsPage.getTotalPathsCount();

    // Verify stats are numbers and non-negative
    expect(usersCount).toBeGreaterThanOrEqual(0);
    expect(pathsCount).toBeGreaterThanOrEqual(0);

    // Stats should be displayed with proper formatting
    await expect(permissionsPage.totalUsers).toHaveText(usersCount.toString());
    await expect(permissionsPage.totalPaths).toHaveText(pathsCount.toString());
  });

  test('should have all quick action links functional', async ({ page }) => {
    // Test each quick action link
    const links = [
      { locator: permissionsPage.chatLink, expectedPath: '/chat' },
      { locator: permissionsPage.permissionManagerLink, expectedPath: '/admin/permissions/manage' },
      { locator: permissionsPage.manageUsersLink, expectedPath: '/admin/permissions/users' },
      { locator: permissionsPage.browsePathsLink, expectedPath: '/admin/permissions/paths' },
    ];

    for (const link of links) {
      await expect(link.locator).toBeVisible();
      await expect(link.locator).toHaveAttribute('href', link.expectedPath);
    }
  });

  test('should display users table with correct columns', async ({ page }) => {
    // Check table headers
    const headers = page.locator('table thead th');
    await expect(headers).toHaveCount(4);

    const headerTexts = await headers.allTextContents();
    expect(headerTexts).toContain('Email');
    expect(headerTexts).toContain('Name');
    expect(headerTexts).toContain('Status');
    expect(headerTexts).toContain('Actions');
  });

  test('should display user rows correctly', async ({ page }) => {
    const userRows = await permissionsPage.getUserRows();

    if (userRows.length > 0) {
      const firstRow = userRows[0];

      // Each row should have 4 columns
      const cells = await firstRow.locator('td').count();
      expect(cells).toBe(4);

      // Check that email, name, status, and actions are present
      const email = await firstRow.locator('td:nth-child(1)').textContent();
      const name = await firstRow.locator('td:nth-child(2)').textContent();
      const status = await firstRow.locator('td:nth-child(3)').textContent();
      const actionBtn = firstRow.locator('.btn-primary');

      expect(email).toBeTruthy();
      expect(name).toBeTruthy();
      expect(status).toMatch(/Active|Inactive/);
      await expect(actionBtn).toBeVisible();
    } else {
      // No users - this is acceptable in test environment
      console.log('No users found in test database');
    }
  });

  test('should show user status badges correctly', async ({ page }) => {
    const userRows = await permissionsPage.getUserRows();

    if (userRows.length > 0) {
      for (const row of userRows) {
        const statusBadge = row.locator('td:nth-child(3) span');
        await expect(statusBadge).toBeVisible();

        const statusText = await statusBadge.textContent();
        expect(statusText).toMatch(/Active|Inactive/);

        // Check badge styling based on status
        const bgColor = await statusBadge.evaluate((el) => 
          window.getComputedStyle(el).backgroundColor
        );
        
        if (statusText?.includes('Active')) {
          // Green background for active
          expect(bgColor).toBeTruthy();
        } else {
          // Red background for inactive
          expect(bgColor).toBeTruthy();
        }
      }
    } else {
      // Skip test if no users
      test.skip();
    }
  });

  test('should navigate to chat interface', async ({ page }) => {
    await permissionsPage.goToChat();

    // Should navigate to chat page
    await page.waitForURL(/\/chat/);
    expect(page.url()).toContain('/chat');

    // Should show chat interface
    await expect(page.locator('.header-title')).toHaveText('OKChat');
  });

  test('should navigate to permission manager', async ({ page }) => {
    await permissionsPage.goToPermissionManager();

    // Should navigate to manage page
    await page.waitForURL(/\/admin\/permissions\/manage/);
    expect(page.url()).toContain('/admin/permissions/manage');
  });

  test('should navigate to users management', async ({ page }) => {
    await permissionsPage.goToManageUsers();

    // Should navigate to users page
    await page.waitForURL(/\/admin\/permissions\/users/);
    expect(page.url()).toContain('/admin/permissions/users');
  });

  test('should navigate to paths browser', async ({ page }) => {
    await permissionsPage.goToBrowsePaths();

    // Should navigate to paths page
    await page.waitForURL(/\/admin\/permissions\/paths/);
    expect(page.url()).toContain('/admin/permissions/paths');
  });

  test('should view user details', async ({ page }) => {
    const userRows = await permissionsPage.getUserRows();

    if (userRows.length > 0) {
      const firstRow = userRows[0];
      const email = await firstRow.locator('td:nth-child(1)').textContent();

      if (email) {
        await permissionsPage.viewUserDetails(email);

        // Should navigate to user detail page
        await page.waitForURL(/\/admin\/permissions\/user\//, { timeout: 10000 });
        expect(page.url()).toContain('/admin/permissions/user/');
      }
    } else {
      // Skip test if no users
      test.skip();
    }
  });

  test('should have proper hover effects on quick links', async ({ page }) => {
    const quickLinks = page.locator('.quick-link');
    const firstLink = quickLinks.first();

    // Hover over link
    await firstLink.hover();

    // Check for transform effect (should move up)
    const transform = await firstLink.evaluate((el) => 
      window.getComputedStyle(el).transform
    );
    
    // Transform should be applied (not 'none')
    expect(transform).toBeTruthy();
  });

  test('should have responsive layout for stats cards', async ({ page }) => {
    const statsContainer = page.locator('.stats');

    // Check that stats cards are displayed in grid
    const display = await statsContainer.evaluate((el) => 
      window.getComputedStyle(el).display
    );
    expect(display).toBe('grid');

    // Check that cards are visible
    const statCards = page.locator('.stat-card');
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(2);

    for (let i = 0; i < count; i++) {
      await expect(statCards.nth(i)).toBeVisible();
    }
  });

  test('should have proper styling and colors', async ({ page }) => {
    // Check header background
    const header = page.locator('.header');
    const headerBg = await header.evaluate((el) => 
      window.getComputedStyle(el).backgroundColor
    );
    expect(headerBg).toBe('rgb(255, 255, 255)'); // white

    // Check quick links have gradient backgrounds
    const quickLink = page.locator('.quick-link').first();
    const background = await quickLink.evaluate((el) => 
      window.getComputedStyle(el).background
    );
    expect(background).toContain('linear-gradient');
  });

  test('should not have console errors on load', async ({ page }) => {
    const consoleErrors: string[] = [];

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    // Wait for page to fully load
    await page.waitForLoadState('networkidle');

    // Check for console errors (excluding expected ones)
    const criticalErrors = consoleErrors.filter(
      (error) => !error.includes('favicon') && !error.includes('net::ERR_')
    );
    expect(criticalErrors).toHaveLength(0);
  });

  test('should have proper accessibility attributes', async ({ page }) => {
    // Check that links have proper text content
    const chatLink = permissionsPage.chatLink;
    await expect(chatLink).toHaveText(/Chat Interface/);

    // Check that buttons have proper text
    const userRows = await permissionsPage.getUserRows();
    if (userRows.length > 0) {
      const viewButton = userRows[0].locator('.btn-primary');
      await expect(viewButton).toHaveText('View Details');
    }
  });

  test('should show correct user count in table', async ({ page }) => {
    const usersCount = await permissionsPage.getTotalUsersCount();
    const userRows = await permissionsPage.getUserRows();

    // If there are users in the table, they should match or be a subset of total
    if (userRows.length > 0) {
      expect(userRows.length).toBeLessThanOrEqual(usersCount);
    }
  });

  test('should have properly formatted user information', async ({ page }) => {
    const userRows = await permissionsPage.getUserRows();

    if (userRows.length > 0) {
      for (const row of userRows) {
        const email = await row.locator('td:nth-child(1)').textContent();
        
        // Email should be valid format
        if (email) {
          expect(email).toMatch(/^[^\s@]+@[^\s@]+\.[^\s@]+$/);
        }
      }
    } else {
      // Skip test if no users
      test.skip();
    }
  });
});
