-- Matches 01-DATABASE-SCHEMA.md. References games(id) and users(id), so
-- run auth-service's and game-engine-service's migrations at least once
-- first against a fresh Supabase project.

CREATE TABLE IF NOT EXISTS chat_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id         UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users(id),
    message         VARCHAR(500) NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_game_id ON chat_messages(game_id);
