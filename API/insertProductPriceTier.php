<?php
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';
require_once __DIR__ . '/php_compat.php';

/**
 * Ensure product_price_tiers exists (additive). Prefer API/migrations/p29_product_price_tier.sql.
 */
function product_price_tier_ensure_table($con)
{
    static $ensured = false;
    if ($ensured || $con === null) {
        return;
    }
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
              UNIQUE KEY `uq_price_tier_local` (`userId`, `local_tier_id`),
              KEY `idx_price_tier_branch` (`branch_id`),
              KEY `idx_price_tier_product` (`productNetworkStatus`)
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

    product_price_tier_ensure_table($con);

    $localTierId = isset($_POST['localTierId']) ? trim($_POST['localTierId']) : '';
    $productId = isset($_POST['productId']) ? trim($_POST['productId']) : '';
    $productNetworkStatus = isset($_POST['productNetworkStatus']) ? trim($_POST['productNetworkStatus']) : '';
    $minQty = isset($_POST['minQty']) ? trim($_POST['minQty']) : '1';
    $tierPrice = isset($_POST['tierPrice']) ? trim($_POST['tierPrice']) : '';
    $tierLabel = isset($_POST['tierLabel']) ? trim($_POST['tierLabel']) : '';
    $tierDeletedStatus = isset($_POST['tierDeletedStatus']) ? trim($_POST['tierDeletedStatus']) : '0';
    $tierNetworkStatus = isset($_POST['tierNetworkStatus']) ? trim($_POST['tierNetworkStatus']) : '';

    if ($localTierId === '') {
        $response['message'] = 'Missing localTierId';
    } elseif ($tierPrice === '' && $tierDeletedStatus !== '1') {
        $response['message'] = 'Missing tierPrice';
    } else {
        if ($minQty === '') {
            $minQty = '1';
        }
        if ($tierDeletedStatus === '') {
            $tierDeletedStatus = '0';
        }

        // Prefer cloud product id via productNetworkStatus (same pattern as portions).
        if ($productNetworkStatus !== '') {
            $prod = db_stmt_fetch_one(
                $con,
                'SELECT `productId` FROM `products` WHERE `userId`=? AND `productNetworkStatus`=? LIMIT 1',
                'ss',
                $userId,
                $productNetworkStatus
            );
            if ($prod !== null) {
                $productId = (string) $prod['productId'];
            }
        }

        $existing = db_stmt_fetch_one(
            $con,
            'SELECT tierId FROM `product_price_tiers` WHERE `userId`=? AND `local_tier_id`=? LIMIT 1',
            'ss',
            $userId,
            $localTierId
        );

        if ($existing !== null) {
            $tierId = (int) $existing['tierId'];
            $updated = db_stmt_execute(
                $con,
                'UPDATE `product_price_tiers` SET
                    `organization_id`=?, `branch_id`=?, `device_id`=?,
                    `productId`=?, `productNetworkStatus`=?, `minQty`=?, `tierPrice`=?,
                    `tierLabel`=?, `tierDeletedStatus`=?, `tierNetworkStatus`=?
                 WHERE `tierId`=?',
                'iissssssssi',
                $orgId,
                $branchId,
                $deviceId,
                $productId,
                $productNetworkStatus,
                $minQty,
                $tierPrice,
                $tierLabel,
                $tierDeletedStatus,
                $tierNetworkStatus,
                $tierId
            );
            $response['status'] = $updated ? '1' : '0';
            $response['message'] = $updated ? 'update successful!' : 'update failed!';
            $response['tierId'] = $tierId;
        } else {
            $insertId = db_stmt_insert_id(
                $con,
                'INSERT INTO `product_price_tiers`
                (`userId`, `organization_id`, `branch_id`, `device_id`, `local_tier_id`,
                 `productId`, `productNetworkStatus`, `minQty`, `tierPrice`, `tierLabel`,
                 `tierDeletedStatus`, `tierNetworkStatus`)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?)',
                'siisssssssss',
                $userId,
                $orgId,
                $branchId,
                $deviceId,
                $localTierId,
                $productId,
                $productNetworkStatus,
                $minQty,
                $tierPrice,
                $tierLabel,
                $tierDeletedStatus,
                $tierNetworkStatus
            );
            $response['status'] = $insertId !== false ? '1' : '0';
            $response['message'] = $insertId !== false ? 'insert successful!' : 'insert failed!';
            if ($insertId !== false) {
                $response['tierId'] = $insertId;
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
