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

  $expensesName = $_POST['expensesName'];
  $expensesAmount = $_POST['expensesAmount'];
  $expensesDate = $_POST['expensesDate'];
  $expensesNetworkStatus = $_POST['expensesNetworkStatus'];

	date_default_timezone_set('Asia/Kolkata');

    $check = db_stmt_fetch_one(
        $con,
        'SELECT * FROM `expenses` WHERE `userId`=? AND `expensesNetworkStatus`=?',
        'ss',
        $userId,
        $expensesNetworkStatus
    );
    if($check !== null) {
        $expensesId = $check['expensesId'];
        $updated = db_stmt_execute(
            $con,
            'UPDATE `expenses` SET `organization_id`=?, `branch_id`=?, `device_id`=?, `expensesName`=?, `expensesAmount`=?, `expensesDate`=? WHERE `expensesId`=?',
            'iissssi',
            $orgId,
            $branchId,
            $deviceId,
            $expensesName,
            $expensesAmount,
            $expensesDate,
            (int) $expensesId
        );
        $response["status"] = $updated ? '1' : '0';
        $response["message"] = $updated ? "update successful!" : "update failed!";
    } else {
        $insertId = db_stmt_insert_id(
            $con,
            'INSERT INTO `expenses`(`userId`, `organization_id`, `branch_id`, `device_id`, `expensesName`, `expensesAmount`, `expensesDate`, `expensesNetworkStatus`, `expensesStatus`) VALUES (?,?,?,?,?,?,?,?,\'active\')',
            'siisssss',
            $userId,
            $orgId,
            $branchId,
            $deviceId,
            $expensesName,
            $expensesAmount,
            $expensesDate,
            $expensesNetworkStatus
        );
        $response["status"] = $insertId !== false ? '1' : '0';
        $response["message"] = $insertId !== false ? "insert successful!" : "insert failed!";
    }
}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
