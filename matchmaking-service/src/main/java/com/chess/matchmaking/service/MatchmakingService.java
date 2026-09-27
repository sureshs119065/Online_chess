package com.chess.matchmaking.service;

import com.chess.common.exception.ApiException;
import com.chess.matchmaking.client.AuthRatingClient;
import com.chess.matchmaking.client.GameEngineClient;
import com.chess.matchmaking.dto.CreateGameFeignRequest;
import com.chess.matchmaking.dto.GameCreatedResponse;
import com.chess.matchmaking.model.QueueEntry;
import com.chess.matchmaking.model.QueueStatus;
import com.chess.matchmaking.repository.QueueRepository;
import com.chess.matchmaking.websocket.MatchmakingWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    /** Starting ELO band; widens the longer a player waits (see bandFor). */
    private static final int BASE_BAND = 100;
    private static final int BAND_GROWTH_PER_10S = 50;
    private static final int MAX_BAND = 500;

    private final QueueRepository queueRepository;
    private final AuthRatingClient authRatingClient;
    private final GameEngineClient gameEngineClient;
    private final MatchmakingWebSocketHandler webSocketHandler;
    private final Random random = new Random();

    public MatchmakingService(
            QueueRepository queueRepository,
            AuthRatingClient authRatingClient,
            GameEngineClient gameEngineClient,
            MatchmakingWebSocketHandler webSocketHandler
    ) {
        this.queueRepository = queueRepository;
        this.authRatingClient = authRatingClient;
        this.gameEngineClient = gameEngineClient;
        this.webSocketHandler = webSocketHandler;
    }

    @Transactional
    public QueueEntry joinQueue(UUID userId, String timeControl) {
        if (queueRepository.existsByUserIdAndStatus(userId, QueueStatus.WAITING)) {
            throw ApiException.conflict("You are already in the matchmaking queue");
        }
        int eloRating = authRatingClient.fetchCurrentRating(userId);
        QueueEntry entry = new QueueEntry(userId, eloRating, timeControl);
        return queueRepository.save(entry);
    }

    @Transactional
    public void leaveQueue(UUID userId) {
        List<QueueEntry> waiting = queueRepository.findByUserIdAndStatus(userId, QueueStatus.WAITING);
        if (waiting.isEmpty()) {
            throw ApiException.notFound("You are not currently in the matchmaking queue");
        }
        waiting.forEach(entry -> entry.setStatus(QueueStatus.CANCELLED));
        queueRepository.saveAll(waiting);
    }

    /**
     * Runs on QueueMatcherScheduler's fixed delay. Groups waiting players by
     * time control (blitz players only get paired with other blitz players,
     * etc.), then greedily pairs within each group: for every entry, scan
     * the rest of that group for the closest-rated opponent still inside
     * the current ELO band (which widens the longer the OLDER of the two
     * has waited) and pair the first match found. O(n^2) per group — fine
     * at the scale a portfolio project's queue will ever see.
     */
    @Transactional
    public void pairPlayers() {
        List<QueueEntry> waiting = queueRepository.findByStatusOrderByQueuedAtAsc(QueueStatus.WAITING);
        if (waiting.size() < 2) {
            return;
        }

        Set<String> timeControls = waiting.stream().map(QueueEntry::getTimeControl).collect(Collectors.toSet());

        for (String timeControl : timeControls) {
            List<QueueEntry> group = new ArrayList<>(
                    queueRepository.findByStatusAndTimeControlOrderByQueuedAtAsc(QueueStatus.WAITING, timeControl));

            while (group.size() >= 2) {
                QueueEntry longestWaiting = group.get(0);
                QueueEntry opponent = findBestMatch(longestWaiting, group);

                if (opponent == null) {
                    // Nobody in range yet for the longest-waiting player —
                    // leave them queued; their band will widen next cycle.
                    // Try the next player in line instead so one stubborn
                    // outlier doesn't block everyone behind them.
                    group.remove(0);
                    continue;
                }

                group.remove(longestWaiting);
                group.remove(opponent);
                match(longestWaiting, opponent);
            }
        }
    }

    private QueueEntry findBestMatch(QueueEntry player, List<QueueEntry> group) {
        int band = bandFor(player);
        QueueEntry closest = null;
        int closestDiff = Integer.MAX_VALUE;

        for (QueueEntry candidate : group) {
            if (candidate.getId().equals(player.getId())) {
                continue;
            }
            int diff = Math.abs(candidate.getEloRating() - player.getEloRating());
            if (diff <= band && diff < closestDiff) {
                closest = candidate;
                closestDiff = diff;
            }
        }
        return closest;
    }

    private int bandFor(QueueEntry entry) {
        long secondsWaited = Duration.between(entry.getQueuedAt(), Instant.now()).getSeconds();
        int widened = BASE_BAND + (int) (secondsWaited / 10) * BAND_GROWTH_PER_10S;
        return Math.min(widened, MAX_BAND);
    }

    private void match(QueueEntry a, QueueEntry b) {
        UUID whiteId = random.nextBoolean() ? a.getUserId() : b.getUserId();
        UUID blackId = whiteId.equals(a.getUserId()) ? b.getUserId() : a.getUserId();

        GameCreatedResponse gameResponse;
        try {
            gameResponse = gameEngineClient.createGame(
                    new CreateGameFeignRequest(whiteId, blackId, a.getTimeControl()));
        } catch (Exception ex) {
            // game-engine-service unreachable — leave both players WAITING
            // so they get retried on the next scheduler tick instead of
            // silently dropping them from the queue.
            log.error("Failed to create game for matched players {} vs {}: {}",
                    a.getUserId(), b.getUserId(), ex.getMessage());
            return;
        }

        a.setStatus(QueueStatus.MATCHED);
        b.setStatus(QueueStatus.MATCHED);
        queueRepository.saveAll(List.of(a, b));

        UUID gameId = gameResponse.gameId();
        webSocketHandler.notifyMatchFound(whiteId, gameId, blackId, "WHITE");
        webSocketHandler.notifyMatchFound(blackId, gameId, whiteId, "BLACK");

        log.info("Matched {} vs {} into game {}", a.getUserId(), b.getUserId(), gameId);
    }
}
