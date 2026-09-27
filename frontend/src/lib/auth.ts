import { api } from "@/lib/api";
import type { AuthResponse, LoginRequest, SignupRequest } from "@/types/auth";

export async function signup(request: SignupRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>("/auth/signup", request);
  return data;
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>("/auth/login", request);
  return data;
}
