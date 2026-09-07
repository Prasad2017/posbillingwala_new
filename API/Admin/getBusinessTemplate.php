<?php
/**
 * Admin: read business template for any licence.
 * POS devices pick this up via androidApp/getBusinessTemplate.php on Fetch Data.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../business_template_ops.php';

admin_require_auth($con);

$response = array('status' => '1', 'message' => 'ok', 'businessTemplateResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $licenceId = isset($_GET['licenceId']) ? trim($_GET['licenceId']) : '';
    if ($licenceId === '') {
        $response['status'] = '0';
        $response['message'] = 'Missing licenceId';
    } else {
        $lic = business_template_licence_row($con, $licenceId);
        if ($lic === null) {
            $response['status'] = '0';
            $response['message'] = 'Licence not found';
        } else {
            $response['businessTemplateResponse'][] = business_template_fetch($con, $licenceId);
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
