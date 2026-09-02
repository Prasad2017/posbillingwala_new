<?php
/**
 * Copy to db_local.php on the server (db_local.php is gitignored).
 * Never commit real credentials to the repository.
 */
$dbHost = 'localhost';
$dbUser = 'your_db_username';
$dbPass = 'your_db_password';
$dbName = 'spllmgkn_posbill';

// Optional: absolute path to RSA private key for license payload signing (gitignored PEM).
// Default fallback: API/license_signing_private.pem
// $licenseSigningPrivateKeyPath = '/secure/path/license_signing_private.pem';

// Required for API/cron/expireLicenses.php (or set env CRON_SECRET).
// $cronSecret = 'change-me-to-a-long-random-string';

// Firebase Cloud Messaging (push notifications)
// Option A — HTTP v1 (recommended): download service account JSON from Firebase Console
// $fcmServiceAccountPath = '/secure/path/firebase-service-account.json';
// $fcmProjectId = 'your-firebase-project-id';
// Option B — legacy server key (deprecated by Google but still works)
// $fcmServerKey = 'your-fcm-server-key';
