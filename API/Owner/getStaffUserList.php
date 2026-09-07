<?php
/**
 * Owner: list staff roster for a licence (outlet) the owner owns.
 * Does not return staffPin (security).
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../branch_scope.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../php_compat.php';

owner_require_auth($con);

$response = array('status' => '1', 'message' => 'ok', 'staffUserResponse' => array());

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
            $licenceId
        );

        if (is_array($rows)) {
            foreach ($rows as $row) {
                $item = array();
                $item['staffId'] = isset($row['staffId']) ? (string) $row['staffId'] : '';
                $item['localStaffId'] = isset($row['local_staff_id']) ? $row['local_staff_id'] : '';
                $item['staffName'] = isset($row['staffName']) ? $row['staffName'] : '';
                $item['staffRole'] = isset($row['staffRole']) ? $row['staffRole'] : 'cashier';
                // Intentionally omit staffPin from Owner responses.
                $item['staffActive'] = isset($row['staffActive']) ? $row['staffActive'] : '1';
                $item['staffDeletedStatus'] = isset($row['staffDeletedStatus'])
                    ? $row['staffDeletedStatus'] : '0';
                $item['staffNetworkStatus'] = isset($row['staffNetworkStatus'])
                    ? $row['staffNetworkStatus'] : '';
                $item['createdAt'] = isset($row['createdAtLocal']) && $row['createdAtLocal'] !== ''
                    ? $row['createdAtLocal']
                    : (isset($row['createdAt']) ? $row['createdAt'] : '');
                $response['staffUserResponse'][] = $item;
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
