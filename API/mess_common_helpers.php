<?php
/**
 * Mess Common QR helpers — shared by public + authenticated endpoints.
 * PHP 7.0+ safe.
 */

require_once __DIR__ . '/db_prepared.php';

if (!function_exists('mess_common_ensure_schema')) {
    function mess_common_ensure_schema($con)
    {
        static $done = false;
        if ($done) {
            return true;
        }

        $col = db_stmt_fetch_one(
            $con,
            "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_member' AND COLUMN_NAME = 'registration_no' LIMIT 1"
        );
        if ($col === null) {
            db_safe_query($con, "ALTER TABLE `mess_member` ADD COLUMN `registration_no` VARCHAR(64) NULL AFTER `member_address`");
        }

        // Prefer mobile as registration no when blank (customers enter mobile on QR page).
        db_safe_query(
            $con,
            "UPDATE `mess_member`
             SET `registration_no` = TRIM(`member_mobile_number`)
             WHERE (`registration_no` IS NULL OR TRIM(`registration_no`) = '')
               AND `member_mobile_number` IS NOT NULL
               AND TRIM(`member_mobile_number`) <> ''"
        );

        db_safe_query(
            $con,
            "CREATE TABLE IF NOT EXISTS `mess_qr` (
              `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `userId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `public_token` VARCHAR(64) NOT NULL,
              `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
              `print_device_id` VARCHAR(255) DEFAULT NULL,
              `mess_label` VARCHAR(255) DEFAULT NULL,
              `branch_label` VARCHAR(255) DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              `deactivated_at` DATETIME DEFAULT NULL,
              PRIMARY KEY (`id`),
              UNIQUE KEY `uq_mess_qr_public_token` (`public_token`),
              KEY `idx_mess_qr_user_status` (`userId`, `status`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        db_safe_query(
            $con,
            "CREATE TABLE IF NOT EXISTS `mess_meal_session` (
              `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `userId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `session_name` VARCHAR(64) NOT NULL,
              `start_time` TIME NOT NULL,
              `end_time` TIME NOT NULL,
              `token_prefix` VARCHAR(8) NOT NULL DEFAULT 'L',
              `is_active` TINYINT(1) NOT NULL DEFAULT 1,
              `menu_notes` TEXT NULL,
              `sort_order` INT NOT NULL DEFAULT 0,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`id`),
              KEY `idx_mess_meal_session_user` (`userId`, `is_active`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        db_safe_query(
            $con,
            "CREATE TABLE IF NOT EXISTS `mess_meal_token` (
              `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `public_id` VARCHAR(64) NOT NULL,
              `userId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `qr_id` INT UNSIGNED DEFAULT NULL,
              `session_id` INT UNSIGNED NOT NULL,
              `session_name` VARCHAR(64) NOT NULL,
              `member_id` INT NOT NULL,
              `registration_no` VARCHAR(64) NOT NULL,
              `member_name` VARCHAR(255) NOT NULL DEFAULT '',
              `token_number` VARCHAR(32) NOT NULL,
              `token_date` DATE NOT NULL,
              `token_seq` INT UNSIGNED NOT NULL DEFAULT 1,
              `print_status` VARCHAR(24) NOT NULL DEFAULT 'PRINT_PENDING',
              `print_device_id` VARCHAR(255) DEFAULT NULL,
              `printed_at` DATETIME DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`id`),
              UNIQUE KEY `uq_mess_meal_token_public` (`public_id`),
              UNIQUE KEY `uq_mess_meal_token_once` (`userId`, `member_id`, `token_date`, `session_id`),
              KEY `idx_mess_meal_token_pending` (`userId`, `print_status`, `token_date`),
              KEY `idx_mess_meal_token_device` (`print_device_id`, `print_status`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        db_safe_query(
            $con,
            "CREATE TABLE IF NOT EXISTS `mess_meal_token_seq` (
              `userId` INT NOT NULL,
              `token_date` DATE NOT NULL,
              `token_prefix` VARCHAR(8) NOT NULL,
              `last_seq` INT UNSIGNED NOT NULL DEFAULT 0,
              PRIMARY KEY (`userId`, `token_date`, `token_prefix`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        db_safe_query(
            $con,
            "CREATE TABLE IF NOT EXISTS `mess_token_audit` (
              `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
              `userId` INT DEFAULT NULL,
              `event_type` VARCHAR(64) NOT NULL,
              `public_token` VARCHAR(64) DEFAULT NULL,
              `registration_no` VARCHAR(64) DEFAULT NULL,
              `token_public_id` VARCHAR(64) DEFAULT NULL,
              `detail` VARCHAR(255) DEFAULT NULL,
              `source_ip` VARCHAR(64) DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (`id`),
              KEY `idx_mess_audit_user_time` (`userId`, `created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        db_safe_query(
            $con,
            "CREATE TABLE IF NOT EXISTS `mess_public_rate_limit` (
              `bucket_key` VARCHAR(128) NOT NULL,
              `window_start` INT UNSIGNED NOT NULL,
              `hit_count` INT UNSIGNED NOT NULL DEFAULT 0,
              PRIMARY KEY (`bucket_key`, `window_start`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        $done = true;
        return true;
    }
}

if (!function_exists('mess_client_ip')) {
    function mess_client_ip()
    {
        if (!empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
            $parts = explode(',', (string) $_SERVER['HTTP_X_FORWARDED_FOR']);
            return trim($parts[0]);
        }
        return isset($_SERVER['REMOTE_ADDR']) ? (string) $_SERVER['REMOTE_ADDR'] : '';
    }
}

if (!function_exists('mess_audit')) {
    function mess_audit($con, $userId, $eventType, $publicToken = null, $registrationNo = null, $tokenPublicId = null, $detail = null)
    {
        $ip = mess_client_ip();
        $uid = ($userId === null || $userId === '') ? 0 : (int) $userId;
        db_stmt_execute(
            $con,
            'INSERT INTO mess_token_audit (userId, event_type, public_token, registration_no, token_public_id, detail, source_ip)
             VALUES (?, ?, ?, ?, ?, ?, ?)',
            'issssss',
            $uid,
            (string) $eventType,
            $publicToken === null ? '' : (string) $publicToken,
            $registrationNo === null ? '' : (string) $registrationNo,
            $tokenPublicId === null ? '' : (string) $tokenPublicId,
            $detail === null ? '' : substr((string) $detail, 0, 255),
            $ip
        );
    }
}

if (!function_exists('mess_rate_limit_allow')) {
    function mess_rate_limit_allow($con, $bucketKey, $maxHits, $windowSeconds = 60)
    {
        $now = time();
        $windowStart = (int) (floor($now / $windowSeconds) * $windowSeconds);
        $key = substr((string) $bucketKey, 0, 128);

        $row = db_stmt_fetch_one(
            $con,
            'SELECT hit_count FROM mess_public_rate_limit WHERE bucket_key = ? AND window_start = ? LIMIT 1',
            'si',
            $key,
            $windowStart
        );

        if ($row === null) {
            db_stmt_execute(
                $con,
                'INSERT INTO mess_public_rate_limit (bucket_key, window_start, hit_count) VALUES (?, ?, 1)
                 ON DUPLICATE KEY UPDATE hit_count = hit_count + 1',
                'si',
                $key,
                $windowStart
            );
            return true;
        }

        if ((int) $row['hit_count'] >= (int) $maxHits) {
            return false;
        }

        db_stmt_execute(
            $con,
            'UPDATE mess_public_rate_limit SET hit_count = hit_count + 1 WHERE bucket_key = ? AND window_start = ?',
            'si',
            $key,
            $windowStart
        );
        return true;
    }
}

if (!function_exists('mess_random_token')) {
    function mess_random_token($bytes = 24)
    {
        try {
            return bin2hex(random_bytes($bytes));
        } catch (Exception $e) {
            return bin2hex(openssl_random_pseudo_bytes($bytes));
        }
    }
}

if (!function_exists('mess_normalize_registration')) {
    function mess_normalize_registration($raw)
    {
        $s = strtoupper(trim((string) $raw));
        $s = preg_replace('/\s+/', '', $s);
        return $s;
    }
}

if (!function_exists('mess_normalize_mobile')) {
    /** Digits only; keep last 10 for Indian mobiles. */
    function mess_normalize_mobile($raw)
    {
        $d = preg_replace('/\D+/', '', (string) $raw);
        if ($d === null) {
            return '';
        }
        if (strlen($d) > 10) {
            $d = substr($d, -10);
        }
        return $d;
    }
}

if (!function_exists('mess_public_qr_url')) {
    function mess_public_qr_url($publicToken)
    {
        $base = 'https://posbillingwala.com/androidApp/mess_q.php';
        if (defined('MESS_PUBLIC_BASE_URL') && MESS_PUBLIC_BASE_URL !== '') {
            $base = rtrim(MESS_PUBLIC_BASE_URL, '/');
            if (stripos($base, 'mess_q.php') === false) {
                $base .= '/mess_q.php';
            }
        }
        return $base . '?t=' . rawurlencode($publicToken);
    }
}

if (!function_exists('mess_ensure_default_sessions')) {
    function mess_ensure_default_sessions($con, $userId)
    {
        $userId = (int) $userId;
        $c = db_stmt_scalar_int($con, 'SELECT COUNT(*) AS c FROM mess_meal_session WHERE userId = ?', 'i', $userId);
        if ($c > 0) {
            return;
        }

        db_stmt_execute(
            $con,
            "INSERT INTO mess_meal_session (userId, session_name, start_time, end_time, token_prefix, is_active, sort_order)
             VALUES (?, 'Lunch', '11:30:00', '15:00:00', 'L', 1, 1)",
            'i',
            $userId
        );
        db_stmt_execute(
            $con,
            "INSERT INTO mess_meal_session (userId, session_name, start_time, end_time, token_prefix, is_active, sort_order)
             VALUES (?, 'Dinner', '19:00:00', '22:30:00', 'D', 1, 2)",
            'i',
            $userId
        );
    }
}

if (!function_exists('mess_resolve_current_session')) {
    function mess_resolve_current_session($con, $userId)
    {
        date_default_timezone_set('Asia/Kolkata');
        mess_ensure_default_sessions($con, $userId);

        $now = date('H:i:s');
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM mess_meal_session WHERE userId = ? AND is_active = 1 ORDER BY sort_order ASC, id ASC',
            'i',
            (int) $userId
        );

        foreach ($rows as $row) {
            $start = $row['start_time'];
            $end = $row['end_time'];
            if ($start <= $end) {
                if ($now >= $start && $now <= $end) {
                    return $row;
                }
            } elseif ($now >= $start || $now <= $end) {
                return $row;
            }
        }
        return null;
    }
}

if (!function_exists('mess_next_token_number')) {
    function mess_next_token_number($con, $userId, $tokenDate, $prefix)
    {
        $userId = (int) $userId;
        $prefix = strtoupper(trim((string) $prefix));
        if ($prefix === '') {
            $prefix = 'T';
        }

        db_stmt_execute(
            $con,
            'INSERT INTO mess_meal_token_seq (userId, token_date, token_prefix, last_seq)
             VALUES (?, ?, ?, 1)
             ON DUPLICATE KEY UPDATE last_seq = last_seq + 1',
            'iss',
            $userId,
            $tokenDate,
            $prefix
        );

        $row = db_stmt_fetch_one(
            $con,
            'SELECT last_seq FROM mess_meal_token_seq WHERE userId = ? AND token_date = ? AND token_prefix = ? LIMIT 1',
            'iss',
            $userId,
            $tokenDate,
            $prefix
        );
        $seq = $row ? (int) $row['last_seq'] : 1;
        return array(
            'seq' => $seq,
            'token_number' => $prefix . '-' . str_pad((string) $seq, 3, '0', STR_PAD_LEFT),
        );
    }
}

if (!function_exists('mess_find_member_by_registration')) {
    /**
     * Find member by registration_no OR primary/alternate mobile number.
     * Customers can enter mobile as their "registration" on the public QR page.
     */
    function mess_find_member_by_registration($con, $userId, $registrationNo, $branchId = null)
    {
        $reg = mess_normalize_registration($registrationNo);
        $mobile = mess_normalize_mobile($registrationNo);
        if ($reg === '' && $mobile === '') {
            return null;
        }

        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM mess_member WHERE userId = ?',
            'i',
            (int) $userId
        );
        foreach ($rows as $row) {
            $match = false;
            if ($reg !== '' && !empty($row['registration_no'])
                && mess_normalize_registration($row['registration_no']) === $reg) {
                $match = true;
            }
            if (!$match && $mobile !== '') {
                $m1 = mess_normalize_mobile(isset($row['member_mobile_number']) ? $row['member_mobile_number'] : '');
                $m2 = mess_normalize_mobile(isset($row['member_altenet_mobile_number']) ? $row['member_altenet_mobile_number'] : '');
                if (($m1 !== '' && $m1 === $mobile) || ($m2 !== '' && $m2 === $mobile)) {
                    $match = true;
                }
            }
            if (!$match) {
                continue;
            }
            if ($branchId !== null && (int) $branchId > 0) {
                $mb = isset($row['branch_id']) ? (int) $row['branch_id'] : 0;
                if ($mb > 0 && $mb !== (int) $branchId) {
                    continue;
                }
            }
            return $row;
        }
        return null;
    }
}

if (!function_exists('mess_get_qr_by_public_token')) {
    function mess_get_qr_by_public_token($con, $publicToken)
    {
        return db_stmt_fetch_one(
            $con,
            'SELECT * FROM mess_qr WHERE public_token = ? LIMIT 1',
            's',
            (string) $publicToken
        );
    }
}

if (!function_exists('mess_notify_pos_token_created')) {
    function mess_notify_pos_token_created($con, array $tokenRow)
    {
        require_once __DIR__ . '/fcm_tables.php';
        require_once __DIR__ . '/fcm_helper.php';
        fcm_ensure_schema($con);

        $deviceId = isset($tokenRow['print_device_id']) ? trim((string) $tokenRow['print_device_id']) : '';
        $userId = (int) $tokenRow['userId'];

        $license = null;
        if ($deviceId !== '') {
            $license = db_stmt_fetch_one(
                $con,
                "SELECT id, fcm_token, android_device_id FROM licenses
                 WHERE android_device_id = ? AND licenseStatus = 'active' LIMIT 1",
                's',
                $deviceId
            );
        }
        if ($license === null) {
            $license = db_stmt_fetch_one(
                $con,
                "SELECT id, fcm_token, android_device_id FROM licenses
                 WHERE id = ? AND licenseStatus = 'active' LIMIT 1",
                'i',
                $userId
            );
        }

        if ($license === null || empty($license['fcm_token'])) {
            return array('ok' => false, 'message' => 'No FCM token');
        }

        $data = array(
            'type' => 'mess.token.created',
            'event' => 'mess.token.created',
            'tokenId' => (string) $tokenRow['public_id'],
            'tokenNumber' => (string) $tokenRow['token_number'],
            'registrationNo' => (string) $tokenRow['registration_no'],
            'mealSession' => (string) $tokenRow['session_name'],
            'date' => (string) $tokenRow['token_date'],
            'createdAt' => (string) $tokenRow['created_at'],
            'printStatus' => (string) $tokenRow['print_status'],
        );

        if (function_exists('fcm_send_data_only')) {
            $result = fcm_send_data_only($license['fcm_token'], $data);
        } else {
            $result = fcm_send_to_token($license['fcm_token'], 'Mess Token', $tokenRow['token_number'], $data);
        }

        if (!$result['ok'] && !empty($result['invalid_token'])) {
            fcm_clear_invalid_token($con, (int) $license['id']);
        }
        return $result;
    }
}

if (!function_exists('mess_member_current_month_paid_sum')) {
    /**
     * Sum of paymentPaidAmount for the given member in the current calendar month (Asia/Kolkata).
     * Matches by memberId first, then falls back to memberName for older rows.
     */
    function mess_member_current_month_paid_sum($con, $userId, $memberId, $memberName = '')
    {
        $ym = date('Y-m');
        $like = $ym . '%';
        $uid = (int) $userId;
        $mid = trim((string) $memberId);

        $paid = 0.0;
        if ($mid !== '' && $mid !== '0') {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT IFNULL(SUM(CAST(paymentPaidAmount AS DECIMAL(12,2))), 0) AS paid
                 FROM mess_member_payment
                 WHERE userId = ? AND CAST(memberId AS CHAR) = ? AND paymentDate LIKE ?',
                'iss',
                $uid,
                $mid,
                $like
            );
            if ($row !== null) {
                $paid = (float) $row['paid'];
            }
        }

        if ($paid <= 0.0001 && $memberName !== null && trim((string) $memberName) !== '') {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT IFNULL(SUM(CAST(paymentPaidAmount AS DECIMAL(12,2))), 0) AS paid
                 FROM mess_member_payment
                 WHERE userId = ? AND memberName = ? AND paymentDate LIKE ?',
                'iss',
                $uid,
                trim((string) $memberName),
                $like
            );
            if ($row !== null) {
                $paid = (float) $row['paid'];
            }
        }

        return $paid;
    }
}

if (!function_exists('mess_member_has_any_previous_payment')) {
    /** True if member has any payment row before the current month. */
    function mess_member_has_any_previous_payment($con, $userId, $memberId, $memberName = '')
    {
        $ym = date('Y-m');
        $uid = (int) $userId;
        $mid = trim((string) $memberId);

        if ($mid !== '' && $mid !== '0') {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT payment_id FROM mess_member_payment
                 WHERE userId = ? AND CAST(memberId AS CHAR) = ?
                   AND IFNULL(paymentDate, \'\') != \'\'
                   AND substr(paymentDate, 1, 7) < ?
                 LIMIT 1',
                'iss',
                $uid,
                $mid,
                $ym
            );
            if ($row !== null) {
                return true;
            }
        }

        if ($memberName !== null && trim((string) $memberName) !== '') {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT payment_id FROM mess_member_payment
                 WHERE userId = ? AND memberName = ?
                   AND IFNULL(paymentDate, \'\') != \'\'
                   AND substr(paymentDate, 1, 7) < ?
                 LIMIT 1',
                'iss',
                $uid,
                trim((string) $memberName),
                $ym
            );
            if ($row !== null) {
                return true;
            }
        }

        return false;
    }
}

if (!function_exists('mess_member_require_current_month_payment')) {
    /**
     * Returns null when OK to issue token, or an error message string when unpaid this month.
     */
    function mess_member_require_current_month_payment($con, $userId, $memberId, $memberName = '')
    {
        $paid = mess_member_current_month_paid_sum($con, $userId, $memberId, $memberName);
        if ($paid > 0.009) {
            return null;
        }

        if (mess_member_has_any_previous_payment($con, $userId, $memberId, $memberName)) {
            return 'You have not paid for this month. Please pay at the mess counter to get today\'s token.';
        }

        return 'No payment found for this month. Please pay at the mess counter to get a token.';
    }
}
