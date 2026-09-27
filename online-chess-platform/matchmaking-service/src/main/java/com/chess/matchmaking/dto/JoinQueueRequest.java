package com.chess.matchmaking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * userId is sent explicitly in the body rather than read from a JWT/gateway
 * header, matching the simplification used across every service so far
 * (see GameController's ResignRequest, for instance) — once api-gateway
 * (Session 8) forwards X-User-Id, you can drop this field and read the
 * header instead without changing the rest of the flow.
 */
public record JoinQueueRequest(

        @NotNull(message = "userId is required")
        UUID userId,

        @NotBlank(message = "timeControl is required")
        String timeControl // e.g. "blitz_5", "rapid_10"
) {
}
