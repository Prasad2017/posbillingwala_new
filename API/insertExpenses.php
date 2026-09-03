<?php
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';

/**
 * Live server may be missing multi-branch scope columns until p7 is applied.
 */
function expenses_ensure_scope_columns($con)
{
    static $ensured = false;
    if ($ensured || $con === null) {
        return;
    }
    require_once __DIR__ . '/php_compat.php';
    $columns = array(
        'organization_id' => "ALTER TABLE `expenses` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `userId`",
        'branch_id' => "ALTER TABLE `expenses` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`",
        'device_id' => "ALTER TABLE `expenses` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`",
    );
    try {
        foreach ($columns as $name => $ddl) {
            $col = db_safe_query($con, "SHOW COLUMNS FROM `expenses` LIKE '" . $name . "'");
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

  expenses_ensure_scope_columns($con);

  $expensesName = isset($_POST['expensesName']) ? $_POST['expensesName'] : '';
  $expensesAmount = isset($_POST['expensesAmount']) ? $_POST['expensesAmount'] : '0';
  $expensesDate = isset($_POST['expensesDate']) ? $_POST['expensesDate'] : date('Y-m-d');
  $expensesNetworkStatus = isset($_POST['expensesNetworkStatus']) ? $_POST['expensesNetworkStatus'] : '';

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
