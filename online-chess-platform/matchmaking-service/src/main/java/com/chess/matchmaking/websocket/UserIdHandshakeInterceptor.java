package com.chess.matchmaking.websocket;

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
 * Same pattern as game-engine-service's GameIdHandshakeInterceptor: clients
 * connect to /ws/matchmaking/{userId}, and this pulls that last path
 * segment into session attributes so MatchmakingWebSocketHandler can route
 * "match found" pushes to the right connection.
 *
 * Same known simplification as the game WebSocket: userId comes from the
 * URL, not a verified JWT. Fine for local testing; tighten before this is
 * public the same way described in GameWebSocketHandler's javadoc.
 */
public class UserIdHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                    @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        String path = request.getURI().getPath();
        String[] segments = path.split("/");
        String lastSegment = segments[segments.length - 1];

        try {
            UUID userId = UUID.fromString(lastSegment);
            attributes.put(USER_ID_ATTRIBUTE, userId);
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
