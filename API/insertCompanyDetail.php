<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/company_store_fields.php';


$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
     mysqli_query($con, 'set names utf8');
    
  $userId = $_POST['userId'];
  $__postedUserId = isset($_POST['userId']) ? $_POST['userId'] : (isset($userId) ? $userId : '');
  pos_require_auth($con, $__postedUserId, isset($response) ? $response : array('status'=>'0','message'=>'Unauthorized'));

  $companyName = isset($_POST['companyName']) ? $_POST['companyName'] : '';
  $cashierName = isset($_POST['cashierName']) ? $_POST['cashierName'] : '';
  $companyMobile = isset($_POST['companyMobile']) ? $_POST['companyMobile'] : '';
  $companyAddress = isset($_POST['companyAddress']) ? $_POST['companyAddress'] : '';
  $shopName1 = isset($_POST['shopName1']) ? $_POST['shopName1'] : '';
  $shopName2 = isset($_POST['shopName2']) ? $_POST['shopName2'] : '';
  $addressLine1 = isset($_POST['addressLine1']) ? $_POST['addressLine1'] : '';
  $addressLine2 = isset($_POST['addressLine2']) ? $_POST['addressLine2'] : '';
  $addressLine3 = isset($_POST['addressLine3']) ? $_POST['addressLine3'] : '';
  $phoneNo1 = isset($_POST['phoneNo1']) ? $_POST['phoneNo1'] : '';
  $phoneNo2 = isset($_POST['phoneNo2']) ? $_POST['phoneNo2'] : '';
  $currencyName = isset($_POST['currencyName']) ? $_POST['currencyName'] : '';
  $tableStatus = isset($_POST['tableStatus']) ? $_POST['tableStatus'] : '';
  $noOfTable = isset($_POST['noOfTable']) ? $_POST['noOfTable'] : '';
  $countryName = isset($_POST['countryName']) ? $_POST['countryName'] : '';
  $stateName = isset($_POST['stateName']) ? $_POST['stateName'] : '';
  $shopCGST = isset($_POST['shopCGST']) ? $_POST['shopCGST'] : '';
  $shopSGST = isset($_POST['shopSGST']) ? $_POST['shopSGST'] : '';
  $gstStatus = isset($_POST['gstStatus']) ? $_POST['gstStatus'] : '';
  $gstNumber = isset($_POST['gstNumber']) ? $_POST['gstNumber'] : '';
  $panNumber = isset($_POST['panNumber']) ? $_POST['panNumber'] : '';
  $companyFssis = isset($_POST['companyFssis']) ? $_POST['companyFssis'] : '';
  $companyLogo = isset($_POST['companyLogo']) ? $_POST['companyLogo'] : '';
  $paymentLogo = isset($_POST['paymentLogo']) ? $_POST['paymentLogo'] : '';
  $openingMinutes = company_normalize_minutes(isset($_POST['openingMinutes']) ? $_POST['openingMinutes'] : '');
  $closingMinutes = company_normalize_minutes(isset($_POST['closingMinutes']) ? $_POST['closingMinutes'] : '');
  $hasBusinessHoursColumns = companys_has_column($con, 'openingMinutes') && companys_has_column($con, 'closingMinutes');
  $hoursSqlFragment = '';
  if ($hasBusinessHoursColumns && $openingMinutes !== null && $closingMinutes !== null) {
      $hoursSqlFragment = ", `openingMinutes`='" . mysqli_real_escape_string($con, $openingMinutes)
          . "', `closingMinutes`='" . mysqli_real_escape_string($con, $closingMinutes) . "'";
  }

  // Backward-compatible mapping when older clients omit structured fields
  if ($shopName1 === '' && $companyName !== '') {
      $shopName1 = $companyName;
  }
  if ($companyName === '' && $shopName1 !== '') {
      $companyName = $shopName1;
  }
  if ($phoneNo1 === '' && $companyMobile !== '') {
      $phoneNo1 = $companyMobile;
  }
  if ($companyMobile === '' && $phoneNo1 !== '') {
      $companyMobile = $phoneNo1;
  }
  if ($addressLine1 === '' && $addressLine2 === '' && $addressLine3 === '' && $companyAddress !== '') {
      $addressLine1 = $companyAddress;
  }
  if ($companyAddress === '') {
      $parts = array();
      if ($addressLine1 !== '') { $parts[] = $addressLine1; }
      if ($addressLine2 !== '') { $parts[] = $addressLine2; }
      if ($addressLine3 !== '') { $parts[] = $addressLine3; }
      $companyAddress = implode("\n", $parts);
  }
  
  if($currencyName== 'Rupee: ₹') {
      $currencyName = "Rupee: ₹";
  }

	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    $sql="SELECT * FROM `companys` WHERE `licenseId`='$userId'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
		
				if(isset($check))
				{
				    
				    $companyId = $check['companyId'];
				    $sql="UPDATE `companys` SET `companyLogo`='$companyLogo', `paymentLogo`='$paymentLogo', `companyName`='$companyName', `cashierName`='$cashierName', `companyMobile`='$companyMobile', `companyAddress`='$companyAddress', `shopName1`='$shopName1', `shopName2`='$shopName2', `addressLine1`='$addressLine1', `addressLine2`='$addressLine2', `addressLine3`='$addressLine3', `phoneNo1`='$phoneNo1', `phoneNo2`='$phoneNo2', `currencyName`='$currencyName', `countryName`='$countryName', `stateName`='$stateName',
				          `tableStatus`='$tableStatus', `noOfTable`='$noOfTable', `gstStatus`='$gstStatus', `gstNumber`='$gstNumber', `shopCGST`='$shopCGST', `shopSGST`='$shopSGST', `panNumber`='$panNumber', `companyFssis`='$companyFssis'" . $hoursSqlFragment . " WHERE `companyId`='$companyId'";

                 if(mysqli_query($con, $sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                     }
				    
				    
				} else {

                 $hoursInsertCols = '';
                 $hoursInsertVals = '';
                 if ($hasBusinessHoursColumns && $openingMinutes !== null && $closingMinutes !== null) {
                     $hoursInsertCols = ", `openingMinutes`, `closingMinutes`";
                     $hoursInsertVals = ", '" . mysqli_real_escape_string($con, $openingMinutes)
                         . "', '" . mysqli_real_escape_string($con, $closingMinutes) . "'";
                 }

                 $sql="INSERT INTO `companys`(`licenseId`, `companyLogo`, `companyName`, `cashierName`, `companyMobile`, `companyAddress`, `shopName1`, `shopName2`, `addressLine1`, `addressLine2`, `addressLine3`, `phoneNo1`, `phoneNo2`, `currencyName`, `tableStatus`, `noOfTable`, `countryName`, `stateName`, `gstStatus`, `gstNumber`, `shopCGST`, `shopSGST`, `panNumber`, `companyFssis`, `paymentLogo`, `companyStatus`" . $hoursInsertCols . ") 
                       VALUES ('$userId', '$companyLogo', '$companyName', '$cashierName', '$companyMobile', '$companyAddress', '$shopName1', '$shopName2', '$addressLine1', '$addressLine2', '$addressLine3', '$phoneNo1', '$phoneNo2', '$currencyName', '$tableStatus', '$noOfTable', '$countryName', '$stateName', '$gstStatus', '$gstNumber', '$shopCGST', '$shopSGST', '$panNumber', '$companyFssis', '$paymentLogo', 'active'" . $hoursInsertVals . ")";

                 if(mysqli_query($con,$sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "insert successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "insert failed!";
 
                     }

                }
}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
