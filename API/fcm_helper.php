<?php
/**
 * Firebase Cloud Messaging — send helpers (HTTP v1 + legacy fallback).
 *
 * Project defaults come from API/fcm_project.php (matches WithTable google-services.json).
 *
 * Configure in API/db_local.php (optional overrides):
 *   $fcmServiceAccountPath = '/path/to/firebase-service-account.json';  // HTTP v1 (recommended)
 *   $fcmProjectId = 'pos-billingwala';
 *   $fcmServerKey = '...';  // legacy (optional)
 *
 * Or env: FCM_SERVICE_ACCOUNT_PATH, FCM_PROJECT_ID, FCM_SERVER_KEY
 */

require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/fcm_project.php';

if (!function_exists('fcm_config_value')) {
    function fcm_config_value($varName, $envName, $default = '')
    {
        global $$varName;
        if (isset($$varName) && is_string($$varName) && $$varName !== '') {
            return $$varName;
        }
        $env = getenv($envName);
        if (is_string($env) && $env !== '') {
            return $env;
        }
        return $default;
    }
}

if (!function_exists('fcm_resolved_project_id')) {
    function fcm_resolved_project_id($serviceAccount = null)
    {
        $projectId = fcm_config_value('fcmProjectId', 'FCM_PROJECT_ID', FCM_DEFAULT_PROJECT_ID);
        if ($projectId === '' && is_array($serviceAccount) && isset($serviceAccount['project_id'])) {
            $projectId = (string) $serviceAccount['project_id'];
        }
        if ($projectId === '') {
            $projectId = FCM_DEFAULT_PROJECT_ID;
        }
        return $projectId;
    }
}

if (!function_exists('fcm_is_configured')) {
    /**
     * @return array{ok:bool,mode:string,projectId:string,message:string}
     */
    function fcm_is_configured()
    {
        $sa = fcm_load_service_account();
        $projectId = fcm_resolved_project_id($sa);
        if ($sa !== null && fcm_get_v1_access_token($sa) !== null && $projectId !== '') {
            return array(
                'ok' => true,
                'mode' => 'http_v1',
                'projectId' => $projectId,
                'message' => 'FCM HTTP v1 ready for project ' . $projectId,
            );
        }
        $serverKey = fcm_config_value('fcmServerKey', 'FCM_SERVER_KEY');
        if ($serverKey !== '') {
            return array(
                'ok' => true,
                'mode' => 'legacy',
                'projectId' => $projectId,
                'message' => 'FCM legacy server key ready (project ' . $projectId . ')',
            );
        }
        return array(
            'ok' => false,
            'mode' => 'none',
            'projectId' => $projectId,
            'message' => 'FCM not configured. Add API/firebase-service-account.json for project pos-billingwala (same as google-services.json).',
        );
    }
}

if (!function_exists('fcm_base64url_encode')) {
    function fcm_base64url_encode($data)
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }
}

if (!function_exists('fcm_load_service_account')) {
    function fcm_load_service_account()
    {
        $path = fcm_config_value('fcmServiceAccountPath', 'FCM_SERVICE_ACCOUNT_PATH');
        if ($path === '' || !is_readable($path)) {
            $local = defined('FCM_DEFAULT_SERVICE_ACCOUNT_FILE')
                ? FCM_DEFAULT_SERVICE_ACCOUNT_FILE
                : (__DIR__ . '/firebase-service-account.json');
            if (is_readable($local)) {
                $path = $local;
            } else {
                return null;
            }
        }
        $json = file_get_contents($path);
        if ($json === false) {
            return null;
        }
        $data = json_decode($json, true);
        return is_array($data) ? $data : null;
    }
}

