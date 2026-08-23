<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array('customerResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    admin_require_auth($con, array('customerResponse' => array()));

    $userId = isset($_GET['userId']) ? $_GET['userId'] : '';

    if ($userId !== '') {
        $userIdEsc = mysqli_real_escape_string($con, $userId);
        $sth = "SELECT * FROM `users` WHERE `id`='$userIdEsc' LIMIT 1";

        if ($result = mysqli_query($con, $sth)) {
            while ($row = mysqli_fetch_assoc($result)) {
                $getdata = array();
                $getdata['id'] = $row['id'];
                $getdata['name'] = $row['name'];
                $getdata['email'] = $row['email'];
                $getdata['contact_number'] = $row['contact_number'];
                $getdata['aadhar_number'] = $row['aadhar_number'];
                $getdata['address'] = $row['address'];
                array_push($response['customerResponse'], $getdata);
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
