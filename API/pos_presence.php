<?php
/**
 * POS device online/offline presence helpers.
 * Online = last activity within LICENCE_ONLINE_MINUTES (default 15).
 */

if (!defined('LICENCE_ONLINE_MINUTES')) {
    define('LICENCE_ONLINE_MINUTES', 15);
}

if (!function_exists('licence_has_last_login_column')) {
    function licence_has_last_login_column($con)
    {
        static $cached = null;
        if ($cached !== null) {
            return $cached;
        }
        $db = mysqli_real_escape_string($con, mysqli_fetch_assoc(mysqli_query($con, 'SELECT DATABASE() AS d'))['d'] ?? '');
        if ($db === '') {
            $cached = false;
            return false;
        }
        $row = db_stmt_fetch_one(
            $con,
            'SELECT COUNT(*) AS c FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA=? AND TABLE_NAME=\'licenses\' AND COLUMN_NAME=\'lastLoginAt\'',
            's',
            $db
        );
        $cached = $row !== null && (int) ($row['c'] ?? 0) > 0;
        return $cached;
    }
}

if (!function_exists('licence_touch_last_login')) {
    /**
     * Record POS login / licence check. Throttled to once per 2 minutes per licence.
     */
    function licence_touch_last_login($con, $licenseId)
    {
        if (!$licenseId || !licence_has_last_login_column($con)) {
            return false;
        }
        return db_stmt_execute(
            $con,
            'UPDATE `licenses` SET `lastLoginAt`=NOW()
             WHERE `id`=? AND (`lastLoginAt` IS NULL OR `lastLoginAt` < DATE_SUB(NOW(), INTERVAL 2 MINUTE))',
            'i',
            (int) $licenseId
        );
    }
}

if (!function_exists('licence_token_last_used_subquery')) {
    function licence_token_last_used_subquery($licenseAlias = 'l')
    {
        return '(SELECT MAX(t.`last_used_at`) FROM `api_tokens` t
                 WHERE t.`actor_type`=\'pos_licence\' AND t.`actor_id`=' . $licenseAlias . '.`id`
                   AND (t.`device_id`=' . $licenseAlias . '.`android_device_id`
                        OR t.`device_id` IS NULL OR TRIM(t.`device_id`)=\'\'))';
    }
}

if (!function_exists('licence_pick_last_seen')) {
    function licence_pick_last_seen($lastLoginAt, $tokenLastUsedAt)
    {
        $best = null;
        foreach (array($lastLoginAt, $tokenLastUsedAt) as $ts) {
            if ($ts === null || $ts === '' || $ts === '0000-00-00 00:00:00') {
                continue;
            }
            if ($best === null || strtotime((string) $ts) > strtotime((string) $best)) {
                $best = (string) $ts;
            }
        }
        return $best;
    }
}

if (!function_exists('licence_connection_status')) {
    function licence_connection_status($lastSeenAt, $onlineMinutes = null)
    {
        if ($onlineMinutes === null) {
            $onlineMinutes = (int) LICENCE_ONLINE_MINUTES;
        }
        if ($lastSeenAt === null || $lastSeenAt === '' || $lastSeenAt === '0000-00-00 00:00:00') {
            return 'OFFLINE';
        }
        $ts = strtotime((string) $lastSeenAt);
        if ($ts === false) {
            return 'OFFLINE';
        }
        return ($ts >= (time() - ($onlineMinutes * 60))) ? 'ONLINE' : 'OFFLINE';
    }
}

if (!function_exists('licence_format_last_seen')) {
    function licence_format_last_seen($lastSeenAt)
    {
        if ($lastSeenAt === null || $lastSeenAt === '' || $lastSeenAt === '0000-00-00 00:00:00') {
            return 'Never';
        }
        $ts = strtotime((string) $lastSeenAt);
        if ($ts === false) {
            return 'Never';
        }
        $diff = time() - $ts;
        if ($diff < 60) {
            return 'Just now';
        }
        if ($diff < 3600) {
            return (int) floor($diff / 60) . ' min ago';
        }
        if ($diff < 86400) {
            return (int) floor($diff / 3600) . ' hr ago';
        }
        if ($diff < 604800) {
            return (int) floor($diff / 86400) . ' day(s) ago';
        }
        return date('d M Y, h:i A', $ts);
    }
}

if (!function_exists('licence_device_presence_fields')) {
    /**
     * @param array $row licence row with optional lastLoginAt, tokenLastUsedAt
     * @return array lastSeenAt, lastSeenLabel, connectionStatus, lastLoginAt
     */
    function licence_device_presence_fields(array $row)
    {
        $lastLoginAt = isset($row['lastLoginAt']) ? (string) $row['lastLoginAt'] : '';
        $tokenLast = isset($row['tokenLastUsedAt']) ? (string) $row['tokenLastUsedAt'] : '';
        $lastSeenAt = licence_pick_last_seen($lastLoginAt, $tokenLast);
        $status = licence_connection_status($lastSeenAt);
        return array(
            'lastLoginAt' => ($lastLoginAt !== '' && $lastLoginAt !== '0000-00-00 00:00:00') ? $lastLoginAt : '',
            'lastSeenAt' => $lastSeenAt ?: '',
            'lastSeenLabel' => licence_format_last_seen($lastSeenAt),
            'connectionStatus' => $status,
        );
    }
}
