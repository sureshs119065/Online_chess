package com.chess.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Sent by matchmaking-service (Session 5) once it pairs two players. */
public record CreateGameRequest(

        @NotNull(message = "whitePlayerId is required")
        UUID whitePlayerId,

        @NotNull(message = "blackPlayerId is required")
        UUID blackPlayerId,

        @NotBlank(message = "timeControl is required")
        String timeControl // e.g. "blitz_5", "rapid_10" — see ChessRulesService.parseTimeControlMillis
) {
}
