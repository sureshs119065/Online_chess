package com.chess.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling // powers GameTimeoutScheduler's periodic flag-fall sweep
public class GameEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameEngineApplication.class, args);
    }
}
