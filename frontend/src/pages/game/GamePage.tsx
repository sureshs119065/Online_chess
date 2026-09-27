import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { ChessBoard } from "@/components/board/ChessBoard";
import { Clock } from "@/components/board/Clock";
import { CapturedPieces } from "@/components/board/CapturedPieces";
import { MoveHistory } from "@/components/board/MoveHistory";
import { GameOverBanner } from "@/components/board/GameOverBanner";
import { ChatPanel } from "@/components/chat/ChatPanel";
import { Button } from "@/components/ui/Button";
import { useGameSocket } from "@/hooks/useGameSocket";
import { useLocalClock } from "@/hooks/useLocalClock";
import { useUserStats } from "@/hooks/useUserStats";
import { getMoveHistory } from "@/lib/games";
import { getUserProfile } from "@/lib/users";
import { useAuthStore } from "@/store/authStore";
import type { MoveResponse } from "@/types/game";
import type { UserProfileResponse } from "@/types/user";

const LOW_TIME_MS = 30_000;

export function GamePage() {
  const { gameId } = useParams<{ gameId: string }>();
  const userId = useAuthStore((state) => state.userId)!;
  const username = useAuthStore((state) => state.username);

  const { gameState, wsErrorMessage, sendMove, sendResign } = useGameSocket(gameId!, userId);
  const { stats: myStats } = useUserStats(userId);

  const [opponent, setOpponent] = useState<UserProfileResponse | null>(null);
  const [moves, setMoves] = useState<MoveResponse[]>([]);
  const [resignArmed, setResignArmed] = useState(false);
  const [sidePanelTab, setSidePanelTab] = useState<"moves" | "chat">("moves");

  const playerColor = gameState
    ? gameState.whitePlayerId === userId
      ? "WHITE"
      : gameState.blackPlayerId === userId
        ? "BLACK"
        : null
    : null;
  const orientation = playerColor ?? "WHITE";
  const isMyTurn = gameState !== null && gameState.sideToMove === playerColor;
  const isGameOver = gameState !== null && gameState.status !== "IN_PROGRESS";

  // Opponent's profile, once we know who that is.
  useEffect(() => {
    if (!gameState) return;
    const opponentId = gameState.whitePlayerId === userId ? gameState.blackPlayerId : gameState.whitePlayerId;
    let cancelled = false;
    getUserProfile(opponentId).then((profile) => {
      if (!cancelled) setOpponent(profile);
    });
    return () => {
      cancelled = true;
    };
  }, [gameState?.whitePlayerId, gameState?.blackPlayerId, userId]);

  // Full move list, re-fetched whenever the position changes - see
  // MoveHistory / lib/games.ts comments for why this is a refetch rather
  // than an incremental append.
  useEffect(() => {
    if (!gameId || !gameState) return;
    getMoveHistory(gameId).then(setMoves);
  }, [gameId, gameState?.fen]);

  // Clocks: see useLocalClock's javadoc-equivalent comment for the honest
  // limitation - these are smooth and correct-feeling but not server
  // enforced today.
  const whiteRunning = gameState?.sideToMove === "WHITE" && !isGameOver;
  const blackRunning = gameState?.sideToMove === "BLACK" && !isGameOver;
  const whiteMs = useLocalClock(gameState?.whiteTimeMs ?? null, whiteRunning);
  const blackMs = useLocalClock(gameState?.blackTimeMs ?? null, blackRunning);

  const myMs = playerColor === "BLACK" ? blackMs : whiteMs;
  const opponentMs = playerColor === "BLACK" ? whiteMs : blackMs;
  const myRunning = playerColor === "BLACK" ? blackRunning : whiteRunning;
  const opponentRunning = playerColor === "BLACK" ? whiteRunning : blackRunning;

  function handleResignClick() {
    if (!resignArmed) {
      setResignArmed(true);
      setTimeout(() => setResignArmed(false), 4000);
      return;
    }
    sendResign();
    setResignArmed(false);
  }

  if (!gameState) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <p className="text-ivory-muted">Connecting to game…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 px-4 py-8 lg:flex-row lg:items-start lg:justify-center">
      <div className="flex flex-col items-center gap-3">
        {/* Opponent bar */}
        <div className="flex w-full max-w-[640px] items-center justify-between">
          <div>
            <p className="text-ivory">
              {opponent?.username ?? "…"}
              {opponent && <span className="ml-2 text-sm text-brass">{opponent.eloRating}</span>}
            </p>
            <CapturedPieces fen={gameState.fen} capturedBy={playerColor === "WHITE" ? "BLACK" : "WHITE"} />
          </div>
          <Clock remainingMs={opponentMs} isRunning={opponentRunning} isLowTime={(opponentMs ?? Infinity) < LOW_TIME_MS} />
        </div>

        {/* Board */}
        <div className="relative w-full max-w-[640px]">
          <ChessBoard
            fen={gameState.fen}
            orientation={orientation}
            playerColor={playerColor}
            isMyTurn={isMyTurn}
            isGameOver={isGameOver}
            isInCheck={gameState.check}
            onMove={sendMove}
          />

          <AnimatePresence>
            {isGameOver && <GameOverBanner game={gameState} playerColor={playerColor} />}
          </AnimatePresence>
        </div>

        {/* My bar */}
        <div className="flex w-full max-w-[640px] items-center justify-between">
          <div>
            <p className="text-ivory">
              {username}
              {myStats && <span className="ml-2 text-sm text-brass">{myStats.eloRating}</span>}
            </p>
            <CapturedPieces fen={gameState.fen} capturedBy={playerColor ?? "WHITE"} />
          </div>
          <Clock remainingMs={myMs} isRunning={myRunning} isLowTime={(myMs ?? Infinity) < LOW_TIME_MS} />
        </div>

        <AnimatePresence>
          {wsErrorMessage && (
            <motion.p
              initial={{ opacity: 0, y: -4 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              role="alert"
              className="text-sm text-brick"
            >
              {wsErrorMessage}
            </motion.p>
          )}
        </AnimatePresence>

        {!isGameOver && (
          <Button variant="ghost" onClick={handleResignClick} className="mt-2">
            {resignArmed ? "Click again to confirm resignation" : "Resign"}
          </Button>
        )}
      </div>

      <div className="w-full lg:w-72">
        <div className="mb-2 flex gap-1 rounded-sm border border-brass-dim/30 bg-surface p-1">
          <button
            type="button"
            onClick={() => setSidePanelTab("moves")}
            className={`flex-1 rounded-sm py-1.5 text-sm transition-colors ${
              sidePanelTab === "moves" ? "bg-brass text-base" : "text-ivory-muted hover:text-ivory"
            }`}
          >
            Moves
          </button>
          <button
            type="button"
            onClick={() => setSidePanelTab("chat")}
            className={`flex-1 rounded-sm py-1.5 text-sm transition-colors ${
              sidePanelTab === "chat" ? "bg-brass text-base" : "text-ivory-muted hover:text-ivory"
            }`}
          >
            Chat
          </button>
        </div>

        {sidePanelTab === "moves" ? (
          <MoveHistory moves={moves} />
        ) : (
          <ChatPanel gameId={gameId!} currentUserId={userId} />
        )}
      </div>
    </div>
  );
}
