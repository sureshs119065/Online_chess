import { Link, Outlet, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/store/authStore";
import { useUserStats } from "@/hooks/useUserStats";
import { NotificationBell } from "@/components/layout/NotificationBell";
import { AmbientBackground } from "@/components/layout/AmbientBackground";

export function AppShell() {
  const { userId, username, clearSession } = useAuthStore();
  const { stats, isLoading } = useUserStats(userId);
  const location = useLocation();
  const rating = isLoading ? "···" : (stats?.eloRating ?? "—");

  return (
    <div className="min-h-screen">
      <AmbientBackground />

      <motion.header
        initial={{ y: -24, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: "spring", stiffness: 220, damping: 24 }}
        className="sticky top-0 z-30 flex items-center justify-between border-b border-brass-dim/25 bg-base/70 px-6 py-3.5 backdrop-blur-md sm:px-10"
      >
        <Link to="/app" className="group flex items-center gap-2 font-display text-xl text-ivory">
          <motion.span
            whileHover={{ rotate: [0, -14, 10, 0], scale: 1.15 }}
            transition={{ duration: 0.5 }}
            className="text-2xl text-brass"
          >
            ♞
          </motion.span>
          Endgame
        </Link>

        <div className="flex items-center gap-5">
          <NotificationBell userId={userId} />

          <Link
            to="/app/profile"
            className="rounded-sm border border-transparent px-3 py-1 text-right leading-tight transition-colors hover:border-brass-dim/50 hover:bg-surface/60"
          >
            <p className="text-sm text-ivory">{username}</p>
            {/* key={rating}: the number slides in whenever the rating changes */}
            <motion.p
              key={String(rating)}
              initial={{ y: 8, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              className="font-display text-sm text-brass"
            >
              {rating}
            </motion.p>
          </Link>
          <button
            onClick={clearSession}
            className="text-sm text-ivory-muted transition-colors hover:text-brick"
          >
            Sign out
          </button>
        </div>
      </motion.header>

      {/* Re-keyed on every route change so each page eases in. */}
      <motion.main
        key={location.pathname}
        initial={{ opacity: 0, y: 14, filter: "blur(4px)" }}
        animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
        transition={{ duration: 0.35, ease: "easeOut" }}
      >
        <Outlet />
      </motion.main>
    </div>
  );
}
