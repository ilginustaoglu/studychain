ALTER TABLE calendar_events
    ADD COLUMN IF NOT EXISTS google_calendar_id VARCHAR(512);
