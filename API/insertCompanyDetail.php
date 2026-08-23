<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
     mysqli_query($con, 'set names utf8');
    
  $userId = $_POST['userId'];
  $companyName = $_POST['companyName'];
  $cashierName = $_POST['cashierName'];
  $companyMobile = $_POST['companyMobile'];
  $companyAddress = $_POST['companyAddress'];
  $currencyName = $_POST['currencyName'];
  $tableStatus = $_POST['tableStatus'];
  $noOfTable = $_POST['noOfTable'];
  $countryName = $_POST['countryName'];
  $stateName = $_POST['stateName'];
  $shopCGST = $_POST['shopCGST'];
  $shopSGST = $_POST['shopSGST'];
  $gstStatus = $_POST['gstStatus'];
  $gstNumber = $_POST['gstNumber'];
  $panNumber = $_POST['panNumber'];
  $companyFssis = $_POST['companyFssis'];
  $companyLogo = $_POST['companyLogo'];
  $paymentLogo = $_POST['paymentLogo'];
  
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
				    $sql="UPDATE `companys` SET `companyLogo`='$companyLogo', `paymentLogo`='$paymentLogo', `companyName`='$companyName', `cashierName`='$cashierName', `companyMobile`='$companyMobile', `companyAddress`='$companyAddress', `currencyName`='$currencyName', `countryName`='$countryName', `stateName`='$stateName',
				          `tableStatus`='$tableStatus', `noOfTable`='$noOfTable', `gstStatus`='$gstStatus', `gstNumber`='$gstNumber', `shopCGST`='$shopCGST', `shopSGST`='$shopSGST', `panNumber`='$panNumber', `companyFssis`='$companyFssis' WHERE `companyId`='$companyId'";

                 if(mysqli_query($con, $sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                     }
				    
				    
				} else {

                 $sql="INSERT INTO `companys`(`licenseId`, `companyLogo`, `companyName`, `cashierName`, `companyMobile`, `companyAddress`, `currencyName`, `tableStatus`, `noOfTable`, `countryName`, `stateName`, `gstStatus`, `gstNumber`, `shopCGST`, `shopSGST`, `panNumber`, `companyFssis`, `companyStatus`) 
                       VALUES ('$userId', '$companyLogo', '$companyName', '$cashierName', '$companyMobile', '$companyAddress', '$currencyName', '$tableStatus', '$noOfTable', '$countryName', '$stateName', '$gstStatus',  '$shopCGST', '$shopSGST', '$gstNumber', '$panNumber', '$companyFssis', 'active')";

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
