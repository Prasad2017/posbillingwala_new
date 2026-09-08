<?php
/**
 * Owner: export mess members + payments as XLSX.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../mess/mess_member_remote.php';

owner_require_auth($con);

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
$userId = auth_user_id_from_request($con, $userId, 'owner');
if ($userId === null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array('status' => '0', 'message' => 'Invalid or expired auth token'));
    mysqli_close($con);
    exit;
}

$allowed = mess_remote_resolve_licence_ids_for_owner($con, $userId);
$licenceId = isset($_GET['licenceId']) ? (int) $_GET['licenceId'] : 0;

if ($licenceId > 0) {
    if (!mess_remote_assert_licence_access($allowed, $licenceId)) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(array('status' => '0', 'message' => 'Licence not found for this owner'));
        mysqli_close($con);
        exit;
    }
    $licenceIds = array($licenceId);
} else {
    $licenceIds = $allowed;
}

$name = $licenceId > 0
    ? 'mess_members_licence_' . $licenceId . '.xlsx'
    : 'mess_members_owner.xlsx';
mess_remote_export_xlsx($con, $licenceIds, true, $name);
mysqli_close($con);
