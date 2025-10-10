import { test as base } from '@playwright/test';
import { ChatPage } from '../pages/ChatPage';
import { PermissionsPage } from '../pages/PermissionsPage';
import { PermissionManagerPage } from '../pages/PermissionManagerPage';

/**
 * Custom test fixtures extending Playwright's base test
 */
type TestFixtures = {
  chatPage: ChatPage;
  permissionsPage: PermissionsPage;
  permissionManagerPage: PermissionManagerPage;
};

/**
 * Extended test object with page objects
 */
export const test = base.extend<TestFixtures>({
  // Chat page fixture
  chatPage: async ({ page }, use) => {
    const chatPage = new ChatPage(page);
    await use(chatPage);
  },

  // Permissions index page fixture
  permissionsPage: async ({ page }, use) => {
    const permissionsPage = new PermissionsPage(page);
    await use(permissionsPage);
  },

  // Permission manager page fixture
  permissionManagerPage: async ({ page }, use) => {
    const permissionManagerPage = new PermissionManagerPage(page);
    await use(permissionManagerPage);
  },
});

export { expect } from '@playwright/test';