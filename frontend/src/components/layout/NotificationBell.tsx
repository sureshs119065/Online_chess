import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { useNotifications } from "@/hooks/useNotifications";
import {
  parseNotificationPayload,
  type GameEndedPayload,
  type MoveMadePayload,
  type NotificationResponse,
} from "@/types/notification";

function formatRelativeTime(iso: string): string {
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

function describe(notification: NotificationResponse): string {
  if (notification.type === "GAME_ENDED") {
    const payload = parseNotificationPayload<GameEndedPayload>(notification);
    if (!payload) return "A game ended.";
    const reason = payload.resultReason?.toLowerCase().replace("_", " ");
    return `Game ended (${payload.status.replace("_", " ").toLowerCase()}${reason ? `, ${reason}` : ""})`;
  }
  if (notification.type === "MOVE_ALERT") {
    const payload = parseNotificationPayload<MoveMadePayload>(notification);
    return payload ? `Your turn - opponent played ${payload.moveSan}` : "It's your turn.";
  }
  return "You have a new game invite.";
}

function gameIdFor(notification: NotificationResponse): string | null {
  const payload = parseNotificationPayload<{ gameId?: string }>(notification);
  return payload?.gameId ?? null;
}

interface NotificationBellProps {
  userId: string | null;
}

export function NotificationBell({ userId }: NotificationBellProps) {
  const { notifications, unreadCount, markRead } = useNotifications(userId);
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    function handleOutsideClick(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    if (isOpen) document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, [isOpen]);

  function handleSelect(notification: NotificationResponse) {
    if (!notification.isRead) markRead(notification.id);
    const gameId = gameIdFor(notification);
    if (gameId) navigate(`/game/${gameId}`);
    setIsOpen(false);
  }

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ""}`}
        className="relative flex h-9 w-9 items-center justify-center rounded-sm text-ivory-muted transition-colors hover:text-ivory"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
          <path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-brick text-[10px] text-ivory">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={{ duration: 0.15 }}
            className="absolute right-0 top-11 z-40 w-80 rounded-sm border border-brass-dim/40 bg-surface shadow-float"
          >
            <div className="max-h-96 overflow-y-auto">
              {notifications.length === 0 ? (
                <p className="p-4 text-sm text-ivory-muted">Nothing yet.</p>
              ) : (
                notifications.map((notification) => (
                  <button
                    key={notification.id}
                    type="button"
                    onClick={() => handleSelect(notification)}
                    className={`flex w-full flex-col gap-0.5 border-b border-brass-dim/20 px-4 py-3 text-left transition-colors last:border-b-0 hover:bg-surface-raised ${
                      notification.isRead ? "" : "bg-surface-raised/40"
                    }`}
                  >
                    <span className="text-sm text-ivory">{describe(notification)}</span>
                    <span className="text-xs text-ivory-muted">{formatRelativeTime(notification.createdAt)}</span>
                  </button>
                ))
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
