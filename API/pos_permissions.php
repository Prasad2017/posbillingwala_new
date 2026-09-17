<?php
/**
 * Fixed POS roles, default permissions, overrides, dependencies.
 * Roles are not user-editable master data.
 */
require_once __DIR__ . '/pos_schema.php';

if (!function_exists('pos_fixed_roles')) {
    function pos_fixed_roles()
    {
        return array(
            'OWNER' => 'Owner',
            'MANAGER' => 'Manager',
            'WAITER' => 'Waiter',
            'KITCHEN' => 'Kitchen',
            'HELPER' => 'Helper',
            'BAR_ATTENDER' => 'Bar Attender',
            'SECURITY' => 'Security',
            'ACCOUNTANT' => 'Accountant',
        );
    }
}

if (!function_exists('pos_permission_catalog')) {
    function pos_permission_catalog()
    {
        return array(
            'dashboard' => array('view'),
            'billing' => array('view', 'create', 'edit', 'delete'),
            'order' => array('view', 'create', 'edit', 'delete'),
            'kot' => array('view', 'create', 'edit', 'delete', 'print', 'reprint'),
            'bill' => array('view', 'create', 'edit', 'delete', 'print', 'reprint'),
            'table' => array('view', 'manage'),
            'takeaway' => array('view', 'create'),
            'product' => array('view', 'create', 'edit', 'delete'),
            'inventory' => array('view', 'manage'),
            'customer' => array('view', 'manage'),
            'report' => array('view', 'export'),
            'expense' => array('view', 'manage'),
            'mess' => array('view', 'manage'),
            'user' => array('view', 'create', 'edit', 'deactivate', 'change_role', 'reset_pin'),
            'printer' => array('view', 'manage', 'test'),
            'device' => array('view', 'manage'),
            'settings' => array('view', 'manage'),
        );
    }
}

if (!function_exists('pos_permission_keys')) {
    function pos_permission_keys()
    {
        $keys = array();
        foreach (pos_permission_catalog() as $module => $actions) {
            foreach ($actions as $action) {
                $keys[] = $module . '.' . $action;
            }
        }
        return $keys;
    }
}

if (!function_exists('pos_permission_dependencies')) {
    function pos_permission_dependencies()
    {
        return array(
            'billing.create' => 'billing.view',
            'billing.edit' => 'billing.view',
            'billing.delete' => 'billing.view',
            'order.create' => 'order.view',
            'order.edit' => 'order.view',
            'order.delete' => 'order.view',
            'kot.create' => 'kot.view',
            'kot.edit' => 'kot.view',
            'kot.delete' => 'kot.view',
            'kot.print' => 'kot.view',
            'kot.reprint' => 'kot.view',
            'bill.create' => 'bill.view',
            'bill.edit' => 'bill.view',
            'bill.delete' => 'bill.view',
            'bill.print' => 'bill.view',
            'bill.reprint' => 'bill.view',
            'table.manage' => 'table.view',
            'takeaway.create' => 'takeaway.view',
            'product.create' => 'product.view',
            'product.edit' => 'product.view',
            'product.delete' => 'product.view',
            'inventory.manage' => 'inventory.view',
            'customer.manage' => 'customer.view',
            'report.export' => 'report.view',
            'expense.manage' => 'expense.view',
            'mess.manage' => 'mess.view',
            'user.create' => 'user.view',
            'user.edit' => 'user.view',
            'user.deactivate' => 'user.view',
            'user.change_role' => 'user.view',
            'user.reset_pin' => 'user.view',
            'printer.manage' => 'printer.view',
            'printer.test' => 'printer.view',
            'device.manage' => 'device.view',
            'settings.manage' => 'settings.view',
        );
    }
}

if (!function_exists('pos_role_default_map')) {
    function pos_role_default_map()
    {
        $all = pos_permission_keys();
        $allow = function ($keys) use ($all) {
            $out = array();
            foreach ($all as $key) {
                $out[$key] = in_array($key, $keys, true) ? 1 : 0;
            }
            return $out;
        };

        $owner = array();
        foreach ($all as $key) {
            $owner[$key] = 1;
        }

        $manager = $owner;
        foreach ($all as $key) {
            if (strpos($key, 'user.') === 0 && $key !== 'user.view') {
                $manager[$key] = 0;
            }
            if ($key === 'settings.manage') {
                $manager[$key] = 0;
            }
        }

        $waiter = $allow(array(
            'dashboard.view',
            'order.view', 'order.create', 'order.edit',
            'table.view', 'table.manage',
            'kot.view', 'kot.create', 'kot.print',
            'takeaway.view', 'takeaway.create',
            'product.view',
        ));

        $kitchen = $allow(array(
            'dashboard.view',
            'order.view',
            'kot.view', 'kot.print', 'kot.edit',
        ));

        $helper = $allow(array(
            'dashboard.view',
            'order.view',
            'table.view',
            'kot.view',
        ));

        $bar = $allow(array(
            'dashboard.view',
            'order.view', 'order.create', 'order.edit',
            'kot.view', 'kot.create', 'kot.print',
            'product.view',
        ));

        $security = $allow(array(
            'dashboard.view',
            'table.view',
        ));

        $accountant = $allow(array(
            'dashboard.view',
            'bill.view', 'bill.print',
            'report.view', 'report.export',
            'expense.view', 'expense.manage',
            'customer.view',
        ));

        return array(
            'OWNER' => $owner,
            'MANAGER' => $manager,
            'WAITER' => $waiter,
            'KITCHEN' => $kitchen,
            'HELPER' => $helper,
            'BAR_ATTENDER' => $bar,
            'SECURITY' => $security,
            'ACCOUNTANT' => $accountant,
        );
    }
}

