package com.chess.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the "chess.events" topic exchange plus this service's own
 * durable queues and bindings. Spring Boot's auto-configured RabbitAdmin
 * declares every Queue/Exchange/Binding bean against the broker on
 * startup — idempotently, so it's safe that game-engine-service also
 * declares the same exchange independently (see its RabbitConfig).
 *
 * Three queues, one per routing key, rather than one queue bound to all
 * three patterns — keeps each event type's consumer method simple and
 * lets you scale or restart processing for one event type without
 * touching the others.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "chess.events";

    public static final String QUEUE_GAME_INVITE = "notification.game.invite";
    public static final String QUEUE_GAME_ENDED = "notification.game.ended";
    public static final String QUEUE_MOVE_MADE = "notification.move.made";

    @Bean
    public TopicExchange chessEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue gameInviteQueue() {
        return new Queue(QUEUE_GAME_INVITE, true);
    }

    @Bean
    public Queue gameEndedQueue() {
        return new Queue(QUEUE_GAME_ENDED, true);
    }

    @Bean
    public Queue moveMadeQueue() {
        return new Queue(QUEUE_MOVE_MADE, true);
    }

    @Bean
    public Binding gameInviteBinding(Queue gameInviteQueue, TopicExchange chessEventsExchange) {
        return BindingBuilder.bind(gameInviteQueue).to(chessEventsExchange).with("game.invite");
    }

    @Bean
    public Binding gameEndedBinding(Queue gameEndedQueue, TopicExchange chessEventsExchange) {
        return BindingBuilder.bind(gameEndedQueue).to(chessEventsExchange).with("game.ended");
    }

    @Bean
    public Binding moveMadeBinding(Queue moveMadeQueue, TopicExchange chessEventsExchange) {
        return BindingBuilder.bind(moveMadeQueue).to(chessEventsExchange).with("move.made");
    }
}
