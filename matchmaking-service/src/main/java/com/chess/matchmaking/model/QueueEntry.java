package com.chess.matchmaking.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matchmaking_queue")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "elo_rating", nullable = false)
    private Integer eloRating;

    @Column(name = "time_control", nullable = false, length = 20)
    private String timeControl;

    @Column(name = "queued_at", nullable = false, updatable = false)
    private Instant queuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QueueStatus status = QueueStatus.WAITING;

    protected QueueEntry() {
        // JPA
    }

    public QueueEntry(UUID userId, Integer eloRating, String timeControl) {
        this.userId = userId;
        this.eloRating = eloRating;
        this.timeControl = timeControl;
    }

    @PrePersist
    protected void onCreate() {
        this.queuedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Integer getEloRating() {
        return eloRating;
    }

    public String getTimeControl() {
        return timeControl;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }
}
