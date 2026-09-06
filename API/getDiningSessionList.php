<?php
/**
 * List dining sessions for a licence (open + recent closed).
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/dine_in_helpers.php';

$response = array('diningSessionResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
pos_require_auth($con, $userId, array('status' => '0', 'message' => 'Unauthorized'));
dine_in_ensure_schema($con);

$openOnly = isset($_GET['openOnly']) && $_GET['openOnly'] === '1';
$where = "`licenseId`='" . mysqli_real_escape_string($con, $userId) . "'";
if ($openOnly) {
    $where .= " AND `sessionStatus` NOT IN ('CLOSED','SETTLED')";
}

$sth = "SELECT * FROM `dining_session` WHERE $where ORDER BY `sessionId` DESC LIMIT 500";
if ($result = mysqli_query($con, $sth)) {
    while ($row = mysqli_fetch_assoc($result)) {
        $response['diningSessionResponse'][] = array(
            'sessionId' => $row['sessionId'],
            'userId' => $row['licenseId'],
            'localSessionId' => isset($row['localSessionId']) ? $row['localSessionId'] : '',
            'primaryTableNumber' => $row['primaryTableNumber'],
            'joinedTableNumbers' => isset($row['joinedTableNumbers']) ? $row['joinedTableNumbers'] : '',
            'sessionStatus' => $row['sessionStatus'],
            'guestCount' => isset($row['guestCount']) ? (string) $row['guestCount'] : '0',
            'startedAt' => isset($row['startedAt']) ? $row['startedAt'] : '',
            'closedAt' => isset($row['closedAt']) ? $row['closedAt'] : '',
            'customerName' => isset($row['customerName']) ? $row['customerName'] : '',
            'customerMobile' => isset($row['customerMobile']) ? $row['customerMobile'] : '',
            'waiterName' => isset($row['waiterName']) ? $row['waiterName'] : '',
            'unpaidInvoiceNumber' => isset($row['unpaidInvoiceNumber']) ? $row['unpaidInvoiceNumber'] : '',
            'paidAmount' => isset($row['paidAmount']) ? $row['paidAmount'] : '0',
            'sessionVersion' => isset($row['sessionVersion']) ? (string) $row['sessionVersion'] : '1',
            'sessionNetworkStatus' => isset($row['sessionNetworkStatus']) ? $row['sessionNetworkStatus'] : '',
            'organizationId' => isset($row['organization_id']) ? (string) $row['organization_id'] : '',
            'branchId' => isset($row['branch_id']) ? (string) $row['branch_id'] : '',
            'deviceId' => isset($row['device_id']) ? $row['device_id'] : '',
        );
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
