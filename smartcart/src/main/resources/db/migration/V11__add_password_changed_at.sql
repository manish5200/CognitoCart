-- Tracks when the password was last changed.
-- JwtFilter compares this against the token's issuedAt claim.
-- Any token issued BEFORE this timestamp is automatically invalidated.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;
