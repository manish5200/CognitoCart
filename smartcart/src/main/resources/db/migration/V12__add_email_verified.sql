-- Tracks whether the user has verified their email address.
-- emailVerified = false → account restricted from checkout.
-- Existing users are marked verified so they don't get locked out on migration.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: treat all existing accounts as already verified
-- (they registered before this feature existed)
UPDATE users SET email_verified = TRUE;
