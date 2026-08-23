<?php	
include_once('config.php');
$i=0;
   
    $response["printerResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        
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
        $getdata["bluetoothAddress"]=$row['bluetoothAddress']!=null?$row['bluetoothAddress']:"";
        $getdata["bluetoothKOTAddress"]=$row['bluetoothKOTAddress']!=null?$row['bluetoothKOTAddress']:"";
        $getdata["printerFeedLines"]=$row['printerFeedLines']!=null?$row['printerFeedLines']:"";
        $getdata["KotPrinterFeedLines"]=$row['KotPrinterFeedLines']!=null?$row['KotPrinterFeedLines']:"";
        $getdata["settingStatus"]=$row['settingStatus'];
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["printerResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>