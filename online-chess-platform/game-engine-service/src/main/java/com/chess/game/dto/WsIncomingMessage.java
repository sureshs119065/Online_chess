package com.chess.game.dto;

import java.util.UUID;

/**
 * Shape of every message a client sends over /ws/game/{gameId}:
 * <pre>{@code
 * { "type": "MOVE",   "playerId": "...", "moveSan": "e4" }
 * { "type": "RESIGN", "playerId": "..." }
 * }</pre>
 * moveSan is only required when type is MOVE.
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
