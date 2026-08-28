<?php
/**
 * Dealer/admin profile by userId.
 * GET: userId
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'true', 'customerResponse' => array(), 'dealerResponse' => array());

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['status'] = 'false';
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => 'false', 'customerResponse' => array(), 'dealerResponse' => array()));
mysqli_query($con, 'set names utf8');

$userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
if ($userId === '') {
    $response['status'] = 'false';
    $response['message'] = 'userId required';
    echo json_encode($response);
    exit;
}

$row = db_stmt_fetch_one(
    $con,
    "SELECT id, name, email, contact_number, aadhar_number, address, shopName, role_id,
            IFNULL(is_active,'1') AS is_active, created_at
     FROM `users` WHERE id = ? LIMIT 1",
    'i',
    (int) $userId
);

if ($row === null) {
    $response['status'] = 'false';
    $response['message'] = 'User not found';
    echo json_encode($response);
    exit;
}

$item = array(
    'id' => (string) $row['id'],
    'name' => isset($row['name']) ? (string) $row['name'] : '',
    'email' => isset($row['email']) ? (string) $row['email'] : '',
    'contact_number' => isset($row['contact_number']) ? (string) $row['contact_number'] : '',
    'aadhar_number' => isset($row['aadhar_number']) ? (string) $row['aadhar_number'] : '',
    'address' => isset($row['address']) ? (string) $row['address'] : '',
    'shopName' => isset($row['shopName']) ? (string) $row['shopName'] : '',
    'is_active' => isset($row['is_active']) ? (string) $row['is_active'] : '1',
    'joiningDate' => isset($row['created_at']) ? (string) $row['created_at'] : '',
    'created_at' => isset($row['created_at']) ? (string) $row['created_at'] : ''
);

// Keep customerResponse for existing DealerProfile compatibility; also dealerResponse for CRM.
$response['customerResponse'][] = $item;
if ((string) $row['role_id'] === '2') {
    $totalCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `users` WHERE role_id='3' AND dealerId=?",
        'i',
        (int) $userId
    );
    $item['totalCustomer'] = $totalCustomer;
    $response['dealerResponse'][] = $item;
}

mysqli_close($con);
echo json_encode($response);
