package com.chess.game.websocket;

import com.chess.common.exception.ApiException;
import com.chess.game.dto.GameStateDto;
import com.chess.game.dto.WsErrorMessage;
import com.chess.game.dto.WsIncomingMessage;
import com.chess.game.service.GameService;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * One WebSocket endpoint per game: /ws/game/{gameId}. Both players (and,
 * if you want spectators later, anyone else) connect here; every valid
 * move or resignation is broadcast to everyone currently connected to that
 * game's session group as the new GameStateDto.
 *
 * Identity comes from GameIdHandshakeInterceptor's JWT verification, stored
 * as AUTH_USER_ID_ATTRIBUTE in session attributes — NOT from whatever
 * playerId a MOVE/RESIGN message payload claims. A client can no longer
 * play as the other side just by changing a field in the JSON it sends;
 * GameService.applyMove/resign still separately confirm the authenticated
 * id is actually one of this game's two players.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final GameService gameService;
    private final ObjectMapper objectMapper;

    private final Map<UUID, Set<WebSocketSession>> sessionsByGame = new ConcurrentHashMap<>();

    public GameWebSocketHandler(GameService gameService, ObjectMapper objectMapper) {
        this.gameService = gameService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        UUID gameId = gameId(session);
        sessionsByGame.computeIfAbsent(gameId, id -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WS connected: session={} gameId={}", session.getId(), gameId);

        // Send current state immediately so a reconnecting client doesn't
        // have to wait for the next move to see where the game stands.
        sendQuietly(session, gameService.getGameState(gameId));
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        UUID gameId = gameId(session);
        Set<WebSocketSession> sessions = sessionsByGame.get(gameId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByGame.remove(gameId);
            }
        }
        log.info("WS disconnected: session={} gameId={} status={}", session.getId(), gameId, status);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        UUID gameId = gameId(session);
        UUID authUserId = authUserId(session);

        try {
            WsIncomingMessage incoming = objectMapper.readValue(message.getPayload(), WsIncomingMessage.class);

            GameStateDto updatedState = switch (incoming.type()) {
                case MOVE -> {
                    if (incoming.moveSan() == null || incoming.moveSan().isBlank()) {
                        throw ApiException.badRequest("moveSan is required for a MOVE message");
                    }
                    yield gameService.applyMove(gameId, authUserId, incoming.moveSan());
                }
                case RESIGN -> gameService.resign(gameId, authUserId);
            };

            broadcast(gameId, updatedState);

        } catch (ApiException ex) {
            sendQuietly(session, new WsErrorMessage(ex.getMessage()));
        } catch (Exception ex) {
            log.warn("Failed to process WS message on game {}: {}", gameId, ex.getMessage());
            sendQuietly(session, new WsErrorMessage("Could not process message: " + ex.getMessage()));
        }
    }

    private void broadcast(UUID gameId, GameStateDto state) {
        Set<WebSocketSession> sessions = sessionsByGame.getOrDefault(gameId, Set.of());
        for (WebSocketSession s : sessions) {
            sendQuietly(s, state);
        }
    }

    /**
     * Public entry point for GameTimeoutScheduler: pushes a state update to
     * every session connected to {@code gameId} from outside the normal
     * "client sent a message" flow, so a flag-fall reaches both players even
     * if neither of them does anything.
     */
    public void broadcastGameState(UUID gameId, GameStateDto state) {
        broadcast(gameId, state);
    }

    private void sendQuietly(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException ex) {
            log.warn("Failed to send WS message to session {}: {}", session.getId(), ex.getMessage());
        }
    }

    private UUID gameId(WebSocketSession session) {
        return (UUID) session.getAttributes().get(GameIdHandshakeInterceptor.GAME_ID_ATTRIBUTE);
    }

    private UUID authUserId(WebSocketSession session) {
        return (UUID) session.getAttributes().get(GameIdHandshakeInterceptor.AUTH_USER_ID_ATTRIBUTE);
    }
}
