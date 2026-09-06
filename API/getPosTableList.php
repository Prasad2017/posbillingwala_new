<?php
/**
 * List dine-in pos_table rows for a licence / branch.
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/dine_in_helpers.php';

$response = array('posTableResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
pos_require_auth($con, $userId, array('status' => '0', 'message' => 'Unauthorized'));
dine_in_ensure_schema($con);

$sth = "SELECT * FROM `pos_table` WHERE `licenseId`='" . mysqli_real_escape_string($con, $userId) . "'
        ORDER BY CAST(`sortOrder` AS UNSIGNED) ASC, CAST(`tableNumber` AS UNSIGNED) ASC, `tableId` ASC";
if ($result = mysqli_query($con, $sth)) {
    while ($row = mysqli_fetch_assoc($result)) {
        $getdata = array(
            'tableId' => $row['tableId'],
            'userId' => $row['licenseId'],
            'tableNumber' => $row['tableNumber'],
            'tableName' => $row['tableName'],
            'tableTypeId' => isset($row['tableTypeId']) ? $row['tableTypeId'] : '',
            'capacity' => isset($row['capacity']) ? (string) $row['capacity'] : '4',
            'areaId' => isset($row['areaId']) ? $row['areaId'] : '',
            'tableActive' => isset($row['tableActive']) ? $row['tableActive'] : '1',
            'positionX' => isset($row['positionX']) ? $row['positionX'] : '',
            'positionY' => isset($row['positionY']) ? $row['positionY'] : '',
            'sortOrder' => isset($row['sortOrder']) ? (string) $row['sortOrder'] : '0',
            'statusOverride' => isset($row['statusOverride']) ? $row['statusOverride'] : '',
            'posTableNetworkStatus' => isset($row['posTableNetworkStatus']) ? $row['posTableNetworkStatus'] : '',
            'organizationId' => isset($row['organization_id']) ? (string) $row['organization_id'] : '',
            'branchId' => isset($row['branch_id']) ? (string) $row['branch_id'] : '',
            'deviceId' => isset($row['device_id']) ? $row['device_id'] : '',
        );
        $response['posTableResponse'][] = $getdata;
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
