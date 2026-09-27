import { api } from "@/lib/api";
import type { JoinQueueRequest, QueueEntryResponse } from "@/types/matchmaking";

export async function joinQueue(request: JoinQueueRequest): Promise<QueueEntryResponse> {
  const { data } = await api.post<QueueEntryResponse>("/matchmaking/join", request);
  return data;
}

export async function leaveQueue(userId: string): Promise<void> {
  // DELETE with a body - axios needs it passed via `data` in the config
  // object rather than as a second positional argument.
  await api.delete("/matchmaking/leave", { data: { userId } });
}
