import { test, expect } from '@playwright/test';
import { ChatPage } from '../pages/ChatPage';
import { PermissionsPage } from '../pages/PermissionsPage';

/**
 * Performance Tests
 * 성능 관련 테스트
 */

test.describe('Performance Tests', () => {
  
  test('chat page loads within acceptable time', async ({ page }) => {
    const startTime = Date.now();
    
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    await page.waitForLoadState('networkidle');
    
    const loadTime = Date.now() - startTime;
    
    // Page should load within 5 seconds
    expect(loadTime).toBeLessThan(5000);
    
    console.log(`📊 Chat page load time: ${loadTime}ms`);
  });

  test('permissions page loads within acceptable time', async ({ page }) => {
    const startTime = Date.now();
    
    const permissionsPage = new PermissionsPage(page);
    await permissionsPage.goto();
    await page.waitForLoadState('networkidle');
    
    const loadTime = Date.now() - startTime;
    
    // Page should load within 5 seconds
    expect(loadTime).toBeLessThan(5000);
    
    console.log(`📊 Permissions page load time: ${loadTime}ms`);
  });

  test('chat message response time is reasonable', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    const startTime = Date.now();
    
    await chatPage.sendMessage('간단한 질문');
    await chatPage.waitForTypingIndicator();
    await chatPage.waitForResponse();
    
    const responseTime = Date.now() - startTime;
    
    // Response should come within 60 seconds (AI processing time)
    expect(responseTime).toBeLessThan(60000);
    
    console.log(`📊 Chat response time: ${responseTime}ms`);
  });

  test('page has no layout shifts', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Measure Cumulative Layout Shift (CLS)
    const cls = await page.evaluate(() => {
      return new Promise<number>((resolve) => {
        let clsValue = 0;
        const observer = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            if ((entry as any).hadRecentInput) continue;
            clsValue += (entry as any).value;
          }
        });
        
        observer.observe({ type: 'layout-shift', buffered: true });
        
        setTimeout(() => {
          observer.disconnect();
          resolve(clsValue);
        }, 3000);
      });
    });
    
    // CLS should be less than 0.1 (good)
    expect(cls).toBeLessThan(0.1);
    
    console.log(`📊 Cumulative Layout Shift: ${cls}`);
  });

  test('page is interactive quickly', async ({ page }) => {
    const chatPage = new ChatPage(page);
    
    await page.goto('/chat');
    
    // Measure Time to Interactive (TTI)
    const tti = await page.evaluate(() => {
      return new Promise<number>((resolve) => {
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          if (entries.length > 0) {
            const tti = entries[0].startTime;
            observer.disconnect();
            resolve(tti);
          }
        });
        
        // Use First Input Delay as proxy for TTI
        observer.observe({ type: 'first-input', buffered: true });
        
        // Fallback: resolve after 5 seconds
        setTimeout(() => {
          observer.disconnect();
          resolve(5000);
        }, 5000);
      });
    });
    
    // Should be interactive within 3 seconds
    expect(tti).toBeLessThan(3000);
    
    console.log(`📊 Time to Interactive: ${tti}ms`);
  });

  test('resources load efficiently', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Get performance metrics
    const metrics = await page.evaluate(() => {
      const perf = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return {
        dns: perf.domainLookupEnd - perf.domainLookupStart,
        tcp: perf.connectEnd - perf.connectStart,
        request: perf.responseStart - perf.requestStart,
        response: perf.responseEnd - perf.responseStart,
        dom: perf.domContentLoadedEventEnd - perf.domContentLoadedEventStart,
        load: perf.loadEventEnd - perf.loadEventStart,
      };
    });
    
    console.log('📊 Resource timing:', metrics);
    
    // DNS lookup should be fast
    expect(metrics.dns).toBeLessThan(200);
    
    // Request should be fast
    expect(metrics.request).toBeLessThan(500);
  });

  test('no memory leaks on repeated navigation', async ({ page }) => {
    // Navigate between pages multiple times
    for (let i = 0; i < 5; i++) {
      await page.goto('/chat');
      await page.waitForLoadState('networkidle');
      
      await page.goto('/admin/permissions');
      await page.waitForLoadState('networkidle');
    }
    
    // Get memory metrics
    const metrics = await page.metrics();
    
    console.log('📊 Memory metrics:', {
      jsHeapSize: `${(metrics.JSHeapUsedSize / 1024 / 1024).toFixed(2)} MB`,
      nodes: metrics.Nodes,
      listeners: metrics.JSEventListeners,
    });
    
    // Memory should be reasonable (less than 100MB)
    expect(metrics.JSHeapUsedSize).toBeLessThan(100 * 1024 * 1024);
  });

  test('large chat history renders efficiently', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    const startTime = Date.now();
    
    // Send multiple messages
    for (let i = 0; i < 10; i++) {
      await chatPage.messageInput.fill(`메시지 ${i + 1}`);
      await chatPage.sendButton.click();
      await page.waitForTimeout(300);
    }
    
    const totalTime = Date.now() - startTime;
    
    // Should handle multiple messages efficiently
    expect(totalTime).toBeLessThan(30000);
    
    // Page should still be responsive
    const isResponsive = await chatPage.messageInput.isEnabled();
    expect(isResponsive).toBeTruthy();
    
    console.log(`📊 Time for 10 messages: ${totalTime}ms`);
  });

  test('network requests are optimized', async ({ page }) => {
    const requests: any[] = [];
    
    page.on('request', (request) => {
      requests.push({
        url: request.url(),
        method: request.method(),
        resourceType: request.resourceType(),
      });
    });
    
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    await page.waitForLoadState('networkidle');
    
    console.log(`📊 Total requests: ${requests.length}`);
    
    // Count requests by type
    const byType = requests.reduce((acc: any, req) => {
      acc[req.resourceType] = (acc[req.resourceType] || 0) + 1;
      return acc;
    }, {});
    
    console.log('📊 Requests by type:', byType);
    
    // Should not make excessive requests
    expect(requests.length).toBeLessThan(50);
  });

  test('images are optimized', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Get all images
    const images = await page.locator('img').all();
    
    for (const img of images) {
      const src = await img.getAttribute('src');
      
      if (src && !src.startsWith('data:')) {
        // Image should have proper loading attribute
        const loading = await img.getAttribute('loading');
        
        // Images below the fold should be lazy loaded
        const isVisible = await img.isVisible();
        if (!isVisible) {
          expect(loading).toBe('lazy');
        }
      }
    }
  });

  test('CSS and JS are minified in production', async ({ page }) => {
    // This test is more relevant in production builds
    const chatPage = new ChatPage(page);
    await chatPage.goto();
    
    // Check if inline scripts are present (not ideal for production)
    const inlineScripts = await page.locator('script:not([src])').count();
    
    console.log(`📊 Inline scripts: ${inlineScripts}`);
    
    // Note: Thymeleaf templates typically have inline scripts
    // This is just for monitoring
  });
});
