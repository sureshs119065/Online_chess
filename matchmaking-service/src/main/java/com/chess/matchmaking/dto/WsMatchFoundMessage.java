package com.chess.matchmaking.dto;

import java.util.UUID;

public record WsMatchFoundMessage(
        String type,       // always "MATCH_FOUND"
        UUID gameId,
        UUID opponentId,
        String yourColor    // "WHITE" or "BLACK"
) {
    public WsMatchFoundMessage(UUID gameId, UUID opponentId, String yourColor) {
        this("MATCH_FOUND", gameId, opponentId, yourColor);
    }
}
