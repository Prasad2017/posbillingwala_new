<?php
/**
 * Owner: update staff active / deleted flags for an outlet.
 * Does not change PIN or create new staff (that stays on POS).
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
    $staffId = isset($_POST['staffId']) ? trim($_POST['staffId']) : '';
    $localStaffId = isset($_POST['localStaffId']) ? trim($_POST['localStaffId']) : '';
    $staffActive = isset($_POST['staffActive']) ? trim($_POST['staffActive']) : '';
    $staffDeletedStatus = isset($_POST['staffDeletedStatus']) ? trim($_POST['staffDeletedStatus']) : '';
    $staffRole = isset($_POST['staffRole']) ? trim($_POST['staffRole']) : '';

    $allowedRoles = array(
        'owner' => true,
        'manager' => true,
        'cashier' => true,
        'waiter' => true,
    );

    if ($licenceId === '') {
        $response['message'] = 'Missing licenceId';
    } elseif (!branch_owner_require_branch_access($con, $ownerId, $licenceId, $response)) {
        // message set
    } elseif ($staffId === '' && $localStaffId === '') {
        $response['message'] = 'Missing staffId';
    } elseif ($staffActive === '' && $staffDeletedStatus === '' && $staffRole === '') {
        $response['message'] = 'Nothing to update';
    } elseif ($staffRole !== '' && !isset($allowedRoles[strtolower($staffRole)])) {
        $response['message'] = 'Invalid staffRole (owner|manager|cashier|waiter)';
    } elseif ($staffActive !== '' && $staffActive !== '0' && $staffActive !== '1') {
        $response['message'] = 'Invalid staffActive (0|1)';
    } elseif ($staffDeletedStatus !== '' && $staffDeletedStatus !== '0' && $staffDeletedStatus !== '1') {
        $response['message'] = 'Invalid staffDeletedStatus (0|1)';
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

        $row = null;
        if ($staffId !== '' && ctype_digit($staffId)) {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT staffId, staffActive, staffDeletedStatus, staffRole FROM `staff_users`
                 WHERE `userId`=? AND `staffId`=? LIMIT 1',
                'si',
                $licenceId,
                (int) $staffId
            );
        }
        if ($row === null && $localStaffId !== '') {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT staffId, staffActive, staffDeletedStatus, staffRole FROM `staff_users`
                 WHERE `userId`=? AND `local_staff_id`=? LIMIT 1',
                'ss',
                $licenceId,
                $localStaffId
            );
        }

        if ($row === null) {
            $response['message'] = 'Staff not found for this outlet';
        } else {
            $id = (int) $row['staffId'];
            $newActive = $staffActive !== '' ? $staffActive : (string) $row['staffActive'];
            $newDeleted = $staffDeletedStatus !== ''
                ? $staffDeletedStatus
                : (string) $row['staffDeletedStatus'];
            $newRole = $staffRole !== ''
                ? strtolower($staffRole)
                : (isset($row['staffRole']) ? $row['staffRole'] : 'cashier');

            $ok = db_stmt_execute(
                $con,
                'UPDATE `staff_users` SET
                    `staffActive`=?, `staffDeletedStatus`=?, `staffRole`=?,
                    `staffNetworkStatus`=?
                 WHERE `staffId`=? AND `userId`=?',
                'ssssis',
                $newActive,
                $newDeleted,
                $newRole,
                'owner_push',
                $id,
                $licenceId
            );
            $response['status'] = $ok ? '1' : '0';
            $response['message'] = $ok ? 'update successful!' : 'update failed!';
            $response['staffId'] = $id;
            $response['staffActive'] = $newActive;
            $response['staffDeletedStatus'] = $newDeleted;
            $response['staffRole'] = $newRole;
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