if (!function_exists('fcm_get_v1_access_token')) {
    function fcm_get_v1_access_token(array $serviceAccount)
    {
        static $cachedToken = null;
        static $cachedUntil = 0;

        if ($cachedToken !== null && time() < ($cachedUntil - 60)) {
            return $cachedToken;
        }

        if (!isset($serviceAccount['client_email'], $serviceAccount['private_key'])) {
            return null;
        }

        $now = time();
        $header = fcm_base64url_encode(json_encode(array('alg' => 'RS256', 'typ' => 'JWT')));
        $claimSet = array(
            'iss' => $serviceAccount['client_email'],
            'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
            'aud' => 'https://oauth2.googleapis.com/token',
            'iat' => $now,
            'exp' => $now + 3600,
        );
        $claim = fcm_base64url_encode(json_encode($claimSet));
        $signInput = $header . '.' . $claim;

        $privateKey = openssl_pkey_get_private($serviceAccount['private_key']);
        if ($privateKey === false) {
            return null;
        }
        $signature = '';
        if (!openssl_sign($signInput, $signature, $privateKey, OPENSSL_ALGO_SHA256)) {
            return null;
        }
        $jwt = $signInput . '.' . fcm_base64url_encode($signature);

        $ch = curl_init('https://oauth2.googleapis.com/token');
        curl_setopt_array($ch, array(
            CURLOPT_POST => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => array('Content-Type: application/x-www-form-urlencoded'),
            CURLOPT_POSTFIELDS => http_build_query(array(
                'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
                'assertion' => $jwt,
            )),
            CURLOPT_TIMEOUT => 20,
        ));
        $response = curl_exec($ch);
        $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($response === false || $code < 200 || $code >= 300) {
            return null;
        }
        $decoded = json_decode($response, true);
        if (!is_array($decoded) || empty($decoded['access_token'])) {
            return null;
        }

        $cachedToken = $decoded['access_token'];
        $cachedUntil = $now + (isset($decoded['expires_in']) ? (int) $decoded['expires_in'] : 3600);
        return $cachedToken;
    }
}

if (!function_exists('fcm_stringify_data')) {
    function fcm_stringify_data(array $data)
    {
        $out = array();
        foreach ($data as $key => $value) {
            if ($value === null) {
                continue;
            }
            $out[(string) $key] = is_scalar($value) ? (string) $value : json_encode($value);
        }
        return $out;
    }
}

if (!function_exists('fcm_send_data_only')) {
    /**
     * Data-only high-priority FCM (no notification tray) for silent POS handling.
     * @return array ['ok'=>bool, 'message'=>string, 'invalid_token'=>bool]
     */
    function fcm_send_data_only($token, array $data = array())
    {
        $token = trim((string) $token);
        if ($token === '') {
            return array('ok' => false, 'message' => 'Empty token', 'invalid_token' => false);
        }

        $data = fcm_stringify_data($data);
        $serviceAccount = fcm_load_service_account();
        if ($serviceAccount !== null) {
            $accessToken = fcm_get_v1_access_token($serviceAccount);
            $projectId = fcm_resolved_project_id($serviceAccount);
            if ($accessToken !== null && $projectId !== '') {
                $message = array(
                    'message' => array(
                        'token' => $token,
                        'data' => $data,
                        'android' => array(
                            'priority' => 'HIGH',
                        ),
                    ),
                );

                $url = 'https://fcm.googleapis.com/v1/projects/' . rawurlencode($projectId) . '/messages:send';
                $ch = curl_init($url);
                curl_setopt_array($ch, array(
                    CURLOPT_POST => true,
                    CURLOPT_RETURNTRANSFER => true,
                    CURLOPT_HTTPHEADER => array(
                        'Authorization: Bearer ' . $accessToken,
                        'Content-Type: application/json; charset=UTF-8',
                    ),
                    CURLOPT_POSTFIELDS => json_encode($message),
                    CURLOPT_TIMEOUT => 20,
                ));
                $response = curl_exec($ch);
                $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
                curl_close($ch);

                if ($response !== false && $code >= 200 && $code < 300) {
                    return array('ok' => true, 'message' => 'Sent', 'invalid_token' => false);
                }

                $invalid = stripos((string) $response, 'NOT_FOUND') !== false
                    || stripos((string) $response, 'UNREGISTERED') !== false
                    || stripos((string) $response, 'INVALID_ARGUMENT') !== false;
                return array(
                    'ok' => false,
                    'message' => 'FCM v1 error HTTP ' . $code . ': ' . substr((string) $response, 0, 240),
                    'invalid_token' => $invalid,
                );
            }
        }

        $serverKey = fcm_config_value('fcmServerKey', 'FCM_SERVER_KEY');
        if ($serverKey === '') {
            return array('ok' => false, 'message' => 'FCM not configured', 'invalid_token' => false);
        }

        $payload = array(
            'to' => $token,
            'priority' => 'high',
            'content_available' => true,
            'data' => $data,
        );

        $ch = curl_init('https://fcm.googleapis.com/fcm/send');
        curl_setopt_array($ch, array(
            CURLOPT_POST => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => array(
                'Authorization: key=' . $serverKey,
                'Content-Type: application/json',
            ),
            CURLOPT_POSTFIELDS => json_encode($payload),
            CURLOPT_TIMEOUT => 20,
        ));
        $response = curl_exec($ch);
        $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($response !== false && $code >= 200 && $code < 300) {
            return array('ok' => true, 'message' => 'Sent', 'invalid_token' => false);
        }
        $invalid = stripos((string) $response, 'NotRegistered') !== false
            || stripos((string) $response, 'InvalidRegistration') !== false;
        return array(
            'ok' => false,
            'message' => 'FCM legacy error HTTP ' . $code,
            'invalid_token' => $invalid,
        );
    }
}

