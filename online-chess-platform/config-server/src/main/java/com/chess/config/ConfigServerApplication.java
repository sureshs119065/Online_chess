package com.chess.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Centralized config server — serves each service's application.yml from
 * classpath:/config (native profile) instead of a Git repo, so there's
 * nothing extra to host or pay for.
 *
 * FREE-TIER DEPLOYMENT NOTE (same reasoning as discovery-server):
 * This is genuinely optional for a solo/portfolio project. It's here mainly
 * for the "centralized config" story in interviews. If you're optimizing
 * purely for free-tier deployment simplicity, you can skip deploying this
 * altogether and just let each service read its own application.yml with
 * ${ENV_VAR} placeholders set directly on the host (Render/Railway both let
 * you set env vars per service in their dashboard) — that's what the actual
 * per-service application.yml files in later sessions will do by default.
 * Run this locally if you want the full microservices demo on your machine.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
