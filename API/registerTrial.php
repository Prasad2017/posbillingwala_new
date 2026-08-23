<?php
/**
 * Self-service POS signup: creates customer + 7-day Demo licence.
 */
include_once __DIR__ . '/config.php';
include_once __DIR__ . '/licence_expiry.php';

header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Invalid request. Please try again from the app.',
);

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

if (!isset($con) || !$con) {
    $response['message'] = 'Database not configured.';
    echo json_encode($response);
    exit;
}

mysqli_query($con, 'set names utf8');

$name = isset($_POST['name']) ? trim((string) $_POST['name']) : '';
$contactNumber = isset($_POST['contact_number']) ? trim((string) $_POST['contact_number']) : '';
$address = isset($_POST['address']) ? trim((string) $_POST['address']) : '';
$shopName = isset($_POST['shopName']) ? trim((string) $_POST['shopName']) : '';

$response = licence_register_trial_customer($con, $name, $contactNumber, $address, $shopName);

echo json_encode($response);
