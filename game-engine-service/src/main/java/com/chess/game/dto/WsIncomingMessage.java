package com.chess.game.dto;

import java.util.UUID;

/**
 * Shape of every message a client sends over /ws/game/{gameId}:
 * <pre>{@code
 * { "type": "MOVE",   "playerId": "...", "moveSan": "e4" }
 * { "type": "RESIGN", "playerId": "..." }
 * }</pre>
 * moveSan is only required when type is MOVE.
 *
 * playerId is deliberately IGNORED by GameWebSocketHandler — kept only so
 * older clients that still send it don't fail to deserialize. The caller's
 * real identity comes from the JWT verified at handshake time
 * (GameIdHandshakeInterceptor), never from this field.
 */
public record WsIncomingMessage(
        WsMessageType type,
        UUID playerId,
        String moveSan
) {
    public enum WsMessageType {
        MOVE,
        RESIGN
    }
}
