import { useEffect, useRef, useState } from "react";

/**
 * KNOWN GAP: game-engine-service's Game entity stores whiteTimeMs /
 * blackTimeMs, but GameService never actually decrements them when a
 * move is applied - the values broadcast in every GameStateDto are just
 * whatever the game started with. Resyncing this clock to the server's
 * value on every broadcast would make it visibly jump back to full time
 * after every move, which is worse than not having a clock at all.
 *
 * So this clock only reads the server's value ONCE, on the first
 * broadcast, as the starting time - after that it counts down locally in
 * real time based on whose turn it is, entirely independent of anything
 * the server sends. It's a real, smooth, correctly-ticking clock from
 * the player's point of view, but it is NOT enforced anywhere: nothing
 * currently makes a player lose on time, and reloading the page resets
 * it to full time again (since the "real" value never left the initial
 * server state). Closing this gap for real needs game-engine-service to
 * track elapsed time server-side and broadcast the true remaining time -
 * a backend change, out of scope for this frontend-only session.
 */
export function useLocalClock(initialMs: number | null, isRunning: boolean): number | null {
  const [remainingMs, setRemainingMs] = useState<number | null>(initialMs);
  const hasInitialized = useRef(false);

  // Capture the starting value exactly once.
  useEffect(() => {
    if (!hasInitialized.current && initialMs != null) {
      setRemainingMs(initialMs);
      hasInitialized.current = true;
    }
  }, [initialMs]);

  useEffect(() => {
    if (!isRunning || remainingMs == null) return;

    const interval = setInterval(() => {
      setRemainingMs((current) => (current == null ? current : Math.max(0, current - 250)));
    }, 250);

    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isRunning]);

  return remainingMs;
}

export function formatClock(ms: number | null): string {
  if (ms == null) return "--:--";
  const totalSeconds = Math.ceil(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}
