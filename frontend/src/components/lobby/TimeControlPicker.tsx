import { motion } from "framer-motion";

export interface TimeControlOption {
  value: string;
  label: string;
  sublabel: string;
}

// Values match the "<category>_<minutes>" format GameService.
// parseTimeControlMillis expects on the backend.
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
      {TIME_CONTROLS.map((option) => {
        const isSelected = option.value === value;
        return (
          <button
            key={option.value}
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
            <span
              className={`relative z-10 block font-display text-base ${
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
          </button>
        );
      })}
    </div>
  );
}
