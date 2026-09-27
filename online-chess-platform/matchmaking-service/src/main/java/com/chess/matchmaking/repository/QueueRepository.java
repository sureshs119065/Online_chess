package com.chess.matchmaking.repository;

import com.chess.matchmaking.model.QueueEntry;
import com.chess.matchmaking.model.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueRepository extends JpaRepository<QueueEntry, UUID> {

    List<QueueEntry> findByStatusOrderByQueuedAtAsc(QueueStatus status);

    List<QueueEntry> findByStatusAndTimeControlOrderByQueuedAtAsc(QueueStatus status, String timeControl);

    List<QueueEntry> findByUserIdAndStatus(UUID userId, QueueStatus status);

    Optional<QueueEntry> findFirstByUserIdAndStatusOrderByQueuedAtDesc(UUID userId, QueueStatus status);

    boolean existsByUserIdAndStatus(UUID userId, QueueStatus status);
}
