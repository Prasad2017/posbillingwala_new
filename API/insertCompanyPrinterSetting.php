<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
     mysqli_query($con, 'set names utf8');
     
  $userId = $_POST['userId'];
  $printerName = $_POST['printerName'];
  $KOTPrinterName = $_POST['KOTPrinterName'];
  $invoicePrefix = $_POST['invoicePrefix'];
  $invoiceTitle = $_POST['invoiceTitle'];
  $invoiceTermsCondition = $_POST['invoiceTermsCondition'];
  $logoUse = $_POST['logoUse'];
  $paymentUse = $_POST['paymentUse'];
  $customerUse = $_POST['customerUse'];
  $productQuantityUpdate = $_POST['productQuantityUpdate'];
  $bluetoothAddress = $_POST['bluetoothAddress'];
  $bluetoothKOTAddress = $_POST['bluetoothKOTAddress'];
  $printerFeedLines = $_POST['printerFeedLines'];
  $KotPrinterFeedLines = $_POST['KotPrinterFeedLines'];

	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    $sql="SELECT * FROM `company_printer_setting` WHERE `licenseId`='$userId'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
		
				if(isset($check))
				{
				    
				    $settingId = $check['settingId'];
				    $sql="UPDATE `company_printer_setting` SET `printerName`='$printerName', `KOTPrinterName`='$KOTPrinterName', `invoicePrefix`='$invoicePrefix', `invoiceTitle`='$invoiceTitle', 
				    `invoiceTermsCondition`='$invoiceTermsCondition', `logoUse`='$logoUse', `paymentUse`='$paymentUse', `customerUse`='$customerUse', `productQuantityUpdate`='$productQuantityUpdate', 
				    `bluetoothAddress`='$bluetoothAddress', `bluetoothKOTAddress`='$bluetoothKOTAddress', `printerFeedLines`='$printerFeedLines', `KotPrinterFeedLines`='$KotPrinterFeedLines' WHERE `settingId`='$settingId'";

                 if(mysqli_query($con, $sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                     }
				    
				    
				} else {

                 $sql="INSERT INTO `company_printer_setting`(`licenseId`, `KOTPrinterName`, `printerName`, `invoicePrefix`, `invoiceTitle`, `invoiceTermsCondition`, `logoUse`, `paymentUse`, `customerUse`, `productQuantityUpdate`, `bluetoothAddress`, `bluetoothKOTAddress`, `printerFeedLines`, `KotPrinterFeedLines`, `settingStatus`) 
                       VALUES ('$userId', '$printerName', '$KOTPrinterName', '$invoicePrefix', '$invoiceTitle', '$invoiceTermsCondition', '$logoUse', '$paymentUse', '$customerUse', '$productQuantityUpdate', '$bluetoothAddress', '$bluetoothKOTAddress', '$printerFeedLines', '$KotPrinterFeedLines', 'active')";

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
