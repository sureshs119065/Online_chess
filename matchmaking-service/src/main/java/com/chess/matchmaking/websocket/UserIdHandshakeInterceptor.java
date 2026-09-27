package com.chess.matchmaking.websocket;

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
 * Clients connect to /ws/matchmaking/{userId} while waiting in queue.
 *
 * The path segment alone used to be trusted as the connection's identity —
 * meaning a client could subscribe to (and receive match-found pushes
 * meant for) any other user just by connecting to their id. Now the client
 * must also pass its access token as a "token" query param (browsers can't
 * set an Authorization header on a WS upgrade request); the token is
 * verified against the same JwtUtil/secret auth-service issued it with,
 * and the handshake is rejected (401) unless the token's own subject
 * matches the {userId} in the path. USER_ID_ATTRIBUTE is set from the
 * verified token, not the path, though by this point they're guaranteed
 * to agree.
 */
public class UserIdHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";

    private final JwtUtil jwtUtil;

    public UserIdHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                    @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        String path = request.getURI().getPath();
        String[] segments = path.split("/");
        String lastSegment = segments[segments.length - 1];

        UUID pathUserId;
        try {
            pathUserId = UUID.fromString(lastSegment);
        } catch (IllegalArgumentException ex) {
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

        if (!authUserId.equals(pathUserId)) {
            // Authenticated, but trying to open someone else's queue channel.
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put(USER_ID_ATTRIBUTE, authUserId);
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
        // no-op
    }
}
