import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    open: true,
    proxy: {
      '/api': {
        // R43 REQ-C: 数据链路云端化——默认走云服务器（本地后端未启动时 Web 全功能可用）；
        // npm run dev:local 切回本地（开发调试离线场景）
        target: process.env.VITE_API_TARGET === 'local'
          ? 'http://localhost:8080'
          : 'http://123.56.153.138:8080',
        changeOrigin: true
      }
    }
  }
})