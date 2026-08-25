<?php
include_once('config.php');
require_once __DIR__ . '/../auth_tokens.php';
require_once __DIR__ . '/../db_prepared.php';

mysqli_query($con, 'set names utf8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Authorization, Content-Type');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => 'false',
    'message' => 'Login Failed.'
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

// Prefer form fields (Retrofit @FormUrlEncoded). Fall back to JSON body if needed.
$userEmail = isset($_POST['userEmail']) ? trim((string) $_POST['userEmail']) : '';
if ($userEmail === '' && isset($_POST['userName'])) {
    $userEmail = trim((string) $_POST['userName']);
}
$password = isset($_POST['password']) ? (string) $_POST['password'] : '';

if ($userEmail === '' || $password === '') {
    $raw = file_get_contents('php://input');
    if ($raw !== false && $raw !== '') {
        $json = json_decode($raw, true);
        if (is_array($json)) {
            if ($userEmail === '' && isset($json['userEmail'])) {
                $userEmail = trim((string) $json['userEmail']);
            }
            if ($userEmail === '' && isset($json['userName'])) {
                $userEmail = trim((string) $json['userName']);
            }
            if ($password === '' && isset($json['password'])) {
                $password = (string) $json['password'];
            }
        }
    }
}

if ($userEmail === '' || $password === '') {
    $response['message'] = 'Enter valid email & password';
    echo json_encode($response);
    exit;
}

date_default_timezone_set('Asia/Kolkata');

// Admin only: role_id = 1, active account, email match (case-insensitive)
$check = db_stmt_fetch_one(
    $con,
    "SELECT `id`, `password`, `email`, `is_active`, `role_id`
     FROM `users`
     WHERE LOWER(`email`)=LOWER(?) AND `role_id`='1' AND `is_active`='1'
     LIMIT 1",
    's',
    $userEmail
);

if ($check !== null && isset($check['password']) && password_verify($password, $check['password'])) {
    $response['status'] = 'true';
    $response['userId'] = (string) $check['id'];
    $response['message'] = 'Login Successfully.';
    auth_token_append_response($con, $response, 'admin', $check['id']);
} else {
    $response['status'] = 'false';
    $response['message'] = 'Invalid email or password.';
}

mysqli_close($con);
echo json_encode($response);
?>
