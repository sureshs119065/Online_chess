package com.chess.gateway.filter;

import com.chess.common.security.JwtUtil;
import com.chess.gateway.security.GatewayJwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Runs on every request before it's routed downstream. Public paths pass
 * straight through; everything else needs a valid "Authorization: Bearer
 * <token>" header, or the gateway rejects it with 401 before it ever
 * reaches a backend service.
 *
 * On success, the caller's user id (the token's subject) is added as an
 * X-User-Id request header for the downstream service — this is what file
 * 02-MICROSERVICES-DETAIL.md means by "individual services can trust
 * X-User-Id header injected by the gateway."
 *
 * Every controller that used to read the caller's id only from the request
 * body/path (GameController's ResignRequest, MatchmakingController's
 * JoinQueueRequest/LeaveQueueRequest, NotificationController's path
 * variable) now resolves it via common-lib's RequestUserResolver, which
 * prefers this header and rejects a body-supplied id that doesn't match
 * it. A missing header (e.g. a request made directly against a service,
 * bypassing this gateway, for local testing) falls back to the
 * body-supplied id so that workflow still works.
 *
 * Inter-service calls (matchmaking → game-engine, game-engine → auth's
 * internal endpoint, etc.) never come through this filter at all — they
 * call each other's URLs directly, bypassing the gateway entirely, exactly
 * as documented in every RestTemplate/Feign client built in Sessions 4–5.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    /**
     * {method, pathPattern} pairs that skip JWT validation entirely.
     * method == null means "any method". Patterns use Ant-style matching.
     */
    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(null, "/api/auth/**"),
            new PublicRoute(HttpMethod.GET, "/api/users/**"),
            new PublicRoute(HttpMethod.GET, "/api/games/**"),
            new PublicRoute(null, "/actuator/**"),
            // WebSocket handshakes: this filter passes them through without
            // checking a header (WS upgrade requests don't carry one from a
            // browser), but they are NOT unauthenticated — game-engine-service,
            // chat-service, and matchmaking-service each verify a "token"
            // query param against the same JWT secret during their own
            // HandshakeInterceptor (GameIdHandshakeInterceptor /
            // UserIdHandshakeInterceptor) and reject the upgrade with 401/403
            // before a WebSocketSession is ever created. The enforcement just
            // lives one hop further downstream than it does for REST routes.
            new PublicRoute(null, "/ws/**")
    );

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthFilter(GatewayJwtService gatewayJwtService) {
        this.jwtUtil = gatewayJwtService.getJwtUtil();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublic(request)) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = header.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        UUID userId;
        try {
            userId = jwtUtil.extractUserId(token);
        } catch (Exception ex) {
            return unauthorized(exchange, "Token subject is not a valid user id");
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId.toString())
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        return PUBLIC_ROUTES.stream().anyMatch(route ->
                (route.method() == null || route.method().equals(method))
                        && pathMatcher.match(route.pathPattern(), path)
        );
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private record PublicRoute(HttpMethod method, String pathPattern) {
    }
}
