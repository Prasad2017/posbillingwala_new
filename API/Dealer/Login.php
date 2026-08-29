<?php
include_once 'config.php';
require_once __DIR__ . '/../auth_tokens.php';
require_once __DIR__ . '/../db_prepared.php';

mysqli_query($con, 'set names utf8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Authorization, Content-Type');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => 'false',
    'message' => 'Login failed.',
);

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    echo json_encode($response);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    $response['message'] = 'Use Post Method';
    echo json_encode($response);
    exit;
}

$contactNumber = isset($_POST['contactNumber']) ? trim((string) $_POST['contactNumber']) : '';
if ($contactNumber === '' && isset($_POST['userName'])) {
    $contactNumber = trim((string) $_POST['userName']);
}
$password = isset($_POST['password']) ? (string) $_POST['password'] : '';

if ($contactNumber === '' || $password === '') {
    $raw = file_get_contents('php://input');
    if ($raw !== false && $raw !== '') {
        $json = json_decode($raw, true);
        if (is_array($json)) {
            if ($contactNumber === '' && isset($json['contactNumber'])) {
                $contactNumber = trim((string) $json['contactNumber']);
            }
            if ($contactNumber === '' && isset($json['userName'])) {
                $contactNumber = trim((string) $json['userName']);
            }
            if ($password === '' && isset($json['password'])) {
                $password = (string) $json['password'];
            }
        }
    }
}

$contactNumber = preg_replace('/\D+/', '', $contactNumber);

if ($contactNumber === '' || strlen($contactNumber) < 10) {
    $response['message'] = 'Enter valid mobile number';
    echo json_encode($response);
    exit;
}

if ($password === '') {
    $response['message'] = 'Enter password';
    echo json_encode($response);
    exit;
}

date_default_timezone_set('Asia/Kolkata');

$dealer = db_stmt_fetch_one(
    $con,
    "SELECT `id`, `password`, `contact_number`, `name`, `is_active`, `role_id`
     FROM `users`
     WHERE `contact_number`=? AND `role_id`='2'
     LIMIT 1",
    's',
    $contactNumber
);

if ($dealer === null) {
    $response['message'] = 'Invalid mobile number or password.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if ((int) $dealer['is_active'] !== 1) {
    $response['message'] = 'Your dealer account is disabled. Please contact support.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$storedPassword = isset($dealer['password']) ? (string) $dealer['password'] : '';
if ($storedPassword === '' || strpos($storedPassword, '$2') !== 0) {
    $response['message'] = 'Password not set for this account. Please contact admin.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if (!password_verify($password, $storedPassword)) {
    $response['message'] = 'Invalid mobile number or password.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$response['status'] = 'true';
$response['message'] = 'Login Successfully.';
$response['userId'] = (string) $dealer['id'];
$response['contact_number'] = $dealer['contact_number'];
$response['name'] = $dealer['name'];
auth_token_append_response($con, $response, 'dealer', $dealer['id']);

mysqli_close($con);
echo json_encode($response);
