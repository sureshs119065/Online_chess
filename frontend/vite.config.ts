import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Dev-server proxy so the frontend can call /api and /ws paths without
// hardcoding http://localhost:8080 everywhere and without CORS headaches
// in local dev. Production (Vercel) instead reads VITE_API_BASE_URL /
// VITE_WS_BASE_URL from env vars - see lib/api.ts and lib/ws.ts.
export default defineConfig({
  plugins: [react()],
  resolve: {
    // tsconfig.json's "@/*": ["src/*"] only affects TypeScript's type
    // checker - Vite's own bundler/dev-server resolver needs this alias
    // separately, or every "@/..." import across the app (there are
    // dozens) fails to resolve at build/dev-server time even though the
    // editor shows no type errors.
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/ws": {
        target: "http://localhost:8080",
        ws: true,
        changeOrigin: true,
      },
    },
  },
});
