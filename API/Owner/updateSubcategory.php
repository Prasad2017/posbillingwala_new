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
    $subcategoryName = isset($_POST['subcategoryName']) ? mysqli_real_escape_string($con, $_POST['subcategoryName']) : '';
    $userIdEsc = mysqli_real_escape_string($con, $userId);

    if ($subcategoryId === '' || $subcategoryName === '') {
        $response['status'] = '0';
        $response['message'] = 'Subcategory name is required';
    } else {
        $sql = "UPDATE `product_subcategories` SET `subcategoryName`='$subcategoryName', `subcategoryStatus`='active'
                WHERE `subcategoryId`='$subcategoryId' AND `userId`='$userIdEsc'";
        if (mysqli_query($con, $sql) && mysqli_affected_rows($con) >= 0) {
            $response['status'] = '1';
            $response['message'] = 'subcategory update successfully';
        } else {
            $response['status'] = '0';
            $response['message'] = 'subcategory failed to update';
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
