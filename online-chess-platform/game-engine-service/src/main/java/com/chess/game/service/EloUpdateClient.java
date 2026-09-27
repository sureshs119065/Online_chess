package com.chess.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Calls auth-service's internal, non-gateway-exposed endpoint to persist a
 * player's new ELO rating and win/loss/draw counters after a game ends.
 *
 * This is a service-to-service call that must never be reachable through
 * api-gateway's public routes (file 02's gateway route table only exposes
 * /api/users/{id} and /api/users/{id}/stats, not /api/internal/**, so this
 * stays unreachable from outside as long as the gateway is the only public
 * entry point). As defense in depth it's also gated by a shared secret
 * header — see auth-service's InternalUserController.
 *
 * A failed call here is logged but deliberately non-fatal: the game itself
 * has already ended and been persisted, and we don't want a transient
 * network blip to a service now down leave the game stuck. In a more
 * mature system this would go through an outbox/retry queue instead.
 */
@Component
public class EloUpdateClient {

    private static final Logger log = LoggerFactory.getLogger(EloUpdateClient.class);

    private final RestTemplate restTemplate;
    private final String authServiceBaseUrl;
    private final String internalServiceKey;

    public EloUpdateClient(
            RestTemplate restTemplate,
            @Value("${services.auth-service.url:http://localhost:8081}") String authServiceBaseUrl,
            @Value("${internal.service-key:change-me-internal-key}") String internalServiceKey
    ) {
        this.restTemplate = restTemplate;
        this.authServiceBaseUrl = authServiceBaseUrl;
        this.internalServiceKey = internalServiceKey;
    }

    public void updateRating(UUID userId, int newEloRating, String result) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalServiceKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    Map.of("eloRating", newEloRating, "result", result),
                    headers
            );

            restTemplate.exchange(
                    authServiceBaseUrl + "/api/internal/users/" + userId + "/game-result",
                    HttpMethod.PUT,
                    request,
                    Void.class
            );
        } catch (RestClientException ex) {
            log.error("Failed to update ELO for user {} (result={}, newRating={}): {}",
                    userId, result, newEloRating, ex.getMessage());
        }
    }

    /**
     * Reads a player's current ELO rating from auth-service's public
     * GET /api/users/{id}/stats endpoint (Session 3) before recalculating.
     * Falls back to the default starting rating (1200) if auth-service is
     * unreachable, rather than failing the whole move/game-end flow over a
     * ratings lookup.
     */
    @SuppressWarnings("unchecked")
    public int fetchCurrentRating(UUID userId) {
        try {
            Map<String, Object> stats = restTemplate.getForObject(
                    authServiceBaseUrl + "/api/users/" + userId + "/stats",
                    Map.class
            );
            if (stats != null && stats.get("eloRating") instanceof Number n) {
                return n.intValue();
            }
        } catch (RestClientException ex) {
            log.error("Failed to fetch current rating for user {}: {}", userId, ex.getMessage());
        }
        return 1200;
    }
}
