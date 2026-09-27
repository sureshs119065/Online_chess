package com.chess.game.dto;

import java.util.UUID;

/**
 * playerId is optional here — when this request comes through api-gateway,
 * the caller's real identity is the X-User-Id header GameController reads
 * via RequestUserResolver, not this field. Include it only if you're
 * calling game-engine-service directly (bypassing the gateway) for local
 * testing; if you do include it while a header is also present, it must
 * match the header or the request is rejected.
 */
public record ResignRequest(
        UUID playerId
) {
}
