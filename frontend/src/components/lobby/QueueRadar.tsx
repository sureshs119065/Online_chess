import { motion } from "framer-motion";

/**
 * The only place in this app (so far) with a looping, non-user-triggered
 * animation - justified because it communicates genuine ongoing
 * background activity (actively searching the queue), the same way a
 * spinner or progress indicator would. Everywhere else, motion answers a
 * specific action; this is the deliberate exception, not the norm.
 */
export function QueueRadar() {
  return (
    <div className="relative flex h-32 w-32 items-center justify-center">
      {[0, 1, 2].map((ring) => (
        <motion.span
          key={ring}
          className="absolute h-full w-full rounded-full border border-brass"
          initial={{ scale: 0.4, opacity: 0.6 }}
          animate={{ scale: 1.4, opacity: 0 }}
          transition={{
            duration: 2.2,
            repeat: Infinity,
            ease: "easeOut",
            delay: ring * 0.7,
          }}
        />
      ))}
      <div className="h-3 w-3 rounded-full bg-brass" />
    </div>
  );
}
