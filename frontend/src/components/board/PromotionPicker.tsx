import { motion } from "framer-motion";

const OPTIONS: { value: string; label: string; glyphWhite: string; glyphBlack: string }[] = [
  { value: "q", label: "Queen", glyphWhite: "♕", glyphBlack: "♛" },
  { value: "r", label: "Rook", glyphWhite: "♖", glyphBlack: "♜" },
  { value: "b", label: "Bishop", glyphWhite: "♗", glyphBlack: "♝" },
  { value: "n", label: "Knight", glyphWhite: "♘", glyphBlack: "♞" },
];

interface PromotionPickerProps {
  color: "WHITE" | "BLACK";
  onSelect: (piece: string) => void;
  onCancel: () => void;
}

export function PromotionPicker({ color, onSelect, onCancel }: PromotionPickerProps) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="absolute inset-0 z-20 flex items-center justify-center bg-base/80"
      onClick={onCancel}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ type: "spring", stiffness: 400, damping: 28 }}
        onClick={(e) => e.stopPropagation()}
        className="flex gap-2 rounded-sm border border-brass-dim/50 bg-surface p-3"
      >
        {OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => onSelect(option.value)}
            className="flex h-16 w-16 flex-col items-center justify-center gap-1 rounded-sm border border-transparent transition-colors hover:border-brass hover:bg-surface-raised"
          >
            <span className={color === "WHITE" ? "text-ivory" : "text-brass"} style={{ fontSize: "2rem" }}>
              {color === "WHITE" ? option.glyphWhite : option.glyphBlack}
            </span>
            <span className="text-[10px] text-ivory-muted">{option.label}</span>
          </button>
        ))}
      </motion.div>
    </motion.div>
  );
}
