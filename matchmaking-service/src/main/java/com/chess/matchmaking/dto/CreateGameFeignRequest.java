package com.chess.matchmaking.dto;

import java.util.UUID;

/**
 * Mirrors game-engine-service's CreateGameRequest field-for-field. Kept as
 * a separate class in this module (rather than shared via common-lib)
 * since it's just a wire-format contract between two services, not real
 * shared business logic — duplicating a small DTO like this is simpler
 * than adding a cross-service compile-time dependency for it.
 */
public record CreateGameFeignRequest(
        UUID whitePlayerId,
        UUID blackPlayerId,
        String timeControl
) {
}
