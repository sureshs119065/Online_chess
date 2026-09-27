package com.chess.matchmaking.dto;

import com.chess.matchmaking.model.QueueEntry;

import java.time.Instant;
import java.util.UUID;

public record QueueEntryResponse(
        UUID id,
        UUID userId,
        Integer eloRating,
        String timeControl,
        String status,
        Instant queuedAt
) {
    public static QueueEntryResponse from(QueueEntry entry) {
        return new QueueEntryResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getEloRating(),
                entry.getTimeControl(),
                entry.getStatus().name(),
                entry.getQueuedAt()
        );
    }
}
