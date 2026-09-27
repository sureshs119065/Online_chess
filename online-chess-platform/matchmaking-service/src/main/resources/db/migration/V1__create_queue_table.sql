-- Matches 01-DATABASE-SCHEMA.md. References users(id), so run auth-service's
-- migration at least once first against a fresh Supabase project.

CREATE TABLE IF NOT EXISTS matchmaking_queue (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    elo_rating      INTEGER NOT NULL,
    time_control    VARCHAR(20) NOT NULL,
    queued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    status          VARCHAR(20) NOT NULL DEFAULT 'WAITING'
);

CREATE INDEX IF NOT EXISTS idx_queue_status_time_control ON matchmaking_queue(status, time_control);
CREATE INDEX IF NOT EXISTS idx_queue_user_id ON matchmaking_queue(user_id);
