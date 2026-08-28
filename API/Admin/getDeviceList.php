<?php
/**
 * Admin: POS device monitoring list (bound licenses + live presence).
 * GET optional: customerId
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../licence_expiry.php';
require_once __DIR__ . '/../pos_presence.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '1', 'deviceResponse' => array());

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => '0', 'deviceResponse' => array()));
mysqli_query($con, 'set names utf8');

$customerId = isset($_GET['customerId']) ? trim($_GET['customerId']) : '';
$tokenSub = licence_token_last_used_subquery('l');
$loginCol = licence_has_last_login_column($con) ? 'l.`lastLoginAt`,' : 'NULL AS lastLoginAt,';

if ($customerId !== '') {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT l.*, u.shopName, u.name AS ownerName, u.contact_number,
                {$loginCol}
                {$tokenSub} AS tokenLastUsedAt
         FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.role_id='3' AND u.id=?
           AND l.android_device_id IS NOT NULL AND TRIM(l.android_device_id) <> ''
         ORDER BY l.id DESC",
        'i',
        (int) $customerId
    );
} else {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT l.*, u.shopName, u.name AS ownerName, u.contact_number,
                {$loginCol}
                {$tokenSub} AS tokenLastUsedAt
         FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.role_id='3'
           AND l.android_device_id IS NOT NULL AND TRIM(l.android_device_id) <> ''
         ORDER BY l.id DESC
         LIMIT 200",
        ''
    );
}

foreach ($rows as $row) {
    $branch = function_exists('licence_branch_fields') ? licence_branch_fields($row) : array(
        'branchLabel' => isset($row['userName']) ? $row['userName'] : ''
    );
    $presence = licence_device_presence_fields($row);
    $response['deviceResponse'][] = array(
        'customerId' => (string) $row['userId'],
        'shopName' => isset($row['shopName']) ? (string) $row['shopName'] : '',
        'ownerName' => isset($row['ownerName']) ? (string) $row['ownerName'] : '',
        'contact_number' => isset($row['contact_number']) ? (string) $row['contact_number'] : '',
        'licenses_id' => (string) $row['id'],
        'licenseKey' => (string) $row['licenseKey'],
        'licenseStatus' => isset($row['licenseStatus']) ? (string) $row['licenseStatus'] : '',
        'licenseType' => isset($row['licenseType']) ? (string) $row['licenseType'] : '',
        'expiryDate' => isset($row['expiryDate']) ? (string) $row['expiryDate'] : '',
        'branchLabel' => isset($branch['branchLabel']) ? (string) $branch['branchLabel'] : '',
        'android_device_id' => isset($row['android_device_id']) ? (string) $row['android_device_id'] : '',
        'android_device_name' => isset($row['android_device_name']) ? (string) $row['android_device_name'] : '',
        'deviceBoundAt' => isset($row['deviceBoundAt']) ? (string) $row['deviceBoundAt'] : '',
        'lastLoginAt' => $presence['lastLoginAt'],
        'lastSeenAt' => $presence['lastSeenAt'],
        'lastSeenLabel' => $presence['lastSeenLabel'],
        'connectionStatus' => $presence['connectionStatus'],
    );
}

echo json_encode($response);
?>
