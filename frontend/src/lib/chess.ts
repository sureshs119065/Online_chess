import { Chess } from "chess.js";

export interface BoardPiece {
  square: string;
  type: string; // 'p' | 'n' | 'b' | 'r' | 'q' | 'k'
  color: "w" | "b";
}

/**
 * All chess-rules work here runs entirely client-side via chess.js, purely
 * to drive the UI (legal-move highlighting, turning a click into a SAN
 * string). The server (game-engine-service's ChessRulesService, wrapping
 * a different library - chesslib) is the sole source of truth for what
 * actually happened - every board render is driven by the FEN the server
 * broadcasts, never by chess.js's own idea of the game. If the two ever
 * disagreed, the server always wins.
 */

export function fenToPieces(fen: string): BoardPiece[] {
  const chess = new Chess(fen);
  const pieces: BoardPiece[] = [];
  for (const row of chess.board()) {
    for (const cell of row) {
      if (cell) pieces.push({ square: cell.square, type: cell.type, color: cell.color });
    }
  }
  return pieces;
}

export interface LegalDestination {
  square: string;
  isCapture: boolean;
  isPromotion: boolean;
}

export function getLegalDestinations(fen: string, fromSquare: string): LegalDestination[] {
  const chess = new Chess(fen);
  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const moves = chess.moves({ square: fromSquare as any, verbose: true }) as any[];
    return moves.map((m) => ({
      square: m.to,
      isCapture: m.flags.includes("c") || m.flags.includes("e"),
      isPromotion: m.flags.includes("p"),
    }));
  } catch {
    return [];
  }
}

/** SAN for a candidate move, or null if illegal in this position. Never mutates shared state. */
export function deriveSan(fen: string, from: string, to: string, promotion?: string): string | null {
  const chess = new Chess(fen);
  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const move = chess.move({ from, to, promotion } as any);
    return move.san;
  } catch {
    return null;
  }
}

export function isPlayersTurn(fen: string, color: "WHITE" | "BLACK"): boolean {
  const chess = new Chess(fen);
  return (chess.turn() === "w") === (color === "WHITE");
}

/** 'w' | 'b' - whoever moves next in this position. */
export function getSideToMove(fen: string): "w" | "b" {
  return new Chess(fen).turn();
}

interface DisplayPosition {
  row: number;
  col: number;
}

/** row 0 = top of the rendered grid, col 0 = left - already accounts for board orientation. */
export function squareToDisplayPosition(square: string, orientation: "WHITE" | "BLACK"): DisplayPosition {
  const file = square.charCodeAt(0) - 97; // 'a' -> 0
  const rank = parseInt(square[1], 10); // 1-8
  const col = orientation === "WHITE" ? file : 7 - file;
  const row = orientation === "WHITE" ? 8 - rank : rank - 1;
  return { row, col };
}

export function displayPositionToSquare(row: number, col: number, orientation: "WHITE" | "BLACK"): string {
  const file = orientation === "WHITE" ? col : 7 - col;
  const rank = orientation === "WHITE" ? 8 - row : row + 1;
  return `${String.fromCharCode(97 + file)}${rank}`;
}

const STARTING_COUNTS: Record<string, number> = { p: 8, n: 2, b: 2, r: 2, q: 1 };

/** Piece-type letters of `color` currently missing from the board - i.e. what the OTHER side has captured. */
export function getMissingPieces(fen: string, color: "w" | "b"): string[] {
  const counts: Record<string, number> = { p: 0, n: 0, b: 0, r: 0, q: 0 };
  for (const piece of fenToPieces(fen)) {
    if (piece.color === color && piece.type in counts) counts[piece.type] += 1;
  }
  const missing: string[] = [];
  for (const [type, starting] of Object.entries(STARTING_COUNTS)) {
    for (let i = 0; i < starting - counts[type]; i += 1) missing.push(type);
  }
  return missing;
}
