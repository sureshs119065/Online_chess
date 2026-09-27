package com.chess.game.controller;

import com.chess.game.dto.CreateGameRequest;
import com.chess.game.dto.GameStateDto;
import com.chess.game.dto.MoveResponse;
import com.chess.game.dto.ResignRequest;
import com.chess.game.model.Game;
import com.chess.game.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /** Called by matchmaking-service once it pairs two players (Session 5). */
    @PostMapping
    public ResponseEntity<GameStateDto> createGame(@Valid @RequestBody CreateGameRequest request) {
        Game game = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.getGameState(game.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameStateDto> getGame(@PathVariable UUID id) {
        return ResponseEntity.ok(gameService.getGameState(id));
    }

    @GetMapping("/{id}/moves")
    public ResponseEntity<List<MoveResponse>> getMoves(@PathVariable UUID id) {
        return ResponseEntity.ok(gameService.getMoveHistory(id));
    }

    @PostMapping("/{id}/resign")
    public ResponseEntity<GameStateDto> resign(@PathVariable UUID id, @Valid @RequestBody ResignRequest request) {
        return ResponseEntity.ok(gameService.resign(id, request.playerId()));
    }

    // Note: there is deliberately no POST /api/games/{id}/moves REST endpoint.
    // Moves are submitted live over the WebSocket at /ws/game/{id} — see
    // GameWebSocketHandler — since a REST round-trip per move doesn't fit a
    // "live game" experience the way a persistent connection does.
}
