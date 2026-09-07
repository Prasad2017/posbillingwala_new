<?php
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';
require_once __DIR__ . '/php_compat.php';

/**
 * Ensure staff_users exists (additive). Prefer API/migrations/p32_staff_user.sql.
 */
function staff_user_ensure_table($con)
{
    static $ensured = false;
    if ($ensured || $con === null) {
        return;
    }
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
              UNIQUE KEY `uq_staff_user_local` (`userId`, `local_staff_id`),
              KEY `idx_staff_user_branch` (`branch_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    } catch (Throwable $e) {
        // Ignore
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

    staff_user_ensure_table($con);

    $localStaffId = isset($_POST['localStaffId']) ? trim($_POST['localStaffId']) : '';
    $staffName = isset($_POST['staffName']) ? trim($_POST['staffName']) : '';
    $staffRole = isset($_POST['staffRole']) ? trim($_POST['staffRole']) : 'cashier';
    $staffPin = isset($_POST['staffPin']) ? trim($_POST['staffPin']) : '';
    $staffActive = isset($_POST['staffActive']) ? trim($_POST['staffActive']) : '1';
    $staffDeletedStatus = isset($_POST['staffDeletedStatus']) ? trim($_POST['staffDeletedStatus']) : '0';
    $staffNetworkStatus = isset($_POST['staffNetworkStatus']) ? trim($_POST['staffNetworkStatus']) : '';
    $createdAtLocal = isset($_POST['createdAt']) ? trim($_POST['createdAt']) : '';

    if ($localStaffId === '') {
        $response['message'] = 'Missing localStaffId';
    } elseif ($staffName === '' && $staffDeletedStatus !== '1') {
        $response['message'] = 'Missing staffName';
    } else {
        if ($staffRole === '') {
            $staffRole = 'cashier';
        }
        if ($staffActive === '') {
            $staffActive = '1';
        }
        if ($staffDeletedStatus === '') {
            $staffDeletedStatus = '0';
        }

        $existing = db_stmt_fetch_one(
            $con,
            'SELECT staffId FROM `staff_users` WHERE `userId`=? AND `local_staff_id`=? LIMIT 1',
            'ss',
            $userId,
            $localStaffId
        );

        if ($existing !== null) {
            $staffId = (int) $existing['staffId'];
            $updated = db_stmt_execute(
                $con,
                'UPDATE `staff_users` SET
                    `organization_id`=?, `branch_id`=?, `device_id`=?,
                    `staffName`=?, `staffRole`=?, `staffPin`=?, `staffActive`=?,
                    `staffDeletedStatus`=?, `staffNetworkStatus`=?, `createdAtLocal`=?
                 WHERE `staffId`=?',
                'iissssssssi',
                $orgId,
                $branchId,
                $deviceId,
                $staffName,
                $staffRole,
                $staffPin,
                $staffActive,
                $staffDeletedStatus,
                $staffNetworkStatus,
                $createdAtLocal,
                $staffId
            );
            $response['status'] = $updated ? '1' : '0';
            $response['message'] = $updated ? 'update successful!' : 'update failed!';
            $response['staffId'] = $staffId;
        } else {
            $insertId = db_stmt_insert_id(
                $con,
                'INSERT INTO `staff_users`
                (`userId`, `organization_id`, `branch_id`, `device_id`, `local_staff_id`,
                 `staffName`, `staffRole`, `staffPin`, `staffActive`, `staffDeletedStatus`,
                 `staffNetworkStatus`, `createdAtLocal`)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?)',
                'siisssssssss',
                $userId,
                $orgId,
                $branchId,
                $deviceId,
                $localStaffId,
                $staffName,
                $staffRole,
                $staffPin,
                $staffActive,
                $staffDeletedStatus,
                $staffNetworkStatus,
                $createdAtLocal
            );
            $response['status'] = $insertId !== false ? '1' : '0';
            $response['message'] = $insertId !== false ? 'insert successful!' : 'insert failed!';
            if ($insertId !== false) {
                $response['staffId'] = $insertId;
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
