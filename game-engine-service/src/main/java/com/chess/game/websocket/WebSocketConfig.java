package com.chess.game.websocket;

import com.chess.common.security.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final JwtUtil jwtUtil;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler, JwtUtil jwtUtil) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game/*")
                .addInterceptors(new GameIdHandshakeInterceptor(jwtUtil))
                // Tighten this to your actual frontend origin once it exists
                // (e.g. your Vercel/Render frontend URL) instead of "*".
                .setAllowedOrigins("*");
    }
}
