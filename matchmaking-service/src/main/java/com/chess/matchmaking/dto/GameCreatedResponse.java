package com.chess.matchmaking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * game-engine-service's POST /api/games response (GameStateDto) has many
 * more fields than this — @JsonIgnoreProperties(ignoreUnknown = true) lets
 * Feign/Jackson deserialize just the ones matchmaking-service cares about
 * without breaking if game-engine adds fields later.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameCreatedResponse(UUID gameId) {
}
