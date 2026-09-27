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

$perm = 'inventory.manage';
if ($entity === 'offer') {
    $perm = 'offer.manage';
} elseif ($entity === 'approval') {
    $perm = 'approval.manage';
}
$actor = pos_require_permission($con, $licenceId, $perm);
$orgId = enterprise_org_id($con, $licenceId);

$clientId = pos_api_post('clientId');
$title = pos_api_post('title');
if ($clientId === '') {
    pos_api_json(array('status' => '0', 'message' => 'clientId is required'));
}
if ($title === '') {
    $title = $clientId;
}

$status = strtoupper(pos_api_post('status', 'ACTIVE'));
$payloadJson = pos_api_post('payloadJson', '{}');
if ($payloadJson === '') {
    $payloadJson = '{}';
}
$decoded = json_decode($payloadJson, true);
if (!is_array($decoded)) {
    pos_api_json(array('status' => '0', 'message' => 'payloadJson must be valid JSON'));
}
$payloadJson = json_encode($decoded);

$existing = db_stmt_fetch_one(
    $con,
    'SELECT `id` FROM `pos_ops_records` WHERE `licenseId`=? AND `entityType`=? AND `clientId`=? LIMIT 1',
    'iss',
    (int) $licenceId,
    $entity,
    $clientId
);

if ($existing !== null) {
    db_stmt_execute(
        $con,
        'UPDATE `pos_ops_records` SET `title`=?, `status`=?, `payloadJson`=? WHERE `licenseId`=? AND `entityType`=? AND `clientId`=?',
        'sssiss',
        $title,
        $status,
        $payloadJson,
        (int) $licenceId,
        $entity,
        $clientId
    );
} else {
    db_stmt_execute(
        $con,
        'INSERT INTO `pos_ops_records` (`organization_id`,`licenseId`,`entityType`,`clientId`,`title`,`status`,`payloadJson`) VALUES (?,?,?,?,?,?,?)',
        'iisssss',
        $orgId,
        (int) $licenceId,
        $entity,
        $clientId,
        $title,
        $status,
        $payloadJson
    );
}

pos_audit(
    $con,
    $licenceId,
    ucfirst($entity) . ($existing ? ' Updated' : ' Created'),
    $entity,
    $clientId,
    isset($actor['id']) ? $actor['id'] : 0,
    array('title' => $title, 'status' => $status)
);

pos_api_json(array('status' => '1', 'message' => 'Saved', 'clientId' => $clientId, 'entity' => $entity));
