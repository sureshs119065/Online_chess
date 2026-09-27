package com.chess.game.scheduler;

import com.chess.game.dto.GameStateDto;
import com.chess.game.model.Game;
import com.chess.game.model.GameStatus;
import com.chess.game.repository.GameRepository;
import com.chess.game.service.GameService;
import com.chess.game.websocket.GameWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * GameService.checkTimeout enforces the clock lazily — on the next move,
 * resignation, or state read for a given game. That's correct but passive:
 * if neither player interacts (e.g. the side to move simply walks away),
 * nobody finds out their opponent flagged until someone happens to poll.
 *
 * This sweep closes that gap actively: every few seconds it asks
 * GameService to check every IN_PROGRESS game's clock, and for any that
 * conclude, pushes the new GameStateDto to that game's connected WebSocket
 * sessions the same way a real move would.
 */
@Component
public class GameTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(GameTimeoutScheduler.class);

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final GameWebSocketHandler gameWebSocketHandler;

    public GameTimeoutScheduler(GameRepository gameRepository, GameService gameService,
                                 GameWebSocketHandler gameWebSocketHandler) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Scheduled(fixedDelay = 3000)
    public void sweep() {
        List<Game> inProgress = gameRepository.findByStatus(GameStatus.IN_PROGRESS);
        for (Game game : inProgress) {
            UUID gameId = game.getId();
            try {
                GameStateDto concluded = gameService.applyTimeoutIfExpired(gameId);
                if (concluded != null) {
                    log.info("Game {} ended by timeout sweep", gameId);
                    gameWebSocketHandler.broadcastGameState(gameId, concluded);
                }
            } catch (Exception ex) {
                // One game's failure shouldn't stop the sweep from checking
                // the rest of the list.
                log.warn("Timeout sweep failed for game {}: {}", gameId, ex.getMessage());
            }
        }
    }
}
