<?php

include_once('config.php');

include_once('licence_expiry.php');

require_once __DIR__ . '/auth_tokens.php';

require_once __DIR__ . '/branch_scope.php';

/**
 * Live server is missing split-payment columns until p21 is applied.
 * Add them on first write so POS cloud sync does not stay pending.
 */
function invoice_ensure_cash_upi_columns($con)
{
    static $ensured = false;
    if ($ensured || $con === null) {
        return;
    }
    require_once __DIR__ . '/php_compat.php';
    try {
        $cash = db_safe_query($con, "SHOW COLUMNS FROM `invoice` LIKE 'cashAmount'");
        if ($cash && mysqli_num_rows($cash) === 0) {
            db_safe_query($con, "ALTER TABLE `invoice` ADD COLUMN `cashAmount` VARCHAR(50) NOT NULL DEFAULT '0' AFTER `paymentMode`");
        }
        if ($cash) {
            mysqli_free_result($cash);
        }
        $upi = db_safe_query($con, "SHOW COLUMNS FROM `invoice` LIKE 'upiAmount'");
        if ($upi && mysqli_num_rows($upi) === 0) {
            db_safe_query($con, "ALTER TABLE `invoice` ADD COLUMN `upiAmount` VARCHAR(50) NOT NULL DEFAULT '0' AFTER `cashAmount`");
        }
        if ($upi) {
            mysqli_free_result($upi);
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



  $post = function ($key, $default = '') {
      return isset($_POST[$key]) ? (string) $_POST[$key] : $default;
  };

  $noOfTable = $post('noOfTable');

  $invoiceType = $post('invoiceType');

  $invoiceNumber = $post('invoiceNumber');

  $customerName = $post('customerName');

  $customerMobile = $post('customerMobile');

  $customerEmail = $post('customerEmail');

  $customerAddress = $post('customerAddress');

  $subTotal = $post('subTotal', '0');

  $totalGSTAmount = $post('totalGSTAmount', '0');

  $discount = $post('discount', '0');

  $discountType = trim($post('discountType', ''));
  if ($discountType !== 'Percentage' && $discountType !== 'Amount') {
      $discountType = 'Amount';
  }

  $packingCharge = $post('packingCharge', '0');

  $packingChargeType = trim($post('packingChargeType', 'Percentage'));
  if ($packingChargeType !== 'Percentage' && $packingChargeType !== 'Amount') {
      $packingChargeType = 'Percentage';
  }

  $totalAmount = $post('totalAmount', '0');

  $paymentMode = $post('paymentMode');

  $cashAmount = $post('cashAmount', '0');

  $upiAmount = $post('upiAmount', '0');

  $invoiceDate = $post('invoiceDate');

  $invoiceOrderStatus = $post('invoiceOrderStatus');

  $invoiceNetworkStatus = $post('invoiceNetworkStatus');

  

  $date = strtotime($invoiceDate);
  if ($date === false) {
      $date = time();
  }

  $invoiceDate = date('Y-m-d H:i:s', $date);

	

	date_default_timezone_set('Asia/Kolkata');

    $date=date('Y-m-d');

    

		    try {

		    invoice_ensure_cash_upi_columns($con);

		    $check = db_stmt_fetch_one(

		        $con,

		        'SELECT * FROM `invoice` WHERE `licenseId`=? AND `invoiceNetworkStatus`=?',

		        'ss',

		        $userId,

		        $invoiceNetworkStatus

		    );

				if($check !== null)

				{

				    

				    $invoiceId = $check['invoiceId'];

				    $updated = db_stmt_execute(

				        $con,

				        'UPDATE `invoice` SET `organization_id`=?, `branch_id`=?, `device_id`=?, `noOfTable`=?, `invoiceType`=?, `invoiceNumber`=?, `customerName`=?, `customerMobile`=?, `customerEmail`=?, `customerAddress`=?, `subTotal`=?, `totalGSTAmount`=?, `discount`=?, `discountType`=?, `packingCharge`=?, `packingChargeType`=?, `totalAmount`=?, `paymentMode`=?, `cashAmount`=?, `upiAmount`=?, `invoiceDate`=?, `invoiceOrderStatus`=?, `invoiceNetworkStatus`=? WHERE `invoiceId`=?',

				        'iisssssssssssssssssssssi',

				        $orgId,

				        $branchId,

				        $deviceId,

				        $noOfTable,

				        $invoiceType,

				        $invoiceNumber,

				        $customerName,

				        $customerMobile,

				        $customerEmail,

				        $customerAddress,

				        $subTotal,

				        $totalGSTAmount,

				        $discount,

				        $discountType,

				        $packingCharge,

				        $packingChargeType,

				        $totalAmount,

				        $paymentMode,

				        $cashAmount,

				        $upiAmount,

				        $invoiceDate,

				        $invoiceOrderStatus,

				        $invoiceNetworkStatus,

				        $invoiceId

				    );



                 if($updated){

	

                       $response["status"] = '1';

                       $response["message"] = "update successful!";

  

                   }

                   else{

    

                        $response["status"] = '0';

                        $response["message"] = "update failed!";

 

                     }

				    

				    

				} else {



                 // P4-2: Demo/Trial licences — enforce server max bill count before insert

                 $licenseRow = licence_load_by_id($con, $userId);

                 if ($licenseRow !== null && !licence_trial_allows_new_bill($con, $licenseRow)) {

                     licence_mark_trial_consumed($con, $userId);

                     $maxBills = licence_trial_max_bills();

                     $response["status"] = '0';

                     $response["message"] = "Trial bill limit reached (max $maxBills). Please upgrade your licence.";

                     $response["trialMaxBills"] = (string) $maxBills;

                     $response["trialBillCount"] = (string) licence_count_bills($con, $userId);

                     header('Content-type: application/json; charset=utf-8');

                     echo json_encode($response);

                     exit;

                 }



                 $insertId = db_stmt_insert_id(

                     $con,

                     'INSERT INTO `invoice`(`licenseId`, `organization_id`, `branch_id`, `device_id`, `noOfTable`, `invoiceType`, `invoiceNumber`, `customerName`, `customerMobile`, `customerEmail`, `customerAddress`, `subTotal`, `totalGSTAmount`, `discount`, `discountType`, `packingCharge`, `packingChargeType`, `totalAmount`, `paymentMode`, `cashAmount`, `upiAmount`, `invoiceDate`, `invoiceOrderStatus`, `invoiceNetworkStatus`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',

                     'siisssssssssssssssssssss',

                     $userId,

                     $orgId,

                     $branchId,

                     $deviceId,

                     $noOfTable,

                     $invoiceType,

                     $invoiceNumber,

                     $customerName,

                     $customerMobile,

                     $customerEmail,

                     $customerAddress,

                     $subTotal,

                     $totalGSTAmount,

                     $discount,

                     $discountType,

                     $packingCharge,

                     $packingChargeType,

                     $totalAmount,

                     $paymentMode,

                     $cashAmount,

                     $upiAmount,

                     $invoiceDate,

                     $invoiceOrderStatus,

                     $invoiceNetworkStatus

                 );



                 if($insertId !== false){

	

                       $response["status"] = '1';

                       $response["message"] = "insert successful!";

  

                   }

                   else{

    

                        $response["status"] = '0';

                        $response["message"] = "insert failed!";

 

                     }



                }

		    } catch (Throwable $e) {
		        error_log('insertInvoice.php: ' . $e->getMessage());
		        $response['status'] = '0';
		        $response['message'] = 'insert failed: ' . $e->getMessage();
		    }

}

header('Content-type: application/json; charset=utf-8');

	echo json_encode($response);

?>


