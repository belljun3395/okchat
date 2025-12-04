import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Only proxy /admin API requests, not frontend routes
      '/admin/users': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/admin/chat': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
