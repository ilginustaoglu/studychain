ALTER TABLE calendar_events
    ADD COLUMN IF NOT EXISTS google_event_id VARCHAR(512);

CREATE UNIQUE INDEX IF NOT EXISTS uk_calendar_events_user_google
    ON calendar_events (user_id, google_event_id)
    WHERE google_event_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS google_calendar_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    google_email VARCHAR(255),
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    token_expires_at TIMESTAMP WITH TIME ZONE,
    connected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
