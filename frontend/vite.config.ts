import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      // Admin APIs (User management)
      '/admin/users': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },

      '/admin/chat': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      // OAuth2 Endpoints
      '/oauth2': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      }
    }
  }
})
