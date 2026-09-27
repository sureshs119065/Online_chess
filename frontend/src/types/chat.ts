// Mirrors chat-service's ChatMessageResponse (com.chess.chat.dto).

export interface ChatMessageResponse {
  id: string;
  gameId: string;
  senderId: string;
  message: string;
  sentAt: string;
}
