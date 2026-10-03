import { motion } from "framer-motion";

export interface TimeControlOption {
  value: string;
  label: string;
  sublabel: string;
}

// Values match the "<category>_<minutes>" format GameService.
// parseTimeControlMillis expects on the backend.
const ICONS: Record<string, string> = { bullet_1: "♙", blitz_5: "♘", rapid_10: "♗", classical_30: "♔" };

export const TIME_CONTROLS: TimeControlOption[] = [
  { value: "bullet_1", label: "Bullet", sublabel: "1 min" },
  { value: "blitz_5", label: "Blitz", sublabel: "5 min" },
  { value: "rapid_10", label: "Rapid", sublabel: "10 min" },
  { value: "classical_30", label: "Classical", sublabel: "30 min" },
];

interface TimeControlPickerProps {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export function TimeControlPicker({ value, onChange, disabled }: TimeControlPickerProps) {
  return (
    <div
      role="radiogroup"
      aria-label="Time control"
      className="grid grid-cols-2 gap-2 rounded-sm border border-brass-dim/40 bg-surface p-2 sm:grid-cols-4"
    >
      {TIME_CONTROLS.map((option, index) => {
        const isSelected = option.value === value;
        return (
          <motion.button
            key={option.value}
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 + index * 0.07, type: "spring", stiffness: 380, damping: 28 }}
            whileHover={disabled ? undefined : { y: -3 }}
            whileTap={{ scale: 0.97 }}
            type="button"
            role="radio"
            aria-checked={isSelected}
            disabled={disabled}
            onClick={() => onChange(option.value)}
            className="relative rounded-sm px-3 py-3 text-center transition-colors duration-150 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isSelected && (
              <motion.div
                layoutId="time-control-highlight"
                className="absolute inset-0 rounded-sm bg-brass"
                transition={{ type: "spring", stiffness: 500, damping: 35 }}
              />
            )}
            <span className="relative z-10 block text-xl leading-none">{ICONS[option.value]}</span>
            <span
              className={`relative z-10 mt-1 block font-display text-base ${
                isSelected ? "text-base" : "text-ivory"
              }`}
            >
              {option.label}
            </span>
            <span
              className={`relative z-10 block text-xs ${
                isSelected ? "text-base/70" : "text-ivory-muted"
              }`}
            >
              {option.sublabel}
            </span>
          </motion.button>
        );
      })}
    </div>
  );
}
