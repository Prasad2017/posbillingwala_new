<?php
require_once __DIR__ . '/pos_schema.php';

if (!function_exists('pos_audit')) {
    function pos_audit($con, $licenseId, $action, $entityType = null, $entityId = null, $staffId = null, $meta = null)
    {
        pos_schema_ensure($con);
        $um = pos_licence_um_row($con, $licenseId);
        $json = null;
        if (is_array($meta)) {
            unset($meta['pin'], $meta['appLoginPin'], $meta['mpin'], $meta['pinHash'], $meta['password'], $meta['token']);
            $json = json_encode($meta);
        }
        db_stmt_execute(
            $con,
            'INSERT INTO `pos_audit_log` (`organization_id`, `licenseId`, `actorStaffId`, `actorType`, `action`, `entityType`, `entityId`, `metaJson`)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
            'iiisssss',
            (int) $um['userId'],
            (int) $licenseId,
            $staffId ? (int) $staffId : 0,
            'pos',
            $action,
            $entityType,
            $entityId === null ? null : (string) $entityId,
            $json
        );
    }
}
