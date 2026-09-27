package com.chess.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Incoming move payload. Moves are submitted over the WebSocket
 * (/ws/game/{gameId}), not REST — see GameWebSocketHandler — but this DTO
 * is reused there since the JSON shape is identical either way.
 */
public record MoveRequest(

        @NotNull(message = "playerId is required")
        UUID playerId,

        @NotBlank(message = "moveSan is required")
        String moveSan // Standard Algebraic Notation, e.g. "e4", "Nf3", "O-O"
) {
}
