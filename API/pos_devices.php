<?php
require_once __DIR__ . '/pos_schema.php';
require_once __DIR__ . '/pos_audit.php';

if (!function_exists('pos_normalize_platform')) {
    function pos_normalize_platform($platform)
    {
        $p = strtoupper(trim((string) $platform));
        if (in_array($p, array('ANDROID', 'IOS', 'WEB'), true)) {
            return $p;
        }
        return 'ANDROID';
    }
}

if (!function_exists('pos_count_active_devices')) {
    function pos_count_active_devices($con, $licenseId)
    {
        $registered = db_stmt_scalar_int(
            $con,
            "SELECT COUNT(*) AS c FROM `pos_devices` WHERE `licenseId`=? AND `status`='ACTIVE'",
            'i',
            (int) $licenseId
        );
        $lic = pos_licence_um_row($con, $licenseId);
        $primary = isset($lic['android_device_id']) ? trim((string) $lic['android_device_id']) : '';
        if ($primary === '') {
            return $registered;
        }
        $hasPrimary = db_stmt_scalar_int(
            $con,
            "SELECT COUNT(*) AS c FROM `pos_devices` WHERE `licenseId`=? AND `deviceId`=? AND `status`='ACTIVE'",
            'is',
            (int) $licenseId,
            $primary
        );
        return $registered + ($hasPrimary > 0 ? 0 : 1);
    }
}

if (!function_exists('pos_device_authorized')) {
    function pos_device_authorized($con, $licenseRow, $deviceId)
    {
        $deviceId = trim((string) $deviceId);
        if ($deviceId === '') {
            return false;
        }
        $primary = isset($licenseRow['android_device_id']) ? trim((string) $licenseRow['android_device_id']) : '';
        if ($primary !== '' && $primary === $deviceId) {
            return true;
        }
        $licenseId = isset($licenseRow['id']) ? (int) $licenseRow['id'] : 0;
        if ($licenseId <= 0) {
            return false;
        }
        if (!pos_um_enabled($con, $licenseId)) {
            return $primary === $deviceId;
        }
        $row = db_stmt_fetch_one(
            $con,
            "SELECT `id` FROM `pos_devices` WHERE `licenseId`=? AND `deviceId`=? AND `status`='ACTIVE' LIMIT 1",
            'is',
            $licenseId,
            $deviceId
        );
        return $row !== null;
    }
}

if (!function_exists('pos_device_can_bind_additional')) {
    function pos_device_can_bind_additional($con, $licenseId, $deviceId)
    {
        if (!pos_um_enabled($con, $licenseId)) {
            return false;
        }
        $deviceId = trim((string) $deviceId);
        $existing = db_stmt_fetch_one(
            $con,
            "SELECT `id` FROM `pos_devices` WHERE `licenseId`=? AND `deviceId`=? LIMIT 1",
            'is',
            (int) $licenseId,
            $deviceId
        );
        if ($existing !== null) {
            return true;
        }
        $um = pos_licence_um_row($con, $licenseId);
        $max = max(1, (int) $um['maxDevices']);
        return pos_count_active_devices($con, $licenseId) < $max;
    }
}

if (!function_exists('pos_device_register')) {
    function pos_device_register($con, $licenseId, $deviceId, $deviceName = '', $platform = 'ANDROID', $appVersion = '', $osVersion = '')
    {
        pos_schema_ensure($con);
        $um = pos_licence_um_row($con, $licenseId);
        $deviceId = trim((string) $deviceId);
        if ($deviceId === '') {
            return array('ok' => false, 'message' => 'Device id required');
        }
        $existing = db_stmt_fetch_one(
            $con,
            'SELECT * FROM `pos_devices` WHERE `licenseId`=? AND `deviceId`=? LIMIT 1',
            'is',
            (int) $licenseId,
            $deviceId
        );
        $now = date('Y-m-d H:i:s');
        $platform = pos_normalize_platform($platform);
        if ($existing !== null) {
            db_stmt_execute(
                $con,
                'UPDATE `pos_devices` SET `deviceName`=?, `platform`=?, `appVersion`=?, `osVersion`=?, `status`=\'ACTIVE\', `lastSeenAt`=? WHERE `id`=?',
                'sssssi',
                $deviceName,
                $platform,
                $appVersion,
                $osVersion,
                $now,
                (int) $existing['id']
            );
            return array('ok' => true, 'id' => (int) $existing['id']);
        }
        $max = max(1, (int) $um['maxDevices']);
        if (pos_count_active_devices($con, $licenseId) >= $max) {
            return array('ok' => false, 'message' => 'Device limit reached for this licence');
        }
        $id = db_stmt_insert_id(
            $con,
            'INSERT INTO `pos_devices` (`organization_id`, `licenseId`, `deviceId`, `deviceName`, `platform`, `appVersion`, `osVersion`, `status`, `lastSeenAt`)
             VALUES (?, ?, ?, ?, ?, ?, ?, \'ACTIVE\', ?)',
            'iissssss',
            (int) $um['userId'],
            (int) $licenseId,
            $deviceId,
            $deviceName,
            $platform,
            $appVersion,
            $osVersion,
            $now
        );
        if ($id === false) {
            return array('ok' => false, 'message' => 'Unable to register device');
        }
        pos_audit($con, $licenseId, 'Device Registered', 'device', $id, null, array('deviceId' => $deviceId, 'platform' => $platform));
        return array('ok' => true, 'id' => (int) $id);
    }
}
