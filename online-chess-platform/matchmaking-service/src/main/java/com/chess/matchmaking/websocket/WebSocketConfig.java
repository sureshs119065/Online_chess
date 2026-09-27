package com.chess.matchmaking.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MatchmakingWebSocketHandler matchmakingWebSocketHandler;

    public WebSocketConfig(MatchmakingWebSocketHandler matchmakingWebSocketHandler) {
        this.matchmakingWebSocketHandler = matchmakingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(matchmakingWebSocketHandler, "/ws/matchmaking/*")
                .addInterceptors(new UserIdHandshakeInterceptor())
                .setAllowedOrigins("*"); // tighten to your frontend origin once it exists
    }
}
