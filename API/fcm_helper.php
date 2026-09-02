<?php
/**
 * Firebase Cloud Messaging — send helpers (HTTP v1 + legacy fallback).
 *
 * Configure in API/db_local.php:
 *   $fcmServerKey = '...';  // legacy (optional)
 *   $fcmServiceAccountPath = '/path/to/firebase-service-account.json';  // HTTP v1 (recommended)
 *   $fcmProjectId = 'your-firebase-project-id';
 *
 * Or env: FCM_SERVER_KEY, FCM_SERVICE_ACCOUNT_PATH, FCM_PROJECT_ID
 */

require_once __DIR__ . '/db_prepared.php';

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
            $local = __DIR__ . '/firebase-service-account.json';
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
            $projectId = fcm_config_value('fcmProjectId', 'FCM_PROJECT_ID');
            if ($projectId === '' && isset($serviceAccount['project_id'])) {
                $projectId = (string) $serviceAccount['project_id'];
            }
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

        $data = array_merge(array('type' => 'promotional'), $extraData);
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

if (!function_exists('fcm_broadcast_promotional')) {
    /**
     * @param string $target all|active|license_ids
     * @param string $licenseIdsCsv comma-separated licence ids when target=license_ids
     * @return array
     */
    function fcm_broadcast_promotional($con, $title, $body, $target = 'active', $licenseIdsCsv = '', array $extraData = array())
    {
        require_once __DIR__ . '/licence_expiry.php';
        require_once __DIR__ . '/fcm_tables.php';
        fcm_ensure_schema($con);

        $title = trim((string) $title);
        $body = trim((string) $body);
        if ($title === '' || $body === '') {
            return array('status' => '0', 'message' => 'Title and message are required', 'sent' => '0', 'failed' => '0', 'skipped' => '0');
        }

        $target = strtolower(trim((string) $target));
        $today = licence_today();
        $params = array();
        $types = '';
        $sql = "SELECT `id`, `fcm_token`, `expiryDate`, `licenseStatus`, `userName`
                FROM `licenses`
                WHERE `fcm_token` IS NOT NULL AND TRIM(`fcm_token`) <> ''";

        if ($target === 'license_ids') {
            $ids = array_filter(array_map('trim', explode(',', (string) $licenseIdsCsv)));
            if (empty($ids)) {
                return array('status' => '0', 'message' => 'No licence ids provided', 'sent' => '0', 'failed' => '0', 'skipped' => '0');
            }
            $placeholders = implode(',', array_fill(0, count($ids), '?'));
            $sql .= " AND `id` IN ($placeholders)";
            foreach ($ids as $id) {
                $types .= 'i';
                $params[] = (int) $id;
            }
        } elseif ($target === 'active') {
            $sql .= " AND LOWER(IFNULL(`licenseStatus`,'')) = 'active'
                      AND (`expiryDate` IS NULL OR `expiryDate` = '' OR `expiryDate` >= ?)";
            $types .= 's';
            $params[] = $today;
        }

        $sent = 0;
        $failed = 0;
        $skipped = 0;

        $stmt = mysqli_prepare($con, $sql);
        if (!$stmt) {
            return array('status' => '0', 'message' => 'Query failed', 'sent' => '0', 'failed' => '0', 'skipped' => '0');
        }
        if ($types !== '') {
            db_stmt_bind_params($stmt, $types, $params);
        }
        mysqli_stmt_execute($stmt);
        $result = mysqli_stmt_get_result($stmt);

        if ($result) {
            while ($row = mysqli_fetch_assoc($result)) {
                $push = fcm_send_promotional($con, $row, $title, $body, $extraData);
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

        return array(
            'status' => '1',
            'message' => 'Push broadcast completed',
            'sent' => (string) $sent,
            'failed' => (string) $failed,
            'skipped' => (string) $skipped,
        );
    }
}

?>
