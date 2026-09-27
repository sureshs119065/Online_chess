package com.chess.game.event;

import com.chess.game.config.RabbitConfig;
import com.chess.game.model.Game;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes plain JSON text (not Java-serialized objects) onto the
 * "chess.events" topic exchange — see notification-service's
 * GameEventListener javadoc for why: it lets the two services stay
 * decoupled without sharing a class for the message body, just an agreed
 * field shape per routing key.
 *
 * A failed publish is logged but non-fatal, matching this project's
 * established pattern for cross-service calls (see EloUpdateClient): the
 * game itself has already been persisted by the time this runs, and a
 * momentarily-unreachable broker shouldn't roll back or fail the whole
 * move/resign flow. Spring AMQP's default CachingConnectionFactory also
 * connects lazily, so this service starts up fine even if no broker is
 * reachable yet — only an actual publish attempt will fail.
 */
@Component
public class RabbitMqGameEventPublisher implements GameEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqGameEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitMqGameEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishGameEnded(Game game) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gameId", game.getId());
        payload.put("whitePlayerId", game.getWhitePlayerId());
        payload.put("blackPlayerId", game.getBlackPlayerId());
        payload.put("status", game.getStatus().name());
        payload.put("resultReason", game.getResultReason() == null ? null : game.getResultReason().name());

        publish("game.ended", payload);
    }

    @Override
    public void publishMoveMade(Game game, UUID moverId, String moveSan) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gameId", game.getId());
        payload.put("moverId", moverId);
        payload.put("whitePlayerId", game.getWhitePlayerId());
        payload.put("blackPlayerId", game.getBlackPlayerId());
        payload.put("moveSan", moveSan);

        publish("move.made", payload);
    }

    private void publish(String routingKey, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, json);
        } catch (Exception ex) {
            // Covers both JSON serialization failures and AmqpException
            // (broker unreachable, channel error, etc.) — either way,
            // log and continue rather than failing the caller.
            log.error("Failed to publish {} event: {}", routingKey, ex.getMessage(), ex);
        }
    }
}
