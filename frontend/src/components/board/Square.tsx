import { motion } from "framer-motion";

interface SquareProps {
  row: number;
  col: number;
  isLight: boolean;
  isSelected: boolean;
  isLegalDestination: boolean;
  isCapture: boolean;
  isLastMove: boolean;
  isCheck: boolean;
  onClick: () => void;
}

export function Square({
  row,
  col,
  isLight,
  isSelected,
  isLegalDestination,
  isCapture,
  isLastMove,
  isCheck,
  onClick,
}: SquareProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{ top: `${row * 12.5}%`, left: `${col * 12.5}%` }}
      className={`absolute flex h-[12.5%] w-[12.5%] items-center justify-center transition-colors duration-150 ${
        isLight ? "bg-surface-raised" : "bg-surface"
      } ${isLastMove ? "after:absolute after:inset-0 after:bg-brass/20" : ""}`}
    >
      {isCheck && (
        <span className="absolute inset-0 rounded-full bg-brick/60 blur-md" aria-hidden />
      )}

      {isSelected && <span className="absolute inset-0 border-2 border-brass" aria-hidden />}

      {isLegalDestination && !isCapture && (
        <motion.span
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ duration: 0.15 }}
          className="h-3.5 w-3.5 rounded-full bg-sage/70"
          aria-hidden
        />
      )}

      {isLegalDestination && isCapture && (
        <motion.span
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ duration: 0.15 }}
          className="absolute inset-[10%] rounded-full border-[3px] border-sage/70"
          aria-hidden
        />
      )}
    </button>
  );
}
