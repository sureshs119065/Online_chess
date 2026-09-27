package com.chess.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoveMadeEvent(
        UUID gameId,
        UUID moverId,
        UUID whitePlayerId,
        UUID blackPlayerId,
        String moveSan
) {
    /** The MOVE_ALERT notification goes to whoever didn't just move — it's their turn now. */
    public UUID opponentOfMover() {
        return moverId.equals(whitePlayerId) ? blackPlayerId : whitePlayerId;
    }
}
