package com.chess.game.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moves")
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "move_number", nullable = false)
    private Integer moveNumber;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "move_san", nullable = false, length = 10)
    private String moveSan;

    @Column(name = "fen_after", nullable = false, columnDefinition = "TEXT")
    private String fenAfter;

    @Column(name = "played_at", nullable = false, updatable = false)
    private Instant playedAt;

    protected Move() {
        // JPA
    }

    public Move(UUID gameId, Integer moveNumber, UUID playerId, String moveSan, String fenAfter) {
        this.gameId = gameId;
        this.moveNumber = moveNumber;
        this.playerId = playerId;
        this.moveSan = moveSan;
        this.fenAfter = fenAfter;
    }

    @PrePersist
    protected void onCreate() {
        this.playedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getGameId() {
        return gameId;
    }

    public Integer getMoveNumber() {
        return moveNumber;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getMoveSan() {
        return moveSan;
    }

    public String getFenAfter() {
        return fenAfter;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }
}
