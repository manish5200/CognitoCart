-- V23: Prepare Users table for OAuth2 Registration

-- 1. Remove NOT NULL constraint from password (Google users have no local password)
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- 2. Remove NOT NULL constraint from phone (Google does not provide phone)
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;

-- 3. Add auth_provider tracking (Defaults to 'LOCAL' for existing rows)
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(50) DEFAULT 'LOCAL' NOT NULL;
