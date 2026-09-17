<?php
require_once __DIR__ . '/pos_schema.php';
require_once __DIR__ . '/pos_audit.php';

if (!function_exists('pos_printer_public')) {
    function pos_printer_public(array $row)
    {
        return array(
            'id' => (string) $row['id'],
            'printerName' => $row['printerName'],
            'printerType' => $row['printerType'],
            'connectionType' => pos_normalize_connection_type(isset($row['connectionType']) ? $row['connectionType'] : 'BLUETOOTH'),
            'ipAddress' => isset($row['ipAddress']) ? $row['ipAddress'] : '',
            'port' => isset($row['port']) ? (string) $row['port'] : '9100',
            'bluetoothAddress' => isset($row['bluetoothAddress']) ? $row['bluetoothAddress'] : '',
            'usbIdentifier' => isset($row['usbIdentifier']) ? $row['usbIdentifier'] : '',
            'usbName' => isset($row['usbName']) ? $row['usbName'] : '',
            'paperSize' => isset($row['paperSize']) ? pos_normalize_paper_size($row['paperSize']) : '2-Inch',
            'purpose' => $row['purpose'],
            'area' => $row['area'],
            'status' => $row['status'],
            'enabled' => (string) $row['enabled'],
            'isDefault' => (string) $row['isDefault'],
            'isBackup' => (string) $row['isBackup'],
            'primaryPrinterId' => isset($row['primaryPrinterId']) ? (string) $row['primaryPrinterId'] : '',
            'deviceId' => isset($row['deviceId']) ? $row['deviceId'] : '',
            'lastHeartbeatAt' => isset($row['lastHeartbeatAt']) ? $row['lastHeartbeatAt'] : '',
            'lastPrintAt' => isset($row['lastPrintAt']) ? $row['lastPrintAt'] : '',
        );
    }
}

if (!function_exists('pos_count_enabled_printers')) {
    function pos_count_enabled_printers($con, $licenseId)
    {
        return db_stmt_scalar_int(
            $con,
            'SELECT COUNT(*) AS c FROM `store_printers` WHERE `licenseId`=? AND `enabled`=1',
            'i',
            (int) $licenseId
        );
    }
}

if (!function_exists('pos_printer_belongs')) {
    function pos_printer_belongs($con, $licenseId, $printerId)
    {
        return db_stmt_fetch_one(
            $con,
            'SELECT * FROM `store_printers` WHERE `id`=? AND `licenseId`=? LIMIT 1',
            'ii',
            (int) $printerId,
            (int) $licenseId
        );
    }
}

if (!function_exists('pos_print_job_create')) {
    function pos_print_job_create($con, $licenseId, array $fields)
    {
        pos_schema_ensure($con);
        $um = pos_licence_um_row($con, $licenseId);
        $idem = isset($fields['idempotencyKey']) ? trim((string) $fields['idempotencyKey']) : '';
        if ($idem === '') {
            $idem = uniqid('pj_', true);
        }
        $existing = db_stmt_fetch_one(
            $con,
            'SELECT * FROM `print_jobs` WHERE `licenseId`=? AND `idempotencyKey`=? LIMIT 1',
            'is',
            (int) $licenseId,
            $idem
        );
        if ($existing !== null) {
            return $existing;
        }
        $printerId = (int) $fields['printerId'];
        $printer = pos_printer_belongs($con, $licenseId, $printerId);
        if ($printer === null || (int) $printer['enabled'] !== 1) {
            return null;
        }
        $payload = isset($fields['payload']) ? $fields['payload'] : '';
        if (is_array($payload)) {
            $payload = json_encode($payload);
        }
        $id = db_stmt_insert_id(
            $con,
            'INSERT INTO `print_jobs`
             (`organization_id`, `licenseId`, `createdByStaffId`, `createdByDeviceId`, `printerId`, `hostDeviceId`,
              `documentType`, `documentId`, `payload`, `status`, `priority`, `idempotencyKey`)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, \'QUEUED\', ?, ?)',
            'iiisissssis',
            (int) $um['userId'],
            (int) $licenseId,
            isset($fields['staffId']) ? (int) $fields['staffId'] : 0,
            isset($fields['deviceId']) ? (string) $fields['deviceId'] : '',
            $printerId,
            isset($printer['deviceId']) ? (string) $printer['deviceId'] : '',
            isset($fields['documentType']) ? (string) $fields['documentType'] : 'KOT',
            isset($fields['documentId']) ? (string) $fields['documentId'] : '',
            (string) $payload,
            isset($fields['priority']) ? (int) $fields['priority'] : 0,
            $idem
        );
        if ($id === false) {
            return db_stmt_fetch_one(
                $con,
                'SELECT * FROM `print_jobs` WHERE `licenseId`=? AND `idempotencyKey`=? LIMIT 1',
                'is',
                (int) $licenseId,
                $idem
            );
        }
        pos_audit($con, $licenseId, 'Print Job Created', 'print_job', $id, isset($fields['staffId']) ? $fields['staffId'] : null, array(
            'documentType' => isset($fields['documentType']) ? $fields['documentType'] : 'KOT',
            'printerId' => $printerId,
        ));
        return db_stmt_fetch_one($con, 'SELECT * FROM `print_jobs` WHERE `id`=? LIMIT 1', 'i', (int) $id);
    }
}

