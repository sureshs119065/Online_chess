package com.chess.game.service;

import com.chess.common.exception.ApiException;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around bhlangonijr/chesslib so the rest of the codebase
 * never touches the library directly — if the rules engine is ever swapped
 * out, only this class changes.
 *
 * chesslib's Board.doMove(String) accepts a move in Standard Algebraic
 * Notation, validates it against the current legal-move set, and applies it
 * atomically — it throws an unchecked exception (subclass of
 * MoveConversionException) if the SAN is malformed or the move is illegal
 * in the current position, which we translate into a 400 ApiException.
 */
@Service
public class ChessRulesService {

    /**
     * Applies {@code sanMove} to the position described by {@code fen} and
     * returns the resulting state. Does not mutate any shared state — a
     * fresh Board is loaded from the given FEN on every call, so this is
     * safe to call concurrently for different games.
     *
     * @throws ApiException (400) if the move is illegal or malformed
     */
    public MoveResult applyMove(String fen, String sanMove) {
        Board board = new Board();
        board.loadFromFen(fen);

        try {
            board.doMove(sanMove);
        } catch (RuntimeException ex) {
            // Covers chesslib's MoveConversionException / MoveException /
            // IllegalArgumentException for malformed or illegal SAN input.
            throw ApiException.badRequest("Illegal move: " + sanMove);
        }

        return evaluate(board);
    }

    /** Read-only evaluation of a position — no move applied. Used for GET endpoints. */
    public MoveResult evaluate(String fen) {
        Board board = new Board();
        board.loadFromFen(fen);
        return evaluate(board);
    }

    private MoveResult evaluate(Board board) {
        boolean checkmate = board.isMated();
        boolean stalemate = board.isStaleMate();
        boolean check = !checkmate && board.isKingAttacked();
        // isDraw() covers threefold repetition, the 50-move rule, and
        // insufficient material, but NOT stalemate — check both explicitly.
        boolean drawByRule = board.isDraw() || board.isInsufficientMaterial();

        String sideToMove = board.getSideToMove() == Side.WHITE ? "WHITE" : "BLACK";

        return new MoveResult(board.getFen(), sideToMove, check, checkmate, stalemate, drawByRule);
    }

    /**
     * @param fen         resulting position after the move (or the position evaluated)
     * @param sideToMove  "WHITE" or "BLACK" — whoever moves next
     * @param check       true if sideToMove is currently in check (and not mated)
     * @param checkmate   true if sideToMove has no legal moves and is in check
     * @param stalemate   true if sideToMove has no legal moves and is NOT in check
     * @param drawByRule  true for threefold repetition, 50-move rule, or insufficient material
     */
    public record MoveResult(
            String fen,
            String sideToMove,
            boolean check,
            boolean checkmate,
            boolean stalemate,
            boolean drawByRule
    ) {
        public boolean isGameOver() {
            return checkmate || stalemate || drawByRule;
        }
    }
}
