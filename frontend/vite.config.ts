import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const appBase = env.VITE_APP_BASE || (mode === 'production' ? '/mailtrace/' : '/')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:8001'
  const apiProxy = {
    '/api': {
      target: proxyTarget,
      changeOrigin: true,
    },
  }

  return {
    base: appBase,
    plugins: [react()],
    server: {
      host: '127.0.0.1',
      port: 5173,
      proxy: apiProxy,
    },
    preview: {
      host: '0.0.0.0',
      port: Number(env.PORT || 5173),
      proxy: apiProxy,
    },
  }
})
