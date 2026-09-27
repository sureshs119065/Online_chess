package com.chess.matchmaking.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * userId is optional here — when this request comes through api-gateway,
 * the caller's real identity is the X-User-Id header
 * MatchmakingController reads via RequestUserResolver, not this field.
 * Include it only if you're calling matchmaking-service directly
 * (bypassing the gateway) for local testing; if you do include it while a
 * header is also present, it must match the header or the request is
 * rejected.
 */
public record JoinQueueRequest(

        UUID userId,

        @NotBlank(message = "timeControl is required")
        String timeControl // e.g. "blitz_5", "rapid_10"
) {
}
