<?php
/**
 * Owner: list custom-order deposits for a licence (outlet) the owner owns.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../branch_scope.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../php_compat.php';

owner_require_auth($con);

$response = array('status' => '1', 'message' => 'ok', 'customOrderDepositResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $postedUserId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $ownerId = owner_resolve_user_id($con, $postedUserId);
    if ($ownerId === null) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(array('status' => '0', 'message' => 'Unauthorized'));
        mysqli_close($con);
        exit;
    }

    $licenceId = isset($_GET['licenceId']) ? trim($_GET['licenceId']) : '';
    if ($licenceId === '') {
        $response['status'] = '0';
        $response['message'] = 'Missing licenceId';
    } elseif (!branch_owner_require_branch_access($con, $ownerId, $licenceId, $response)) {
        // message set
    } else {
        try {
            db_safe_query(
                $con,
                "CREATE TABLE IF NOT EXISTS `custom_order_deposits` (
                  `depositId` INT NOT NULL AUTO_INCREMENT,
                  `userId` VARCHAR(64) NOT NULL,
                  `organization_id` INT NULL DEFAULT NULL,
                  `branch_id` INT NULL DEFAULT NULL,
                  `device_id` VARCHAR(255) NULL DEFAULT NULL,
                  `local_deposit_id` VARCHAR(64) NOT NULL,
                  `productName` VARCHAR(255) NULL DEFAULT NULL,
                  `orderNote` TEXT NULL,
                  `depositAmount` VARCHAR(64) NULL DEFAULT NULL,
                  `dueDate` VARCHAR(64) NULL DEFAULT NULL,
                  `photoFile` VARCHAR(512) NULL DEFAULT NULL,
                  `depositStatus` VARCHAR(32) NOT NULL DEFAULT 'open',
                  `depositNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
                  `createdAtLocal` VARCHAR(64) NULL DEFAULT NULL,
                  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`depositId`),
                  UNIQUE KEY `uq_custom_deposit_local` (`userId`, `local_deposit_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Throwable $e) {
            // ignore
        }

        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `custom_order_deposits` WHERE `userId`=? ORDER BY `depositId` DESC',
            's',
            $licenceId
        );

        if (is_array($rows)) {
            foreach ($rows as $row) {
                $item = array();
                $item['depositId'] = isset($row['depositId']) ? (string) $row['depositId'] : '';
                $item['localDepositId'] = isset($row['local_deposit_id']) ? $row['local_deposit_id'] : '';
                $item['productName'] = isset($row['productName']) ? $row['productName'] : '';
                $item['orderNote'] = isset($row['orderNote']) ? $row['orderNote'] : '';
                $item['depositAmount'] = isset($row['depositAmount']) ? $row['depositAmount'] : '';
                $item['dueDate'] = isset($row['dueDate']) ? $row['dueDate'] : '';
                $item['photoFile'] = isset($row['photoFile']) ? $row['photoFile'] : '';
                $item['depositStatus'] = isset($row['depositStatus']) ? $row['depositStatus'] : 'open';
                $item['depositNetworkStatus'] = isset($row['depositNetworkStatus'])
                    ? $row['depositNetworkStatus'] : '';
                $item['createdAt'] = isset($row['createdAtLocal']) && $row['createdAtLocal'] !== ''
                    ? $row['createdAtLocal']
                    : (isset($row['createdAt']) ? $row['createdAt'] : '');
                $response['customOrderDepositResponse'][] = $item;
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
