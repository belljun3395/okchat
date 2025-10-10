import { test, expect } from '../../src/fixtures/test-fixtures';
import { logStep, delay } from '../../src/utils/test-helpers';

test.describe('Permission Manager', () => {
  test.beforeEach(async ({ permissionManagerPage }) => {
    await permissionManagerPage.goto();
    await expect(permissionManagerPage.container).toBeVisible();
  });

  test('should load permission manager interface', async ({ permissionManagerPage }) => {
    logStep('Verify permission manager components');
    
    // Check main panels
    await expect(permissionManagerPage.userPanel).toBeVisible();
    await expect(permissionManagerPage.treePanel).toBeVisible();
    
    // Check user panel elements
    await expect(permissionManagerPage.userSearch).toBeVisible();
    await expect(permissionManagerPage.userList).toBeVisible();
    
    // Check tree panel elements
    await expect(permissionManagerPage.selectedUserLabel).toBeVisible();
    await expect(permissionManagerPage.saveButton).toBeVisible();
    await expect(permissionManagerPage.expandAllButton).toBeVisible();
    await expect(permissionManagerPage.collapseAllButton).toBeVisible();
    
    // Check action buttons
    await expect(permissionManagerPage.grantAllButton).toBeVisible();
    await expect(permissionManagerPage.revokeAllButton).toBeVisible();
    await expect(permissionManagerPage.toggleDescendantsButton).toBeVisible();
  });

  test('should search and select user', async ({ permissionManagerPage }) => {
    logStep('Test user search and selection');
    
    // Get initial user list
    const users = await permissionManagerPage.getUserList();
    
    if (users.length > 0) {
      const testUser = users[0];
      
      // Search for user
      await permissionManagerPage.searchUser(testUser.email);
      await delay(500);
      
      // Select user
      await permissionManagerPage.selectUser(testUser.email);
      
      // Verify user is selected
      const selectedUser = await permissionManagerPage.getSelectedUser();
      expect(selectedUser).toBe(testUser.email);
      
      // Tree should be loaded
      await permissionManagerPage.waitForTreeToLoad();
      const paths = await permissionManagerPage.getVisiblePaths();
      expect(paths.length).toBeGreaterThan(0);
    } else {
      test.skip();
    }
  });

  test('should expand and collapse tree nodes', async ({ permissionManagerPage }) => {
    logStep('Test tree expand/collapse functionality');
    
    // Select a user first
    const users = await permissionManagerPage.getUserList();
    if (users.length === 0) {
      test.skip();
      return;
    }
    
    await permissionManagerPage.selectUser(users[0].email);
    await permissionManagerPage.waitForTreeToLoad();
    
    // Test expand all
    await permissionManagerPage.expandAll();
    let paths = await permissionManagerPage.getVisiblePaths();
    const expandedCount = paths.length;
    
    // Test collapse all
    await permissionManagerPage.collapseAll();
    paths = await permissionManagerPage.getVisiblePaths();
    const collapsedCount = paths.length;
    
    // Expanded should show more paths than collapsed
    expect(expandedCount).toBeGreaterThan(collapsedCount);
  });

  test('should toggle permissions', async ({ permissionManagerPage }) => {
    logStep('Test permission toggling');
    
    // Select a user
    const users = await permissionManagerPage.getUserList();
    if (users.length === 0) {
      test.skip();
      return;
    }
    
    await permissionManagerPage.selectUser(users[0].email);
    await permissionManagerPage.waitForTreeToLoad();
    
    // Get visible paths
    const paths = await permissionManagerPage.getVisiblePaths();
    if (paths.length === 0) {
      test.skip();
      return;
    }
    
    const testPath = paths[0];
    
    // Check initial permission state
    const initialState = await permissionManagerPage.hasPermission(testPath);
    
    // Toggle permission
    await permissionManagerPage.togglePermission(testPath);
    
    // Verify state changed
    const newState = await permissionManagerPage.hasPermission(testPath);
    expect(newState).toBe(!initialState);
    
    // Save button should be enabled
    const saveState = await permissionManagerPage.getSaveButtonState();
    expect(saveState.enabled).toBe(true);
  });

  test('should grant and revoke all permissions', async ({ permissionManagerPage }) => {
    logStep('Test grant/revoke all functionality');
    
    // Select a user
    const users = await permissionManagerPage.getUserList();
    if (users.length === 0) {
      test.skip();
      return;
    }
    
    await permissionManagerPage.selectUser(users[0].email);
    await permissionManagerPage.waitForTreeToLoad();
    await permissionManagerPage.expandAll();
    
    // Grant all permissions
    await permissionManagerPage.grantAll();
    
    // Check that permissions are granted
    const paths = await permissionManagerPage.getVisiblePaths();
    for (const path of paths.slice(0, 3)) { // Check first 3 paths
      const hasPermission = await permissionManagerPage.hasPermission(path);
      expect(hasPermission).toBe(true);
    }
    
    // Revoke all permissions
    await permissionManagerPage.revokeAll();
    
    // Check that permissions are revoked
    for (const path of paths.slice(0, 3)) { // Check first 3 paths
      const hasPermission = await permissionManagerPage.hasPermission(path);
      expect(hasPermission).toBe(false);
    }
  });

  test('should toggle descendants mode', async ({ permissionManagerPage }) => {
    logStep('Test descendants mode');
    
    // Check initial state
    let isActive = await permissionManagerPage.isDescendantsModeActive();
    expect(isActive).toBe(false);
    
    // Toggle descendants mode
    await permissionManagerPage.toggleDescendantsMode();
    
    // Check new state
    isActive = await permissionManagerPage.isDescendantsModeActive();
    expect(isActive).toBe(true);
    
    // Toggle again
    await permissionManagerPage.toggleDescendantsMode();
    isActive = await permissionManagerPage.isDescendantsModeActive();
    expect(isActive).toBe(false);
  });

  test('should save permissions', async ({ permissionManagerPage }) => {
    logStep('Test save functionality');
    
    // Select a user
    const users = await permissionManagerPage.getUserList();
    if (users.length === 0) {
      test.skip();
      return;
    }
    
    await permissionManagerPage.selectUser(users[0].email);
    await permissionManagerPage.waitForTreeToLoad();
    
    // Make a change
    const paths = await permissionManagerPage.getVisiblePaths();
    if (paths.length > 0) {
      await permissionManagerPage.togglePermission(paths[0]);
    }
    
    // Save
    await permissionManagerPage.savePermissions();
    
    // Save button should show success state
    const saveState = await permissionManagerPage.getSaveButtonState();
    expect(saveState.text).toContain('Saved');
  });

  test('should handle panel resizing', async ({ permissionManagerPage }) => {
    logStep('Test panel resizing');
    
    // Get initial panel widths
    const userPanelBox = await permissionManagerPage.userPanel.boundingBox();
    const initialWidth = userPanelBox?.width || 0;
    
    // Resize panels
    await permissionManagerPage.resizePanels(100); // Move splitter 100px to the right
    
    // Check new width
    const newUserPanelBox = await permissionManagerPage.userPanel.boundingBox();
    const newWidth = newUserPanelBox?.width || 0;
    
    // Width should have increased
    expect(newWidth).toBeGreaterThan(initialWidth);
  });

  test('should navigate back', async ({ permissionManagerPage, page }) => {
    logStep('Test back navigation');
    
    await permissionManagerPage.goBack();
    
    // Should navigate back to permissions index
    await page.waitForURL('**/admin/permissions');
    expect(page.url()).toContain('/admin/permissions');
  });

  test('should handle empty search results', async ({ permissionManagerPage }) => {
    logStep('Test empty search');
    
    // Search for non-existent user
    await permissionManagerPage.searchUser('nonexistent@example.com');
    await delay(500);
    
    // User list should be empty or filtered
    const users = await permissionManagerPage.getUserList();
    const matchingUsers = users.filter(u => u.email.includes('nonexistent@example.com'));
    expect(matchingUsers).toHaveLength(0);
  });

  test('should maintain state after tree operations', async ({ permissionManagerPage }) => {
    logStep('Test state persistence');
    
    // Select a user
    const users = await permissionManagerPage.getUserList();
    if (users.length === 0) {
      test.skip();
      return;
    }
    
    const testUser = users[0];
    await permissionManagerPage.selectUser(testUser.email);
    await permissionManagerPage.waitForTreeToLoad();
    
    // Make changes
    const paths = await permissionManagerPage.getVisiblePaths();
    if (paths.length > 0) {
      await permissionManagerPage.togglePermission(paths[0]);
      const stateAfterToggle = await permissionManagerPage.hasPermission(paths[0]);
      
      // Expand/collapse operations
      await permissionManagerPage.collapseAll();
      await permissionManagerPage.expandAll();
      
      // State should be maintained
      const stateAfterExpand = await permissionManagerPage.hasPermission(paths[0]);
      expect(stateAfterExpand).toBe(stateAfterToggle);
    }
  });
});