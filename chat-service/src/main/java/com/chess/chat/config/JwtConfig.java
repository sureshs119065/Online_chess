package com.chess.chat.config;

import com.chess.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Same JwtUtil auth-service issues tokens with, used here only to
 * validate/extract — never to issue. Backs the /ws/chat handshake's
 * JwtHandshakeInterceptor. jwt.secret MUST match auth-service's value.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(@Value("${jwt.secret}") String secret) {
        return new JwtUtil(secret);
    }
}
