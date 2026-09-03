<?php
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';

/**
 * Live server may be missing multi-branch scope columns until p7 is applied.
 * Add them on first write so POS inventory sync does not return HTTP 500.
 */
function inventory_ensure_scope_columns($con)
{
    static $ensured = false;
    if ($ensured || $con === null) {
        return;
    }
    require_once __DIR__ . '/php_compat.php';
    $columns = array(
        'organization_id' => "ALTER TABLE `inventory` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `userId`",
        'branch_id' => "ALTER TABLE `inventory` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`",
        'device_id' => "ALTER TABLE `inventory` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`",
    );
    try {
        foreach ($columns as $name => $ddl) {
            $col = db_safe_query($con, "SHOW COLUMNS FROM `inventory` LIKE '" . $name . "'");
            if ($col && mysqli_num_rows($col) === 0) {
                db_safe_query($con, $ddl);
            }
            if ($col) {
                mysqli_free_result($col);
            }
        }
    } catch (Throwable $e) {
        // Ignore schema probe failures — request can still proceed.
    }
    $ensured = true;
}

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    mysqli_query($con, 'set names utf8');

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

  inventory_ensure_scope_columns($con);

  $productId = isset($_POST['productId']) ? $_POST['productId'] : '';
  $productInventoryQuantity = isset($_POST['productInventoryQuantity']) ? $_POST['productInventoryQuantity'] : '0';
  $afterSaleInventoryQuantity = isset($_POST['afterSaleInventoryQuantity']) ? $_POST['afterSaleInventoryQuantity'] : '0';
  $saleInventoryQuantity = isset($_POST['saleInventoryQuantity']) ? $_POST['saleInventoryQuantity'] : '0';
  $inventoryDate = isset($_POST['inventoryDate']) ? $_POST['inventoryDate'] : date('Y-m-d');
  $inventoryNetworkStatus = isset($_POST['inventoryNetworkStatus']) ? $_POST['inventoryNetworkStatus'] : '';

	date_default_timezone_set('Asia/Kolkata');

    $check = db_stmt_fetch_one(
        $con,
        'SELECT * FROM `inventory` WHERE `userId`=? AND `inventoryNetworkStatus`=?',
        'ss',
        $userId,
        $inventoryNetworkStatus
    );
    if($check !== null) {
        $inventoryId = $check['inventoryId'];
        $updated = db_stmt_execute(
            $con,
            'UPDATE `inventory` SET `organization_id`=?, `branch_id`=?, `device_id`=?, `productId`=?, `productInventoryQuantity`=?, `afterSaleInventoryQuantity`=?, `saleInventoryQuantity`=?, `inventoryDate`=? WHERE `inventoryId`=?',
            'iissssssi',
            $orgId,
            $branchId,
            $deviceId,
            $productId,
            $productInventoryQuantity,
            $afterSaleInventoryQuantity,
            $saleInventoryQuantity,
            $inventoryDate,
            (int) $inventoryId
        );
        $response["status"] = $updated ? '1' : '0';
        $response["message"] = $updated ? "update successful!" : "update failed!";
    } else {
        $insertId = db_stmt_insert_id(
            $con,
            'INSERT INTO `inventory`(`userId`, `organization_id`, `branch_id`, `device_id`, `productId`, `productInventoryQuantity`, `afterSaleInventoryQuantity`, `saleInventoryQuantity`, `inventoryDate`, `inventoryNetworkStatus`, `inventoryStatus`) VALUES (?,?,?,?,?,?,?,?,?,?,\'active\')',
            'siisssssss',
            $userId,
            $orgId,
            $branchId,
            $deviceId,
            $productId,
            $productInventoryQuantity,
            $afterSaleInventoryQuantity,
            $saleInventoryQuantity,
            $inventoryDate,
            $inventoryNetworkStatus
        );
        $response["status"] = $insertId !== false ? '1' : '0';
        $response["message"] = $insertId !== false ? "insert successful!" : "insert failed!";
    }
}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
