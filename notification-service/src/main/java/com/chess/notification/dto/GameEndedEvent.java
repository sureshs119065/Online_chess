package com.chess.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Mirrors the JSON body game-engine-service's RabbitMqGameEventPublisher
 * sends on routing key "game.ended". Deliberately a plain duplicated DTO
 * rather than a shared common-lib class — same reasoning as
 * matchmaking-service's CreateGameFeignRequest: it's a wire contract
 * between two services, not real shared logic.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameEndedEvent(
        UUID gameId,
        UUID whitePlayerId,
        UUID blackPlayerId,
        String status,
        String resultReason
) {
}
