package com.chess.matchmaking.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeaveQueueRequest(
        @NotNull(message = "userId is required")
        UUID userId
) {
}
