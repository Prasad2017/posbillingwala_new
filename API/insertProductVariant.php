<?php
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';
require_once __DIR__ . '/php_compat.php';

/**
 * Ensure product_variants exists (additive). Prefer API/migrations/p30_product_variant.sql.
 */
function product_variant_ensure_table($con)
{
    static $ensured = false;
    if ($ensured || $con === null) {
        return;
    }
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
              UNIQUE KEY `uq_product_variant_local` (`userId`, `local_variant_id`),
              KEY `idx_product_variant_branch` (`branch_id`),
              KEY `idx_product_variant_product` (`productNetworkStatus`)
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

    product_variant_ensure_table($con);

    $localVariantId = isset($_POST['localVariantId']) ? trim($_POST['localVariantId']) : '';
    $productId = isset($_POST['productId']) ? trim($_POST['productId']) : '';
    $productNetworkStatus = isset($_POST['productNetworkStatus']) ? trim($_POST['productNetworkStatus']) : '';
    $variantSize = isset($_POST['variantSize']) ? trim($_POST['variantSize']) : '';
    $variantColor = isset($_POST['variantColor']) ? trim($_POST['variantColor']) : '';
    $variantSku = isset($_POST['variantSku']) ? trim($_POST['variantSku']) : '';
    $variantPrice = isset($_POST['variantPrice']) ? trim($_POST['variantPrice']) : '';
    $variantDeletedStatus = isset($_POST['variantDeletedStatus']) ? trim($_POST['variantDeletedStatus']) : '0';
    $variantSortOrder = isset($_POST['variantSortOrder']) ? trim($_POST['variantSortOrder']) : '0';
    $variantNetworkStatus = isset($_POST['variantNetworkStatus']) ? trim($_POST['variantNetworkStatus']) : '';

    if ($localVariantId === '') {
        $response['message'] = 'Missing localVariantId';
    } else {
        if ($variantDeletedStatus === '') {
            $variantDeletedStatus = '0';
        }
        if ($variantSortOrder === '' || !is_numeric($variantSortOrder)) {
            $variantSortOrder = '0';
        }

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
            'SELECT variantId FROM `product_variants` WHERE `userId`=? AND `local_variant_id`=? LIMIT 1',
            'ss',
            $userId,
            $localVariantId
        );

        if ($existing !== null) {
            $variantId = (int) $existing['variantId'];
            $updated = db_stmt_execute(
                $con,
                'UPDATE `product_variants` SET
                    `organization_id`=?, `branch_id`=?, `device_id`=?,
                    `productId`=?, `productNetworkStatus`=?, `variantSize`=?, `variantColor`=?,
                    `variantSku`=?, `variantPrice`=?, `variantDeletedStatus`=?,
                    `variantSortOrder`=?, `variantNetworkStatus`=?
                 WHERE `variantId`=?',
                'iissssssssisi',
                $orgId,
                $branchId,
                $deviceId,
                $productId,
                $productNetworkStatus,
                $variantSize,
                $variantColor,
                $variantSku,
                $variantPrice,
                $variantDeletedStatus,
                (int) $variantSortOrder,
                $variantNetworkStatus,
                $variantId
            );
            $response['status'] = $updated ? '1' : '0';
            $response['message'] = $updated ? 'update successful!' : 'update failed!';
            $response['variantId'] = $variantId;
        } else {
            $insertId = db_stmt_insert_id(
                $con,
                'INSERT INTO `product_variants`
                (`userId`, `organization_id`, `branch_id`, `device_id`, `local_variant_id`,
                 `productId`, `productNetworkStatus`, `variantSize`, `variantColor`, `variantSku`,
                 `variantPrice`, `variantDeletedStatus`, `variantSortOrder`, `variantNetworkStatus`)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
                'siisssssssssis',
                $userId,
                $orgId,
                $branchId,
                $deviceId,
                $localVariantId,
                $productId,
                $productNetworkStatus,
                $variantSize,
                $variantColor,
                $variantSku,
                $variantPrice,
                $variantDeletedStatus,
                (int) $variantSortOrder,
                $variantNetworkStatus
            );
            $response['status'] = $insertId !== false ? '1' : '0';
            $response['message'] = $insertId !== false ? 'insert successful!' : 'insert failed!';
            if ($insertId !== false) {
                $response['variantId'] = $insertId;
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
