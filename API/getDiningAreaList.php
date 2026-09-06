<?php
/**
 * List dining_area rows for a licence.
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/dine_in_helpers.php';

$response = array('diningAreaResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
pos_require_auth($con, $userId, array('status' => '0', 'message' => 'Unauthorized'));
dine_in_ensure_schema($con);

$sth = "SELECT * FROM `dining_area` WHERE `licenseId`='" . mysqli_real_escape_string($con, $userId) . "'
        ORDER BY CAST(`areaSortOrder` AS UNSIGNED) ASC, `areaId` ASC";
if ($result = mysqli_query($con, $sth)) {
    while ($row = mysqli_fetch_assoc($result)) {
        $response['diningAreaResponse'][] = array(
            'areaId' => $row['areaId'],
            'userId' => $row['licenseId'],
            'areaName' => $row['areaName'],
            'areaSortOrder' => isset($row['areaSortOrder']) ? (string) $row['areaSortOrder'] : '0',
            'areaActive' => isset($row['areaActive']) ? $row['areaActive'] : '1',
            'areaNetworkStatus' => isset($row['areaNetworkStatus']) ? $row['areaNetworkStatus'] : '',
            'organizationId' => isset($row['organization_id']) ? (string) $row['organization_id'] : '',
            'branchId' => isset($row['branch_id']) ? (string) $row['branch_id'] : '',
            'deviceId' => isset($row['device_id']) ? $row['device_id'] : '',
        );
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
