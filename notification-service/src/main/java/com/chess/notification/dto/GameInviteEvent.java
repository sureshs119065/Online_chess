package com.chess.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * No service in this project currently publishes to "game.invite" — the
 * matchmaking flow (Session 5) pairs players automatically rather than
 * one player inviting a specific other, so there's no invite step to
 * notify about yet. The queue/binding/listener for it are still wired up
 * per the original architecture doc (file 02), ready for a future
 * "challenge a friend directly" feature to publish into without any
 * notification-service changes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameInviteEvent(
        UUID invitedUserId,
        UUID fromUserId,
        UUID gameId
) {
}
