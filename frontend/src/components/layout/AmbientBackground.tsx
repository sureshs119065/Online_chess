/**
 * Fixed, non-interactive backdrop: a slowly drifting checkerboard that
 * fades out toward the bottom, plus two blurred brass/sage glows that
 * float independently. Pure CSS (see keyframes in index.css) so it costs
 * no React re-renders; collapses to static under prefers-reduced-motion.
 */
export function AmbientBackground() {
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 overflow-hidden bg-base">
      <div className="board-drift absolute inset-0" />
      <div className="absolute -left-40 top-[-10%] h-[28rem] w-[28rem] animate-[float_18s_ease-in-out_infinite] rounded-full bg-brass/15 blur-[110px]" />
      <div className="absolute -right-32 bottom-[-15%] h-[26rem] w-[26rem] animate-[float_24s_ease-in-out_infinite_reverse] rounded-full bg-sage/10 blur-[110px]" />
    </div>
  );
}
