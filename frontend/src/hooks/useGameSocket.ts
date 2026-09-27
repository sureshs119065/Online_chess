import { useCallback, useState } from "react";
import { useWebSocket } from "@/hooks/useWebSocket";
import { authenticatedWsUrl } from "@/lib/ws";
import type { GameStateDto } from "@/types/game";

interface WsErrorMessage {
  type: "ERROR";
  message: string;
}

function isErrorMessage(data: unknown): data is WsErrorMessage {
  return typeof data === "object" && data !== null && (data as { type?: string }).type === "ERROR";
}

export function useGameSocket(gameId: string, playerId: string) {
  const [gameState, setGameState] = useState<GameStateDto | null>(null);
  const [wsErrorMessage, setWsErrorMessage] = useState<string | null>(null);

  // playerId is still sent below for backward-compat logging, but the
  // backend no longer trusts it - GameIdHandshakeInterceptor verifies this
  // connection's identity from the access token in the URL (see
  // authenticatedWsUrl) and that's what game-engine-service actually uses.
  const { status, send } = useWebSocket(authenticatedWsUrl(`/ws/game/${gameId}`), {
    onMessage: (data) => {
      if (isErrorMessage(data)) {
        setWsErrorMessage(data.message);
        return;
      }
      setGameState(data as GameStateDto);
      setWsErrorMessage(null);
    },
  });

  const sendMove = useCallback(
    (moveSan: string) => {
      send({ type: "MOVE", playerId, moveSan });
    },
    [send, playerId],
  );

  const sendResign = useCallback(() => {
    send({ type: "RESIGN", playerId });
  }, [send, playerId]);

  return { gameState, wsStatus: status, wsErrorMessage, sendMove, sendResign };
}
