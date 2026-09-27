package com.chess.matchmaking.config;

import com.chess.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Same JwtUtil auth-service issues tokens with, used here only to
     * validate/extract — never to issue. Backs the /ws/matchmaking
     * handshake's JwtHandshakeInterceptor. jwt.secret MUST match
     * auth-service's value.
     */
    @Bean
    public JwtUtil jwtUtil(@Value("${jwt.secret}") String secret) {
        return new JwtUtil(secret);
    }
}
