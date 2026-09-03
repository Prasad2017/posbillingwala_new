<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/licence_expiry.php';

header('Content-Type: application/json; charset=utf-8');
date_default_timezone_set('Asia/Kolkata');

$response = array(
    'status' => '0',
    'period' => 'today',
    'primarySalesLabel' => 'total_sales',
    'primarySales' => '0',
    'todaySales' => '0',
    'monthSales' => '0',
    'allTimeSales' => '0',
    'primarySalesTrend' => '0',
    'todaySalesTrend' => '0',
    'totalSubcategory' => '0',
    'totalProduct' => '0',
    'totalCombo' => '0',
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

$userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
$period = isset($_GET['period']) ? trim($_GET['period']) : 'today';
$licenseId = pos_require_auth($con, $userId, $response);

mysqli_query($con, 'set names utf8');

try {
    $overview = licence_home_sales_overview($con, $licenseId, $period);
    $response = array_merge($response, $overview);
    $response['status'] = 'true';
} catch (Throwable $e) {
    http_response_code(200);
    $response['status'] = '0';
    $response['message'] = 'Failed to load sales overview';
}

mysqli_close($con);
echo json_encode($response);

?>
