import { api } from "@/lib/api";
import type { ChatMessageResponse } from "@/types/chat";

export async function getChatHistory(gameId: string): Promise<ChatMessageResponse[]> {
  const { data } = await api.get<ChatMessageResponse[]>(`/games/${gameId}/chat`);
  return data;
}
