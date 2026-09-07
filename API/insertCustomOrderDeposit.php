<?php
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';
require_once __DIR__ . '/php_compat.php';

/**
 * Ensure custom_order_deposits exists (additive). Prefer API/migrations/p28_custom_order_deposit.sql.
 */
function custom_order_deposit_ensure_table($con)
{
    static $ensured = false;
    if ($ensured || $con === null) {
        return;
    }
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
              UNIQUE KEY `uq_custom_deposit_local` (`userId`, `local_deposit_id`),
              KEY `idx_custom_deposit_branch` (`branch_id`),
              KEY `idx_custom_deposit_status` (`depositStatus`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    } catch (Throwable $e) {
        // Ignore — request may still fail clearly below.
    }
    $ensured = true;
}

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    mysqli_query($con, 'set names utf8mb4');

    $postedUserId = isset($_POST['userId']) ? $_POST['userId'] : '';
    $ctx = branch_pos_prepare_write($con, $postedUserId, $response);
    if ($ctx === null) {
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }
    $userId = $ctx['licenseId'];
    $orgId = $ctx['triplet']['organization_id'];
    $branchId = $ctx['triplet']['branch_id'];
    $deviceId = $ctx['triplet']['device_id'];

    custom_order_deposit_ensure_table($con);

    $localDepositId = isset($_POST['localDepositId']) ? trim($_POST['localDepositId']) : '';
    $productName = isset($_POST['productName']) ? trim($_POST['productName']) : '';
    $orderNote = isset($_POST['orderNote']) ? trim($_POST['orderNote']) : '';
    $depositAmount = isset($_POST['depositAmount']) ? trim($_POST['depositAmount']) : '';
    $dueDate = isset($_POST['dueDate']) ? trim($_POST['dueDate']) : '';
    $photoFile = isset($_POST['photoFile']) ? trim($_POST['photoFile']) : '';
    $depositStatus = isset($_POST['depositStatus']) ? trim($_POST['depositStatus']) : 'open';
    $depositNetworkStatus = isset($_POST['depositNetworkStatus'])
        ? trim($_POST['depositNetworkStatus']) : '';
    $createdAtLocal = isset($_POST['createdAt']) ? trim($_POST['createdAt']) : '';

    if ($localDepositId === '') {
        $response['message'] = 'Missing localDepositId';
    } else {
        date_default_timezone_set('Asia/Kolkata');
        if ($depositStatus === '') {
            $depositStatus = 'open';
        }

        $existing = db_stmt_fetch_one(
            $con,
            'SELECT depositId FROM `custom_order_deposits` WHERE `userId`=? AND `local_deposit_id`=? LIMIT 1',
            'ss',
            $userId,
            $localDepositId
        );

        if ($existing !== null) {
            $depositId = (int) $existing['depositId'];
            $updated = db_stmt_execute(
                $con,
                'UPDATE `custom_order_deposits` SET
                    `organization_id`=?, `branch_id`=?, `device_id`=?,
                    `productName`=?, `orderNote`=?, `depositAmount`=?, `dueDate`=?,
                    `photoFile`=?, `depositStatus`=?, `depositNetworkStatus`=?, `createdAtLocal`=?
                 WHERE `depositId`=?',
                'iisssssssssi',
                $orgId,
                $branchId,
                $deviceId,
                $productName,
                $orderNote,
                $depositAmount,
                $dueDate,
                $photoFile,
                $depositStatus,
                $depositNetworkStatus,
                $createdAtLocal,
                $depositId
            );
            $response['status'] = $updated ? '1' : '0';
            $response['message'] = $updated ? 'update successful!' : 'update failed!';
            $response['depositId'] = $depositId;
        } else {
            $insertId = db_stmt_insert_id(
                $con,
                'INSERT INTO `custom_order_deposits`
                (`userId`, `organization_id`, `branch_id`, `device_id`, `local_deposit_id`,
                 `productName`, `orderNote`, `depositAmount`, `dueDate`, `photoFile`,
                 `depositStatus`, `depositNetworkStatus`, `createdAtLocal`)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)',
                'siissssssssss',
                $userId,
                $orgId,
                $branchId,
                $deviceId,
                $localDepositId,
                $productName,
                $orderNote,
                $depositAmount,
                $dueDate,
                $photoFile,
                $depositStatus,
                $depositNetworkStatus,
                $createdAtLocal
            );
            $response['status'] = $insertId !== false ? '1' : '0';
            $response['message'] = $insertId !== false ? 'insert successful!' : 'insert failed!';
            if ($insertId !== false) {
                $response['depositId'] = $insertId;
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
