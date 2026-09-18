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

// Cron HTTP auth (optional). Default: plain URL cron works (no secret required).
// To harden expireLicenses / notifyExpiringLicenses:
//   $cronRequireSecret = true;
//   $cronSecret = 'change-me-to-a-long-random-string';
// Then call with ?secret=… or header X-Cron-Secret.
// $cronRequireSecret = false;
// $cronSecret = 'change-me-to-a-long-random-string';

// API Bearer tokens (optional). Default: APIs work without Authorization.
// Set true to require a login-issued Bearer token on protected endpoints.
// $db_local = true;
// $requireAuthToken = true;

// ---------------------------------------------------------------------------
// Firebase Cloud Messaging
// MUST match WithTable/app/google-services.json:
//   project_id     = pos-billingwala
//   project_number = 855823167459
//
// Setup (recommended HTTP v1):
//   1. Open Firebase Console → project "pos-billingwala"
//   2. Project settings → Service accounts → Generate new private key
//   3. Save JSON as API/firebase-service-account.json on the server
//   4. Keep $fcmProjectId = 'pos-billingwala'
//
// Same project delivers:
//   - Admin "Send Push" promotional notifications → POS
//   - Licence expiry reminders → POS
//   - Mess Common QR token events → POS (silent data push)
// ---------------------------------------------------------------------------
$fcmProjectId = 'pos-billingwala';
$fcmServiceAccountPath = __DIR__ . '/firebase-service-account.json';
// Web Push VAPID public key (Firebase → Cloud Messaging → Web Push certificates).
// Used only by browser clients: getToken(messaging, { vapidKey: '...' }).
// Not required for Android FCM send (service account handles that).
// $fcmWebPushVapidKey = 'B….';
// Legacy (optional, deprecated by Google):
// $fcmServerKey = 'your-fcm-server-key';
