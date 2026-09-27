package com.chess.game.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Only declares the exchange — the queues and bindings belong to whoever
 * consumes (notification-service's own RabbitConfig, Session 7). A
 * publisher only needs the exchange to exist; declaring it here too is
 * just defensive idempotent setup in case this service starts before
 * notification-service ever has.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "chess.events";

    @Bean
    public TopicExchange chessEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}
