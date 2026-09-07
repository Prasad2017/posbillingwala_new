<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/php_compat.php';

$response = array('status' => '1', 'message' => 'ok', 'customOrderDepositResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
    pos_require_auth($con, $userId, isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized'));

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
        'SELECT * FROM `custom_order_deposits` WHERE `userId`=? ORDER BY `depositId` ASC',
        's',
        $userId
    );

    if (is_array($rows)) {
        foreach ($rows as $row) {
            $item = array();
            $item['depositId'] = isset($row['depositId']) ? $row['depositId'] : '';
            $item['localDepositId'] = isset($row['local_deposit_id']) ? $row['local_deposit_id'] : '';
            $item['productName'] = isset($row['productName']) ? $row['productName'] : '';
            $item['orderNote'] = isset($row['orderNote']) ? $row['orderNote'] : '';
            $item['depositAmount'] = isset($row['depositAmount']) ? $row['depositAmount'] : '';
            $item['dueDate'] = isset($row['dueDate']) ? $row['dueDate'] : '';
            $item['photoFile'] = isset($row['photoFile']) ? $row['photoFile'] : '';
            $item['depositStatus'] = isset($row['depositStatus']) ? $row['depositStatus'] : 'open';
            $item['depositNetworkStatus'] = isset($row['depositNetworkStatus'])
                ? $row['depositNetworkStatus'] : '';
            $item['createdAt'] = isset($row['createdAtLocal']) ? $row['createdAtLocal'] : '';
            $response['customOrderDepositResponse'][] = $item;
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
