<?php
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';

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

  $productId = $_POST['productId'];
  $productInventoryQuantity = $_POST['productInventoryQuantity'];
  $afterSaleInventoryQuantity = $_POST['afterSaleInventoryQuantity'];
  $saleInventoryQuantity = $_POST['saleInventoryQuantity'];
  $inventoryDate = $_POST['inventoryDate'];
  $inventoryNetworkStatus = $_POST['inventoryNetworkStatus'];

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
