package com.chess.matchmaking.dto;

import java.util.UUID;

/**
 * userId is optional here — see JoinQueueRequest's javadoc for why: the
 * X-User-Id header (via RequestUserResolver) is what MatchmakingController
 * actually trusts once this goes through api-gateway.
 */
public record LeaveQueueRequest(
        UUID userId
) {
}
