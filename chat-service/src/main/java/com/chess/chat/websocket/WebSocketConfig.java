package com.chess.chat.websocket;

import com.chess.common.security.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtUtil jwtUtil;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler, JwtUtil jwtUtil) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat/*")
                .addInterceptors(new GameIdHandshakeInterceptor(jwtUtil))
                .setAllowedOrigins("*"); // tighten to your frontend origin once it exists
    }
}
