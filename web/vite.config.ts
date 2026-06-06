import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'   // 需要安装 @types/node

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': resolve(__dirname, './src')   // 将 @ 指向 src 目录
        }
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            '/uploads': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            }

        }
    }
})
