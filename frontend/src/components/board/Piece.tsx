import { motion } from "framer-motion";
import { squareToDisplayPosition } from "@/lib/chess";
import type { AnimatedPiece } from "@/hooks/useAnimatedPieces";

const GLYPHS: Record<string, string> = {
  wk: "♔", wq: "♕", wr: "♖", wb: "♗", wn: "♘", wp: "♙",
  bk: "♚", bq: "♛", br: "♜", bb: "♝", bn: "♞", bp: "♟",
};

interface PieceProps {
  piece: AnimatedPiece;
  orientation: "WHITE" | "BLACK";
  isDragOrigin: boolean;
}

// Matches AnimatedBoardHero's stylization: ivory for White, brass for
// Black - not literal black, since a true-black glyph would have poor
// contrast against this board's dark squares.
export function Piece({ piece, orientation, isDragOrigin }: PieceProps) {
  const { row, col } = squareToDisplayPosition(piece.square, orientation);

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.5 }}
      animate={{
        opacity: isDragOrigin ? 0.35 : 1,
        scale: 1,
        top: `${row * 12.5}%`,
        left: `${col * 12.5}%`,
      }}
      exit={{ opacity: 0, scale: 0.4 }}
      transition={{ type: "spring", stiffness: 500, damping: 32 }}
      className="pointer-events-none absolute flex h-[12.5%] w-[12.5%] select-none items-center justify-center leading-none"
      // cqw = % of the board's width (see containerType on ChessBoard), so
      // glyphs scale with the board instead of overflowing small squares.
      style={{ fontSize: "9cqw" }}
    >
      <span className={piece.color === "w" ? "text-ivory" : "text-brass"}>
        {GLYPHS[`${piece.color}${piece.type}`]}
      </span>
    </motion.div>
  );
}
