// Mirrors auth-service's AuthResponse / SignupRequest / LoginRequest DTOs
// (com.chess.auth.dto). Kept as plain duplicated types here rather than
// generated from the backend - same reasoning used throughout the backend
// itself for cross-service DTOs (see matchmaking-service's
// CreateGameFeignRequest comment): it's a wire contract, not shared logic.

export interface AuthResponse {
  userId: string;
  username: string;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
