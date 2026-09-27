<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'audit.view', false);
if ($actor === null) {
    $actor = pos_require_permission($con, $licenceId, 'report.view');
}

$limit = (int) pos_api_post('limit', '100');
if ($limit < 1) { $limit = 100; }
if ($limit > 500) { $limit = 500; }

$rows = db_stmt_fetch_all(
    $con,
    'SELECT `id`,`actorStaffId`,`actorType`,`action`,`entityType`,`entityId`,`metaJson`,`createdAt`
     FROM `pos_audit_log` WHERE `licenseId`=?
     ORDER BY `createdAt` DESC LIMIT ' . (int) $limit,
    'i',
    (int) $licenceId
);

$list = array();
foreach ($rows as $row) {
    $meta = null;
    if (!empty($row['metaJson'])) {
        $decoded = json_decode($row['metaJson'], true);
        if (is_array($decoded)) {
            $meta = $decoded;
        }
    }
    $list[] = array(
        'id' => (string) $row['id'],
        'actorStaffId' => (int) $row['actorStaffId'],
        'actorType' => $row['actorType'],
        'action' => $row['action'],
        'entityType' => $row['entityType'],
        'entityId' => $row['entityId'],
        'meta' => $meta,
        'createdAt' => $row['createdAt'],
    );
}

pos_api_json(array('status' => '1', 'auditResponse' => $list));
