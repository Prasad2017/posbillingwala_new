-- P52: Normalize legacy licenseStatus 'expire' → 'expired'
-- Canonical value matches admin UI / CustomerController / licence_expiry.php writers.
-- Safe to run more than once (no-op when no 'expire' rows remain).

UPDATE `licenses`
SET `licenseStatus` = 'expired'
WHERE LOWER(IFNULL(`licenseStatus`, '')) = 'expire';

SELECT ROW_COUNT() AS licenses_normalized_expire_to_expired;
