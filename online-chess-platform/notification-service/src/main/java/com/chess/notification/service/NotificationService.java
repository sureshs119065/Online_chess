package com.chess.notification.service;

import com.chess.common.exception.ApiException;
import com.chess.notification.dto.NotificationResponse;
import com.chess.notification.model.Notification;
import com.chess.notification.model.NotificationType;
import com.chess.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification create(UUID userId, NotificationType type, String payloadJson) {
        return notificationRepository.save(new Notification(userId, type, payloadJson));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found: " + notificationId));
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}
