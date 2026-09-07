<?php
/**
 * Owner: update appointment status for an outlet the owner owns.
 * POS devices pick up the new status on Fetch Data.
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
    $appointmentId = isset($_POST['appointmentId']) ? trim($_POST['appointmentId']) : '';
    $localAppointmentId = isset($_POST['localAppointmentId']) ? trim($_POST['localAppointmentId']) : '';
    $appointmentStatus = isset($_POST['appointmentStatus']) ? trim($_POST['appointmentStatus']) : '';

    $allowed = array('booked' => true, 'done' => true, 'cancelled' => true);
    if ($licenceId === '') {
        $response['message'] = 'Missing licenceId';
    } elseif (!branch_owner_require_branch_access($con, $ownerId, $licenceId, $response)) {
        // message set
    } elseif ($appointmentStatus === '' || !isset($allowed[$appointmentStatus])) {
        $response['message'] = 'Invalid appointmentStatus (booked|done|cancelled)';
    } elseif ($appointmentId === '' && $localAppointmentId === '') {
        $response['message'] = 'Missing appointmentId';
    } else {
        try {
            db_safe_query(
                $con,
                "CREATE TABLE IF NOT EXISTS `service_appointments` (
                  `appointmentId` INT NOT NULL AUTO_INCREMENT,
                  `userId` VARCHAR(64) NOT NULL,
                  `organization_id` INT NULL DEFAULT NULL,
                  `branch_id` INT NULL DEFAULT NULL,
                  `device_id` VARCHAR(255) NULL DEFAULT NULL,
                  `local_appointment_id` VARCHAR(64) NOT NULL,
                  `productId` VARCHAR(64) NULL DEFAULT NULL,
                  `productName` VARCHAR(255) NULL DEFAULT NULL,
                  `customerName` VARCHAR(255) NULL DEFAULT NULL,
                  `customerMobile` VARCHAR(64) NULL DEFAULT NULL,
                  `appointmentAt` VARCHAR(64) NULL DEFAULT NULL,
                  `notes` TEXT NULL,
                  `appointmentStatus` VARCHAR(32) NOT NULL DEFAULT 'booked',
                  `staffId` VARCHAR(64) NULL DEFAULT NULL,
                  `staffName` VARCHAR(255) NULL DEFAULT NULL,
                  `appointmentNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
                  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`appointmentId`),
                  UNIQUE KEY `uq_service_appt_local` (`userId`, `local_appointment_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Throwable $e) {
            // ignore
        }

        $row = null;
        if ($appointmentId !== '' && ctype_digit($appointmentId)) {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT appointmentId FROM `service_appointments`
                 WHERE `userId`=? AND `appointmentId`=? LIMIT 1',
                'si',
                $licenceId,
                (int) $appointmentId
            );
        }
        if ($row === null && $localAppointmentId !== '') {
            $row = db_stmt_fetch_one(
                $con,
                'SELECT appointmentId FROM `service_appointments`
                 WHERE `userId`=? AND `local_appointment_id`=? LIMIT 1',
                'ss',
                $licenceId,
                $localAppointmentId
            );
        }

        if ($row === null) {
            $response['message'] = 'Appointment not found for this outlet';
        } else {
            $id = (int) $row['appointmentId'];
            $ok = db_stmt_execute(
                $con,
                'UPDATE `service_appointments` SET
                    `appointmentStatus`=?, `appointmentNetworkStatus`=?
                 WHERE `appointmentId`=? AND `userId`=?',
                'ssis',
                $appointmentStatus,
                'owner_push',
                $id,
                $licenceId
            );
            $response['status'] = $ok ? '1' : '0';
            $response['message'] = $ok ? 'update successful!' : 'update failed!';
            $response['appointmentId'] = $id;
            $response['appointmentStatus'] = $appointmentStatus;
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
