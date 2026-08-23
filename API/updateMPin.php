<?php
include_once "config.php";

$response = [];
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    
    $mpin = $_POST["mpin"];
    $app_licence_key = $_POST["app_licence_key"];

    date_default_timezone_set("Asia/Kolkata");
    $date = date("Y-m-d");

    $sql = "UPDATE `licenses` SET `mpin`='$mpin' WHERE `licenseKey`='$app_licence_key'";

    if (mysqli_query($con, $sql)) {
        $response["status"] = "1";
        $response["message"] = " successful!";
    } else {
        $response["status"] = "0";
        $response["message"] = " failed!";
    }
}
echo json_encode($response);
?>
