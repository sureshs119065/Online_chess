-- Matches 01-DATABASE-SCHEMA.md. Foreign keys reference users(id), which is
-- owned by auth-service's own migration (V1__create_users_table.sql) -- both
-- point at the same physical Postgres database (your local install, or
-- Render's free Postgres once deployed), so this works as long as
-- auth-service's migration has already run at least once against that DB.
-- If you're setting up a brand-new database, run auth-service first.

CREATE TABLE IF NOT EXISTS games (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    white_player_id UUID NOT NULL REFERENCES users(id),
    black_player_id UUID NOT NULL REFERENCES users(id),
    fen_current     TEXT NOT NULL DEFAULT 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    status          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    time_control    VARCHAR(20) NOT NULL,
    white_time_ms   INTEGER,
    black_time_ms   INTEGER,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at        TIMESTAMPTZ,
    result_reason   VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS moves (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id         UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    move_number     INTEGER NOT NULL,
    player_id       UUID NOT NULL REFERENCES users(id),
    move_san        VARCHAR(10) NOT NULL,
    fen_after       TEXT NOT NULL,
    played_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_moves_game_id ON moves(game_id);
