<?php
/**
 * Owner: read business template for a licence (outlet) the owner owns.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../branch_scope.php';
require_once __DIR__ . '/../business_template_ops.php';

owner_require_auth($con);

$response = array('status' => '1', 'message' => 'ok', 'businessTemplateResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $postedUserId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $ownerId = owner_resolve_user_id($con, $postedUserId);
    if ($ownerId === null) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(array('status' => '0', 'message' => 'Unauthorized'));
        mysqli_close($con);
        exit;
    }

    $licenceId = isset($_GET['licenceId']) ? trim($_GET['licenceId']) : '';
    if ($licenceId === '') {
        $response['status'] = '0';
        $response['message'] = 'Missing licenceId';
    } elseif (!branch_owner_require_branch_access($con, $ownerId, $licenceId, $response)) {
        // message set by helper
    } else {
        $response['businessTemplateResponse'][] = business_template_fetch($con, $licenceId);
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
