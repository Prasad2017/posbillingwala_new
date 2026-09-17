<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../pos_schema.php';
require_once __DIR__ . '/../pos_staff.php';
require_once __DIR__ . '/../pos_devices.php';
require_once __DIR__ . '/../pos_print_ops.php';
require_once __DIR__ . '/../pos_ops_panel.php';

header('Content-Type: application/json; charset=utf-8');
admin_require_auth($con);
$licenseId = isset($_POST['licenseId']) ? (int) $_POST['licenseId'] : 0;
if ($licenseId <= 0) {
    echo json_encode(array('status' => 'false', 'message' => 'licenseId required'));
    exit;
}
$payload = pos_ops_panel_payload($con, $licenseId);
$payload['status'] = 'true';
echo json_encode($payload);
