<?php
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';
require_once __DIR__ . '/business_template_ops.php';

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    mysqli_query($con, 'set names utf8mb4');

    $postedUserId = isset($_POST['userId']) ? $_POST['userId'] : '';
    $ctx = branch_pos_prepare_write($con, $postedUserId, $response);
    if ($ctx === null) {
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }
    $userId = $ctx['licenseId'];
    $orgId = $ctx['triplet']['organization_id'];
    $branchId = $ctx['triplet']['branch_id'];
    $deviceId = $ctx['triplet']['device_id'];

    $businessType = isset($_POST['businessType']) ? trim($_POST['businessType']) : 'restaurant';
    $businessTemplateId = isset($_POST['businessTemplateId'])
        ? trim($_POST['businessTemplateId']) : 'restaurant_default';
    $businessTemplateJson = isset($_POST['businessTemplateJson'])
        ? trim($_POST['businessTemplateJson']) : '';
    $templateNetworkStatus = isset($_POST['templateNetworkStatus'])
        ? trim($_POST['templateNetworkStatus']) : '';

    if ($businessType === '') {
        $businessType = 'restaurant';
    }
    if ($businessTemplateId === '') {
        $businessTemplateId = 'restaurant_default';
    }

    $response = business_template_upsert(
        $con,
        $userId,
        $orgId,
        $branchId,
        $businessType,
        $businessTemplateId,
        $businessTemplateJson,
        $templateNetworkStatus,
        $deviceId
    );
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
