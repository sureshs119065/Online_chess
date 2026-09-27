package com.chess.matchmaking.scheduler;

import com.chess.matchmaking.service.MatchmakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueMatcherScheduler {

    private static final Logger log = LoggerFactory.getLogger(QueueMatcherScheduler.class);

    private final MatchmakingService matchmakingService;

    public QueueMatcherScheduler(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    /**
     * fixedDelay (not fixedRate): waits for one pass to fully finish before
     * starting the countdown to the next, so a slow game-engine-service
     * call during match() can't cause overlapping runs. 2000ms matches the
     * interval called out in 02-MICROSERVICES-DETAIL.md.
     */
    @Scheduled(fixedDelay = 2000)
    public void run() {
        try {
            matchmakingService.pairPlayers();
        } catch (Exception ex) {
            // A single bad pass should never kill the recurring schedule —
            // log and let the next tick try again.
            log.error("Queue matching pass failed: {}", ex.getMessage(), ex);
        }
    }
}
