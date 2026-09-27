package com.chess.game.service;

import org.springframework.stereotype.Service;

/**
 * Standard ELO rating update, K=32 (common choice for non-professional/
 * online play — FIDE uses variable K based on rating/age, but a flat K is
 * plenty for a portfolio project).
 */
@Service
public class EloCalculatorService {

    private static final int K_FACTOR = 32;

    public enum Outcome {
        WIN(1.0),
        LOSS(0.0),
        DRAW(0.5);

        final double score;

        Outcome(double score) {
            this.score = score;
        }
    }

    /** Result of recalculating both players' ratings after one game. */
    public record EloResult(int newWhiteRating, int newBlackRating) {
    }

    /**
     * @param whiteRating   white's rating before this game
     * @param blackRating   black's rating before this game
     * @param whiteOutcome  WIN/LOSS/DRAW from white's perspective (black's is the inverse)
     */
    public EloResult recalculate(int whiteRating, int blackRating, Outcome whiteOutcome) {
        double expectedWhite = expectedScore(whiteRating, blackRating);
        double expectedBlack = expectedScore(blackRating, whiteRating);

        double blackScore = 1.0 - whiteOutcome.score;

        int newWhite = Math.round((float) (whiteRating + K_FACTOR * (whiteOutcome.score - expectedWhite)));
        int newBlack = Math.round((float) (blackRating + K_FACTOR * (blackScore - expectedBlack)));

        return new EloResult(newWhite, newBlack);
    }

    private double expectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }
}
