<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    mysqli_query($con, 'set names utf8');

    $userId = owner_resolve_user_id($con, isset($_POST['userId']) ? $_POST['userId'] : '');
    if ($userId === null) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(array('status' => '0', 'message' => 'Unauthorized'));
        mysqli_close($con);
        exit;
    }

    $subcategoryId = isset($_POST['subcategoryId']) ? mysqli_real_escape_string($con, $_POST['subcategoryId']) : '';
    $userIdEsc = mysqli_real_escape_string($con, $userId);

    if ($subcategoryId === '') {
        $response['status'] = '0';
        $response['message'] = 'subcategoryId is required';
    } else {
        $sql = "UPDATE `product_subcategories` SET `subcategoryStatus`='deactive'
                WHERE `subcategoryId`='$subcategoryId' AND `userId`='$userIdEsc'";
        if (mysqli_query($con, $sql)) {
            $response['status'] = '1';
            $response['message'] = 'subcategory delete successfully';
        } else {
            $response['status'] = '0';
            $response['message'] = 'subcategory failed to delete';
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
