package com.chess.game.repository;

import com.chess.game.model.Move;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MoveRepository extends JpaRepository<Move, UUID> {

    List<Move> findByGameIdOrderByMoveNumberAsc(UUID gameId);

    long countByGameId(UUID gameId);
}
