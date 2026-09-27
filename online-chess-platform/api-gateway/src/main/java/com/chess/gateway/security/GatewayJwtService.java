package com.chess.gateway.security;

import com.chess.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The gateway never issues tokens (only auth-service does — Session 3), so
 * this only ever calls validateToken/extractUserId on JwtUtil. jwt.secret
 * MUST be the exact same value configured on auth-service, or every token
 * auth-service issues will fail validation here.
 */
@Service
public class GatewayJwtService {

    private final JwtUtil jwtUtil;

    public GatewayJwtService(@Value("${jwt.secret}") String secret) {
        this.jwtUtil = new JwtUtil(secret);
    }

    public JwtUtil getJwtUtil() {
        return jwtUtil;
    }
}
