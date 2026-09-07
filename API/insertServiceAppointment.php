<?php
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';
require_once __DIR__ . '/php_compat.php';

/**
 * Ensure service_appointments exists (additive). Prefer API/migrations/p27_service_appointment.sql.
 */
function service_appointment_ensure_table($con)
{
    static $ensured = false;
    if ($ensured || $con === null) {
        return;
    }
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
              UNIQUE KEY `uq_service_appt_local` (`userId`, `local_appointment_id`),
              KEY `idx_service_appt_branch` (`branch_id`),
              KEY `idx_service_appt_at` (`appointmentAt`)
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

    service_appointment_ensure_table($con);

    $localAppointmentId = isset($_POST['localAppointmentId']) ? trim($_POST['localAppointmentId']) : '';
    $productId = isset($_POST['productId']) ? trim($_POST['productId']) : '';
    $productName = isset($_POST['productName']) ? trim($_POST['productName']) : '';
    $customerName = isset($_POST['customerName']) ? trim($_POST['customerName']) : '';
    $customerMobile = isset($_POST['customerMobile']) ? trim($_POST['customerMobile']) : '';
    $appointmentAt = isset($_POST['appointmentAt']) ? trim($_POST['appointmentAt']) : '';
    $notes = isset($_POST['notes']) ? trim($_POST['notes']) : '';
    $appointmentStatus = isset($_POST['appointmentStatus']) ? trim($_POST['appointmentStatus']) : 'booked';
    $staffId = isset($_POST['staffId']) ? trim($_POST['staffId']) : '';
    $staffName = isset($_POST['staffName']) ? trim($_POST['staffName']) : '';
    $appointmentNetworkStatus = isset($_POST['appointmentNetworkStatus'])
        ? trim($_POST['appointmentNetworkStatus']) : '';

    if ($localAppointmentId === '') {
        $response['message'] = 'Missing localAppointmentId';
    } else {
        date_default_timezone_set('Asia/Kolkata');
        if ($appointmentStatus === '') {
            $appointmentStatus = 'booked';
        }

        $existing = db_stmt_fetch_one(
            $con,
            'SELECT appointmentId FROM `service_appointments` WHERE `userId`=? AND `local_appointment_id`=? LIMIT 1',
            'ss',
            $userId,
            $localAppointmentId
        );

        if ($existing !== null) {
            $appointmentId = (int) $existing['appointmentId'];
            $updated = db_stmt_execute(
                $con,
                'UPDATE `service_appointments` SET
                    `organization_id`=?, `branch_id`=?, `device_id`=?,
                    `productId`=?, `productName`=?, `customerName`=?, `customerMobile`=?,
                    `appointmentAt`=?, `notes`=?, `appointmentStatus`=?,
                    `staffId`=?, `staffName`=?, `appointmentNetworkStatus`=?
                 WHERE `appointmentId`=?',
                'iisssssssssssi',
                $orgId,
                $branchId,
                $deviceId,
                $productId,
                $productName,
                $customerName,
                $customerMobile,
                $appointmentAt,
                $notes,
                $appointmentStatus,
                $staffId,
                $staffName,
                $appointmentNetworkStatus,
                $appointmentId
            );
            $response['status'] = $updated ? '1' : '0';
            $response['message'] = $updated ? 'update successful!' : 'update failed!';
            $response['appointmentId'] = $appointmentId;
        } else {
            $insertId = db_stmt_insert_id(
                $con,
                'INSERT INTO `service_appointments`
                (`userId`, `organization_id`, `branch_id`, `device_id`, `local_appointment_id`,
                 `productId`, `productName`, `customerName`, `customerMobile`, `appointmentAt`,
                 `notes`, `appointmentStatus`, `staffId`, `staffName`, `appointmentNetworkStatus`)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
                'siissssssssssss',
                $userId,
                $orgId,
                $branchId,
                $deviceId,
                $localAppointmentId,
                $productId,
                $productName,
                $customerName,
                $customerMobile,
                $appointmentAt,
                $notes,
                $appointmentStatus,
                $staffId,
                $staffName,
                $appointmentNetworkStatus
            );
            $response['status'] = $insertId !== false ? '1' : '0';
            $response['message'] = $insertId !== false ? 'insert successful!' : 'insert failed!';
            if ($insertId !== false) {
                $response['appointmentId'] = $insertId;
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
