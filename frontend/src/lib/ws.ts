import { useAuthStore } from "@/store/authStore";

/**
 * In dev, VITE_WS_BASE_URL is unset, so this falls back to the current
 * page's own host - vite.config.ts's dev-server proxy forwards /ws to the
 * gateway on :8080 from there, same trick as lib/api.ts uses for /api.
 *
 * In production, set VITE_WS_BASE_URL to the deployed gateway's wss://
 * URL (see frontend/.env.example).
 */
function wsBase(path: string): string {
  const base = import.meta.env.VITE_WS_BASE_URL;
  if (base) return `${base}${path}`;

  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}${path}`;
}

/**
 * Every WS endpoint in this app (game, chat, matchmaking) now verifies a
 * JWT at handshake time server-side (browsers can't set an Authorization
 * header on a WS upgrade request), so the current access token has to ride
 * along as a "token" query param. Use this instead of the old bare wsUrl
 * for any authenticated socket - see each *HandshakeInterceptor on the
 * backend for what happens if the token is missing or invalid (401).
 */
export function authenticatedWsUrl(path: string): string {
  const token = useAuthStore.getState().accessToken;
  const url = wsBase(path);
  if (!token) return url;
  const separator = url.includes("?") ? "&" : "?";
  return `${url}${separator}token=${encodeURIComponent(token)}`;
}

/** Kept for any still-unauthenticated socket use case; prefer authenticatedWsUrl above. */
export function wsUrl(path: string): string {
  return wsBase(path);
}
