<?php	
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
$i=0;
   
    $response["customerResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $customerId = $_GET['customerId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `users` WHERE `id`='$customerId'";
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
        
        
        $sql_licenses="SELECT * FROM `licenses` LEFT JOIN `companys` ON `companys`.`licenseId` = `licenses`.`id` WHERE `userId`='$id'";
    $check_licenses= mysqli_query($con, $sql_licenses);
    $rowcount_licenses=mysqli_num_rows($check_licenses);
    

     if($rowcount_licenses>0){
      
     while($user_licenses=mysqli_fetch_array($check_licenses))
	{
       
        $licenses_id=$user_licenses['id'];
        $companyAddress=$user_licenses['companyAddress'];
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
        $branch = licence_branch_fields($user_licenses);
        
        
        $json[] = array("licenses_id"=>$licenses_id, "companyAddress"=>$companyAddress, "licenseKey"=>$licenseKey, "licenseValidity"=>$licenseValidity, "licenseType"=>$licenseType, "licenseStatus"=>$licenseStatus,
                         "registrationDate"=>$registrationDate, "expiryDate"=>$expiryDate, "paymentStatus"=>$paymentStatus, "amount"=>$amount, "fastBilling"=>$fastBilling, "takeAway"=>$takeAway, "dineIn"=>$dineIn,
                         "totalSaleData"=>$totalSaleData, "todaySaleData"=>$todaySaleData,
                         "userType"=>$branch['userType'], "userName"=>$branch['userName'], "branchLabel"=>$branch['branchLabel']);
        
        
        }
      
        }
        
        $json1[] = array("id"=>$id, "name"=>$name, "email"=>$email, "contact_number"=>$contact_number, "aadhar_number"=>$aadhar_number, "address"=>$address, "shopName"=>$shopName, "licensesResponse"=>$json);
        
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