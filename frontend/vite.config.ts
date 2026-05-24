import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  root: '.',  // 👈 关键：明确指定根目录
  server: {
    port: 5173,  // 强制指定端口
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})