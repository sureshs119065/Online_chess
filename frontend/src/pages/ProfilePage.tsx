import { motion } from "framer-motion";
import { useAuthStore } from "@/store/authStore";
import { useUserStats } from "@/hooks/useUserStats";
import { useUserProfile } from "@/hooks/useUserProfile";

function formatMemberSince(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "long" });
}

interface RecordSegmentProps {
  label: string;
  value: number;
  colorClassName: string;
}

function RecordSegment({ label, value, colorClassName }: RecordSegmentProps) {
  return (
    <div className="flex flex-col items-center gap-1">
      <span className={`font-display text-3xl ${colorClassName}`}>{value}</span>
      <span className="text-xs uppercase tracking-wider text-ivory-muted">{label}</span>
    </div>
  );
}

/**
 * Session 5. Built entirely from what auth-service already exposes
 * (UserStatsResponse: elo, W/L/D, win rate, member-since) - there's no
 * per-user match-history endpoint anywhere in the backend yet, so this
 * stays a record summary rather than a game list. Add
 * GET /api/users/{id}/games (or similar) first if that's wanted later.
 */
export function ProfilePage() {
  const { userId, username } = useAuthStore();
  const { stats, isLoading } = useUserStats(userId);
  const { profile } = useUserProfile(userId);

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-8 px-6 py-10 sm:px-10">
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="flex flex-col gap-1"
      >
        <h1 className="font-display text-3xl text-ivory">{username}</h1>
        {profile && (
          <p className="text-sm text-ivory-muted">Playing since {formatMemberSince(profile.createdAt)}</p>
        )}
      </motion.div>

      {isLoading ? (
        <div className="rounded-sm border border-brass-dim/30 bg-surface p-8 text-center text-ivory-muted">
          Loading record…
        </div>
      ) : !stats ? (
        <div className="rounded-sm border border-brass-dim/30 bg-surface p-8 text-center text-ivory-muted">
          Couldn't load your record. Try refreshing.
        </div>
      ) : (
        <>
          <div className="flex flex-col items-center gap-2 rounded-sm border border-brass-dim/40 bg-surface px-8 py-10">
            <span className="text-xs uppercase tracking-wider text-ivory-muted">Rating</span>
            <span className="font-display text-6xl text-brass">{stats.eloRating}</span>
          </div>

          <div className="grid grid-cols-3 gap-4 rounded-sm border border-brass-dim/30 bg-surface px-6 py-8">
            <RecordSegment label="Won" value={stats.gamesWon} colorClassName="text-sage" />
            <RecordSegment label="Drawn" value={stats.gamesDrawn} colorClassName="text-ivory" />
            <RecordSegment label="Lost" value={stats.gamesLost} colorClassName="text-brick" />
          </div>

          <div className="flex items-center justify-between rounded-sm border border-brass-dim/30 bg-surface px-6 py-4">
            <span className="text-sm text-ivory-muted">Games played</span>
            <span className="font-display text-xl text-ivory">{stats.gamesPlayed}</span>
          </div>

          <div className="flex items-center justify-between rounded-sm border border-brass-dim/30 bg-surface px-6 py-4">
            <span className="text-sm text-ivory-muted">Win rate</span>
            <span className="font-display text-xl text-ivory">{Math.round(stats.winRate * 100)}%</span>
          </div>
        </>
      )}
    </div>
  );
}
