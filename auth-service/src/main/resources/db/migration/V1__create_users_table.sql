-- Matches 01-DATABASE-SCHEMA.md exactly. gen_random_uuid() has been a
-- built-in PostgreSQL core function since version 13 (no pgcrypto
-- extension needed) -- both a freshly installed local Postgres and
-- Render's free Postgres are well past that, so this works with no setup
-- beyond creating the database itself.

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    elo_rating      INTEGER NOT NULL DEFAULT 1200,
    games_played    INTEGER NOT NULL DEFAULT 0,
    games_won       INTEGER NOT NULL DEFAULT 0,
    games_lost      INTEGER NOT NULL DEFAULT 0,
    games_drawn     INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