if (!function_exists('pos_permissions_seed_defaults')) {
    function pos_permissions_seed_defaults($con)
    {
        pos_schema_ensure($con);
        $count = db_stmt_scalar_int($con, 'SELECT COUNT(*) AS c FROM `pos_role_permission`', '');
        if ($count > 0) {
            return;
        }
        foreach (pos_role_default_map() as $role => $map) {
            foreach ($map as $key => $allowed) {
                db_stmt_execute(
                    $con,
                    'INSERT IGNORE INTO `pos_role_permission` (`role`, `permissionKey`, `allowed`) VALUES (?, ?, ?)',
                    'ssi',
                    $role,
                    $key,
                    (int) $allowed
                );
            }
        }
    }
}

if (!function_exists('pos_role_defaults')) {
    function pos_role_defaults($con, $role)
    {
        pos_permissions_seed_defaults($con);
        $role = strtoupper(trim((string) $role));
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT `permissionKey`, `allowed` FROM `pos_role_permission` WHERE `role`=?',
            's',
            $role
        );
        $map = array();
        foreach (pos_permission_keys() as $key) {
            $map[$key] = 0;
        }
        foreach ($rows as $row) {
            $map[$row['permissionKey']] = (int) $row['allowed'] === 1 ? 1 : 0;
        }
        if (empty($rows)) {
            $fallback = pos_role_default_map();
            if (isset($fallback[$role])) {
                return $fallback[$role];
            }
        }
        return $map;
    }
}

if (!function_exists('pos_normalize_overrides')) {
    function pos_normalize_overrides($raw)
    {
        $out = array();
        if (is_string($raw) && $raw !== '') {
            $decoded = json_decode($raw, true);
            if (is_array($decoded)) {
                $raw = $decoded;
            }
        }
        if (!is_array($raw)) {
            return $out;
        }
        $keys = pos_permission_keys();
        foreach ($raw as $key => $state) {
            $key = strtolower(trim((string) $key));
            $state = strtoupper(trim((string) $state));
            if (!in_array($key, $keys, true)) {
                continue;
            }
            if ($state === 'ALLOW' || $state === 'DENY') {
                $out[$key] = $state;
            }
        }
        return pos_apply_permission_dependencies_to_overrides($out);
    }
}

if (!function_exists('pos_apply_permission_dependencies_to_overrides')) {
    function pos_apply_permission_dependencies_to_overrides(array $overrides)
    {
        $deps = pos_permission_dependencies();
        foreach ($overrides as $key => $state) {
            if ($state === 'ALLOW' && isset($deps[$key])) {
                $parent = $deps[$key];
                if (!isset($overrides[$parent]) || $overrides[$parent] !== 'DENY') {
                    $overrides[$parent] = 'ALLOW';
                }
            }
            if ($state === 'DENY') {
                foreach ($deps as $child => $parent) {
                    if ($parent === $key) {
                        $overrides[$child] = 'DENY';
                    }
                }
            }
        }
        return $overrides;
    }
}

if (!function_exists('pos_effective_permissions')) {
    /**
     * @return array permissionKey => 0|1
     */
    function pos_effective_permissions($con, $role, array $overrides)
    {
        $effective = pos_role_defaults($con, $role);
        foreach ($overrides as $key => $state) {
            if (!isset($effective[$key])) {
                continue;
            }
            if ($state === 'ALLOW') {
                $effective[$key] = 1;
            } elseif ($state === 'DENY') {
                $effective[$key] = 0;
            }
        }
        $deps = pos_permission_dependencies();
        foreach ($deps as $child => $parent) {
            if (empty($effective[$parent])) {
                $effective[$child] = 0;
            }
        }
        return $effective;
    }
}

if (!function_exists('pos_load_staff_overrides')) {
    function pos_load_staff_overrides($con, $staffId)
    {
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT `permissionKey`, `overrideState` FROM `pos_staff_permission_override` WHERE `staffId`=?',
            'i',
            (int) $staffId
        );
        $out = array();
        foreach ($rows as $row) {
            $out[$row['permissionKey']] = strtoupper((string) $row['overrideState']);
        }
        return $out;
    }
}

if (!function_exists('pos_save_staff_overrides')) {
    function pos_save_staff_overrides($con, $staffId, array $overrides)
    {
        db_stmt_execute(
            $con,
            'DELETE FROM `pos_staff_permission_override` WHERE `staffId`=?',
            'i',
            (int) $staffId
        );
        foreach ($overrides as $key => $state) {
            if ($state !== 'ALLOW' && $state !== 'DENY') {
                continue;
            }
            db_stmt_execute(
                $con,
                'INSERT INTO `pos_staff_permission_override` (`staffId`, `permissionKey`, `overrideState`) VALUES (?, ?, ?)',
                'iss',
                (int) $staffId,
                $key,
                $state
            );
        }
    }
}

if (!function_exists('pos_bump_permission_version')) {
    function pos_bump_permission_version($con, $licenseId)
    {
        db_stmt_execute(
            $con,
            'UPDATE `licenses` SET `permissionVersion` = COALESCE(`permissionVersion`,1) + 1 WHERE `id`=?',
            'i',
            (int) $licenseId
        );
    }
}
