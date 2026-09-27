import { formatClock } from "@/hooks/useLocalClock";

interface ClockProps {
  remainingMs: number | null;
  isRunning: boolean;
  isLowTime: boolean;
}

export function Clock({ remainingMs, isRunning, isLowTime }: ClockProps) {
  return (
    <div
      className={`rounded-sm border px-3 py-1.5 font-display text-xl tabular-nums transition-colors ${
        isRunning ? "border-brass bg-surface-raised text-brass" : "border-brass-dim/30 text-ivory-muted"
      } ${isLowTime && isRunning ? "text-brick" : ""}`}
    >
      {formatClock(remainingMs)}
    </div>
  );
}
