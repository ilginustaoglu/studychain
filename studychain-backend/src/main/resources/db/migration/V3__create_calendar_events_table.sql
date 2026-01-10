CREATE TABLE IF NOT EXISTS calendar_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    color VARCHAR(20) DEFAULT '#6366f1'
);

CREATE INDEX IF NOT EXISTS idx_calendar_events_user_start ON calendar_events(user_id, start_time);

