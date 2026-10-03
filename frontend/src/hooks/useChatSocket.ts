import { useCallback, useState } from "react";
import { useWebSocket } from "@/hooks/useWebSocket";
import { authenticatedWsUrl } from "@/lib/ws";
import type { ChatMessageResponse } from "@/types/chat";

interface WsErrorMessage {
  type: "ERROR";
  message: string;
}

function isErrorMessage(data: unknown): data is WsErrorMessage {
  return typeof data === "object" && data !== null && (data as { type?: string }).type === "ERROR";
}

/**
 * chat-service sends two different shapes over the same socket: an ARRAY
 * of ChatMessageResponse right after connecting (the last 50 messages,
 * oldest-first - see ChatWebSocketHandler.afterConnectionEstablished on
 * the backend), and a single ChatMessageResponse OBJECT every time
 * afterward as new messages arrive. This hook tells them apart by
 * Array.isArray rather than a "type" discriminant, since the backend
 * doesn't wrap either shape in an envelope.
 */
export function useChatSocket(gameId: string, senderId: string) {
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // chat-service now derives the real sender from the access token in the
  // URL (see authenticatedWsUrl / GameIdHandshakeInterceptor) rather than
  // trusting a senderId in the message payload.
  const { status, send } = useWebSocket(authenticatedWsUrl(`/ws/chat/${gameId}`), {
    onMessage: (data) => {
      if (Array.isArray(data)) {
        setMessages(data as ChatMessageResponse[]);
        return;
      }
      if (isErrorMessage(data)) {
        setErrorMessage(data.message);
        return;
      }
      setMessages((prev) => [...prev, data as ChatMessageResponse]);
      setErrorMessage(null);
    },
  });

  const sendMessage = useCallback(
    (message: string): boolean => {
      if (!message.trim()) return false;
      return send({ senderId, message: message.trim() });
    },
    [send, senderId],
  );

  return { messages, wsStatus: status, errorMessage, sendMessage };
}
