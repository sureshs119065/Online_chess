import { api } from "@/lib/api";
import type { NotificationResponse } from "@/types/notification";

export async function getNotifications(userId: string): Promise<NotificationResponse[]> {
  const { data } = await api.get<NotificationResponse[]>(`/notifications/${userId}`);
  return data;
}

export async function markNotificationRead(id: string): Promise<NotificationResponse> {
  const { data } = await api.put<NotificationResponse>(`/notifications/${id}/read`);
  return data;
}
