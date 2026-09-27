// Mirrors game-engine-service's GameStateDto (com.chess.game.dto). Session
// 3 will likely extend this with move-history types; kept minimal here
// since this session only needs enough to prove a created game is real.

export type GameStatus = "IN_PROGRESS" | "WHITE_WON" | "BLACK_WON" | "DRAW" | "ABANDONED";

export interface GameStateDto {
  gameId: string;
  whitePlayerId: string;
  blackPlayerId: string;
  fen: string;
  status: GameStatus;
  resultReason: string | null;
  timeControl: string;
  whiteTimeMs: number;
  blackTimeMs: number;
  sideToMove: "WHITE" | "BLACK";
  check: boolean;
  checkmate: boolean;
  stalemate: boolean;
  lastMoveSan: string | null;
  startedAt: string;
  endedAt: string | null;
}

// Mirrors game-engine-service's MoveResponse.
export interface MoveResponse {
  id: string;
  moveNumber: number;
  playerId: string;
  moveSan: string;
  fenAfter: string;
  playedAt: string;
}
