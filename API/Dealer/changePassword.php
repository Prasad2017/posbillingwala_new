<?php
include_once 'config.php';
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../auth_tokens.php';
require_once __DIR__ . '/../db_prepared.php';

mysqli_query($con, 'set names utf8');
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Unable to change password.',
);

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    $response['message'] = 'Use Post Method';
    echo json_encode($response);
    exit;
}

dealer_require_auth($con);

$userId = auth_user_id_from_request(
    $con,
    isset($_POST['userId']) ? trim((string) $_POST['userId']) : '',
    'dealer'
);

if ($userId === null) {
    $response['message'] = 'Unauthorized';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$currentPassword = isset($_POST['currentPassword']) ? (string) $_POST['currentPassword'] : '';
$newPassword = isset($_POST['newPassword']) ? (string) $_POST['newPassword'] : '';

if ($currentPassword === '' || $newPassword === '') {
    $response['message'] = 'Current and new password are required.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if (strlen($newPassword) < 6) {
    $response['message'] = 'New password must be at least 6 characters.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if ($currentPassword === $newPassword) {
    $response['message'] = 'New password must be different from current password.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$dealer = db_stmt_fetch_one(
    $con,
    "SELECT `id`, `password`, `is_active`, `role_id`
     FROM `users`
     WHERE `id`=? AND `role_id`='2'
     LIMIT 1",
    'i',
    (int) $userId
);

if ($dealer === null || (int) $dealer['is_active'] !== 1) {
    $response['message'] = 'Dealer account not found or disabled.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$storedPassword = isset($dealer['password']) ? (string) $dealer['password'] : '';
if ($storedPassword === '' || strpos($storedPassword, '$2') !== 0) {
    $response['message'] = 'Password not set for this account.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if (!password_verify($currentPassword, $storedPassword)) {
    $response['message'] = 'Current password is incorrect.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$hash = password_hash($newPassword, PASSWORD_BCRYPT);
$ok = db_stmt_execute(
    $con,
    'UPDATE `users` SET `password`=? WHERE `id`=? AND `role_id`=\'2\'',
    'si',
    $hash,
    (int) $userId
);

if ($ok) {
    $response['status'] = '1';
    $response['message'] = 'Password changed successfully.';
} else {
    $response['message'] = 'Unable to update password.';
}

mysqli_close($con);
echo json_encode($response);
