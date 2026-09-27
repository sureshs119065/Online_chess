package com.chess.game.repository;

import com.chess.game.model.Game;
import com.chess.game.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    /** Used by GameTimeoutScheduler's periodic sweep for flag-falls. */
    List<Game> findByStatus(GameStatus status);
}
