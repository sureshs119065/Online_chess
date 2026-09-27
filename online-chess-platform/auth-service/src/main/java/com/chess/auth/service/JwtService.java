package com.chess.auth.service;

import com.chess.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Thin Spring-managed wrapper around common-lib's JwtUtil, so JwtUtil itself
 * stays a plain framework-agnostic class (no Spring annotations) that any
 * service — including non-Spring code, in theory — could reuse.
 *
 * TTLs and the secret are configurable via application.yml / env vars rather
 * than hardcoded, so you can tune them per environment without a redeploy of
 * code (just an env var change).
 */
@Service
public class JwtService {

    private final JwtUtil jwtUtil;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-ttl-minutes:15}") long accessTtlMinutes,
            @Value("${jwt.refresh-ttl-days:7}") long refreshTtlDays
    ) {
        this.jwtUtil = new JwtUtil(
                secret,
                Duration.ofMinutes(accessTtlMinutes),
                Duration.ofDays(refreshTtlDays)
        );
    }

    public JwtUtil getJwtUtil() {
        return jwtUtil;
    }
}
