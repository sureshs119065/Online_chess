import { api } from "@/lib/api";
import type { UserProfileResponse, UserStatsResponse } from "@/types/user";

export async function getUserProfile(userId: string): Promise<UserProfileResponse> {
  const { data } = await api.get<UserProfileResponse>(`/users/${userId}`);
  return data;
}

export async function getUserStats(userId: string): Promise<UserStatsResponse> {
  const { data } = await api.get<UserStatsResponse>(`/users/${userId}/stats`);
  return data;
}
