<?php
/**
 * User identity rules: mobile / Aadhaar uniqueness scoped by role.
 *
 * - Customer mobile may match a dealer mobile (same person / same SIM).
 * - Dealer mobile must be unique among dealers (role_id = 2).
 * - Dealer Aadhaar must be unique among dealers (role_id = 2).
 * - Customer mobile must be unique among customers (role_id = 3).
 */

require_once __DIR__ . '/licence_expiry.php';

if (!function_exists('user_role_mobile_taken')) {
    /**
     * @param mysqli $con
     * @param string|int $roleId 2 = dealer, 3 = customer
     * @param string $contact
     * @param int|null $excludeUserId
     */
    function user_role_mobile_taken($con, $roleId, $contact, $excludeUserId = null)
    {
        $digits = licence_normalize_contact($contact);
        if (strlen($digits) < 10) {
            return false;
        }

        $sql = "SELECT COUNT(*) AS c FROM `users`
                WHERE `role_id`=?
                  AND (
                    `contact_number`=?
                    OR `contact_number` LIKE CONCAT('%', ?)
                    OR RIGHT(REPLACE(REPLACE(`contact_number`,' ',''),'-',''),10)=?
                  )";
        $types = 'isss';
        $params = array((string) $roleId, $digits, $digits, $digits);

        if ($excludeUserId !== null && (int) $excludeUserId > 0) {
            $sql .= ' AND `id`<>?';
            $types .= 'i';
            $params[] = (int) $excludeUserId;
        }

        return db_stmt_scalar_int($con, $sql, $types, ...$params) > 0;
    }
}

if (!function_exists('user_dealer_mobile_taken')) {
    function user_dealer_mobile_taken($con, $contact, $excludeUserId = null)
    {
        return user_role_mobile_taken($con, '2', $contact, $excludeUserId);
    }
}

if (!function_exists('user_customer_mobile_taken')) {
    function user_customer_mobile_taken($con, $contact, $excludeUserId = null)
    {
        return user_role_mobile_taken($con, '3', $contact, $excludeUserId);
    }
}

if (!function_exists('user_normalize_aadhar')) {
    function user_normalize_aadhar($aadhar)
    {
        return preg_replace('/\D+/', '', (string) $aadhar);
    }
}

if (!function_exists('user_dealer_aadhar_taken')) {
    function user_dealer_aadhar_taken($con, $aadhar, $excludeUserId = null)
    {
        $digits = user_normalize_aadhar($aadhar);
        if ($digits === '') {
            return false;
        }

        $sql = "SELECT COUNT(*) AS c FROM `users`
                WHERE `role_id`='2' AND `aadhar_number`=?";
        $types = 's';
        $params = array($digits);

        if ($excludeUserId !== null && (int) $excludeUserId > 0) {
            $sql .= ' AND `id`<>?';
            $types .= 'i';
            $params[] = (int) $excludeUserId;
        }

        return db_stmt_scalar_int($con, $sql, $types, ...$params) > 0;
    }
}
