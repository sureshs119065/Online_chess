package com.chess.game.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * Plain WebSocketHandler registrations don't resolve {gameId}-style path
 * variables the way @Controller mappings do, so this interceptor pulls the
 * last path segment off the handshake URI (e.g. /ws/game/<uuid>) and stores
 * it as a session attribute, which GameWebSocketHandler then reads in
 * afterConnectionEstablished.
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
            // Not a valid UUID in the URL — reject the handshake with 400
            // rather than accepting a connection we can't route messages for.
            response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
            return false;
        }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
        // no-op
    }
}
