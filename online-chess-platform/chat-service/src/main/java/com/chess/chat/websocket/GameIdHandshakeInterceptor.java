package com.chess.chat.websocket;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * Same pattern as game-engine-service's and matchmaking-service's
 * handshake interceptors: clients connect to /ws/chat/{gameId}, and this
 * pulls that last path segment into session attributes.
 */
public class GameIdHandshakeInterceptor implements HandshakeInterceptor {

    public static final String GAME_ID_ATTRIBUTE = "gameId";

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                    @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        String path = request.getURI().getPath();
        String[] segments = path.split("/");
        String lastSegment = segments[segments.length - 1];

        try {
            UUID gameId = UUID.fromString(lastSegment);
            attributes.put(GAME_ID_ATTRIBUTE, gameId);
            return true;
        } catch (IllegalArgumentException ex) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
        // no-op
    }
}
