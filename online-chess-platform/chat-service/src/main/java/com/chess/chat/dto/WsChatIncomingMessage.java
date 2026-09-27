package com.chess.chat.dto;

import java.util.UUID;

/**
 * Shape of every message a client sends over /ws/chat/{gameId}:
 * {"senderId": "...", "message": "gg!"}
 *
 * Same known simplification as the game and matchmaking WebSockets:
 * senderId is trusted from the payload rather than verified against a JWT
 * during the handshake. See GameWebSocketHandler's javadoc (Session 4) for
 * what to add before this is internet-facing.
 */
public record WsChatIncomingMessage(
        UUID senderId,
        String message
) {
}
