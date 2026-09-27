package com.chess.chat.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected ChatMessage() {
        // JPA
    }

    public ChatMessage(UUID gameId, UUID senderId, String message) {
        this.gameId = gameId;
        this.senderId = senderId;
        this.message = message;
    }

    @PrePersist
    protected void onCreate() {
        this.sentAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getGameId() {
        return gameId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
