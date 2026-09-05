<?php
/**
 * Firebase Cloud Messaging — project defaults for POS Billingwala.
 *
 * Must match WithTable/app/google-services.json:
 *   project_id: pos-billingwala
 *   project_number: 855823167459
 *
 * Server send setup (pick one):
 *   A) Place Firebase service-account JSON at:
 *        API/firebase-service-account.json
 *      (Firebase Console → Project settings → Service accounts → Generate new private key)
 *   B) Or set in db_local.php:
 *        $fcmServiceAccountPath = '/secure/path/firebase-service-account.json';
 *        $fcmProjectId = 'pos-billingwala';
 *   C) Legacy (optional):
 *        $fcmServerKey = '...';
 *
 * Same project is used for:
 *   - Admin promotional push
 *   - Licence expiry reminders
 *   - Mess Common QR token realtime (data-only)
 */

if (!defined('FCM_DEFAULT_PROJECT_ID')) {
    define('FCM_DEFAULT_PROJECT_ID', 'pos-billingwala');
}

if (!defined('FCM_DEFAULT_PROJECT_NUMBER')) {
    define('FCM_DEFAULT_PROJECT_NUMBER', '855823167459');
}

if (!defined('FCM_DEFAULT_SERVICE_ACCOUNT_FILE')) {
    define('FCM_DEFAULT_SERVICE_ACCOUNT_FILE', __DIR__ . '/firebase-service-account.json');
}
