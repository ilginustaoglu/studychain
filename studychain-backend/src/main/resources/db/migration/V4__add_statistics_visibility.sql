ALTER TABLE users ADD COLUMN IF NOT EXISTS statistics_visibility VARCHAR(20) DEFAULT 'everyone';
