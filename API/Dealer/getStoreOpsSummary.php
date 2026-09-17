<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../pos_schema.php';
require_once __DIR__ . '/../pos_staff.php';
require_once __DIR__ . '/../pos_devices.php';
require_once __DIR__ . '/../pos_print_ops.php';
require_once __DIR__ . '/../pos_ops_panel.php';
require_once __DIR__ . '/../auth_tokens.php';

header('Content-Type: application/json; charset=utf-8');
dealer_require_auth($con);
$licenseId = isset($_POST['licenseId']) ? (int) $_POST['licenseId'] : 0;
$dealerId = auth_user_id_from_request($con, isset($_POST['userId']) ? $_POST['userId'] : '', 'dealer');
if ($licenseId <= 0) {
    echo json_encode(array('status' => 'false', 'message' => 'licenseId required'));
    exit;
}
$owned = db_stmt_fetch_one(
    $con,
    'SELECT l.id FROM licenses l INNER JOIN users u ON u.id = l.userId WHERE l.id=? AND u.dealerId=? LIMIT 1',
    'ii',
    $licenseId,
    (int) $dealerId
);
if ($owned === null) {
    echo json_encode(array('status' => 'false', 'message' => 'Licence not found'));
    exit;
}
$payload = pos_ops_panel_payload($con, $licenseId);
$payload['status'] = 'true';
echo json_encode($payload);
