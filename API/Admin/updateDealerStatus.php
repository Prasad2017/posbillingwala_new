<?php
include_once('config.php');
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/auth_guard.php';

/**
 * Soft deactivate / reactivate dealer (is_active toggle).
 * POST: userId, action = deactivate | activate
 */
$response = array('status' => '0', 'message' => 'update failed');

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    admin_require_auth($con);
    mysqli_query($con, 'set names utf8');

    $userId = isset($_POST['userId']) ? trim($_POST['userId']) : '';
    $action = isset($_POST['action']) ? strtolower(trim($_POST['action'])) : '';

    if ($userId === '' || ($action !== 'deactivate' && $action !== 'activate')) {
        $response['message'] = 'userId and action (deactivate|activate) are required.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $row = db_stmt_fetch_one(
        $con,
        'SELECT id, role_id, is_active FROM `users` WHERE `id`=? AND `role_id`=\'2\' LIMIT 1',
        'i',
        (int) $userId
    );
    if ($row === null) {
        $response['message'] = 'Dealer not found.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $isActive = ($action === 'activate') ? '1' : '0';
    $ok = db_stmt_execute(
        $con,
        'UPDATE `users` SET `is_active`=? WHERE `id`=? AND `role_id`=\'2\'',
        'si',
        $isActive,
        (int) $userId
    );
    if ($ok) {
        $response['status'] = '1';
        $response['message'] = $action === 'activate' ? 'Dealer activated.' : 'Dealer deactivated.';
        $response['is_active'] = $isActive;
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
