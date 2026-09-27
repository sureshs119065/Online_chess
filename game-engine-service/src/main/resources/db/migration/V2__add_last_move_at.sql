-- Backing column for server-side chess-clock enforcement (GameService.checkTimeout).
-- Existing in-progress rows get last_move_at = started_at as a reasonable
-- starting point (their clock "restarts" from now on rather than being
-- retroactively penalized for time already elapsed before this migration).
ALTER TABLE games ADD COLUMN last_move_at TIMESTAMPTZ;
UPDATE games SET last_move_at = started_at WHERE last_move_at IS NULL;
ALTER TABLE games ALTER COLUMN last_move_at SET NOT NULL;
ALTER TABLE games ALTER COLUMN last_move_at SET DEFAULT now();
