<?php	
include_once('config.php');
require_once __DIR__ . '/../auth_tokens.php';
$i=0;
   
    $response["customerResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = isset($_GET['userId']) ? $_GET['userId'] : '';
        $userId = auth_user_id_from_request($con, $userId, 'owner');
        if ($userId === null) {
            header('Content-Type: application/json');
            echo json_encode(array('customerResponse' => array()));
            mysqli_close($con);
            exit;
        }
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `users` WHERE `id`='$userId'";
    $checkuser= mysqli_query($con, $sth);
    $rowcount=mysqli_num_rows($checkuser);

     if($rowcount>0){
      
     while($user=mysqli_fetch_array($checkuser))
	{
       
        $id=$user['id'];
        $name=$user['name'];
        $email=$user['email'];
        $contact_number=$user['contact_number'];
        $aadhar_number=$user['aadhar_number'];
        $address=$user['address'];
        $shopName=$user['shopName'];
        $reportPin=$user['reportPin'];
        
        
        $sql_licenses="SELECT * FROM `licenses` LEFT JOIN `companys` ON `companys`.`licenseId` = `licenses`.`id` WHERE `userId`='$id'";
    $check_licenses= mysqli_query($con, $sql_licenses);
    $rowcount_licenses=mysqli_num_rows($check_licenses);
    

     if($rowcount_licenses>0){
      
     while($user_licenses=mysqli_fetch_array($check_licenses))
	{
       
        $licenses_id=$user_licenses['id'];
        $companyAddress=$user_licenses['companyAddress']!=null?$user_licenses['companyAddress']:"-";
        $licenseKey=$user_licenses['licenseKey'];
        $licenseValidity=$user_licenses['licenseValidity'];
        $licenseType=$user_licenses['licenseType'];
        $licenseStatus=$user_licenses['licenseStatus'];
        $registrationDate=$user_licenses['created_at'];
        $expiryDate=$user_licenses['expiryDate'];
        $paymentStatus=$user_licenses['paymentStatus'];
        $amount=$user_licenses['amount'];
        $fastBilling=$user_licenses['fastBilling'];
        $takeAway=$user_licenses['takeAway'];
        $dineIn=$user_licenses['dineIn'];
        $totalSaleData=$user_licenses['total_sale_data'];
        $todaySaleData=$user_licenses['today_sale_data'];
        
        
        $json[] = array("licenses_id"=>$licenses_id, "companyAddress"=>$companyAddress, "licenseKey"=>$licenseKey, "licenseValidity"=>$licenseValidity, "licenseType"=>$licenseType, "licenseStatus"=>$licenseStatus,
                         "registrationDate"=>$registrationDate, "expiryDate"=>$expiryDate, "paymentStatus"=>$paymentStatus, "amount"=>$amount, "fastBilling"=>$fastBilling, "takeAway"=>$takeAway, "dineIn"=>$dineIn,
                         "totalSaleData"=>$totalSaleData, "todaySaleData"=>$todaySaleData);
        
        
        }
      
        }
        
        $json1[] = array("id"=>$id, "name"=>$name, "email"=>$email, "contact_number"=>$contact_number, "aadhar_number"=>$aadhar_number, "address"=>$address, "shopName"=>$shopName, "reportPin"=>$reportPin, "licensesResponse"=>$json);
        
          $json2 = array("customerResponse"=>$json1);
            
        }
        
        print_r (json_encode($json2));
	    unset($json);
	    unset($json1);
	    unset($json2);
	    mysqli_close($con);
        
     }
        else { 
           $minfo = array("customerResponse"=>[]);
           $jsondata = json_encode($minfo); 
           print_r($jsondata);   
	       mysqli_close($con); exit();
	  }
    }
    
?>