if (!function_exists('fcm_send_to_token')) {
    /**
     * @return array ['ok'=>bool, 'message'=>string, 'invalid_token'=>bool]
     */
    function fcm_send_to_token($token, $title, $body, array $data = array())
    {
        $token = trim((string) $token);
        if ($token === '') {
            return array('ok' => false, 'message' => 'Empty token', 'invalid_token' => false);
        }

        $data = fcm_stringify_data($data);
        $serviceAccount = fcm_load_service_account();
        if ($serviceAccount !== null) {
            $accessToken = fcm_get_v1_access_token($serviceAccount);
            $projectId = fcm_resolved_project_id($serviceAccount);
            if ($accessToken !== null && $projectId !== '') {
                $message = array(
                    'message' => array(
                        'token' => $token,
                        'notification' => array(
                            'title' => (string) $title,
                            'body' => (string) $body,
                        ),
                        'data' => $data,
                        'android' => array(
                            'priority' => 'HIGH',
                            'notification' => array(
                                'channel_id' => 'pos_push_alerts',
                                'sound' => 'default',
                                'default_vibrate_timings' => true,
                            ),
                        ),
                    ),
                );

                $url = 'https://fcm.googleapis.com/v1/projects/' . rawurlencode($projectId) . '/messages:send';
                $ch = curl_init($url);
                curl_setopt_array($ch, array(
                    CURLOPT_POST => true,
                    CURLOPT_RETURNTRANSFER => true,
                    CURLOPT_HTTPHEADER => array(
                        'Authorization: Bearer ' . $accessToken,
                        'Content-Type: application/json; charset=UTF-8',
                    ),
                    CURLOPT_POSTFIELDS => json_encode($message),
                    CURLOPT_TIMEOUT => 20,
                ));
                $response = curl_exec($ch);
                $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
                curl_close($ch);

                if ($response !== false && $code >= 200 && $code < 300) {
                    return array('ok' => true, 'message' => 'Sent', 'invalid_token' => false);
                }

                $invalid = stripos((string) $response, 'NOT_FOUND') !== false
                    || stripos((string) $response, 'UNREGISTERED') !== false
                    || stripos((string) $response, 'INVALID_ARGUMENT') !== false;
                return array(
                    'ok' => false,
                    'message' => 'FCM v1 error HTTP ' . $code . ': ' . substr((string) $response, 0, 240),
                    'invalid_token' => $invalid,
                );
            }
        }

        $serverKey = fcm_config_value('fcmServerKey', 'FCM_SERVER_KEY');
        if ($serverKey === '') {
            return array('ok' => false, 'message' => 'FCM not configured', 'invalid_token' => false);
        }

        $payload = array(
            'to' => $token,
            'priority' => 'high',
            'notification' => array(
                'title' => (string) $title,
                'body' => (string) $body,
                'sound' => 'default',
            ),
            'data' => $data,
        );

        $ch = curl_init('https://fcm.googleapis.com/fcm/send');
        curl_setopt_array($ch, array(
            CURLOPT_POST => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => array(
                'Authorization: key=' . $serverKey,
                'Content-Type: application/json',
            ),
            CURLOPT_POSTFIELDS => json_encode($payload),
            CURLOPT_TIMEOUT => 20,
        ));
        $response = curl_exec($ch);
        $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($response === false || $code < 200 || $code >= 300) {
            return array('ok' => false, 'message' => 'FCM legacy HTTP ' . $code, 'invalid_token' => false);
        }

        $decoded = json_decode($response, true);
        $failure = is_array($decoded) && isset($decoded['failure']) ? (int) $decoded['failure'] : 0;
        if ($failure > 0) {
            $invalid = false;
            if (isset($decoded['results'][0]['error'])) {
                $err = (string) $decoded['results'][0]['error'];
                $invalid = stripos($err, 'NotRegistered') !== false || stripos($err, 'InvalidRegistration') !== false;
            }
            return array('ok' => false, 'message' => 'FCM legacy failure', 'invalid_token' => $invalid);
        }

        return array('ok' => true, 'message' => 'Sent', 'invalid_token' => false);
    }
}

