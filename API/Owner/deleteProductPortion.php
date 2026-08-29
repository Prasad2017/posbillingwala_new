<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);

require_once __DIR__ . '/../auth_tokens.php';
require_once dirname(__DIR__) . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

$userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
$userId = auth_user_id_from_request($con, $userId, 'owner');
if ($userId === null) {
    $response['message'] = 'Unauthorized';
    echo json_encode($response);
    exit;
}

$portionId = isset($_POST['portionId']) ? trim((string) $_POST['portionId']) : '';
if ($portionId === '') {
    $response['message'] = 'portionId is required';
    echo json_encode($response);
    exit;
}

$row = db_stmt_fetch_one(
    $con,
    'SELECT pp.portionId FROM `product_portions` pp
     INNER JOIN `products` p ON p.productId = pp.productId
     WHERE pp.portionId=? AND p.userId=? LIMIT 1',
    'ss',
    $portionId,
    $userId
);

if ($row === null) {
    $response['message'] = 'Portion not found';
    echo json_encode($response);
    exit;
}

$ok = db_stmt_execute(
    $con,
    "UPDATE `product_portions` SET `portionStatus`='deactive', `updated_at`=NOW() WHERE `portionId`=?",
    's',
    $portionId
);
$response['status'] = $ok ? '1' : '0';
$response['message'] = $ok ? 'portion delete successfully' : 'portion failed to delete';
echo json_encode($response);
?>
