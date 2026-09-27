package com.chess.auth.dto;

import com.chess.auth.model.User;

import java.time.Instant;
import java.util.UUID;

/** Public-facing profile — deliberately excludes passwordHash and email. */
public record UserProfileResponse(
        UUID id,
        String username,
        Integer eloRating,
        Integer gamesPlayed,
        Integer gamesWon,
        Integer gamesLost,
        Integer gamesDrawn,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEloRating(),
                user.getGamesPlayed(),
                user.getGamesWon(),
                user.getGamesLost(),
                user.getGamesDrawn(),
                user.getCreatedAt()
        );
    }
}
