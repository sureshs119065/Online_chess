package com.chess.chat.dto;

import com.chess.chat.model.ChatMessage;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID gameId,
        UUID senderId,
        String message,
        Instant sentAt
) {
    public static ChatMessageResponse from(ChatMessage entity) {
        return new ChatMessageResponse(
                entity.getId(),
                entity.getGameId(),
                entity.getSenderId(),
                entity.getMessage(),
                entity.getSentAt()
        );
    }
}
