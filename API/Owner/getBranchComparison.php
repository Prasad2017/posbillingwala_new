<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
include_once(__DIR__ . '/../branch_scope.php');
require_once __DIR__ . '/../auth_tokens.php';

/**
 * Owner branch comparison — all branches in org with sales metrics side-by-side.
 */
$response = array('branches' => array(), 'organizationId' => '', 'branchCount' => '0');
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
$userId = auth_user_id_from_request($con, $userId, 'owner');
if ($userId === null || $userId === '') {
    $response['status'] = '0';
    $response['message'] = 'Invalid or expired auth token';
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$response['organizationId'] = (string) $userId;
$today = licence_today();
$userIdEsc = mysqli_real_escape_string($con, (string) $userId);

$sql = "SELECT `licenses`.*, `companys`.`companyName`, `companys`.`currencyName`
        FROM `licenses`
        LEFT JOIN `companys` ON `companys`.`licenseId` = `licenses`.`id`
        WHERE `licenses`.`userId`='$userIdEsc'
        ORDER BY CASE WHEN LOWER(`licenses`.`userType`) = 'owner' THEN 0 ELSE 1 END,
                 `licenses`.`id` ASC";

$result = mysqli_query($con, $sql);
$branches = array();

if ($result && mysqli_num_rows($result) > 0) {
    while ($row = mysqli_fetch_assoc($result)) {
        $branch = licence_branch_fields($row);
        $licenseId = $row['id'];
        $sales = licence_store_sales($con, $licenseId, $today);

        $billCount = db_stmt_scalar_string(
            $con,
            'SELECT COUNT(*) FROM `invoice` WHERE `branch_id`=? OR (`branch_id` IS NULL AND `licenseId`=?)',
            'ii',
            (int) $licenseId,
            (int) $licenseId
        );
        $todayBillCount = db_stmt_scalar_string(
            $con,
            'SELECT COUNT(*) FROM `invoice` WHERE (`branch_id`=? OR (`branch_id` IS NULL AND `licenseId`=?)) AND `invoiceDate` LIKE ?',
            'iis',
            (int) $licenseId,
            (int) $licenseId,
            $today . '%'
        );

        $totalSale = (float) $sales['totalSale'];
        $billCountInt = (int) $billCount;
        $avgBill = $billCountInt > 0 ? round($totalSale / $billCountInt, 2) : 0.0;

        $deviceBound = isset($row['android_device_id']) && trim((string) $row['android_device_id']) !== '';

        $branches[] = array(
            'branchId' => (string) $licenseId,
            'organizationId' => (string) $userId,
            'branchLabel' => $branch['branchLabel'],
            'userType' => $branch['userType'],
            'userName' => $branch['userName'],
            'companyName' => isset($row['companyName']) ? $row['companyName'] : '',
            'totalSale' => $sales['totalSale'],
            'todaySale' => $sales['todaySale'],
            'billCount' => (string) $billCountInt,
            'todayBillCount' => (string) (int) $todayBillCount,
            'avgBillAmount' => (string) $avgBill,
            'deviceBound' => $deviceBound ? '1' : '0',
            'androidDeviceName' => isset($row['android_device_name']) ? $row['android_device_name'] : '',
            'currencyName' => licence_format_currency_name(isset($row['currencyName']) ? $row['currencyName'] : null),
        );
    }
}

$response['branches'] = $branches;
$response['branchCount'] = (string) count($branches);
$response['status'] = '1';

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
