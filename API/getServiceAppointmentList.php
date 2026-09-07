<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/php_compat.php';

$response = array('status' => '1', 'message' => 'ok', 'serviceAppointmentResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
    pos_require_auth($con, $userId, isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized'));

    // Ensure table exists (same as insert endpoint)
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

    $rows = db_stmt_fetch_all(
        $con,
        'SELECT * FROM `service_appointments` WHERE `userId`=? ORDER BY `appointmentAt` ASC, `appointmentId` ASC',
        's',
        $userId
    );

    if (is_array($rows)) {
        foreach ($rows as $row) {
            $item = array();
            $item['appointmentId'] = isset($row['appointmentId']) ? $row['appointmentId'] : '';
            $item['localAppointmentId'] = isset($row['local_appointment_id']) ? $row['local_appointment_id'] : '';
            $item['productId'] = isset($row['productId']) ? $row['productId'] : '';
            $item['productName'] = isset($row['productName']) ? $row['productName'] : '';
            $item['customerName'] = isset($row['customerName']) ? $row['customerName'] : '';
            $item['customerMobile'] = isset($row['customerMobile']) ? $row['customerMobile'] : '';
            $item['appointmentAt'] = isset($row['appointmentAt']) ? $row['appointmentAt'] : '';
            $item['notes'] = isset($row['notes']) ? $row['notes'] : '';
            $item['appointmentStatus'] = isset($row['appointmentStatus']) ? $row['appointmentStatus'] : 'booked';
            $item['staffId'] = isset($row['staffId']) ? $row['staffId'] : '';
            $item['staffName'] = isset($row['staffName']) ? $row['staffName'] : '';
            $item['appointmentNetworkStatus'] = isset($row['appointmentNetworkStatus'])
                ? $row['appointmentNetworkStatus'] : '';
            $response['serviceAppointmentResponse'][] = $item;
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
