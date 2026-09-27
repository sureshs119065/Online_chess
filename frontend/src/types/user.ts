// Mirrors auth-service's UserProfileResponse / UserStatsResponse
// (com.chess.auth.dto).

export interface UserProfileResponse {
  id: string;
  username: string;
  eloRating: number;
  gamesPlayed: number;
  gamesWon: number;
  gamesLost: number;
  gamesDrawn: number;
  createdAt: string;
}

export interface UserStatsResponse {
  userId: string;
  username: string;
  eloRating: number;
  gamesPlayed: number;
  gamesWon: number;
  gamesLost: number;
  gamesDrawn: number;
  winRate: number;
}
