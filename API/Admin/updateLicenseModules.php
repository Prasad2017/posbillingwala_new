<?php
/**
 * Update Fast Billing / Dine In / Take Away / Mess flags on a license.
 * POST: licensesId, fastBilling, takeAway, dineIn, mess (0 or 1)
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'update failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    $response['message'] = 'Use POST';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, $response);
mysqli_query($con, 'set names utf8');

$licensesId = isset($_POST['licensesId']) ? (int) $_POST['licensesId'] : 0;
$fastBilling = isset($_POST['fastBilling']) && (string) $_POST['fastBilling'] === '1' ? 1 : 0;
$takeAway = isset($_POST['takeAway']) && (string) $_POST['takeAway'] === '1' ? 1 : 0;
$dineIn = isset($_POST['dineIn']) && (string) $_POST['dineIn'] === '1' ? 1 : 0;
$mess = isset($_POST['mess']) && (string) $_POST['mess'] === '1' ? 1 : 0;

if ($licensesId <= 0) {
    $response['message'] = 'licensesId is required.';
    echo json_encode($response);
    exit;
}

$ok = db_stmt_execute(
    $con,
    "UPDATE `licenses` SET `fastBilling`=?, `takeAway`=?, `dineIn`=?, `mess`=? WHERE `id`=?",
    'iiiii',
    $fastBilling,
    $takeAway,
    $dineIn,
    $mess,
    $licensesId
);

if ($ok) {
    $response['status'] = '1';
    $response['message'] = 'Modules updated.';
} else {
    $response['message'] = 'Unable to update modules.';
}

mysqli_close($con);
echo json_encode($response);
