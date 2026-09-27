import { Link, Outlet } from "react-router-dom";
import { useAuthStore } from "@/store/authStore";
import { useUserStats } from "@/hooks/useUserStats";
import { NotificationBell } from "@/components/layout/NotificationBell";

export function AppShell() {
  const { userId, username, clearSession } = useAuthStore();
  const { stats, isLoading } = useUserStats(userId);

  return (
    <div className="min-h-screen bg-base">
      <header className="flex items-center justify-between border-b border-brass-dim/30 px-6 py-4 sm:px-10">
        <Link to="/app" className="font-display text-lg text-ivory">
          Endgame
        </Link>

        <div className="flex items-center gap-4">
          <NotificationBell userId={userId} />

          <Link to="/app/profile" className="text-right leading-tight transition-opacity hover:opacity-80">
            <p className="text-sm text-ivory">{username}</p>
            <p className="font-display text-sm text-brass">
              {isLoading ? "···" : (stats?.eloRating ?? "—")}
            </p>
          </Link>
          <button
            onClick={clearSession}
            className="text-sm text-ivory-muted transition-colors hover:text-brick"
          >
            Sign out
          </button>
        </div>
      </header>

      <main>
        <Outlet />
      </main>
    </div>
  );
}
