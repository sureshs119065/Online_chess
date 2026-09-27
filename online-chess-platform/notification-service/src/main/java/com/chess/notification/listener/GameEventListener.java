package com.chess.notification.listener;

import com.chess.notification.config.RabbitConfig;
import com.chess.notification.dto.GameEndedEvent;
import com.chess.notification.dto.GameInviteEvent;
import com.chess.notification.dto.MoveMadeEvent;
import com.chess.notification.model.NotificationType;
import com.chess.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the three event types game-engine-service (and, eventually,
 * any future invite feature) publish onto the "chess.events" topic
 * exchange. Each listener method receives the raw JSON string the
 * publisher sent (see game-engine-service's RabbitMqGameEventPublisher —
 * events are plain JSON text, not Java-serialized objects, so publisher
 * and consumer never need to share a class, just a field-level contract)
 * and parses it into this service's own local DTO.
 */
@Component
public class GameEventListener {

    private static final Logger log = LoggerFactory.getLogger(GameEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public GameEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_GAME_ENDED)
    public void onGameEnded(String messageJson) {
        try {
            GameEndedEvent event = objectMapper.readValue(messageJson, GameEndedEvent.class);
            notificationService.create(event.whitePlayerId(), NotificationType.GAME_ENDED, messageJson);
            notificationService.create(event.blackPlayerId(), NotificationType.GAME_ENDED, messageJson);
            log.info("Recorded GAME_ENDED notifications for game {}", event.gameId());
        } catch (Exception ex) {
            // A malformed message shouldn't crash the listener container —
            // log and move on. In a production system you'd route this to
            // a dead-letter queue instead of just dropping it.
            log.error("Failed to process game.ended message: {}", ex.getMessage(), ex);
        }
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_MOVE_MADE)
    public void onMoveMade(String messageJson) {
        try {
            MoveMadeEvent event = objectMapper.readValue(messageJson, MoveMadeEvent.class);
            notificationService.create(event.opponentOfMover(), NotificationType.MOVE_ALERT, messageJson);
        } catch (Exception ex) {
            log.error("Failed to process move.made message: {}", ex.getMessage(), ex);
        }
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_GAME_INVITE)
    public void onGameInvite(String messageJson) {
        // See GameInviteEvent's javadoc — nothing publishes this yet, but
        // the consumer side is ready for when something does.
        try {
            GameInviteEvent event = objectMapper.readValue(messageJson, GameInviteEvent.class);
            notificationService.create(event.invitedUserId(), NotificationType.GAME_INVITE, messageJson);
        } catch (Exception ex) {
            log.error("Failed to process game.invite message: {}", ex.getMessage(), ex);
        }
    }
}
