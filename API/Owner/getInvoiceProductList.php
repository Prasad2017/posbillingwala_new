<?php	
include_once('config.php');
require_once __DIR__ . '/../company_store_fields.php';
$i=0;
   
    $response["invoiceProductResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $invoiceId = $_GET['invoiceId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `invoice_final_product` 
	      LEFT JOIN `invoice` ON `invoice`.`invoiceNumber`=`invoice_final_product`.`invoiceNumber` 
	      LEFT JOIN `licenses` ON `licenses`.`id` = `invoice`.`licenseId`
          LEFT JOIN `users` ON `users`.`id` = `licenses`.`userId`
          LEFT JOIN `companys` ON `companys`.`licenseId` = `licenses`.`id`
          WHERE `invoice`.`invoiceId` = '$invoiceId'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["invoiceProductId"]=$row['invoiceProductId'];
        $getdata["productName"]=$row['productName'];
        $getdata["productCode"]=$row['productCode'];
        $getdata["productPrice"]=$row['productPrice'];
        $getdata["productUnit"]=$row['productUnit'];
        $getdata["productCGST"]=$row['productCGST'];
        $getdata["productSGST"]=$row['productSGST'];
        $getdata["productQuantity"]=$row['productQuantity'];
        $getdata["productStatus"]=$row['productStatus'];
    
        
        $getdata["invoiceId"]=$row['invoiceId'];
        $getdata["userId"]=$row['licenseId'];
        $getdata["noOfTable"]=$row['noOfTable'];
        $getdata["invoiceType"]=$row['invoiceType'];
        $getdata["invoiceNumber"]=$row['invoiceNumber'];
        $getdata["customerName"]=$row['customerName'];
        $getdata["customerMobile"]=$row['customerMobile'];
        $getdata["customerEmail"]=$row['customerEmail'];
        $getdata["customerAddress"]=$row['customerAddress'];
        $getdata["subTotal"]=$row['subTotal'];
        $getdata["totalGSTAmount"]=$row['totalGSTAmount'];
        $getdata["discount"]=$row['discount'];
        $getdata["discountType"]=$row['discountType'];
        $getdata["totalAmount"]=$row['totalAmount'];
        $getdata["paymentMode"]=$row['paymentMode'];
        $getdata["invoiceDate"]=$row['invoiceDate'];
        $getdata["invoiceOrderStatus"]=$row['invoiceOrderStatus'];
        
        $getdata["companyId"]=$row['companyId'];
        $getdata["companyName"]=$row['companyName'];
        $getdata["cashierName"]=$row['cashierName'];
        $getdata["companyMobile"]=$row['companyMobile'];
        $getdata["companyAddress"]=$row['companyAddress'];
        $store = company_structured_fields($row);
        $getdata["shopName1"]=$store['shopName1'];
        $getdata["shopName2"]=$store['shopName2'];
        $getdata["addressLine1"]=$store['addressLine1'];
        $getdata["addressLine2"]=$store['addressLine2'];
        $getdata["addressLine3"]=$store['addressLine3'];
        $getdata["phoneNo1"]=$store['phoneNo1'];
        $getdata["phoneNo2"]=$store['phoneNo2'];
        $getdata["companyName"]=$store['companyName'];
        $getdata["companyAddress"]=$store['companyAddress'];
        $getdata["companyMobile"]=$store['companyMobile'];
        $getdata["countryName"]=$row['countryName'];
        $getdata["tableStatus"]=$row['tableStatus'];
        
        if($row['currencyName']== 'Dinar: Ø¯.Ùƒ') {
            $currencyName = "د.ك";
        } else if($row['currencyName']== 'Rupee: â‚¹') {
            $currencyName = "₹";
        } else if($row['currencyName']== 'Cent: Â¢') {
            $currencyName = "¢";
        } else if($row['currencyName']== 'Pound: Â£') {
            $currencyName = "£";
        } else if($row['currencyName']== 'Yen: Â¥') {
            $currencyName = "¥";
        } else if($row['currencyName']== 'French Franc: â‚£') {
            $currencyName = "₣";
        } else if($row['currencyName']== 'Euro: â‚¬') {
            $currencyName = "€";
        } else {
            $currencyName = "₹";
        }
        
        $getdata["currencyName"]=$currencyName;
        $getdata["stateName"]=$row['stateName'];
        $getdata["gstStatus"]=$row['gstStatus'];
        $getdata["gstNumber"]=$row['gstNumber'];
        $getdata["shopCGST"]=$row['shopCGST'];
        $getdata["shopSGST"]=$row['shopSGST'];
        $getdata["panNumber"]=$row['panNumber'];
        $getdata["companyFssis"]=$row['companyFssis'];
      
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["invoiceProductResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>