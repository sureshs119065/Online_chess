package com.chess.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Shared JWT issue/validate logic so auth-service and api-gateway (and any
 * other service that needs to read the token) agree on exactly one format.
 *
 * Uses the jjwt 0.12.x builder API (Jwts.builder()...signWith(key), not the
 * older setSubject()/parserBuilder() style from 0.11.x — those methods were
 * deprecated/removed).
 *
 * The signing secret is never hardcoded here: it's read from an env var
 * (JWT_SECRET) at construction time, matching the .env.example from the
 * config samples. In local dev, put a long random string in your .env; on
 * Render/Railway, set JWT_SECRET in the service's environment variables —
 * the same code runs unmodified in both places.
 */
public class JwtUtil {

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    /**
     * @param secret     raw secret string (min 32 chars recommended for HS256);
     *                   pass System.getenv("JWT_SECRET") or inject via Spring config
     * @param accessTtl  how long an access token stays valid, e.g. Duration.ofMinutes(15)
     * @param refreshTtl how long a refresh token stays valid, e.g. Duration.ofDays(7)
     */
    public JwtUtil(String secret, Duration accessTtl, Duration refreshTtl) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret must not be blank — set the JWT_SECRET environment variable");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTtl;
        this.refreshTokenTtl = refreshTtl;
    }

    /** Convenience constructor with sensible default TTLs (15 min access / 7 day refresh). */
    public JwtUtil(String secret) {
        this(secret, Duration.ofMinutes(15), Duration.ofDays(7));
    }

    /** Issues a short-lived access token carrying the user's id and username. */
    public String generateToken(UUID userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /** Issues a longer-lived refresh token carrying only the user's id. */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Returns true if the token is well-formed, correctly signed, and not
     * expired. Never throws — callers can use this as a simple guard before
     * trusting the token.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** Extracts the user id (subject) from a token. Throws JwtException if invalid/expired. */
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    /** Extracts the username claim from an access token. Throws JwtException if invalid/expired. */
    public String extractUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
