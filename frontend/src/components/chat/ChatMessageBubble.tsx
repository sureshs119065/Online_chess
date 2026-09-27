import { motion } from "framer-motion";
import type { ChatMessageResponse } from "@/types/chat";

interface ChatMessageBubbleProps {
  message: ChatMessageResponse;
  isMine: boolean;
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

export function ChatMessageBubble({ message, isMine }: ChatMessageBubbleProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.15 }}
      className={`flex flex-col ${isMine ? "items-end" : "items-start"}`}
    >
      <div
        className={`max-w-[85%] rounded-sm px-3 py-2 text-sm ${
          isMine ? "bg-brass text-base" : "bg-surface-raised text-ivory"
        }`}
      >
        {message.message}
      </div>
      <span className="mt-0.5 text-[10px] text-ivory-muted">{formatTime(message.sentAt)}</span>
    </motion.div>
  );
}
