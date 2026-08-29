<?php
/**
 * Catalog counts for a customer (categories / subcategories / products).
 * GET: customerId (or userId)
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array(
    'status' => 'false',
    'categoryCount' => '0',
    'subcategoryCount' => '0',
    'productCount' => '0',
    'portionCount' => '0'
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, $response);

$customerId = isset($_GET['customerId']) ? trim($_GET['customerId']) : '';
if ($customerId === '' && isset($_GET['userId'])) {
    $customerId = trim($_GET['userId']);
}
if ($customerId === '') {
    $response['message'] = 'customerId required';
    echo json_encode($response);
    exit;
}

$uid = (int) $customerId;

$categoryCount = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `categories` WHERE `userId`=?",
    'i',
    $uid
);

$subcategoryCount = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `product_subcategories` WHERE `userId`=?",
    'i',
    $uid
);

$productCount = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `products` WHERE `userId`=?",
    'i',
    $uid
);

$portionCount = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `portion_master` WHERE `userId`=? AND `portionMasterStatus` IN ('active','inactive')",
    'i',
    $uid
);

$response = array(
    'status' => 'true',
    'categoryCount' => $categoryCount !== '' ? $categoryCount : '0',
    'subcategoryCount' => $subcategoryCount !== '' ? $subcategoryCount : '0',
    'productCount' => $productCount !== '' ? $productCount : '0',
    'portionCount' => $portionCount !== '' ? $portionCount : '0'
);

mysqli_close($con);
echo json_encode($response);
