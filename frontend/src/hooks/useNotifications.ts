import { useCallback, useEffect, useState } from "react";
import { getNotifications, markNotificationRead } from "@/lib/notifications";
import type { NotificationResponse } from "@/types/notification";

/**
 * notification-service (Session 7, backend) only exposes REST endpoints -
 * no WebSocket push exists for notifications the way it does for moves,
 * matchmaking, and chat. So this polls on an interval instead. 20s keeps
 * the unread badge reasonably fresh without hammering the gateway; there's
 * no technical reason it couldn't be shorter, just a reasonable default.
 */
export function useNotifications(userId: string | null, intervalMs = 20_000) {
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);

  const refresh = useCallback(() => {
    if (!userId) return;
    getNotifications(userId)
      .then(setNotifications)
      .catch(() => {
        // Non-fatal - the bell just doesn't update this cycle, tries again next poll.
      });
  }, [userId]);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, intervalMs);
    return () => clearInterval(interval);
  }, [refresh, intervalMs]);

  async function markRead(id: string) {
    // Optimistic - flip it locally immediately, reconcile with a refresh
    // only if the actual request fails.
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
    try {
      await markNotificationRead(id);
    } catch {
      refresh();
    }
  }

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return { notifications, unreadCount, markRead, refresh };
}
