package com.chess.chat.dto;

import java.util.UUID;

/**
 * Shape of every message a client sends over /ws/chat/{gameId}:
 * {"message": "gg!"}
 *
 * senderId is deliberately absent from what ChatWebSocketHandler trusts —
 * the sender's real identity comes from the JWT verified at handshake time
 * (GameIdHandshakeInterceptor). The field is kept here only so older
 * clients that still send a senderId don't fail to deserialize; it's
 * ignored if present.
 */
public record WsChatIncomingMessage(
        UUID senderId,
        String message
) {
}
