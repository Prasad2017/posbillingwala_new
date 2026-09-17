<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../pos_schema.php';
require_once __DIR__ . '/../pos_staff.php';
require_once __DIR__ . '/../pos_devices.php';
require_once __DIR__ . '/../pos_print_ops.php';
require_once __DIR__ . '/../pos_ops_panel.php';
require_once __DIR__ . '/../branch_scope.php';

header('Content-Type: application/json; charset=utf-8');
owner_require_auth($con);
$licenseId = isset($_POST['licenseId']) ? (int) $_POST['licenseId'] : 0;
$ownerId = owner_resolve_user_id($con, isset($_POST['userId']) ? $_POST['userId'] : '');
if ($licenseId <= 0 || $ownerId === null || $ownerId === '') {
    echo json_encode(array('status' => '0', 'message' => 'Unauthorized'));
    exit;
}
$scope = branch_scope_from_license($con, $licenseId);
if ($scope === null || (string) $scope['organizationId'] !== (string) $ownerId) {
    echo json_encode(array('status' => '0', 'message' => 'Store not found'));
    exit;
}
$payload = pos_ops_panel_payload($con, $licenseId);
$payload['status'] = '1';
echo json_encode($payload);
