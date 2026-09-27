package com.chess.auth.dto;

import com.chess.auth.model.User;

import java.util.UUID;

public record UserStatsResponse(
        UUID userId,
        String username,
        Integer eloRating,
        Integer gamesPlayed,
        Integer gamesWon,
        Integer gamesLost,
        Integer gamesDrawn,
        double winRate
) {
    public static UserStatsResponse from(User user) {
        int played = user.getGamesPlayed() == null ? 0 : user.getGamesPlayed();
        int won = user.getGamesWon() == null ? 0 : user.getGamesWon();
        double winRate = played == 0 ? 0.0 : (double) won / played;

        return new UserStatsResponse(
                user.getId(),
                user.getUsername(),
                user.getEloRating(),
                user.getGamesPlayed(),
                user.getGamesWon(),
                user.getGamesLost(),
                user.getGamesDrawn(),
                Math.round(winRate * 1000.0) / 1000.0
        );
    }
}
