<?php	
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/dine_in_helpers.php';

$i=0;
   
    $response["printerResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        $__postedUserId = isset($_GET['userId']) ? $_GET['userId'] : (isset($userId) ? $userId : '');
        pos_require_auth($con, $__postedUserId, isset($response) ? $response : array('status'=>'0','message'=>'Unauthorized'));

        dine_in_ensure_printer_kot_columns($con);
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `company_printer_setting` WHERE `licenseId`='$userId'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["settingId"]=$row['settingId'];
        $getdata["userId"]=$row['licenseId'];
        $getdata["printerName"]=$row['printerName'];
        $getdata["KOTPrinterName"]=$row['KOTPrinterName'];
        $getdata["invoicePrefix"]=$row['invoicePrefix'];
        $getdata["invoiceTitle"]=$row['invoiceTitle'];
        $getdata["invoiceTermsCondition"]=$row['invoiceTermsCondition'];
        $getdata["logoUse"]=$row['logoUse'];
        $getdata["paymentUse"]=$row['paymentUse'];
        $getdata["customerUse"]=$row['customerUse'];
        $getdata["productQuantityUpdate"]=$row['productQuantityUpdate'];
        $getdata["duplicateBillUse"]=isset($row['duplicateBillUse']) && $row['duplicateBillUse']!=null && $row['duplicateBillUse']!=='' ? $row['duplicateBillUse'] : "off";
        $getdata["bluetoothAddress"]=$row['bluetoothAddress']!=null?$row['bluetoothAddress']:"";
        $getdata["bluetoothKOTAddress"]=$row['bluetoothKOTAddress']!=null?$row['bluetoothKOTAddress']:"";
        $getdata["printerFeedLines"]=$row['printerFeedLines']!=null?$row['printerFeedLines']:"";
        $getdata["KotPrinterFeedLines"]=$row['KotPrinterFeedLines']!=null?$row['KotPrinterFeedLines']:"";
        $getdata["kotEnable"]=isset($row['kotEnable']) && $row['kotEnable']!=='' ? $row['kotEnable'] : "on";
        $getdata["kotPrefix"]=isset($row['kotPrefix']) && $row['kotPrefix']!=='' ? $row['kotPrefix'] : "KOT-";
        $getdata["kotCopies"]=isset($row['kotCopies']) && $row['kotCopies']!=='' ? $row['kotCopies'] : "1";
        $getdata["kotAutoPrint"]=isset($row['kotAutoPrint']) && $row['kotAutoPrint']!=='' ? $row['kotAutoPrint'] : "off";
        $getdata["kotPreview"]=isset($row['kotPreview']) && $row['kotPreview']!=='' ? $row['kotPreview'] : "on";
        $getdata["settingStatus"]=$row['settingStatus'];
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["printerResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>
