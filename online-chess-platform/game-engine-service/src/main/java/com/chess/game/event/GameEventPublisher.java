package com.chess.game.event;

import com.chess.game.model.Game;

import java.util.UUID;

/**
 * Publishes game lifecycle events for notification-service to consume.
 *
 * This interface exists so GameService never depends on a specific message
 * broker. It shipped with a logging-only implementation through Session 4
 * (no broker wired up yet); as of Session 7, RabbitMqGameEventPublisher is
 * the real implementation, publishing JSON text onto the "chess.events"
 * topic exchange that notification-service consumes from. GameService's
 * code never changed when that swap happened — it only ever depends on
 * this interface.
 */
public interface GameEventPublisher {

    void publishGameEnded(Game game);

    /** @param moverId the player who just made {@code moveSan} — lets the consumer notify their opponent. */
    void publishMoveMade(Game game, UUID moverId, String moveSan);
}
