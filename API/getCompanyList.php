<?php	
include_once('config.php');
$i=0;
   
    $response["companyResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `companys` WHERE `licenseId`='$userId'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["companyId"]=$row['companyId'];
        $getdata["userId"]=$row['licenseId'];
        $getdata["companyName"]=$row['companyName'];
        $getdata["cashierName"]=$row['cashierName'];
        $getdata["companyMobile"]=$row['companyMobile'];
        $getdata["companyAddress"]=$row['companyAddress'];
        $getdata["countryName"]=$row['countryName'];
        $getdata["tableStatus"]=$row['tableStatus'];
        $getdata["noOfTable"]=$row['noOfTable'];
        
        if($row['currencyName']== 'Dinar: Ø¯.Ùƒ') {
            $currencyName = "Dinar: د.ك";
        } else if($row['currencyName']== 'Rupee: â‚¹') {
            $currencyName = "Rupee: ₹";
        } else if($row['currencyName']== 'Cent: Â¢') {
            $currencyName = "Cent: ¢";
        } else if($row['currencyName']== 'Pound: Â£') {
            $currencyName = "Pound: £";
        } else if($row['currencyName']== 'Yen: Â¥') {
            $currencyName = "Yen: ¥";
        } else if($row['currencyName']== 'French Franc: â‚£') {
            $currencyName = "French Franc: ₣";
        } else if($row['currencyName']== 'Euro: â‚¬') {
            $currencyName = "Euro: €";
        } else {
            $currencyName = $row['currencyName'];
        }
        
        $getdata["currencyName"]=$currencyName;
        $getdata["stateName"]=$row['stateName'];
        $getdata["gstStatus"]=$row['gstStatus'];
        $getdata["gstNumber"]=$row['gstNumber'];
        $getdata["shopCGST"]=$row['shopCGST'];
        $getdata["shopSGST"]=$row['shopSGST'];
        $getdata["panNumber"]=$row['panNumber'];
        $getdata["companyFssis"]=$row['companyFssis'];
        $getdata["companyLogo"]=$row['companyLogo'];
        $getdata["paymentLogo"]=$row['paymentLogo'];
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["companyResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>