if (!function_exists('pos_print_job_ack')) {
    function pos_print_job_ack($con, $licenseId, $jobId, $status, $errorMessage = '')
    {
        $job = db_stmt_fetch_one(
            $con,
            'SELECT * FROM `print_jobs` WHERE `id`=? AND `licenseId`=? LIMIT 1',
            'ii',
            (int) $jobId,
            (int) $licenseId
        );
        if ($job === null) {
            return null;
        }
        $status = strtoupper(trim((string) $status));
        $map = array(
            'JOB_RECEIVED' => 'RECEIVED',
            'RECEIVED' => 'RECEIVED',
            'PRINT_STARTED' => 'PRINTING',
            'PRINTING' => 'PRINTING',
            'SENT' => 'SENT',
            'PRINT_SUCCESS' => 'PRINTED',
            'PRINTED' => 'PRINTED',
            'PRINT_FAILED' => 'FAILED',
            'FAILED' => 'FAILED',
            'CANCELLED' => 'CANCELLED',
        );
        if (!isset($map[$status])) {
            return $job;
        }
        $next = $map[$status];
        $printedAt = $next === 'PRINTED' ? date('Y-m-d H:i:s') : null;
        $failedAt = $next === 'FAILED' ? date('Y-m-d H:i:s') : null;
        db_stmt_execute(
            $con,
            'UPDATE `print_jobs` SET `status`=?, `errorMessage`=?, `printedAt`=COALESCE(?, `printedAt`), `failedAt`=COALESCE(?, `failedAt`) WHERE `id`=?',
            'ssssi',
            $next,
            $errorMessage,
            $printedAt,
            $failedAt,
            (int) $jobId
        );
        if ($next === 'PRINTED') {
            db_stmt_execute(
                $con,
                'UPDATE `store_printers` SET `lastPrintAt`=?, `status`=\'ONLINE\' WHERE `id`=?',
                'si',
                date('Y-m-d H:i:s'),
                (int) $job['printerId']
            );
        }
        if ($next === 'FAILED') {
            pos_audit($con, $licenseId, 'Print Job Failed', 'print_job', $jobId, null, array('error' => $errorMessage));
        }
        return db_stmt_fetch_one($con, 'SELECT * FROM `print_jobs` WHERE `id`=? LIMIT 1', 'i', (int) $jobId);
    }
}

if (!function_exists('pos_print_job_claim_for_host')) {
    function pos_print_job_claim_for_host($con, $licenseId, $hostDeviceId, $limit = 10)
    {
        $limit = max(1, min(50, (int) $limit));
        $printers = db_stmt_fetch_all(
            $con,
            'SELECT `id` FROM `store_printers` WHERE `licenseId`=? AND `enabled`=1 AND (`deviceId`=? OR `deviceId` IS NULL OR `deviceId`=\'\')',
            'is',
            (int) $licenseId,
            $hostDeviceId
        );
        if (empty($printers)) {
            $printers = db_stmt_fetch_all(
                $con,
                'SELECT `id` FROM `store_printers` WHERE `licenseId`=? AND `enabled`=1 AND `deviceId`=?',
                'is',
                (int) $licenseId,
                $hostDeviceId
            );
        }
        $ids = array();
        foreach ($printers as $p) {
            $ids[] = (int) $p['id'];
        }
        if (empty($ids)) {
            return array();
        }
        $in = implode(',', $ids);
        $jobs = db_stmt_fetch_all(
            $con,
            "SELECT * FROM `print_jobs` WHERE `licenseId`=? AND `status` IN ('QUEUED','SENT') AND `printerId` IN ($in)
             ORDER BY `priority` DESC, `id` ASC LIMIT $limit",
            'i',
            (int) $licenseId
        );
        foreach ($jobs as $job) {
            db_stmt_execute(
                $con,
                'UPDATE `print_jobs` SET `status`=\'SENT\', `hostDeviceId`=? WHERE `id`=? AND `status` IN (\'QUEUED\',\'SENT\')',
                'si',
                $hostDeviceId,
                (int) $job['id']
            );
        }
        return $jobs;
    }
}

if (!function_exists('pos_print_host_heartbeat')) {
    function pos_print_host_heartbeat($con, $licenseId, $deviceId, $platform = 'ANDROID')
    {
        pos_schema_ensure($con);
        $um = pos_licence_um_row($con, $licenseId);
        $now = date('Y-m-d H:i:s');
        $existing = db_stmt_fetch_one(
            $con,
            'SELECT `id` FROM `print_hosts` WHERE `licenseId`=? AND `deviceId`=? LIMIT 1',
            'is',
            (int) $licenseId,
            $deviceId
        );
        if ($existing !== null) {
            db_stmt_execute(
                $con,
                'UPDATE `print_hosts` SET `status`=\'ONLINE\', `platform`=?, `lastHeartbeatAt`=? WHERE `id`=?',
                'ssi',
                pos_normalize_platform($platform),
                $now,
                (int) $existing['id']
            );
        } else {
            db_stmt_insert_id(
                $con,
                'INSERT INTO `print_hosts` (`organization_id`, `licenseId`, `deviceId`, `platform`, `status`, `lastHeartbeatAt`) VALUES (?, ?, ?, ?, \'ONLINE\', ?)',
                'iisss',
                (int) $um['userId'],
                (int) $licenseId,
                $deviceId,
                pos_normalize_platform($platform),
                $now
            );
        }
        db_stmt_execute(
            $con,
            'UPDATE `pos_devices` SET `isPrintHost`=1, `lastSeenAt`=? WHERE `licenseId`=? AND `deviceId`=?',
            'sis',
            $now,
            (int) $licenseId,
            $deviceId
        );
    }
}
