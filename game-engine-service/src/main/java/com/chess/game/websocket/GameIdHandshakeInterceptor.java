package com.chess.game.websocket;

import com.chess.common.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plain WebSocketHandler registrations don't resolve {gameId}-style path
 * variables the way @Controller mappings do, so this interceptor pulls the
 * last path segment off the handshake URI (e.g. /ws/game/<uuid>) and stores
 * it as a session attribute, which GameWebSocketHandler then reads in
 * afterConnectionEstablished.
 *
 * It also verifies the caller's identity before accepting the connection:
 * the client passes its access token as a "token" query param (browsers
 * can't set an Authorization header on a WS upgrade request), which is
 * validated against the same JwtUtil/secret auth-service issued it with.
 * The resulting userId is stored as AUTH_USER_ID_ATTRIBUTE and is what
 * GameWebSocketHandler trusts for MOVE/RESIGN messages — NOT whatever
 * playerId a message payload happens to claim. A missing or invalid token
 * rejects the handshake with 401 before any WebSocketSession is created.
 */
public class GameIdHandshakeInterceptor implements HandshakeInterceptor {

    public static final String GAME_ID_ATTRIBUTE = "gameId";
    public static final String AUTH_USER_ID_ATTRIBUTE = "authUserId";

    private final JwtUtil jwtUtil;

    public GameIdHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                    @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        String path = request.getURI().getPath();
        String[] segments = path.split("/");
        String lastSegment = segments[segments.length - 1];

        UUID gameId;
        try {
            gameId = UUID.fromString(lastSegment);
        } catch (IllegalArgumentException ex) {
            // Not a valid UUID in the URL — reject the handshake with 400
            // rather than accepting a connection we can't route messages for.
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        List<String> tokenParam = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .get("token");
        String token = (tokenParam == null || tokenParam.isEmpty()) ? null : tokenParam.get(0);

        if (token == null || !jwtUtil.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        UUID authUserId;
        try {
            authUserId = jwtUtil.extractUserId(token);
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(GAME_ID_ATTRIBUTE, gameId);
        attributes.put(AUTH_USER_ID_ATTRIBUTE, authUserId);
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
        // no-op
    }
}
