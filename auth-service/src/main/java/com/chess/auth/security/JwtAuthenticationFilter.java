package com.chess.auth.security;

import com.chess.common.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads the Authorization: Bearer <token> header, validates it with
 * common-lib's JwtUtil, and — if valid — populates the SecurityContext so
 * @PreAuthorize / SecurityConfig's authorizeHttpRequests rules can trust
 * request.getUserPrincipal(). No roles/authorities system yet (every
 * authenticated user has the same access level for now), so this grants a
 * single generic ROLE_USER authority.
 *
 * Note: when this service sits behind api-gateway in production, the
 * gateway's own JwtAuthFilter (Session 8) does this same validation and
 * forwards X-User-Id downstream. This filter stays here regardless so
 * auth-service is independently secure if called directly (e.g. in local
 * dev without the gateway running, or if the gateway is bypassed).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.validateToken(token)) {
                try {
                    UUID userId = jwtUtil.extractUserId(token);

                    var authToken = new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            List.of(() -> "ROLE_USER")
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } catch (Exception ex) {
                    // Malformed subject or similar — leave request unauthenticated,
                    // downstream authorizeHttpRequests rules will reject it.
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
