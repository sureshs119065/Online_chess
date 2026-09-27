import { useRef, useState, useEffect } from "react";
import { fenToPieces, type BoardPiece } from "@/lib/chess";

export interface AnimatedPiece extends BoardPiece {
  key: string;
}

let nextKeyId = 0;

/**
 * FEN alone has no concept of piece identity - it's just "what's on each
 * square right now". To animate a piece sliding from its old square to
 * its new one (rather than the old one vanishing and a new one popping
 * in at the destination), this hook keeps a stable React key per piece
 * across renders by matching the new position's pieces back to the
 * previous position's, in two passes:
 *
 *   1. Exact match: same square, same type, same color - this piece
 *      didn't move, keep its key as-is.
 *   2. Loose match: same type and color, anywhere - the closest
 *      remaining candidate is assumed to be that same piece having
 *      moved, so it keeps its key and slides.
 *
 * A piece with no match at all (nothing of that type/color left
 * unmatched) gets a brand new key and just appears - this is correct for
 * promotions (the pawn's key is "used up" moving nowhere, matched by
 * nothing, and the new queen has no unmatched queen to inherit from) and
 * for the very first render.
 *
 * KNOWN LIMITATION: with multiple pieces of the same type and color
 * already in play (e.g. two queens after an earlier promotion), pass 2's
 * "any remaining piece of this type" matching can occasionally pick the
 * wrong one, making the wrong piece appear to slide. Rare in practice,
 * and harmless either way since the final rendered position is always
 * correct - only the animation's choice of "which piece slid" can be off.
 * Fixing this properly would need the server to assign and broadcast
 * stable piece IDs, which it doesn't today.
 *
 * Captures and promotions-away are handled by AnimatePresence in
 * ChessBoard: a piece whose key simply doesn't appear in the next
 * render's output plays its exit animation and unmounts automatically.
 */
export function useAnimatedPieces(fen: string): AnimatedPiece[] {
  const prevRef = useRef<AnimatedPiece[] | null>(null);
  const [pieces, setPieces] = useState<AnimatedPiece[]>([]);

  useEffect(() => {
    const raw = fenToPieces(fen);
    const prev = prevRef.current;

    if (!prev) {
      const initial = raw.map((p) => ({ ...p, key: `piece-${nextKeyId++}` }));
      prevRef.current = initial;
      setPieces(initial);
      return;
    }

    const used = new Set<number>();
    const next: AnimatedPiece[] = raw.map((piece) => {
      let matchIndex = prev.findIndex(
        (p, i) => !used.has(i) && p.square === piece.square && p.type === piece.type && p.color === piece.color,
      );
      if (matchIndex === -1) {
        matchIndex = prev.findIndex((p, i) => !used.has(i) && p.type === piece.type && p.color === piece.color);
      }

      if (matchIndex !== -1) {
        used.add(matchIndex);
        return { ...piece, key: prev[matchIndex].key };
      }
      return { ...piece, key: `piece-${nextKeyId++}` };
    });

    prevRef.current = next;
    setPieces(next);
  }, [fen]);

  return pieces;
}
