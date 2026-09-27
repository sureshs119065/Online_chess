package com.chess.chat.service;

import com.chess.chat.dto.ChatMessageResponse;
import com.chess.chat.model.ChatMessage;
import com.chess.chat.repository.ChatMessageRepository;
import com.chess.common.exception.ApiException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int RECENT_HISTORY_SIZE = 50;

    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID gameId, UUID senderId, String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw ApiException.badRequest("Message cannot be empty");
        }
        String trimmed = rawMessage.strip();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw ApiException.badRequest("Message cannot exceed " + MAX_MESSAGE_LENGTH + " characters");
        }

        ChatMessage saved = chatMessageRepository.save(new ChatMessage(gameId, senderId, trimmed));
        return ChatMessageResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(UUID gameId) {
        return chatMessageRepository.findByGameIdOrderBySentAtAsc(gameId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    /** Most recent N messages, oldest-first — used to seed a newly-connected WebSocket client. */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentHistory(UUID gameId) {
        return chatMessageRepository.findByGameIdOrderBySentAtDesc(gameId, Limit.of(RECENT_HISTORY_SIZE)).stream()
                .sorted(Comparator.comparing(ChatMessage::getSentAt))
                .map(ChatMessageResponse::from)
                .toList();
    }
}
