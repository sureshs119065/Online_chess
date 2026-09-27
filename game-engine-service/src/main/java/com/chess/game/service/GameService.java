package com.chess.game.service;

import com.chess.common.exception.ApiException;
import com.chess.game.dto.CreateGameRequest;
import com.chess.game.dto.GameStateDto;
import com.chess.game.dto.MoveResponse;
import com.chess.game.event.GameEventPublisher;
import com.chess.game.model.Game;
import com.chess.game.model.GameStatus;
import com.chess.game.model.Move;
import com.chess.game.model.ResultReason;
import com.chess.game.repository.GameRepository;
import com.chess.game.repository.MoveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GameService {

    private static final Pattern TIME_CONTROL_MINUTES = Pattern.compile("_(\\d+)$");
    private static final int DEFAULT_TIME_MINUTES = 10;

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final ChessRulesService chessRulesService;
    private final EloCalculatorService eloCalculatorService;
    private final EloUpdateClient eloUpdateClient;
    private final GameEventPublisher eventPublisher;

    public GameService(
            GameRepository gameRepository,
            MoveRepository moveRepository,
            ChessRulesService chessRulesService,
            EloCalculatorService eloCalculatorService,
            EloUpdateClient eloUpdateClient,
            GameEventPublisher eventPublisher
    ) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.chessRulesService = chessRulesService;
        this.eloCalculatorService = eloCalculatorService;
        this.eloUpdateClient = eloUpdateClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Game createGame(CreateGameRequest request) {
        if (request.whitePlayerId().equals(request.blackPlayerId())) {
            throw ApiException.badRequest("A player cannot play against themselves");
        }
        int initialTimeMs = parseTimeControlMillis(request.timeControl());
        Game game = new Game(request.whitePlayerId(), request.blackPlayerId(),
                request.timeControl(), initialTimeMs);
        return gameRepository.save(game);
    }

    @Transactional(readOnly = true)
    public Game getGame(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> ApiException.notFound("Game not found: " + gameId));
    }

    // Not readOnly: a "read" of an in-progress game can discover the
    // mover's clock has already run out and conclude the game right here
    // (checkTimeout), which writes. This is what makes the clock actually
    // server-enforced instead of decorative — a player finds out their
    // opponent flagged just by loading the game, not only on their next move.
    @Transactional
    public GameStateDto getGameState(UUID gameId) {
        Game game = getGame(gameId);
        checkTimeout(game, Instant.now());
        return stateFor(game, null);
    }

    /**
     * Ends {@code game} on time if the side to move has no time left,
     * comparing wall-clock elapsed time since {@link Game#getLastMoveAt()}
     * against their stored remaining time — never a client-supplied clock
     * value. Returns true if the game was concluded by this call.
     */
    private boolean checkTimeout(Game game, Instant now) {
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            return false;
        }

        ChessRulesService.MoveResult evaluation = chessRulesService.evaluate(game.getFenCurrent());
        boolean whiteToMove = "WHITE".equals(evaluation.sideToMove());
        Integer storedMs = whiteToMove ? game.getWhiteTimeMs() : game.getBlackTimeMs();
        if (storedMs == null) {
            return false; // untimed game
        }

        long elapsedMs = Duration.between(game.getLastMoveAt(), now).toMillis();
        if (storedMs - elapsedMs > 0) {
            return false; // still time on the clock
        }

        if (whiteToMove) {
            game.setWhiteTimeMs(0);
            game.setStatus(GameStatus.BLACK_WON);
        } else {
            game.setBlackTimeMs(0);
            game.setStatus(GameStatus.WHITE_WON);
        }
        game.setResultReason(ResultReason.TIMEOUT);
        game.setEndedAt(now);
        gameRepository.save(game);

        updateRatingsAfterGame(game);
        eventPublisher.publishGameEnded(game);
        return true;
    }

    private GameStateDto stateFor(Game game, String lastMoveSanOverride) {
        ChessRulesService.MoveResult evaluation = chessRulesService.evaluate(game.getFenCurrent());
        String lastMoveSan = lastMoveSanOverride;
        if (lastMoveSan == null) {
            lastMoveSan = moveRepository.findByGameIdOrderByMoveNumberAsc(game.getId()).stream()
                    .reduce((first, second) -> second)
                    .map(Move::getMoveSan)
                    .orElse(null);
        }
        return GameStateDto.of(game, evaluation.sideToMove(), evaluation.check(),
                evaluation.checkmate(), evaluation.stalemate(), lastMoveSan);
    }

    /**
     * Called by GameTimeoutScheduler's periodic sweep so a flag-fall gets
     * broadcast to connected clients even if neither player sends a
     * message or polls — without this, checkTimeout only fires lazily
     * inside applyMove/resign/getGameState. Returns the concluded state,
     * or null if the game wasn't timed out (including if it wasn't
     * IN_PROGRESS to begin with).
     */
    @Transactional
    public GameStateDto applyTimeoutIfExpired(UUID gameId) {
        Game game = getGame(gameId);
        if (checkTimeout(game, Instant.now())) {
            return stateFor(game, null);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<MoveResponse> getMoveHistory(UUID gameId) {
        // Confirms the game exists (404s otherwise) before returning what
        // may legitimately be an empty list for a game with no moves yet.
        getGame(gameId);
        return moveRepository.findByGameIdOrderByMoveNumberAsc(gameId).stream()
                .map(MoveResponse::from)
                .toList();
    }

    @Transactional
    public GameStateDto applyMove(UUID gameId, UUID playerId, String sanMove) {
        Game game = getGame(gameId);
        Instant now = Instant.now();

        // Check the mover's own clock BEFORE validating/applying their move:
        // if they've already run out of time, the move never happened as
        // far as the server is concerned, regardless of what the client
        // thinks its local countdown says.
        if (checkTimeout(game, now)) {
            return stateFor(game, null);
        }
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw ApiException.conflict("This game has already ended");
        }
        if (!game.isPlayer(playerId)) {
            throw ApiException.forbidden("You are not a player in this game");
        }

        ChessRulesService.MoveResult before = chessRulesService.evaluate(game.getFenCurrent());
        boolean isWhiteToMove = "WHITE".equals(before.sideToMove());
        boolean callerIsWhite = game.getWhitePlayerId().equals(playerId);
        if (isWhiteToMove != callerIsWhite) {
            throw ApiException.badRequest("It's not your turn");
        }

        ChessRulesService.MoveResult result = chessRulesService.applyMove(game.getFenCurrent(), sanMove);

        long moveNumber = moveRepository.countByGameId(gameId) + 1;
        Move move = new Move(gameId, (int) moveNumber, playerId, sanMove, result.fen());
        moveRepository.save(move);

        game.setFenCurrent(result.fen());

        // Deduct the time the mover actually took from their stored clock,
        // then hand the clock to the opponent — this is the write half of
        // server-side enforcement (checkTimeout above is the read half).
        long elapsedMs = Duration.between(game.getLastMoveAt(), now).toMillis();
        if (callerIsWhite && game.getWhiteTimeMs() != null) {
            game.setWhiteTimeMs((int) Math.max(0, game.getWhiteTimeMs() - elapsedMs));
        } else if (!callerIsWhite && game.getBlackTimeMs() != null) {
            game.setBlackTimeMs((int) Math.max(0, game.getBlackTimeMs() - elapsedMs));
        }
        game.setLastMoveAt(now);

        eventPublisher.publishMoveMade(game, playerId, sanMove);

        if (result.isGameOver()) {
            concludeGame(game, callerIsWhite, result);
        } else {
            gameRepository.save(game);
        }

        List<Move> history = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
        String lastMoveSan = history.isEmpty() ? null : history.get(history.size() - 1).getMoveSan();

        return GameStateDto.of(game, result.sideToMove(), result.check(),
                result.checkmate(), result.stalemate(), lastMoveSan);
    }

    @Transactional
    public GameStateDto resign(UUID gameId, UUID playerId) {
        Game game = getGame(gameId);
        Instant now = Instant.now();

        // A resignation from a player who already flagged is moot — the
        // clock beats them to it, and the recorded reason should say TIMEOUT
        // rather than RESIGNATION.
        if (checkTimeout(game, now)) {
            return stateFor(game, null);
        }
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw ApiException.conflict("This game has already ended");
        }
        if (!game.isPlayer(playerId)) {
            throw ApiException.forbidden("You are not a player in this game");
        }

        boolean resigningPlayerIsWhite = game.getWhitePlayerId().equals(playerId);
        game.setStatus(resigningPlayerIsWhite ? GameStatus.BLACK_WON : GameStatus.WHITE_WON);
        game.setResultReason(ResultReason.RESIGNATION);
        game.setEndedAt(now);
        gameRepository.save(game);

        updateRatingsAfterGame(game);
        eventPublisher.publishGameEnded(game);

        return stateFor(game, null);
    }

    /** Marks the game finished based on the rules-engine result and records why. */
    private void concludeGame(Game game, boolean lastMoverWasWhite, ChessRulesService.MoveResult result) {
        if (result.checkmate()) {
            // The side to move now (after the move) is the one who got mated.
            game.setStatus(lastMoverWasWhite ? GameStatus.WHITE_WON : GameStatus.BLACK_WON);
            game.setResultReason(ResultReason.CHECKMATE);
        } else if (result.stalemate()) {
            game.setStatus(GameStatus.DRAW);
            game.setResultReason(ResultReason.STALEMATE);
        } else if (result.drawByRule()) {
            game.setStatus(GameStatus.DRAW);
            game.setResultReason(ResultReason.INSUFFICIENT_MATERIAL);
        }
        game.setEndedAt(java.time.Instant.now());
        gameRepository.save(game);

        updateRatingsAfterGame(game);
        eventPublisher.publishGameEnded(game);
    }

    private void updateRatingsAfterGame(Game game) {
        EloCalculatorService.Outcome whiteOutcome = switch (game.getStatus()) {
            case WHITE_WON -> EloCalculatorService.Outcome.WIN;
            case BLACK_WON -> EloCalculatorService.Outcome.LOSS;
            default -> EloCalculatorService.Outcome.DRAW; // DRAW or ABANDONED
        };

        int whiteRating = eloUpdateClient.fetchCurrentRating(game.getWhitePlayerId());
        int blackRating = eloUpdateClient.fetchCurrentRating(game.getBlackPlayerId());

        EloCalculatorService.EloResult updated = eloCalculatorService.recalculate(
                whiteRating, blackRating, whiteOutcome);

        String whiteResult = switch (whiteOutcome) {
            case WIN -> "WON";
            case LOSS -> "LOST";
            case DRAW -> "DRAWN";
        };
        String blackResult = switch (whiteOutcome) {
            case WIN -> "LOST";
            case LOSS -> "WON";
            case DRAW -> "DRAWN";
        };

        eloUpdateClient.updateRating(game.getWhitePlayerId(), updated.newWhiteRating(), whiteResult);
        eloUpdateClient.updateRating(game.getBlackPlayerId(), updated.newBlackRating(), blackResult);
    }

    /**
     * Parses a trailing "_<minutes>" from a time control string, e.g.
     * "blitz_5" → 5 minutes, "rapid_10" → 10 minutes. Falls back to
     * DEFAULT_TIME_MINUTES for anything that doesn't match rather than
     * rejecting the request — matchmaking-service (Session 5) is expected
     * to send well-formed values, but game-engine shouldn't hard-fail on a
     * format it doesn't recognize.
     */
    private int parseTimeControlMillis(String timeControl) {
        Matcher matcher = TIME_CONTROL_MINUTES.matcher(timeControl);
        int minutes = matcher.find() ? Integer.parseInt(matcher.group(1)) : DEFAULT_TIME_MINUTES;
        return minutes * 60 * 1000;
    }
}
