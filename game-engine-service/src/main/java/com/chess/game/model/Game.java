package com.chess.game.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "games")
public class Game {

    public static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "white_player_id", nullable = false)
    private UUID whitePlayerId;

    @Column(name = "black_player_id", nullable = false)
    private UUID blackPlayerId;

    @Column(name = "fen_current", nullable = false, columnDefinition = "TEXT")
    private String fenCurrent = STARTING_FEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameStatus status = GameStatus.IN_PROGRESS;

    @Column(name = "time_control", nullable = false, length = 20)
    private String timeControl;

    @Column(name = "white_time_ms")
    private Integer whiteTimeMs;

    @Column(name = "black_time_ms")
    private Integer blackTimeMs;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    /**
     * When the side-to-move's clock last started ticking — i.e. the
     * timestamp of the last move (or game creation, if none yet). Server
     * clock enforcement (GameService.checkTimeout) compares "now minus
     * this" against the mover's stored remaining time on every move,
     * resignation, and state read, instead of trusting a client-reported
     * clock value.
     */
    @Column(name = "last_move_at", nullable = false)
    private Instant lastMoveAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_reason", length = 30)
    private ResultReason resultReason;

    protected Game() {
        // JPA
    }

    public Game(UUID whitePlayerId, UUID blackPlayerId, String timeControl, int initialTimeMs) {
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.timeControl = timeControl;
        this.whiteTimeMs = initialTimeMs;
        this.blackTimeMs = initialTimeMs;
    }

    @PrePersist
    protected void onCreate() {
        this.startedAt = Instant.now();
        this.lastMoveAt = this.startedAt;
    }

    public boolean isPlayer(UUID userId) {
        return whitePlayerId.equals(userId) || blackPlayerId.equals(userId);
    }

    public UUID opponentOf(UUID userId) {
        return whitePlayerId.equals(userId) ? blackPlayerId : whitePlayerId;
    }

    // --- getters / setters ---

    public UUID getId() {
        return id;
    }

    public UUID getWhitePlayerId() {
        return whitePlayerId;
    }

    public UUID getBlackPlayerId() {
        return blackPlayerId;
    }

    public String getFenCurrent() {
        return fenCurrent;
    }

    public void setFenCurrent(String fenCurrent) {
        this.fenCurrent = fenCurrent;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public String getTimeControl() {
        return timeControl;
    }

    public Integer getWhiteTimeMs() {
        return whiteTimeMs;
    }

    public void setWhiteTimeMs(Integer whiteTimeMs) {
        this.whiteTimeMs = whiteTimeMs;
    }

    public Integer getBlackTimeMs() {
        return blackTimeMs;
    }

    public void setBlackTimeMs(Integer blackTimeMs) {
        this.blackTimeMs = blackTimeMs;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getLastMoveAt() {
        return lastMoveAt;
    }

    public void setLastMoveAt(Instant lastMoveAt) {
        this.lastMoveAt = lastMoveAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public ResultReason getResultReason() {
        return resultReason;
    }

    public void setResultReason(ResultReason resultReason) {
        this.resultReason = resultReason;
    }
}
