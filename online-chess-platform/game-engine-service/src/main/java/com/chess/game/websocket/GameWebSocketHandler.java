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
 * SIMPLIFICATION — read before wiring up JWT auth here: playerId currently
 * comes from the message payload itself (WsIncomingMessage.playerId), not
 * from a verified token, meaning a client could claim to be either player.
 * That's acceptable to get live play working end-to-end in this session,
 * but before this is internet-facing you'll want to verify the caller's
 * identity during the handshake instead (e.g. a HandshakeInterceptor that
 * validates a JWT passed as a query param or Sec-WebSocket-Protocol header,
 * since browsers can't set Authorization headers on WS upgrade requests)
 * and store the authenticated userId in session attributes the same way
 * GameIdHandshakeInterceptor stores gameId, then trust that instead of the
 * payload's playerId.
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

        try {
            WsIncomingMessage incoming = objectMapper.readValue(message.getPayload(), WsIncomingMessage.class);

            GameStateDto updatedState = switch (incoming.type()) {
                case MOVE -> {
                    if (incoming.moveSan() == null || incoming.moveSan().isBlank()) {
                        throw ApiException.badRequest("moveSan is required for a MOVE message");
                    }
                    yield gameService.applyMove(gameId, incoming.playerId(), incoming.moveSan());
                }
                case RESIGN -> gameService.resign(gameId, incoming.playerId());
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
}
