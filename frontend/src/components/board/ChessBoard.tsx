import { useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Square } from "@/components/board/Square";
import { Piece } from "@/components/board/Piece";
import { PromotionPicker } from "@/components/board/PromotionPicker";
import { useAnimatedPieces } from "@/hooks/useAnimatedPieces";
import { useLastMoveSquares } from "@/hooks/useLastMoveSquares";
import { deriveSan, displayPositionToSquare, getLegalDestinations, getSideToMove } from "@/lib/chess";

interface ChessBoardProps {
  fen: string;
  /** Which side this browser views the board from - flips for the Black player. */
  orientation: "WHITE" | "BLACK";
  /** Which side this browser's player actually plays - null for a spectator. */
  playerColor: "WHITE" | "BLACK" | null;
  isMyTurn: boolean;
  isGameOver: boolean;
  isInCheck: boolean;
  onMove: (san: string) => void;
}

export function ChessBoard({
  fen,
  orientation,
  playerColor,
  isMyTurn,
  isGameOver,
  isInCheck,
  onMove,
}: ChessBoardProps) {
  const pieces = useAnimatedPieces(fen);
  const lastMoveSquares = useLastMoveSquares(fen);

  const [selectedSquare, setSelectedSquare] = useState<string | null>(null);
  const [pendingPromotion, setPendingPromotion] = useState<{ from: string; to: string } | null>(null);

  const legalDestinations = useMemo(
    () => (selectedSquare ? getLegalDestinations(fen, selectedSquare) : []),
    [fen, selectedSquare],
  );
  const legalDestinationSquares = useMemo(
    () => new Set(legalDestinations.map((d) => d.square)),
    [legalDestinations],
  );
  const captureSquares = useMemo(
    () => new Set(legalDestinations.filter((d) => d.isCapture).map((d) => d.square)),
    [legalDestinations],
  );

  // isInCheck means whoever moves next (per the FEN itself) is in check -
  // reading it straight from the FEN avoids any confusion with which
  // browser/player is viewing the board.
  const checkSquare = useMemo(() => {
    if (!isInCheck) return null;
    const sideToMove = getSideToMove(fen);
    return pieces.find((p) => p.type === "k" && p.color === sideToMove)?.square ?? null;
  }, [isInCheck, fen, pieces]);

  const canInteract = playerColor !== null && isMyTurn && !isGameOver && !pendingPromotion;
  const playerColorLetter = playerColor === "WHITE" ? "w" : "b";

  function handleSquareClick(square: string) {
    if (!canInteract) return;

    if (selectedSquare) {
      if (legalDestinationSquares.has(square)) {
        const destination = legalDestinations.find((d) => d.square === square);
        if (destination?.isPromotion) {
          setPendingPromotion({ from: selectedSquare, to: square });
        } else {
          const san = deriveSan(fen, selectedSquare, square);
          if (san) onMove(san);
        }
        setSelectedSquare(null);
        return;
      }

      const clickedOwnPiece = pieces.find((p) => p.square === square && p.color === playerColorLetter);
      setSelectedSquare(clickedOwnPiece ? square : null);
      return;
    }

    const clickedOwnPiece = pieces.find((p) => p.square === square && p.color === playerColorLetter);
    if (clickedOwnPiece) setSelectedSquare(square);
  }

  function handlePromotionSelect(promotionPiece: string) {
    if (!pendingPromotion) return;
    const san = deriveSan(fen, pendingPromotion.from, pendingPromotion.to, promotionPiece);
    if (san) onMove(san);
    setPendingPromotion(null);
  }

  const squares = [];
  for (let row = 0; row < 8; row += 1) {
    for (let col = 0; col < 8; col += 1) {
      const square = displayPositionToSquare(row, col, orientation);
      squares.push(
        <Square
          key={square}
          row={row}
          col={col}
          isLight={(row + col) % 2 === 0}
          isSelected={selectedSquare === square}
          isLegalDestination={legalDestinationSquares.has(square)}
          isCapture={captureSquares.has(square)}
          isLastMove={lastMoveSquares.has(square)}
          isCheck={checkSquare === square}
          onClick={() => handleSquareClick(square)}
        />,
      );
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.94, rotateX: 8 }}
      animate={{ opacity: 1, scale: 1, rotateX: 0 }}
      transition={{ duration: 0.55, ease: "easeOut" }}
      className="relative aspect-square shadow-[0_24px_60px_-20px_rgba(0,0,0,0.8),0_0_60px_-30px_rgba(201,162,75,0.5)] w-full max-w-[640px] overflow-hidden rounded-sm border border-brass-dim/40"
      style={{ containerType: "inline-size" }}
    >
      {squares}

      <AnimatePresence>
        {pieces.map((piece) => (
          <Piece
            key={piece.key}
            piece={piece}
            orientation={orientation}
            isDragOrigin={selectedSquare === piece.square}
          />
        ))}
      </AnimatePresence>

      <AnimatePresence>
        {pendingPromotion && playerColor && (
          <PromotionPicker
            color={playerColor}
            onSelect={handlePromotionSelect}
            onCancel={() => setPendingPromotion(null)}
          />
        )}
      </AnimatePresence>
    </motion.div>
  );
}
