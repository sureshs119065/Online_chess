package com.chess.notification.dto;

import com.chess.notification.model.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String type,
        String payload, // raw JSON text — frontend parses it based on `type`
        boolean isRead,
        Instant createdAt
) {
    public static NotificationResponse from(Notification entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getType().name(),
                entity.getPayload(),
                entity.isRead(),
                entity.getCreatedAt()
        );
    }
}
