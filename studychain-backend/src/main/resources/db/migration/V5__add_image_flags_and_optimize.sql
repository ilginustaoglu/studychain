-- Add boolean flags to avoid loading byte arrays unnecessarily
ALTER TABLE users ADD COLUMN IF NOT EXISTS has_profile_image BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS has_cover_image BOOLEAN DEFAULT FALSE;

-- Update existing records based on current data
UPDATE users SET has_profile_image = TRUE WHERE profile_image IS NOT NULL AND LENGTH(profile_image) > 0;
UPDATE users SET has_cover_image = TRUE WHERE cover_image IS NOT NULL AND LENGTH(cover_image) > 0;
