import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        // Disable proxy-level buffering for SSE streams
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes, req, res) => {
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              // Disable buffering at every layer
              proxyRes.headers['cache-control'] = 'no-cache, no-transform';
              proxyRes.headers['connection'] = 'keep-alive';
              proxyRes.headers['x-accel-buffering'] = 'no'; // nginx/reverse proxy
              // Remove content-length to force chunked transfer
              delete proxyRes.headers['content-length'];
            }
          });
        },
      },
    },
  },
})

