<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/php_compat.php';

$response = array('status' => '1', 'message' => 'ok', 'productPriceTierResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
    pos_require_auth($con, $userId, isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized'));

    try {
        db_safe_query(
            $con,
            "CREATE TABLE IF NOT EXISTS `product_price_tiers` (
              `tierId` INT NOT NULL AUTO_INCREMENT,
              `userId` VARCHAR(64) NOT NULL,
              `organization_id` INT NULL DEFAULT NULL,
              `branch_id` INT NULL DEFAULT NULL,
              `device_id` VARCHAR(255) NULL DEFAULT NULL,
              `local_tier_id` VARCHAR(64) NOT NULL,
              `productId` VARCHAR(64) NULL DEFAULT NULL,
              `productNetworkStatus` VARCHAR(128) NULL DEFAULT NULL,
              `minQty` VARCHAR(64) NOT NULL DEFAULT '1',
              `tierPrice` VARCHAR(64) NOT NULL,
              `tierLabel` VARCHAR(255) NULL DEFAULT NULL,
              `tierDeletedStatus` VARCHAR(8) NOT NULL DEFAULT '0',
              `tierNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
              `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`tierId`),
              UNIQUE KEY `uq_price_tier_local` (`userId`, `local_tier_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    } catch (Throwable $e) {
        // ignore
    }

    $rows = db_stmt_fetch_all(
        $con,
        'SELECT * FROM `product_price_tiers` WHERE `userId`=? ORDER BY `tierId` ASC',
        's',
        $userId
    );

    if (is_array($rows)) {
        foreach ($rows as $row) {
            $item = array();
            $item['tierId'] = isset($row['tierId']) ? $row['tierId'] : '';
            $item['localTierId'] = isset($row['local_tier_id']) ? $row['local_tier_id'] : '';
            $item['productId'] = isset($row['productId']) ? $row['productId'] : '';
            $item['productNetworkStatus'] = isset($row['productNetworkStatus'])
                ? $row['productNetworkStatus'] : '';
            $item['minQty'] = isset($row['minQty']) ? $row['minQty'] : '1';
            $item['tierPrice'] = isset($row['tierPrice']) ? $row['tierPrice'] : '';
            $item['tierLabel'] = isset($row['tierLabel']) ? $row['tierLabel'] : '';
            $item['tierDeletedStatus'] = isset($row['tierDeletedStatus'])
                ? $row['tierDeletedStatus'] : '0';
            $item['tierNetworkStatus'] = isset($row['tierNetworkStatus'])
                ? $row['tierNetworkStatus'] : '';
            $response['productPriceTierResponse'][] = $item;
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
