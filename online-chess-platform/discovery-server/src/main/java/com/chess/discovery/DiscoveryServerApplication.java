package com.chess.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Standalone Eureka service registry.
 *
 * Local dev: run this first, every other service registers with it at
 * http://localhost:8761.
 *
 * FREE-TIER DEPLOYMENT NOTE:
 * Running Eureka as its own always-on free web service works fine on Render,
 * but free instances spin down when idle and cold-start on the next request —
 * that's fatal for a service registry every other service depends on at
 * startup. Two practical options once you leave localhost:
 *   1. Keep using Eureka, but deploy discovery-server on the smallest paid
 *      tier (or a host with no idle spin-down) so it's always reachable.
 *   2. Skip Eureka in production entirely: hardcode each service's base URL
 *      as an env var (e.g. GAME_ENGINE_URL=https://game-engine.onrender.com)
 *      and remove the eureka-client dependency from services when you deploy
 *      them. This is the simpler, cheaper path for a portfolio project and
 *      is what I'd recommend once you're past local dev.
 * Keep Eureka for local dev either way — it's genuinely useful there and
 * costs nothing running on your own machine.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
