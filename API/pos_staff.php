<?php
/**
 * POS staff + hashed PIN. Never log PIN or hash.
 */
require_once __DIR__ . '/pos_schema.php';
require_once __DIR__ . '/pos_permissions.php';
require_once __DIR__ . '/pos_audit.php';

if (!function_exists('pos_pin_hash')) {
    function pos_pin_hash($pin)
    {
        return password_hash((string) $pin, PASSWORD_BCRYPT);
    }
}

if (!function_exists('pos_pin_verify')) {
    function pos_pin_verify($pin, $hash)
    {
        if ($hash === null || $hash === '') {
            return false;
        }
        return password_verify((string) $pin, (string) $hash);
    }
}

if (!function_exists('pos_pin_valid_format')) {
    function pos_pin_valid_format($pin)
    {
        $pin = trim((string) $pin);
        return (bool) preg_match('/^[0-9]{4}$|^[0-9]{6}$/', $pin);
    }
}

if (!function_exists('pos_mobile_valid')) {
    function pos_mobile_valid($mobile)
    {
        $mobile = preg_replace('/\D+/', '', (string) $mobile);
        return (bool) preg_match('/^[6-9][0-9]{9}$/', $mobile);
    }
}

if (!function_exists('pos_normalize_mobile')) {
    function pos_normalize_mobile($mobile)
    {
        return preg_replace('/\D+/', '', (string) $mobile);
    }
}

if (!function_exists('pos_role_valid')) {
    function pos_role_valid($role)
    {
        $roles = pos_fixed_roles();
        return isset($roles[strtoupper(trim((string) $role))]);
    }
}

if (!function_exists('pos_staff_public')) {
    function pos_staff_public(array $row, array $effective = null, array $overrides = null)
    {
        $out = array(
            'id' => (string) $row['id'],
            'licenseId' => (string) $row['licenseId'],
            'name' => $row['name'],
            'mobileNumber' => $row['mobileNumber'],
            'address' => isset($row['address']) ? $row['address'] : '',
            'profileImage' => isset($row['profileImage']) ? $row['profileImage'] : '',
            'role' => $row['role'],
            'roleLabel' => isset(pos_fixed_roles()[$row['role']]) ? pos_fixed_roles()[$row['role']] : $row['role'],
            'status' => $row['status'],
            'monthlySalary' => isset($row['monthlySalary']) ? (string) $row['monthlySalary'] : '0',
            'lastLoginAt' => isset($row['lastLoginAt']) ? $row['lastLoginAt'] : '',
        );
        if ($effective !== null) {
            $out['effectivePermissions'] = $effective;
        }
        if ($overrides !== null) {
            $out['permissionOverrides'] = $overrides;
        }
        return $out;
    }
}

if (!function_exists('pos_count_active_staff')) {
    function pos_count_active_staff($con, $licenseId)
    {
        return db_stmt_scalar_int(
            $con,
            "SELECT COUNT(*) AS c FROM `pos_staff` WHERE `licenseId`=? AND `status`='ACTIVE'",
            'i',
            (int) $licenseId
        );
    }
}

if (!function_exists('pos_count_active_owners')) {
    function pos_count_active_owners($con, $licenseId, $exceptStaffId = 0)
    {
        if ($exceptStaffId) {
            return db_stmt_scalar_int(
                $con,
                "SELECT COUNT(*) AS c FROM `pos_staff` WHERE `licenseId`=? AND `role`='OWNER' AND `status`='ACTIVE' AND `id`<>?",
                'ii',
                (int) $licenseId,
                (int) $exceptStaffId
            );
        }
        return db_stmt_scalar_int(
            $con,
            "SELECT COUNT(*) AS c FROM `pos_staff` WHERE `licenseId`=? AND `role`='OWNER' AND `status`='ACTIVE'",
            'i',
            (int) $licenseId
        );
    }
}

if (!function_exists('pos_protect_final_owner')) {
    function pos_protect_final_owner($con, $licenseId, $staffId, $newRole = null, $newStatus = null)
    {
        $staff = db_stmt_fetch_one($con, 'SELECT * FROM `pos_staff` WHERE `id`=? AND `licenseId`=? LIMIT 1', 'ii', (int) $staffId, (int) $licenseId);
        if ($staff === null) {
            return 'User not found';
        }
        if ($staff['role'] !== 'OWNER') {
            return null;
        }
        $others = pos_count_active_owners($con, $licenseId, $staffId);
        if ($others > 0) {
            return null;
        }
        if ($newStatus !== null && strtoupper($newStatus) !== 'ACTIVE') {
            return 'A store must keep at least one active Owner';
        }
        if ($newRole !== null && strtoupper($newRole) !== 'OWNER') {
            return 'A store must keep at least one active Owner';
        }
        return null;
    }
}

