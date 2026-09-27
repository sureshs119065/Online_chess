// Mirrors matchmaking-service's DTOs (com.chess.matchmaking.dto).

export interface JoinQueueRequest {
  userId: string;
  timeControl: string;
}

export interface QueueEntryResponse {
  id: string;
  userId: string;
  eloRating: number;
  timeControl: string;
  status: "WAITING" | "MATCHED" | "CANCELLED";
  queuedAt: string;
}

/** Pushed once over /ws/matchmaking/{userId} when QueueMatcherScheduler pairs two players. */
export interface WsMatchFoundMessage {
  type: "MATCH_FOUND";
  gameId: string;
  opponentId: string;
  yourColor: "WHITE" | "BLACK";
}
