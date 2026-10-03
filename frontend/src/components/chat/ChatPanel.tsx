import { useEffect, useRef, useState, type FormEvent } from "react";
import { AnimatePresence } from "framer-motion";
import { ChatMessageBubble } from "@/components/chat/ChatMessageBubble";
import { useChatSocket } from "@/hooks/useChatSocket";

interface ChatPanelProps {
  gameId: string;
  currentUserId: string;
}

export function ChatPanel({ gameId, currentUserId }: ChatPanelProps) {
  const { messages, wsStatus, errorMessage, sendMessage } = useChatSocket(gameId, currentUserId);
  const [draft, setDraft] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages.length]);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!draft.trim()) return;
    // Only clear the draft if it really went out - otherwise the message
    // would vanish silently while the chat socket is down.
    if (sendMessage(draft)) setDraft("");
  }

  return (
    <div className="flex h-48 flex-col rounded-sm border border-brass-dim/30 bg-surface sm:h-full">
      <div ref={scrollRef} className="flex flex-1 flex-col gap-2 overflow-y-auto p-3">
        {messages.length === 0 ? (
          <p className="text-sm text-ivory-muted">No messages yet - say hello.</p>
        ) : (
          <AnimatePresence initial={false}>
            {messages.map((message) => (
              <ChatMessageBubble key={message.id} message={message} isMine={message.senderId === currentUserId} />
            ))}
          </AnimatePresence>
        )}
      </div>

      {errorMessage && <p className="px-3 pb-1 text-xs text-brick">{errorMessage}</p>}
      {wsStatus !== "open" && (
        <p className="px-3 pb-1 text-xs text-ivory-muted">
          {wsStatus === "closed" || wsStatus === "error"
            ? "Chat disconnected - reconnecting…"
            : "Connecting to chat…"}
        </p>
      )}

      <form onSubmit={handleSubmit} className="flex gap-2 border-t border-brass-dim/30 p-2">
        <input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          maxLength={500}
          placeholder="Message"
          className="flex-1 rounded-sm border border-brass-dim/40 bg-base px-3 py-2 text-sm text-ivory placeholder:text-ivory-muted/50 focus:border-brass focus:outline-none"
        />
        <button
          type="submit"
          disabled={!draft.trim() || wsStatus !== "open"}
          className="rounded-sm bg-brass px-3 py-2 text-sm font-medium text-base transition-opacity disabled:opacity-40"
        >
          Send
        </button>
      </form>
    </div>
  );
}
