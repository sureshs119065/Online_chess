import { useEffect, useRef, useState } from "react";

export type WsStatus = "idle" | "connecting" | "open" | "closed" | "error";

interface UseWebSocketOptions {
  /** Called with each parsed JSON message. */
  onMessage: (data: unknown) => void;
  /** Max automatic reconnect attempts before giving up. Default 3. */
  maxRetries?: number;
}

/**
 * Generic WebSocket lifecycle hook: connects when `url` is non-null,
 * cleans up on unmount or when `url` changes, and retries a dropped
 * connection a few times with a short fixed delay before giving up.
 *
 * This is the one hook every WS feature in the app builds on -
 * matchmaking here in Session 2, then the live game board and chat panel
 * in Sessions 3-4 - so the reconnect/cleanup logic only needs solving
 * once. Pass `url: null` to stay disconnected (e.g. before the person has
 * clicked "Find match").
 */
export function useWebSocket(url: string | null, { onMessage, maxRetries = 3 }: UseWebSocketOptions) {
  const [status, setStatus] = useState<WsStatus>("idle");
  const socketRef = useRef<WebSocket | null>(null);
  const retriesRef = useRef(0);
  const onMessageRef = useRef(onMessage);
  onMessageRef.current = onMessage;

  useEffect(() => {
    if (!url) {
      setStatus("idle");
      return;
    }

    let cancelled = false;
    retriesRef.current = 0;

    function connect(targetUrl: string) {
      setStatus("connecting");
      const socket = new WebSocket(targetUrl);
      socketRef.current = socket;

      socket.onopen = () => {
        if (cancelled) return;
        retriesRef.current = 0;
        setStatus("open");
      };

      socket.onmessage = (event) => {
        if (cancelled) return;
        try {
          onMessageRef.current(JSON.parse(event.data));
        } catch {
          // Non-JSON frame - ignore rather than crash the connection.
        }
      };

      socket.onerror = () => {
        if (cancelled) return;
        setStatus("error");
      };

      socket.onclose = () => {
        if (cancelled) return;
        setStatus("closed");

        if (retriesRef.current < maxRetries) {
          retriesRef.current += 1;
          setTimeout(() => {
            if (!cancelled) connect(targetUrl);
          }, 1500);
        }
      };
    }

    connect(url);

    return () => {
      cancelled = true;
      socketRef.current?.close();
      socketRef.current = null;
    };
  }, [url, maxRetries]);

  function send(data: unknown) {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify(data));
    }
  }

  return { status, send };
}
