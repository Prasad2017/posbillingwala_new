<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';


$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
  $categoryName = $_POST['categoryName'];
  $categoryDeletedStatus = $_POST['categoryDeletedStatus'];
  $categoryNetworkStatus = $_POST['categoryNetworkStatus'];
  $userId = $_POST['userId'];
  $__postedUserId = isset($_POST['userId']) ? $_POST['userId'] : (isset($userId) ? $userId : '');
  pos_require_auth($con, $__postedUserId, isset($response) ? $response : array('status'=>'0','message'=>'Unauthorized'));

  $foodTypeId = isset($_POST['foodTypeId']) ? $_POST['foodTypeId'] : '';
  $foodTypeCode = isset($_POST['foodTypeCode']) ? $_POST['foodTypeCode'] : '';
  if ($foodTypeId == '' && $foodTypeCode != '') {
      $ftRes = mysqli_query($con, "SELECT foodTypeId FROM `food_types` WHERE `foodTypeCode`='$foodTypeCode' LIMIT 1");
      $ftRow = mysqli_fetch_array($ftRes);
      if (isset($ftRow)) {
          $foodTypeId = $ftRow['foodTypeId'];
      }
  }
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    if($categoryDeletedStatus == "1") {
        $categoryDeletedStatus = 'deactive';
    } else {
        $categoryDeletedStatus = 'active';
    }
    
    $sql="SELECT * FROM `categories` WHERE `userId`='$userId' AND `categoryNetworkStatus`='$categoryNetworkStatus'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
				if(isset($check))
				{
				    
				    $categoryId = $check['categoryId'];
				    $foodTypeSql = ($foodTypeId != '') ? ", `foodTypeId`='$foodTypeId'" : "";
				    $sql="UPDATE `categories` SET `categoryName`='$categoryName', `categoryStatus`='$categoryDeletedStatus'$foodTypeSql WHERE `categoryId`='$categoryId'";

                 if(mysqli_query($con, $sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                     }
				    
				    
				} else {

                 
                 $foodTypeCol = ($foodTypeId != '') ? ", `foodTypeId`" : "";
                 $foodTypeVal = ($foodTypeId != '') ? ", '$foodTypeId'" : "";
                 $sql="INSERT INTO `categories`(`userId`, `categoryName`, `categoryNetworkStatus`, `categoryStatus`$foodTypeCol) VALUES ('$userId', '$categoryName', '$categoryNetworkStatus', '$categoryDeletedStatus'$foodTypeVal)";

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