if (!function_exists('fcm_clear_invalid_token')) {
    function fcm_clear_invalid_token($con, $licenseId)
    {
        db_stmt_execute(
            $con,
            "UPDATE `licenses` SET `fcm_token`=NULL, `fcm_token_updated_at`=NULL WHERE `id`=?",
            'i',
            (int) $licenseId
        );
    }
}

if (!function_exists('fcm_log_sent')) {
    function fcm_log_sent($con, $licenseId, $notificationType, $notificationKey, $sentDate)
    {
        db_stmt_execute(
            $con,
            "INSERT IGNORE INTO `push_notification_log`
             (`license_id`, `notification_type`, `notification_key`, `sent_date`)
             VALUES (?,?,?,?)",
            'isss',
            (int) $licenseId,
            (string) $notificationType,
            (string) $notificationKey,
            (string) $sentDate
        );
    }
}

if (!function_exists('fcm_already_sent_today')) {
    function fcm_already_sent_today($con, $licenseId, $notificationType, $notificationKey, $sentDate)
    {
        $row = db_stmt_fetch_one(
            $con,
            "SELECT `id` FROM `push_notification_log`
             WHERE `license_id`=? AND `notification_type`=? AND `notification_key`=? AND `sent_date`=?
             LIMIT 1",
            'isss',
            (int) $licenseId,
            (string) $notificationType,
            (string) $notificationKey,
            (string) $sentDate
        );
        return $row !== null;
    }
}

if (!function_exists('fcm_send_license_expiring')) {
    function fcm_send_license_expiring($con, array $licenseRow, $daysLeft, $today)
    {
        $token = isset($licenseRow['fcm_token']) ? trim((string) $licenseRow['fcm_token']) : '';
        if ($token === '') {
            return array('ok' => false, 'skipped' => true, 'message' => 'No token');
        }

        $licenseId = (int) $licenseRow['id'];
        $notificationType = 'license_expiring';
        $notificationKey = 'daily';

        if (fcm_already_sent_today($con, $licenseId, $notificationType, $notificationKey, $today)) {
            return array('ok' => true, 'skipped' => true, 'message' => 'Already sent today');
        }

        $daysLeft = max(0, (int) $daysLeft);
        if ($daysLeft === 0) {
            $title = 'Licence expires today';
            $body = 'Your POS Billingwala licence expires today. Renew now to avoid interruption.';
        } elseif ($daysLeft === 1) {
            $title = 'Licence expires tomorrow';
            $body = 'Your POS Billingwala licence expires tomorrow. Please renew to keep billing uninterrupted.';
        } else {
            $title = 'Licence expiring soon';
            $body = 'Your POS Billingwala licence expires in ' . $daysLeft . ' days. Please renew soon.';
        }

        $data = array(
            'type' => 'license_expiring',
            'title' => $title,
            'body' => $body,
            'message' => $body,
            'days_left' => (string) $daysLeft,
            'expiry_date' => isset($licenseRow['expiryDate']) ? (string) $licenseRow['expiryDate'] : '',
        );

        $result = fcm_send_to_token($token, $title, $body, $data);
        if ($result['ok']) {
            fcm_log_sent($con, $licenseId, $notificationType, $notificationKey, $today);
        } elseif (!empty($result['invalid_token'])) {
            fcm_clear_invalid_token($con, $licenseId);
        }
        return $result;
    }
}

