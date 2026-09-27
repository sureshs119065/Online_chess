import { getMissingPieces } from "@/lib/chess";

const GLYPHS: Record<string, string> = { p: "♟", n: "♞", b: "♝", r: "♜", q: "♛" };
const VALUES: Record<string, number> = { p: 1, n: 3, b: 3, r: 5, q: 9 };

interface CapturedPiecesProps {
  fen: string;
  /** The color whose captures this tray displays - i.e. shows the OPPONENT's missing pieces. */
  capturedBy: "WHITE" | "BLACK";
}

export function CapturedPieces({ fen, capturedBy }: CapturedPiecesProps) {
  const opponentColor = capturedBy === "WHITE" ? "b" : "w";
  const captured = getMissingPieces(fen, opponentColor);
  const materialValue = captured.reduce((sum, type) => sum + (VALUES[type] ?? 0), 0);

  if (captured.length === 0) {
    return <div className="h-6" />;
  }

  return (
    <div className="flex h-6 items-center gap-0.5 text-ivory-muted">
      {captured
        .sort((a, b) => VALUES[b] - VALUES[a])
        .map((type, index) => (
          <span key={index} className="text-lg leading-none">
            {GLYPHS[type]}
          </span>
        ))}
      {materialValue > 0 && <span className="ml-1 text-xs">+{materialValue}</span>}
    </div>
  );
}
