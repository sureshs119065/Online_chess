package com.chess.notification.controller;

import com.chess.common.security.RequestUserResolver;
import com.chess.notification.dto.NotificationResponse;
import com.chess.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> getForUser(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        // Previously any caller could list any userId's notifications just
        // by putting it in the path - this now requires the path id to
        // match the authenticated caller (when the request went through
        // the gateway).
        UUID authenticatedUserId = RequestUserResolver.resolve(userIdHeader, userId);
        return ResponseEntity.ok(notificationService.getForUser(authenticatedUserId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        // No body carries a "claimed" user id for this endpoint, so resolve
        // against null and let a missing header fail loudly rather than
        // silently allowing an unauthenticated mark-as-read.
        UUID authenticatedUserId = RequestUserResolver.resolve(userIdHeader, null);
        return ResponseEntity.ok(notificationService.markRead(id, authenticatedUserId));
    }
}
