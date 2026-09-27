package com.chess.game.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResignRequest(
        @NotNull(message = "playerId is required")
        UUID playerId
) {
}
