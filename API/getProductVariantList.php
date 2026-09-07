<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/php_compat.php';

$response = array('status' => '1', 'message' => 'ok', 'productVariantResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
    pos_require_auth($con, $userId, isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized'));

    try {
        db_safe_query(
            $con,
            "CREATE TABLE IF NOT EXISTS `product_variants` (
              `variantId` INT NOT NULL AUTO_INCREMENT,
              `userId` VARCHAR(64) NOT NULL,
              `organization_id` INT NULL DEFAULT NULL,
              `branch_id` INT NULL DEFAULT NULL,
              `device_id` VARCHAR(255) NULL DEFAULT NULL,
              `local_variant_id` VARCHAR(64) NOT NULL,
              `productId` VARCHAR(64) NULL DEFAULT NULL,
              `productNetworkStatus` VARCHAR(128) NULL DEFAULT NULL,
              `variantSize` VARCHAR(128) NULL DEFAULT NULL,
              `variantColor` VARCHAR(128) NULL DEFAULT NULL,
              `variantSku` VARCHAR(128) NULL DEFAULT NULL,
              `variantPrice` VARCHAR(64) NULL DEFAULT NULL,
              `variantDeletedStatus` VARCHAR(8) NOT NULL DEFAULT '0',
              `variantSortOrder` INT NOT NULL DEFAULT 0,
              `variantNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
              `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`variantId`),
              UNIQUE KEY `uq_product_variant_local` (`userId`, `local_variant_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    } catch (Throwable $e) {
        // ignore
    }

    $rows = db_stmt_fetch_all(
        $con,
        'SELECT * FROM `product_variants` WHERE `userId`=? ORDER BY `variantId` ASC',
        's',
        $userId
    );

    if (is_array($rows)) {
        foreach ($rows as $row) {
            $item = array();
            $item['variantId'] = isset($row['variantId']) ? $row['variantId'] : '';
            $item['localVariantId'] = isset($row['local_variant_id']) ? $row['local_variant_id'] : '';
            $item['productId'] = isset($row['productId']) ? $row['productId'] : '';
            $item['productNetworkStatus'] = isset($row['productNetworkStatus'])
                ? $row['productNetworkStatus'] : '';
            $item['variantSize'] = isset($row['variantSize']) ? $row['variantSize'] : '';
            $item['variantColor'] = isset($row['variantColor']) ? $row['variantColor'] : '';
            $item['variantSku'] = isset($row['variantSku']) ? $row['variantSku'] : '';
            $item['variantPrice'] = isset($row['variantPrice']) ? $row['variantPrice'] : '';
            $item['variantDeletedStatus'] = isset($row['variantDeletedStatus'])
                ? $row['variantDeletedStatus'] : '0';
            $item['variantSortOrder'] = isset($row['variantSortOrder']) ? $row['variantSortOrder'] : '0';
            $item['variantNetworkStatus'] = isset($row['variantNetworkStatus'])
                ? $row['variantNetworkStatus'] : '';
            $response['productVariantResponse'][] = $item;
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
