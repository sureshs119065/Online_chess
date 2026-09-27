package com.chess.game.dto;

import com.chess.game.model.Move;

import java.time.Instant;
import java.util.UUID;

public record MoveResponse(
        UUID id,
        Integer moveNumber,
        UUID playerId,
        String moveSan,
        String fenAfter,
        Instant playedAt
) {
    public static MoveResponse from(Move move) {
        return new MoveResponse(
                move.getId(),
                move.getMoveNumber(),
                move.getPlayerId(),
                move.getMoveSan(),
                move.getFenAfter(),
                move.getPlayedAt()
        );
    }
}
