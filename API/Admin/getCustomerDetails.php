<?php	
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/../company_store_fields.php';
require_once __DIR__ . '/auth_guard.php';
$i=0;
   
    $response["customerResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {

        admin_require_auth($con, array('customerResponse' => array()));
        
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
        $store = company_structured_fields($user_licenses);
        $companyAddress = company_display_address_oneline($user_licenses);
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
        $branch = licence_branch_fields($user_licenses);
        
        
        $json[] = array("licenses_id"=>$licenses_id, "companyAddress"=>$companyAddress,
                         "shopName1"=>$store['shopName1'], "shopName2"=>$store['shopName2'],
                         "addressLine1"=>$store['addressLine1'], "addressLine2"=>$store['addressLine2'], "addressLine3"=>$store['addressLine3'],
                         "phoneNo1"=>$store['phoneNo1'], "phoneNo2"=>$store['phoneNo2'],
                         "licenseKey"=>$licenseKey, "licenseValidity"=>$licenseValidity, "licenseType"=>$licenseType, "licenseStatus"=>$licenseStatus,
                         "registrationDate"=>$registrationDate, "expiryDate"=>$expiryDate, "paymentStatus"=>$paymentStatus, "amount"=>$amount, "fastBilling"=>$fastBilling, "takeAway"=>$takeAway, "dineIn"=>$dineIn,
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