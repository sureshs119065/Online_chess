package com.chess.game.dto;

import com.chess.game.model.Game;
import com.chess.game.model.GameStatus;
import com.chess.game.model.ResultReason;

import java.time.Instant;
import java.util.UUID;

public record GameStateDto(
        UUID gameId,
        UUID whitePlayerId,
        UUID blackPlayerId,
        String fen,
        GameStatus status,
        ResultReason resultReason,
        String timeControl,
        Integer whiteTimeMs,
        Integer blackTimeMs,
        String sideToMove,   // "WHITE" or "BLACK"
        boolean check,
        boolean checkmate,
        boolean stalemate,
        String lastMoveSan,  // null if no moves played yet
        Instant startedAt,
        Instant endedAt
) {
    public static GameStateDto of(Game game, String sideToMove, boolean check,
                                   boolean checkmate, boolean stalemate, String lastMoveSan) {
        return new GameStateDto(
                game.getId(),
                game.getWhitePlayerId(),
                game.getBlackPlayerId(),
                game.getFenCurrent(),
                game.getStatus(),
                game.getResultReason(),
                game.getTimeControl(),
                game.getWhiteTimeMs(),
                game.getBlackTimeMs(),
                sideToMove,
                check,
                checkmate,
                stalemate,
                lastMoveSan,
                game.getStartedAt(),
                game.getEndedAt()
        );
    }
}
