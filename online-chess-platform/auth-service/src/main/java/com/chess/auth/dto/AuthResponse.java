package com.chess.auth.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String username,
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public AuthResponse(UUID userId, String username, String accessToken, String refreshToken) {
        this(userId, username, accessToken, refreshToken, "Bearer");
    }
}
