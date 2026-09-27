package com.chess.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

/**
 * Without this, a browser-based frontend calling the gateway from a
 * different origin (e.g. your eventual Vercel/Render frontend URL) gets
 * blocked by CORS before your JwtAuthFilter even runs. allowed-origin
 * defaults to "*" so local development (any localhost port) just works;
 * set FRONTEND_ORIGIN once you have a real deployed frontend URL and
 * narrow this before going further than a personal demo.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origin:*}") String allowedOrigin
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        if ("*".equals(allowedOrigin)) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(List.of(allowedOrigin));
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
