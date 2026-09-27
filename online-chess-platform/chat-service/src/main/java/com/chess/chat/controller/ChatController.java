package com.chess.chat.controller;

import com.chess.chat.dto.ChatMessageResponse;
import com.chess.chat.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Full chat history for a game, oldest first. Mapped under /api/games
     * rather than /api/chat to match the route file 02 specifies
     * (GET /api/games/{id}/chat) and the api-gateway predicate
    * (Path=/api/games/{id}/chat/**) planned for Session 8.
     */
    @GetMapping("/{id}/chat")
    public ResponseEntity<List<ChatMessageResponse>> getHistory(@PathVariable("id") UUID gameId) {
        return ResponseEntity.ok(chatService.getHistory(gameId));
    }
}
