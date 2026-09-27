import { useEffect, useState } from "react";
import { getUserStats } from "@/lib/users";
import type { UserStatsResponse } from "@/types/user";

export function useUserStats(userId: string | null) {
  const [stats, setStats] = useState<UserStatsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!userId) {
      setIsLoading(false);
      return;
    }

    let cancelled = false;
    setIsLoading(true);

    getUserStats(userId)
      .then((data) => {
        if (!cancelled) setStats(data);
      })
      .catch(() => {
        // Non-fatal - the header just shows nothing where the rating
        // would go. See apiErrorMessage's callers elsewhere for the
        // pattern used when a failure genuinely needs surfacing to the
        // person instead.
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [userId]);

  return { stats, isLoading };
}
