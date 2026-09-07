<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/php_compat.php';

$response = array('status' => '1', 'message' => 'ok', 'staffUserResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
    pos_require_auth($con, $userId, isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized'));

    try {
        db_safe_query(
            $con,
            "CREATE TABLE IF NOT EXISTS `staff_users` (
              `staffId` INT NOT NULL AUTO_INCREMENT,
              `userId` VARCHAR(64) NOT NULL,
              `organization_id` INT NULL DEFAULT NULL,
              `branch_id` INT NULL DEFAULT NULL,
              `device_id` VARCHAR(255) NULL DEFAULT NULL,
              `local_staff_id` VARCHAR(64) NOT NULL,
              `staffName` VARCHAR(255) NOT NULL,
              `staffRole` VARCHAR(64) NOT NULL DEFAULT 'cashier',
              `staffPin` VARCHAR(64) NULL DEFAULT NULL,
              `staffActive` VARCHAR(8) NOT NULL DEFAULT '1',
              `staffDeletedStatus` VARCHAR(8) NOT NULL DEFAULT '0',
              `staffNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
              `createdAtLocal` VARCHAR(64) NULL DEFAULT NULL,
              `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`staffId`),
              UNIQUE KEY `uq_staff_user_local` (`userId`, `local_staff_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    } catch (Throwable $e) {
        // ignore
    }

    $rows = db_stmt_fetch_all(
        $con,
        'SELECT * FROM `staff_users` WHERE `userId`=? ORDER BY `staffId` ASC',
        's',
        $userId
    );

    if (is_array($rows)) {
        foreach ($rows as $row) {
            $item = array();
            $item['staffId'] = isset($row['staffId']) ? $row['staffId'] : '';
            $item['localStaffId'] = isset($row['local_staff_id']) ? $row['local_staff_id'] : '';
            $item['staffName'] = isset($row['staffName']) ? $row['staffName'] : '';
            $item['staffRole'] = isset($row['staffRole']) ? $row['staffRole'] : 'cashier';
            $item['staffPin'] = isset($row['staffPin']) ? $row['staffPin'] : '';
            $item['staffActive'] = isset($row['staffActive']) ? $row['staffActive'] : '1';
            $item['staffDeletedStatus'] = isset($row['staffDeletedStatus'])
                ? $row['staffDeletedStatus'] : '0';
            $item['staffNetworkStatus'] = isset($row['staffNetworkStatus'])
                ? $row['staffNetworkStatus'] : '';
            $item['createdAt'] = isset($row['createdAtLocal']) ? $row['createdAtLocal'] : '';
            $response['staffUserResponse'][] = $item;
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
