import { useEffect, useState } from "react";
import { getUserProfile } from "@/lib/users";
import type { UserProfileResponse } from "@/types/user";

export function useUserProfile(userId: string | null) {
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!userId) {
      setIsLoading(false);
      return;
    }

    let cancelled = false;
    setIsLoading(true);

    getUserProfile(userId)
      .then((data) => {
        if (!cancelled) setProfile(data);
      })
      .catch(() => {
        // Non-fatal - ProfilePage falls back to stats-only fields.
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [userId]);

  return { profile, isLoading };
}
