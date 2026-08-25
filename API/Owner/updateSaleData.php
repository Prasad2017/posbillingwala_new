<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);
require_once __DIR__ . '/../branch_scope.php';
require_once __DIR__ . '/../db_prepared.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    mysqli_query($con, 'set names utf8');

    $ownerUserId = owner_resolve_user_id($con, '');
    $licenseId = isset($_POST['licenseId']) ? $_POST['licenseId'] : '';
    $totalSaleData = isset($_POST['totalSaleData']) ? $_POST['totalSaleData'] : '';
    $todaySaleData = isset($_POST['todaySaleData']) ? $_POST['todaySaleData'] : '';

    if ($ownerUserId === null || $licenseId === '' || !branch_owner_owns_branch($con, $ownerUserId, $licenseId)) {
        $response['status'] = '0';
        $response['message'] = 'Unauthorized';
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }

    $ok = db_stmt_execute(
        $con,
        'UPDATE `licenses` SET `total_sale_data`=?, `today_sale_data`=? WHERE `id`=?',
        'ssi',
        $totalSaleData,
        $todaySaleData,
        (int) $licenseId
    );

    if ($ok) {
        $response['status'] = '1';
        $response['message'] = 'update successful!';
    } else {
        $response['status'] = '0';
        $response['message'] = 'update failed...';
    }
}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
