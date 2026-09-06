import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [tailwindcss(), sveltekit()],
  server: {
    port: 4200,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
        secure: false,
      },
    },
  },
  preview: {
    port: 4173,
  },
});
