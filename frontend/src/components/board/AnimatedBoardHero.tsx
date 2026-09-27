import { motion } from "framer-motion";

// A mid-game position, not the starting rank-and-file lineup - reads as
// "a game in progress" rather than a static diagram. Row 0 = rank 8 (top).
// null = empty square. Unicode chess glyphs, no image assets needed.
const POSITION: (string | null)[][] = [
  [null, null, "♜", null, null, "♜", "♚", null],
  ["♟", null, null, null, null, "♟", "♟", "♟"],
  [null, "♟", null, null, "♝", null, null, null],
  [null, null, null, "♟", null, null, null, null],
  [null, null, "♗", null, "♙", null, null, null],
  [null, null, "♘", null, null, "♘", null, null],
  ["♙", "♙", "♙", null, null, "♙", "♙", "♙"],
  [null, null, null, "♖", null, "♖", "♔", null],
];

const isWhitePiece = (piece: string) => "♔♕♖♗♘♙".includes(piece);

export function AnimatedBoardHero() {
  return (
    <div className="grid aspect-square w-full max-w-md grid-cols-8 overflow-hidden rounded-sm border border-brass-dim/40">
      {POSITION.flatMap((row, rankIndex) =>
        row.map((piece, fileIndex) => {
          const isLightSquare = (rankIndex + fileIndex) % 2 === 0;
          const delay = (rankIndex * 8 + fileIndex) * 0.012;

          return (
            <div
              key={`${rankIndex}-${fileIndex}`}
              className={`flex aspect-square items-center justify-center ${
                isLightSquare ? "bg-surface-raised" : "bg-surface"
              }`}
            >
              {piece && (
                <motion.span
                  initial={{ opacity: 0, scale: 0.4, y: 6 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  transition={{ delay, duration: 0.4, ease: "easeOut" }}
                  className={`select-none text-3xl leading-none sm:text-4xl ${
                    isWhitePiece(piece) ? "text-ivory" : "text-brass"
                  }`}
                >
                  {piece}
                </motion.span>
              )}
            </div>
          );
        }),
      )}
    </div>
  );
}
