<?php
/**
 * Admin: update resolution notes on an error log.
 * POST: id, resolutionNotes, resolvedBy (optional)
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../log_sanitizer.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'Failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

admin_require_auth($con, $response);
mysqli_query($con, 'set names utf8');
date_default_timezone_set('Asia/Kolkata');

$id = isset($_POST['id']) ? (int) $_POST['id'] : 0;
$notes = log_post_str('resolutionNotes', 4096);
$resolvedBy = log_post_str('resolvedBy', 128);
if ($id <= 0) {
    $response['message'] = 'Missing id';
    echo json_encode($response);
    exit;
}

$now = date('Y-m-d H:i:s');
$ok = db_stmt_execute(
    $con,
    'UPDATE `error_logs` SET `resolution_notes`=?, `resolved_at`=?, `resolved_by`=? WHERE `id`=?',
    'sssi',
    $notes,
    $now,
    $resolvedBy,
    $id
);

$response['status'] = $ok ? '1' : '0';
$response['message'] = $ok ? 'updated' : 'update failed';
echo json_encode($response);
?>
