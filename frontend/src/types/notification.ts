// Mirrors notification-service's NotificationResponse (com.chess.notification.dto).

export type NotificationType = "GAME_INVITE" | "MOVE_ALERT" | "GAME_ENDED";

export interface NotificationResponse {
  id: string;
  userId: string;
  type: NotificationType;
  /** Raw JSON text - shape depends on `type`, parse with the matching payload type below. */
  payload: string;
  isRead: boolean;
  createdAt: string;
}

// Mirrors notification-service's GameEndedEvent / MoveMadeEvent / GameInviteEvent -
// the JSON shape game-engine-service actually publishes for each routing key.

export interface GameEndedPayload {
  gameId: string;
  whitePlayerId: string;
  blackPlayerId: string;
  status: string;
  resultReason: string | null;
}

export interface MoveMadePayload {
  gameId: string;
  moverId: string;
  whitePlayerId: string;
  blackPlayerId: string;
  moveSan: string;
}

export interface GameInvitePayload {
  invitedUserId: string;
  fromUserId: string;
  gameId: string;
}

export function parseNotificationPayload<T>(notification: NotificationResponse): T | null {
  try {
    return JSON.parse(notification.payload) as T;
  } catch {
    return null;
  }
}
