<?php
/**
 * Owner: update custom-order deposit status for an outlet the owner owns.
 * Allowed: open | applied | refunded. POS picks up on Fetch Data.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../branch_scope.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../php_compat.php';

owner_require_auth($con);

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    mysqli_query($con, 'set names utf8mb4');

    $postedUserId = isset($_POST['userId']) ? $_POST['userId'] : '';
    $ownerId = owner_resolve_user_id($con, $postedUserId);
    if ($ownerId === null) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(array('status' => '0', 'message' => 'Unauthorized'));
        mysqli_close($con);
        exit;
    }

    $licenceId = isset($_POST['licenceId']) ? trim($_POST['licenceId']) : '';
    $depositId = isset($_POST['depositId']) ? trim($_POST['depositId']) : '';
    $localDepositId = isset($_POST['localDepositId']) ? trim($_POST['localDepositId']) : '';
    $depositStatus = isset($_POST['depositStatus']) ? trim($_POST['depositStatus']) : '';

    $allowed = array('open' => true, 'applied' => true, 'refunded' => true);
    if ($licenceId === '') {
        $response['message'] = 'Missing licenceId';
    } elseif (!branch_owner_require_branch_access($con, $ownerId, $licenceId, $response)) {
        // message set
    } elseif ($depositStatus === '' || !isset($allowed[$depositStatus])) {
        $response['message'] = 'Invalid depositStatus (open|applied|refunded)';
    } elseif ($depositId === '' && $localDepositId === '') {
        $response['message'] = 'Missing depositId';
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

        $row = null;
        if ($depositId !== '' && ctype_digit($depositId)) {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT depositId FROM `custom_order_deposits`
                 WHERE `userId`=? AND `depositId`=? LIMIT 1',
                'si',
                $licenceId,
                (int) $depositId
            );
        }
        if ($row === null && $localDepositId !== '') {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT depositId FROM `custom_order_deposits`
                 WHERE `userId`=? AND `local_deposit_id`=? LIMIT 1',
                'ss',
                $licenceId,
                $localDepositId
            );
        }

        if ($row === null) {
            $response['message'] = 'Deposit not found for this outlet';
        } else {
            $id = (int) $row['depositId'];
            $ok = db_stmt_execute(
                $con,
                'UPDATE `custom_order_deposits` SET
                    `depositStatus`=?, `depositNetworkStatus`=?
                 WHERE `depositId`=? AND `userId`=?',
                'ssis',
                $depositStatus,
                'owner_push',
                $id,
                $licenceId
            );
            $response['status'] = $ok ? '1' : '0';
            $response['message'] = $ok ? 'update successful!' : 'update failed!';
            $response['depositId'] = $id;
            $response['depositStatus'] = $depositStatus;
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
