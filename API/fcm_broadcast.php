<?php
/**
 * Multi-app FCM broadcast (POS licences + Owner/Dealer/Admin device tokens).
 * Included from fcm_helper.php after core send helpers are defined.
 */

if (!function_exists('fcm_broadcast_promotional')) {
    /**
     * @param string $target all|active|license_ids  (POS licence filter)
     * @param string $licenseIdsCsv comma-separated licence ids when target=license_ids
     * @param string $audience pos|owner|dealer|admin|all
     * @return array
     */
    function fcm_broadcast_promotional($con, $title, $body, $target = 'active', $licenseIdsCsv = '', array $extraData = array(), $audience = 'pos')
    {
        require_once __DIR__ . '/licence_expiry.php';
        require_once __DIR__ . '/fcm_tables.php';
        fcm_ensure_schema($con);

        $cfg = fcm_is_configured();
        if (empty($cfg['ok'])) {
            return array(
                'status' => '0',
                'message' => $cfg['message'],
                'sent' => '0',
                'failed' => '0',
                'skipped' => '0',
                'fcmProjectId' => isset($cfg['projectId']) ? $cfg['projectId'] : FCM_DEFAULT_PROJECT_ID,
            );
        }

        $title = trim((string) $title);
        $body = trim((string) $body);
        if ($title === '' || $body === '') {
            return array('status' => '0', 'message' => 'Title and message are required', 'sent' => '0', 'failed' => '0', 'skipped' => '0');
        }

        $audience = strtolower(trim((string) $audience));
        if ($audience === '' || $audience === 'active' || $audience === 'license_ids' || $audience === 'all_licences') {
            $audience = 'pos';
        }
        if (!in_array($audience, array('pos', 'owner', 'dealer', 'admin', 'all'), true)) {
            $audience = 'pos';
        }

        $sent = 0;
        $failed = 0;
        $skipped = 0;
        $apps = ($audience === 'all') ? array('pos', 'owner', 'dealer', 'admin') : array($audience);

        foreach ($apps as $app) {
            if ($app === 'pos') {
                $stats = fcm_broadcast_to_pos_licenses($con, $title, $body, $target, $licenseIdsCsv, $extraData);
            } else {
                $stats = fcm_broadcast_to_app_devices($con, $app, $title, $body, $extraData);
            }
            $sent += (int) $stats['sent'];
            $failed += (int) $stats['failed'];
            $skipped += (int) $stats['skipped'];
        }

        return array(
            'status' => '1',
            'message' => 'Push broadcast completed (project ' . $cfg['projectId'] . ', audience ' . $audience . ')',
            'sent' => (string) $sent,
            'failed' => (string) $failed,
            'skipped' => (string) $skipped,
            'fcmProjectId' => $cfg['projectId'],
            'fcmMode' => $cfg['mode'],
            'audience' => $audience,
        );
    }
}

if (!function_exists('fcm_broadcast_to_pos_licenses')) {
    function fcm_broadcast_to_pos_licenses($con, $title, $body, $target, $licenseIdsCsv, array $extraData)
    {
        require_once __DIR__ . '/licence_expiry.php';
        $target = strtolower(trim((string) $target));
        $today = licence_today();
        $params = array();
        $types = '';
        $sql = "SELECT `id`, `fcm_token`, `expiryDate`, `licenseStatus`, `userId`
                FROM `licenses`
                WHERE `fcm_token` IS NOT NULL AND TRIM(`fcm_token`) <> ''";

        if ($target === 'license_ids') {
            $ids = array_filter(array_map('trim', explode(',', (string) $licenseIdsCsv)));
            if (empty($ids)) {
                return array('sent' => 0, 'failed' => 0, 'skipped' => 0);
            }
            $placeholders = implode(',', array_fill(0, count($ids), '?'));
            $sql .= " AND `id` IN ($placeholders)";
            foreach ($ids as $id) {
                $types .= 'i';
                $params[] = (int) $id;
            }
        } elseif ($target === 'active' || $target === '') {
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
            return array('sent' => 0, 'failed' => 0, 'skipped' => 0);
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
        return array('sent' => $sent, 'failed' => $failed, 'skipped' => $skipped);
    }
}

if (!function_exists('fcm_broadcast_to_app_devices')) {
    function fcm_broadcast_to_app_devices($con, $appType, $title, $body, array $extraData)
    {
        $rows = fcm_device_list_tokens($con, $appType);
        $sent = 0;
        $failed = 0;
        $skipped = 0;
        $data = array_merge(array(
            'type' => 'promotional',
            'title' => (string) $title,
            'body' => (string) $body,
            'message' => (string) $body,
            'app' => (string) $appType,
        ), $extraData);

        foreach ($rows as $row) {
            $token = isset($row['fcm_token']) ? trim((string) $row['fcm_token']) : '';
            if ($token === '') {
                $skipped++;
                continue;
            }
            $result = fcm_send_to_token($token, $title, $body, $data);
            if (!empty($result['ok'])) {
                $sent++;
            } else {
                $failed++;
                if (!empty($result['invalid_token'])) {
                    fcm_clear_invalid_device_token($con, (int) $row['id']);
                }
            }
        }
        return array('sent' => $sent, 'failed' => $failed, 'skipped' => $skipped);
    }
}
