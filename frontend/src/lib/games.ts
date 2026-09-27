import { api } from "@/lib/api";
import type { GameStateDto, MoveResponse } from "@/types/game";

export async function getGame(gameId: string): Promise<GameStateDto> {
  const { data } = await api.get<GameStateDto>(`/games/${gameId}`);
  return data;
}

export async function getMoveHistory(gameId: string): Promise<MoveResponse[]> {
  const { data } = await api.get<MoveResponse[]>(`/games/${gameId}/moves`);
  return data;
}
