<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);

$entity = strtolower(trim(pos_api_post('entity')));
$allowed = array('warehouse','brand','offer','approval','transfer','lot','return','serial');
if (!in_array($entity, $allowed, true)) {
    pos_api_json(array('status' => '0', 'message' => 'Invalid entity'));
}

$perm = 'inventory.view';
if ($entity === 'offer') {
    $perm = 'offer.view';
} elseif ($entity === 'approval') {
    $perm = 'approval.view';
}
pos_require_permission($con, $licenceId, $perm);

$statusFilter = strtoupper(trim(pos_api_post('status', '')));

if ($statusFilter !== '') {
    $rows = db_stmt_fetch_all(
        $con,
        'SELECT `clientId`,`title`,`status`,`payloadJson`,`createdAt`,`updatedAt`
         FROM `pos_ops_records` WHERE `licenseId`=? AND `entityType`=? AND `status`=?
         ORDER BY `updatedAt` DESC LIMIT 500',
        'iss',
        (int) $licenceId,
        $entity,
        $statusFilter
    );
} else {
    $rows = db_stmt_fetch_all(
        $con,
        'SELECT `clientId`,`title`,`status`,`payloadJson`,`createdAt`,`updatedAt`
         FROM `pos_ops_records` WHERE `licenseId`=? AND `entityType`=?
         ORDER BY `updatedAt` DESC LIMIT 500',
        'is',
        (int) $licenceId,
        $entity
    );
}

$list = array();
foreach ($rows as $row) {
    $payload = array();
    if (!empty($row['payloadJson'])) {
        $decoded = json_decode($row['payloadJson'], true);
        if (is_array($decoded)) {
            $payload = $decoded;
        }
    }
    $item = $payload;
    $item['id'] = $row['clientId'];
    $item['title'] = $row['title'];
    $item['status'] = $row['status'];
    $item['createdAt'] = $row['createdAt'];
    $item['updatedAt'] = $row['updatedAt'];
    $list[] = $item;
}

pos_api_json(array('status' => '1', 'opsResponse' => $list, 'entity' => $entity));
