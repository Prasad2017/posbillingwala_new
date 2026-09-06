<?php
/**
 * List table_type rows for a licence.
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/dine_in_helpers.php';

$response = array('tableTypeResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
pos_require_auth($con, $userId, array('status' => '0', 'message' => 'Unauthorized'));
dine_in_ensure_schema($con);

$sth = "SELECT * FROM `table_type` WHERE `licenseId`='" . mysqli_real_escape_string($con, $userId) . "'
        ORDER BY CAST(`defaultCapacity` AS UNSIGNED) ASC, `tableTypeId` ASC";
if ($result = mysqli_query($con, $sth)) {
    while ($row = mysqli_fetch_assoc($result)) {
        $response['tableTypeResponse'][] = array(
            'tableTypeId' => $row['tableTypeId'],
            'userId' => $row['licenseId'],
            'tableTypeName' => $row['tableTypeName'],
            'defaultCapacity' => isset($row['defaultCapacity']) ? (string) $row['defaultCapacity'] : '4',
            'tableTypeActive' => isset($row['tableTypeActive']) ? $row['tableTypeActive'] : '1',
            'tableTypeNetworkStatus' => isset($row['tableTypeNetworkStatus']) ? $row['tableTypeNetworkStatus'] : '',
            'organizationId' => isset($row['organization_id']) ? (string) $row['organization_id'] : '',
            'branchId' => isset($row['branch_id']) ? (string) $row['branch_id'] : '',
            'deviceId' => isset($row['device_id']) ? $row['device_id'] : '',
        );
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
