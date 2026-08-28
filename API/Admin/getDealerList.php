<?php
/**
 * Admin dealer list with customer counts.
 * GET optional: status=all|active|inactive
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'true', 'dealerResponse' => array());

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['status'] = 'false';
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => 'false', 'dealerResponse' => array()));
mysqli_query($con, 'set names utf8');

$status = isset($_GET['status']) ? strtolower(trim($_GET['status'])) : 'all';
$whereActive = '';
if ($status === 'active') {
    $whereActive = " AND IFNULL(d.is_active,'1') = '1'";
} elseif ($status === 'inactive') {
    $whereActive = " AND IFNULL(d.is_active,'1') = '0'";
}

$rows = db_stmt_fetch_all(
    $con,
    "SELECT d.id, d.name, d.email, d.contact_number, d.aadhar_number, d.address,
            IFNULL(d.is_active,'1') AS is_active, d.created_at,
            COUNT(DISTINCT c.id) AS totalCustomer
     FROM `users` d
     LEFT JOIN `users` c ON c.dealerId = d.id AND c.role_id = '3'
     WHERE d.role_id = '2'" . $whereActive . "
     GROUP BY d.id, d.name, d.email, d.contact_number, d.aadhar_number, d.address, d.is_active, d.created_at
     ORDER BY d.id DESC",
    ''
);

foreach ($rows as $row) {
    $response['dealerResponse'][] = array(
        'id' => (string) $row['id'],
        'name' => isset($row['name']) ? (string) $row['name'] : '',
        'email' => isset($row['email']) ? (string) $row['email'] : '',
        'contact_number' => isset($row['contact_number']) ? (string) $row['contact_number'] : '',
        'aadhar_number' => isset($row['aadhar_number']) ? (string) $row['aadhar_number'] : '',
        'address' => isset($row['address']) ? (string) $row['address'] : '',
        'is_active' => isset($row['is_active']) ? (string) $row['is_active'] : '1',
        'joiningDate' => isset($row['created_at']) ? (string) $row['created_at'] : '',
        'created_at' => isset($row['created_at']) ? (string) $row['created_at'] : '',
        'totalCustomer' => isset($row['totalCustomer']) ? (string) $row['totalCustomer'] : '0'
    );
}

mysqli_close($con);
echo json_encode($response);
