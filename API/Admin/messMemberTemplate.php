<?php
/**
 * Admin: empty mess member/payment Excel template.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../mess/mess_member_remote.php';

admin_require_auth($con);

$adminId = isset($_GET['userId']) ? $_GET['userId'] : '';
$adminId = auth_user_id_from_request($con, $adminId, 'admin');
if ($adminId === null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array('status' => '0', 'message' => 'Invalid or expired auth token'));
    mysqli_close($con);
    exit;
}

mess_remote_stream_template('mess_member_template.xlsx');
mysqli_close($con);
