package com.chess.matchmaking.websocket;

import com.chess.matchmaking.dto.WsMatchFoundMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clients connect to /ws/matchmaking/{userId} while waiting in queue and
 * receive a single push once QueueMatcherScheduler pairs them: a
 * WsMatchFoundMessage carrying the new gameId. The client then opens a
 * separate connection to game-engine-service's /ws/game/{gameId} to
 * actually play — this socket's only job is the "you've been matched"
 * notification.
 *
 * One session per user (a second connection replaces the first) since a
 * player only needs one active queue notification channel at a time.
 */
@Component
public class MatchmakingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<UUID, WebSocketSession> sessionsByUser = new ConcurrentHashMap<>();

    public MatchmakingWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        UUID userId = userId(session);
        sessionsByUser.put(userId, session);
        log.info("WS connected: session={} userId={}", session.getId(), userId);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        UUID userId = userId(session);
        sessionsByUser.remove(userId, session);
        log.info("WS disconnected: session={} userId={} status={}", session.getId(), userId, status);
    }

    /** Called by QueueMatcherScheduler once two players are paired. No-op if the player isn't connected. */
    public void notifyMatchFound(UUID userId, UUID gameId, UUID opponentId, String yourColor) {
        WebSocketSession session = sessionsByUser.get(userId);
        if (session == null || !session.isOpen()) {
            log.debug("No open WS session for user {} — match-found push skipped (client can still poll)", userId);
            return;
        }
        try {
            WsMatchFoundMessage message = new WsMatchFoundMessage(gameId, opponentId, yourColor);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException ex) {
            log.warn("Failed to push match-found to user {}: {}", userId, ex.getMessage());
        }
    }

    private UUID userId(WebSocketSession session) {
        return (UUID) session.getAttributes().get(UserIdHandshakeInterceptor.USER_ID_ATTRIBUTE);
    }
}
