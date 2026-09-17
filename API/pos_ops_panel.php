<?php
/**
 * Read-only store operations payload for Admin / Dealer / Owner panels.
 * Never includes PIN or pinHash.
 */
require_once __DIR__ . '/pos_schema.php';
require_once __DIR__ . '/pos_staff.php';
require_once __DIR__ . '/pos_devices.php';
require_once __DIR__ . '/pos_print_ops.php';

if (!function_exists('pos_device_public')) {
    function pos_device_public(array $row)
    {
        return array(
            'id' => (string) $row['id'],
            'deviceId' => isset($row['deviceId']) ? (string) $row['deviceId'] : '',
            'deviceName' => isset($row['deviceName']) ? (string) $row['deviceName'] : '',
            'platform' => isset($row['platform']) ? (string) $row['platform'] : '',
            'appVersion' => isset($row['appVersion']) ? (string) $row['appVersion'] : '',
            'status' => isset($row['status']) ? (string) $row['status'] : '',
            'isPrintHost' => isset($row['isPrintHost']) ? (string) $row['isPrintHost'] : '0',
            'lastSeenAt' => isset($row['lastSeenAt']) ? (string) $row['lastSeenAt'] : '',
        );
    }
}

if (!function_exists('pos_route_public')) {
    function pos_route_public(array $row)
    {
        return array(
            'id' => (string) $row['id'],
            'printerId' => isset($row['printerId']) ? (string) $row['printerId'] : '',
            'documentType' => isset($row['documentType']) ? (string) $row['documentType'] : '',
            'foodTypeCode' => isset($row['foodTypeCode']) ? (string) $row['foodTypeCode'] : '',
            'categoryId' => isset($row['categoryId']) ? (string) $row['categoryId'] : '',
            'subcategoryId' => isset($row['subcategoryId']) ? (string) $row['subcategoryId'] : '',
            'productId' => isset($row['productId']) ? (string) $row['productId'] : '',
        );
    }
}

if (!function_exists('pos_ops_panel_payload')) {
    function pos_ops_panel_payload($con, $licenseId)
    {
        pos_schema_ensure($con);
        pos_permissions_seed_defaults($con);
        $um = pos_licence_um_row($con, $licenseId);

        $staff = array();
        $staffRows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `pos_staff` WHERE `licenseId`=? ORDER BY `role` ASC, `name` ASC',
            'i',
            (int) $licenseId
        );
        foreach ($staffRows as $row) {
            $overrides = pos_load_staff_overrides($con, $row['id']);
            $effective = pos_effective_permissions($con, $row['role'], $overrides);
            $allowed = array();
            foreach ($effective as $key => $on) {
                if ((int) $on === 1) {
                    $allowed[] = $key;
                }
            }
            $pub = pos_staff_public($row, $effective, $overrides);
            $pub['allowedPermissions'] = $allowed;
            $staff[] = $pub;
        }

        $devices = array();
        $deviceRows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `pos_devices` WHERE `licenseId`=? ORDER BY `lastSeenAt` DESC',
            'i',
            (int) $licenseId
        );
        foreach ($deviceRows as $row) {
            $devices[] = pos_device_public($row);
        }

        $printers = array();
        $printerRows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `store_printers` WHERE `licenseId`=? ORDER BY `isDefault` DESC, `printerName` ASC',
            'i',
            (int) $licenseId
        );
        foreach ($printerRows as $row) {
            $printers[] = pos_printer_public($row);
        }

        $routes = array();
        $routeRows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `printer_routes` WHERE `licenseId`=? ORDER BY `id` ASC',
            'i',
            (int) $licenseId
        );
        foreach ($routeRows as $row) {
            $routes[] = pos_route_public($row);
        }

        return array(
            'userManagementEnabled' => (string) $um['userManagementEnabled'],
            'maxUsers' => (string) $um['maxUsers'],
            'maxDevices' => (string) $um['maxDevices'],
            'maxPrinters' => (string) $um['maxPrinters'],
            'permissionVersion' => (string) $um['permissionVersion'],
            'staffCount' => (string) pos_count_active_staff($con, $licenseId),
            'deviceCount' => (string) pos_count_active_devices($con, $licenseId),
            'printerCount' => (string) pos_count_enabled_printers($con, $licenseId),
            'staffResponse' => $staff,
            'deviceResponse' => $devices,
            'printerResponse' => $printers,
            'routeResponse' => $routes,
        );
    }
}
