package com.chess.chat.websocket;

import com.chess.chat.dto.ChatMessageResponse;
import com.chess.chat.dto.WsChatIncomingMessage;
import com.chess.chat.dto.WsErrorMessage;
import com.chess.chat.service.ChatService;
import com.chess.common.exception.ApiException;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * One WebSocket endpoint per game: /ws/chat/{gameId}. Straightforward once
 * game-engine-service's GameWebSocketHandler (Session 4) pattern exists —
 * same session-group-per-gameId shape, no chess-rules complexity here.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    private final Map<UUID, Set<WebSocketSession>> sessionsByGame = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        UUID gameId = gameId(session);
        sessionsByGame.computeIfAbsent(gameId, id -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WS connected: session={} gameId={}", session.getId(), gameId);

        // Seed the newly-connected client with recent context so joining
        // mid-conversation isn't a blank screen.
        List<ChatMessageResponse> recent = chatService.getRecentHistory(gameId);
        sendQuietly(session, recent);
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
            WsChatIncomingMessage incoming =
                    objectMapper.readValue(message.getPayload(), WsChatIncomingMessage.class);

            if (incoming.senderId() == null) {
                throw ApiException.badRequest("senderId is required");
            }

            ChatMessageResponse saved = chatService.sendMessage(gameId, incoming.senderId(), incoming.message());
            broadcast(gameId, saved);

        } catch (ApiException ex) {
            sendQuietly(session, new WsErrorMessage(ex.getMessage()));
        } catch (Exception ex) {
            log.warn("Failed to process chat WS message on game {}: {}", gameId, ex.getMessage());
            sendQuietly(session, new WsErrorMessage("Could not process message: " + ex.getMessage()));
        }
    }

    private void broadcast(UUID gameId, ChatMessageResponse message) {
        Set<WebSocketSession> sessions = sessionsByGame.getOrDefault(gameId, Set.of());
        for (WebSocketSession s : sessions) {
            sendQuietly(s, message);
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
