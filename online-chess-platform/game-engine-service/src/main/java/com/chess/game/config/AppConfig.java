package com.chess.game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * Plain RestTemplate pointed at a configurable auth-service base URL
     * (see EloUpdateClient + application.yml's services.auth-service.url).
     * Not @LoadBalanced / Eureka-aware — Session 5's matchmaking-service
     * introduces a Feign client for the analogous game-engine call, which
     * is the nicer pattern; this stays simple since it's a single outbound
     * call. Swap to Feign later if you want service-discovery-based lookup
     * instead of a hardcoded/env-var URL.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
