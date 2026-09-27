package com.chess.matchmaking.client;

import com.chess.matchmaking.dto.CreateGameFeignRequest;
import com.chess.matchmaking.dto.GameCreatedResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Points at game-engine-service's base URL, configurable via
 * services.game-engine-service.url (see application.yml — defaults to
 * localhost:8083 for local dev, override with an env var pointing at the
 * deployed URL on Render/Railway). Kept as a plain URL rather than relying
 * on Eureka-based discovery through the "name" attribute, matching the
 * simpler RestTemplate-with-configurable-URL pattern used everywhere else
 * in this project (see game-engine-service's EloUpdateClient) — one less
 * thing to debug if Eureka isn't running or isn't deployed.
 */
@FeignClient(name = "game-engine-service", url = "${services.game-engine-service.url:http://localhost:8083}")
public interface GameEngineClient {

    @PostMapping("/api/games")
    GameCreatedResponse createGame(@RequestBody CreateGameFeignRequest request);
}
