-- ==========================================
-- V2: Migrate KycStatus enum values
-- ==========================================
-- Old states: PENDING, ACTIVE, INACTIVE, VERIFIED, IN_PROGRESS
-- New states:  PENDING, IN_REVIEW, VERIFIED, REJECTED, SUSPENDED
-- ==========================================

-- ACTIVE was used as "approved/selling" → maps to VERIFIED
UPDATE seller_profiles
SET kyc_status = 'VERIFIED'
WHERE kyc_status = 'ACTIVE';

-- INACTIVE was used as "blocked/disabled" → maps to SUSPENDED
UPDATE seller_profiles
SET kyc_status = 'SUSPENDED'
WHERE kyc_status = 'INACTIVE';

-- IN_PROGRESS was used as "under review" → maps to IN_REVIEW
UPDATE seller_profiles
SET kyc_status = 'IN_REVIEW'
WHERE kyc_status = 'IN_PROGRESS';

-- PENDING and VERIFIED are unchanged.
-- REJECTED is a new state with no existing rows.