if (!function_exists('fcm_send_promotional')) {
    function fcm_send_promotional($con, array $licenseRow, $title, $body, array $extraData = array())
    {
        $token = isset($licenseRow['fcm_token']) ? trim((string) $licenseRow['fcm_token']) : '';
        if ($token === '') {
            return array('ok' => false, 'skipped' => true, 'message' => 'No token');
        }

        // Include title/body in data so POS can show tray when app is in foreground
        // (notification payload alone is handled by system when app is backgrounded).
        $data = array_merge(array(
            'type' => 'promotional',
            'title' => (string) $title,
            'body' => (string) $body,
            'message' => (string) $body,
        ), $extraData);
        $result = fcm_send_to_token($token, $title, $body, $data);
        if (!$result['ok'] && !empty($result['invalid_token'])) {
            fcm_clear_invalid_token($con, (int) $licenseRow['id']);
        }
        return $result;
    }
}

if (!function_exists('fcm_notify_expiring_licenses')) {
    /**
     * Send once per day to licences expiring within $daysBefore days (inclusive).
     *
     * @return array stats
     */
    function fcm_notify_expiring_licenses($con, $daysBefore = 3)
    {
        require_once __DIR__ . '/licence_expiry.php';
        require_once __DIR__ . '/fcm_tables.php';
        fcm_ensure_schema($con);

        $today = licence_today();
        $daysBefore = max(1, (int) $daysBefore);
        $endDate = date('Y-m-d', strtotime($today . ' +' . $daysBefore . ' days'));

        $stats = array(
            'today' => $today,
            'daysBefore' => (string) $daysBefore,
            'candidates' => '0',
            'sent' => '0',
            'skipped' => '0',
            'failed' => '0',
        );

        $sql = "SELECT `id`, `expiryDate`, `fcm_token`, `licenseStatus`, `userName`
                FROM `licenses`
                WHERE `fcm_token` IS NOT NULL AND TRIM(`fcm_token`) <> ''
                  AND LOWER(IFNULL(`licenseStatus`,'')) = 'active'
                  AND `expiryDate` IS NOT NULL AND `expiryDate` <> ''
                  AND `expiryDate` >= ? AND `expiryDate` <= ?";

        $stmt = mysqli_prepare($con, $sql);
        if (!$stmt) {
            return $stats;
        }
        mysqli_stmt_bind_param($stmt, 'ss', $today, $endDate);
        mysqli_stmt_execute($stmt);
        $result = mysqli_stmt_get_result($stmt);

        $candidates = 0;
        $sent = 0;
        $skipped = 0;
        $failed = 0;

        if ($result) {
            while ($row = mysqli_fetch_assoc($result)) {
                $candidates++;
                $expiryTs = strtotime((string) $row['expiryDate']);
                $todayTs = strtotime($today);
                $daysLeft = (int) floor(($expiryTs - $todayTs) / 86400);
                if ($daysLeft < 0 || $daysLeft > $daysBefore) {
                    $skipped++;
                    continue;
                }

                $push = fcm_send_license_expiring($con, $row, $daysLeft, $today);
                if (!empty($push['skipped'])) {
                    $skipped++;
                } elseif (!empty($push['ok'])) {
                    $sent++;
                } else {
                    $failed++;
                }
            }
        }
        mysqli_stmt_close($stmt);

        $stats['candidates'] = (string) $candidates;
        $stats['sent'] = (string) $sent;
        $stats['skipped'] = (string) $skipped;
        $stats['failed'] = (string) $failed;
        return $stats;
    }
}

require_once __DIR__ . '/fcm_broadcast.php';
