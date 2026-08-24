<?php	
include_once('config.php');
$i=0;
   
    $response["invoiceProductResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        $invoiceDate = isset($_GET['invoiceDate']) ? $_GET['invoiceDate'] : '';
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	if ($invoiceDate !== '') {
		$sth="SELECT * FROM `invoice_final_product` LEFT JOIN `invoice` ON `invoice`.`invoiceNumber`=`invoice_final_product`.`invoiceNumber` WHERE `invoice`.`licenseId`='$userId' AND `invoice`.`invoiceDate` LIKE '%$invoiceDate%'";
	} else {
		$sth="SELECT * FROM `invoice_final_product` LEFT JOIN `invoice` ON `invoice`.`invoiceNumber`=`invoice_final_product`.`invoiceNumber` WHERE `invoice`.`licenseId`='$userId'";
	}

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["invoiceProductId"]=$row['invoiceProductId'];
        $getdata["invoiceNumber"]=$row['invoiceNumber'];
        $getdata["productName"]=$row['productName'];
        $getdata["productPrice"]=$row['productPrice'];
        $getdata["productUnit"]=$row['productUnit'];
        $getdata["productCGST"]=$row['productCGST'];
        $getdata["productSGST"]=$row['productSGST'];
        $getdata["productQuantity"]=$row['productQuantity'];
        $getdata["productStatus"]=$row['productStatus'];
        $getdata["invoiceNetworkStatus"]=$row['invoiceNetworkStatus'];
        $getdata["invoiceProductStatus"]=$row['invoiceProductNetworkStatus'];
        if (!empty($row['portionId'])) {
            $getdata["portionId"] = $row['portionId'];
        }
        if (!empty($row['portionName'])) {
            $getdata["portionName"] = $row['portionName'];
        }
        if (!empty($row['snapshotProductName'])) {
            $getdata["snapshotProductName"] = $row['snapshotProductName'];
        }
        if (!empty($row['snapshotLinePrice'])) {
            $getdata["snapshotLinePrice"] = $row['snapshotLinePrice'];
        }
        if (!empty($row['invoiceItemType'])) {
            $getdata["invoiceItemType"] = $row['invoiceItemType'];
        }
        if (!empty($row['comboNetworkStatus'])) {
            $getdata["comboNetworkStatus"] = $row['comboNetworkStatus'];
        }
        if (!empty($row['snapshotComboComponents'])) {
            $getdata["snapshotComboComponents"] = $row['snapshotComboComponents'];
        }
      
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["invoiceProductResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>
