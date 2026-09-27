import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/Button";
import type { GameStateDto } from "@/types/game";

interface GameOverBannerProps {
  game: GameStateDto;
  playerColor: "WHITE" | "BLACK" | null;
}

const REASON_LABELS: Record<string, string> = {
  CHECKMATE: "by checkmate",
  RESIGNATION: "by resignation",
  STALEMATE: "by stalemate",
  DRAW_AGREEMENT: "by agreement",
  INSUFFICIENT_MATERIAL: "by insufficient material",
  TIMEOUT: "on time",
  ABANDONED: "- abandoned",
};

export function GameOverBanner({ game, playerColor }: GameOverBannerProps) {
  const didWin =
    (game.status === "WHITE_WON" && playerColor === "WHITE") ||
    (game.status === "BLACK_WON" && playerColor === "BLACK");
  const isDraw = game.status === "DRAW";

  const headline = isDraw ? "Draw" : didWin ? "You won" : "You lost";
  const reason = game.resultReason ? REASON_LABELS[game.resultReason] ?? "" : "";

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="absolute inset-0 z-30 flex items-center justify-center bg-base/85"
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.9, y: 8 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ type: "spring", stiffness: 300, damping: 24 }}
        className="flex flex-col items-center gap-3 rounded-sm border border-brass-dim/50 bg-surface px-10 py-8 text-center"
      >
        <p className={`font-display text-4xl ${isDraw ? "text-ivory" : didWin ? "text-sage" : "text-brick"}`}>
          {headline}
        </p>
        {reason && <p className="text-ivory-muted">{reason}</p>}
        <Link to="/app" className="mt-4">
          <Button variant="primary">Back to lobby</Button>
        </Link>
      </motion.div>
    </motion.div>
  );
}
