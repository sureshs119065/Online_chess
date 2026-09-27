package com.chess.matchmaking.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Same pattern as game-engine-service's EloUpdateClient.fetchCurrentRating
 * — reads a player's current ELO from auth-service's public
 * GET /api/users/{id}/stats before putting them in the queue, so matching
 * is based on their real current rating rather than a stale value the
 * client might send. Falls back to the default starting rating (1200) if
 * auth-service is briefly unreachable, so joining the queue doesn't hard
 * fail over a ratings lookup blip.
 */
@Component
public class AuthRatingClient {

    private static final Logger log = LoggerFactory.getLogger(AuthRatingClient.class);

    private final RestTemplate restTemplate;
    private final String authServiceBaseUrl;

    public AuthRatingClient(
            RestTemplate restTemplate,
            @Value("${services.auth-service.url:http://localhost:8081}") String authServiceBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.authServiceBaseUrl = authServiceBaseUrl;
    }

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
