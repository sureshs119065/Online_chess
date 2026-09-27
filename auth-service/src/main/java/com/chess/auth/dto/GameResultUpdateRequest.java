package com.chess.auth.dto;

import jakarta.validation.constraints.NotNull;

public record GameResultUpdateRequest(

        @NotNull(message = "eloRating is required")
        Integer eloRating,

        @NotNull(message = "result is required")
        GameResult result
) {
    public enum GameResult {
        WON, LOST, DRAWN
    }
}
