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
 * IMPORTANT — read before assuming every controller already uses this:
 * every service built so far (Sessions 3–7) reads the caller's id from the
 * REQUEST BODY instead (e.g. ResignRequest.playerId, JoinQueueRequest.userId)
 * as a documented simplification for testing each service directly without
 * the gateway running. X-User-Id is now injected and available, but no
 * controller trusts it yet over the body value — updating them to prefer
 * the header (and reject a body value that doesn't match it) is the
 * natural next hardening step once the gateway is always in front of
 * everything.
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
            // WebSocket handshakes: the WS handlers themselves don't verify a
            // JWT yet (documented in every *WebSocketHandler's javadoc as a
            // known simplification) — since the gateway can't add value it
            // can't itself enforce, these pass through unauthenticated for
            // now. Tighten this together with the handshake interceptors
            // when you close that gap.
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
