import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { useAuthStore } from "@/store/authStore";
import type { AuthResponse } from "@/types/auth";

// In dev, VITE_API_BASE_URL is unset and vite.config.ts's proxy forwards
// /api straight to the gateway on :8080 - no CORS setup needed locally.
// In production (Vercel), set VITE_API_BASE_URL to the deployed gateway's
// URL (see api-gateway's CORS_ALLOWED_ORIGIN note on the backend side -
// it needs to allow whatever origin this frontend ends up deployed to).
const baseURL = `${import.meta.env.VITE_API_BASE_URL ?? ""}/api`;

export const api = axios.create({ baseURL });

api.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// Tracks an in-flight refresh so concurrent 401s from several simultaneous
// requests trigger only one /api/auth/refresh call, not one per request.
let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const { refreshToken, setSession, clearSession } = useAuthStore.getState();
  if (!refreshToken) return null;

  try {
    const { data } = await axios.post<AuthResponse>(`${baseURL}/auth/refresh`, {
      refreshToken,
    });
    setSession(data);
    return data.accessToken;
  } catch {
    clearSession();
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;

    const isAuthEndpoint = original?.url?.includes("/auth/");
    if (error.response?.status === 401 && original && !original._retried && !isAuthEndpoint) {
      original._retried = true;

      refreshPromise ??= refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
      const newToken = await refreshPromise;

      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      }
    }

    return Promise.reject(error);
  },
);

/** Extracts a human-readable message from any API error, falling back to a generic one. */
export function apiErrorMessage(error: unknown, fallback = "Something went wrong. Please try again."): string {
  if (axios.isAxiosError(error)) {
    const message = (error.response?.data as { message?: string } | undefined)?.message;
    if (message) return message;
    if (error.code === "ERR_NETWORK") return "Can't reach the server. Is the backend running?";
  }
  return fallback;
}