if (!function_exists('pos_seed_owner_from_licence')) {
    function pos_seed_owner_from_licence($con, $licenseId)
    {
        pos_schema_ensure($con);
        pos_permissions_seed_defaults($con);
        $count = pos_count_active_staff($con, $licenseId);
        if ($count > 0) {
            return;
        }
        $lic = db_stmt_fetch_one(
            $con,
            'SELECT l.`id`, l.`userId`, l.`mpin`, l.`userName`, u.`name`, u.`contact_number`, u.`address`
             FROM `licenses` l LEFT JOIN `users` u ON u.id = l.userId WHERE l.id=? LIMIT 1',
            'i',
            (int) $licenseId
        );
        if ($lic === null) {
            return;
        }
        $mobile = pos_normalize_mobile(isset($lic['contact_number']) ? $lic['contact_number'] : '');
        if (!pos_mobile_valid($mobile)) {
            $mobile = '9000000000';
        }
        $name = trim((string) (isset($lic['name']) && $lic['name'] !== '' ? $lic['name'] : $lic['userName']));
        if ($name === '') {
            $name = 'Owner';
        }
        $pin = isset($lic['mpin']) ? (string) $lic['mpin'] : '9082';
        if (!pos_pin_valid_format($pin)) {
            $pin = '9082';
        }
        $hash = pos_pin_hash($pin);
        db_stmt_insert_id(
            $con,
            'INSERT INTO `pos_staff` (`organization_id`, `licenseId`, `name`, `mobileNumber`, `address`, `role`, `pinHash`, `status`)
             VALUES (?, ?, ?, ?, ?, \'OWNER\', ?, \'ACTIVE\')',
            'iissss',
            (int) $lic['userId'],
            (int) $licenseId,
            $name,
            $mobile,
            isset($lic['address']) ? (string) $lic['address'] : '',
            $hash
        );
        pos_audit($con, $licenseId, 'User Created', 'staff', null, null, array('seed' => 'owner'));
    }
}

if (!function_exists('pos_staff_by_id')) {
    function pos_staff_by_id($con, $licenseId, $staffId)
    {
        return db_stmt_fetch_one(
            $con,
            'SELECT * FROM `pos_staff` WHERE `id`=? AND `licenseId`=? LIMIT 1',
            'ii',
            (int) $staffId,
            (int) $licenseId
        );
    }
}

if (!function_exists('pos_require_permission')) {
    /**
     * When UM is off, allow (legacy licence access).
     * When UM is on, require active staff + effective permission.
     * Returns staff row or null (and exits on failure if $exitOnFail).
     */
    function pos_require_permission($con, $licenseId, $permissionKey, $exitOnFail = true)
    {
        pos_schema_ensure($con);
        if (!pos_um_enabled($con, $licenseId)) {
            return array('id' => 0, 'role' => 'OWNER', 'status' => 'ACTIVE');
        }
        $staffId = pos_posted_staff_id();
        if ($staffId <= 0) {
            /* Owner PB-PIN / licence Bearer session has no X-Pos-Staff-Id.
             * Treat as OWNER so sync + settings saves still work. Staff apps
             * always send the header and stay permission-scoped. */
            return array('id' => 0, 'role' => 'OWNER', 'status' => 'ACTIVE');
        }
        $staff = pos_staff_by_id($con, $licenseId, $staffId);
        if ($staff === null || strtoupper($staff['status']) !== 'ACTIVE') {
            if ($exitOnFail) {
                header('Content-Type: application/json; charset=utf-8');
                echo json_encode(array('status' => '0', 'message' => 'Staff inactive or not found'));
                exit;
            }
            return null;
        }
        $overrides = pos_load_staff_overrides($con, $staffId);
        $effective = pos_effective_permissions($con, $staff['role'], $overrides);
        if (empty($effective[$permissionKey])) {
            if ($exitOnFail) {
                header('Content-Type: application/json; charset=utf-8');
                echo json_encode(array('status' => '0', 'message' => 'Permission denied'));
                exit;
            }
            return null;
        }
        return $staff;
    }
}
