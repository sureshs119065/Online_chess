import { useEffect, useRef, useState } from "react";
import { Chess } from "chess.js";

type Cell = { type: string; color: "w" | "b" } | null;

function cellsEqual(a: Cell, b: Cell): boolean {
  if (a === null || b === null) return a === b;
  return a.type === b.type && a.color === b.color;
}

/**
 * Simpler than trying to reuse useAnimatedPieces' identity-matching
 * heuristic for this: rather than figuring out exactly which piece moved
 * where, this just flags every square whose occupant changed between the
 * previous FEN and this one. For a normal move that's the from+to pair;
 * for castling, all four king/rook squares; for en passant, the captured
 * pawn's square too. Visually this reads the same as highlighting "what
 * just changed," which is what a last-move highlight is for anyway.
 */
export function useLastMoveSquares(fen: string): Set<string> {
  const prevBoardRef = useRef<Cell[][] | null>(null);
  const [changedSquares, setChangedSquares] = useState<Set<string>>(new Set());

  useEffect(() => {
    const board = new Chess(fen).board() as unknown as Cell[][];
    const prev = prevBoardRef.current;

    if (prev) {
      const changed = new Set<string>();
      const files = "abcdefgh";
      for (let row = 0; row < 8; row += 1) {
        for (let col = 0; col < 8; col += 1) {
          if (!cellsEqual(prev[row][col], board[row][col])) {
            const rank = 8 - row;
            changed.add(`${files[col]}${rank}`);
          }
        }
      }
      setChangedSquares(changed);
    }

    prevBoardRef.current = board;
  }, [fen]);

  return changedSquares;
}
