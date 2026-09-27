import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthResponse } from "@/types/auth";

interface AuthState {
  userId: string | null;
  username: string | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  setSession: (auth: AuthResponse) => void;
  clearSession: () => void;
}

// Persisted to localStorage under "chess-auth" so refreshing the page (or
// closing and reopening the tab) doesn't drop the session. Only the token
// pair and identity are persisted - nothing about game state lives here.
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      userId: null,
      username: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      setSession: (auth) =>
        set({
          userId: auth.userId,
          username: auth.username,
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
          isAuthenticated: true,
        }),

      clearSession: () =>
        set({
          userId: null,
          username: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        }),
    }),
    { name: "chess-auth" },
  ),
);
