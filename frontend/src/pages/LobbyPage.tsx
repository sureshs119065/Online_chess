import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { Button } from "@/components/ui/Button";
import { TimeControlPicker } from "@/components/lobby/TimeControlPicker";
import { QueueRadar } from "@/components/lobby/QueueRadar";
import { joinQueue, leaveQueue } from "@/lib/matchmaking";
import { apiErrorMessage } from "@/lib/api";
import { useWebSocket } from "@/hooks/useWebSocket";
import { authenticatedWsUrl } from "@/lib/ws";
import { useAuthStore } from "@/store/authStore";
import type { WsMatchFoundMessage } from "@/types/matchmaking";

type LobbyState = "idle" | "searching" | "matched" | "error";

function formatElapsed(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

export function LobbyPage() {
  const navigate = useNavigate();
  const userId = useAuthStore((state) => state.userId)!;

  const [timeControl, setTimeControl] = useState("blitz_5");
  const [state, setState] = useState<LobbyState>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [matchInfo, setMatchInfo] = useState<WsMatchFoundMessage | null>(null);

  // Only connects once we're actually searching - see useWebSocket's
  // `url: null` short-circuit.
  // matchmaking-service now requires this connection's access token to
  // match {userId} in the path (UserIdHandshakeInterceptor) - a client can
  // no longer subscribe to someone else's match-found notifications just
  // by connecting to their id.
  const { status: wsStatus } = useWebSocket(
    state === "searching" ? authenticatedWsUrl(`/ws/matchmaking/${userId}`) : null,
    {
      onMessage: (data) => {
        const message = data as WsMatchFoundMessage;
        if (message.type === "MATCH_FOUND") {
          setMatchInfo(message);
          setState("matched");
        }
      },
    },
  );

  // Elapsed-time counter while searching.
  useEffect(() => {
    if (state !== "searching") {
      setElapsedSeconds(0);
      return;
    }
    const interval = setInterval(() => setElapsedSeconds((s) => s + 1), 1000);
    return () => clearInterval(interval);
  }, [state]);

  // Once matched, let the person see the "Match found" moment for a beat
  // before jumping into the game - an instant redirect would feel like a
  // glitch rather than a deliberate transition.
  useEffect(() => {
    if (state === "matched" && matchInfo) {
      const timeout = setTimeout(() => {
        navigate(`/game/${matchInfo.gameId}`);
      }, 1100);
      return () => clearTimeout(timeout);
    }
  }, [state, matchInfo, navigate]);

  async function handleFindMatch() {
    setErrorMessage(null);
    try {
      await joinQueue({ userId, timeControl });
      setState("searching");
    } catch (error) {
      // 409 means a queue entry already exists for this user (e.g. a
      // previous tab, or a reload mid-search) - the queue entry is real
      // even though this join call failed, so just start listening
      // instead of surfacing it as an error.
      const status = (error as { response?: { status?: number } }).response?.status;
      if (status === 409) {
        setState("searching");
        return;
      }
      setErrorMessage(apiErrorMessage(error, "Couldn't join the queue. Please try again."));
    }
  }

  async function handleCancel() {
    setState("idle");
    try {
      await leaveQueue(userId);
    } catch {
      // If this fails, the backend queue entry may briefly outlive the
      // UI's idea of "not searching" - the next matchmaker tick will
      // still see it as WAITING. Not worth blocking the UI over.
    }
  }

  return (
    <div className="mx-auto flex max-w-lg flex-col items-center gap-10 px-6 py-16 text-center">
      <AnimatePresence mode="wait">
        {state === "idle" || state === "error" ? (
          <motion.div
            key="picker"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="flex w-full flex-col items-center gap-8"
          >
            <div>
              <h1 className="font-display text-3xl text-ivory">Find a game</h1>
              <p className="mt-2 text-ivory-muted">Pick a time control and we'll match you by rating.</p>
            </div>

            <TimeControlPicker value={timeControl} onChange={setTimeControl} />

            {errorMessage && (
              <p role="alert" className="text-sm text-brick">
                {errorMessage}
              </p>
            )}

            <Button onClick={handleFindMatch} className="w-full max-w-xs">
              Find match
            </Button>
          </motion.div>
        ) : state === "searching" ? (
          <motion.div
            key="searching"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="flex w-full flex-col items-center gap-6"
          >
            <QueueRadar />
            <div>
              <p className="font-display text-2xl text-ivory">Searching for an opponent</p>
              <p className="mt-1 text-ivory-muted">
                {formatElapsed(elapsedSeconds)}
                {wsStatus === "error" && " (reconnecting…)"}
              </p>
            </div>
            <Button variant="ghost" onClick={handleCancel}>
              Cancel
            </Button>
          </motion.div>
        ) : (
          <motion.div
            key="matched"
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.3, ease: "easeOut" }}
            className="flex w-full flex-col items-center gap-4"
          >
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: "spring", stiffness: 400, damping: 15, delay: 0.1 }}
              className="flex h-16 w-16 items-center justify-center rounded-full bg-sage text-2xl text-base"
            >
              ✓
            </motion.div>
            <p className="font-display text-2xl text-ivory">Match found</p>
            <p className="text-ivory-muted">
              Playing as {matchInfo?.yourColor === "WHITE" ? "White" : "Black"}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
