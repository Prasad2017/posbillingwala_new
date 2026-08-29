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
        'status' => '0',
        'message' => 'Login failed',
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
    $password = isset($_POST['password']) ? (string) $_POST['password'] : '';

    if ($contactNumber === '') {
        $raw = file_get_contents('php://input');
        if ($raw !== false && $raw !== '') {
            $json = json_decode($raw, true);
            if (is_array($json)) {
                if (isset($json['contactNumber'])) {
                    $contactNumber = trim((string) $json['contactNumber']);
                }
                if ($password === '' && isset($json['password'])) {
                    $password = (string) $json['password'];
                }
            }
        }
    }

    date_default_timezone_set('Asia/Kolkata');

    if ($contactNumber === '') {
        $response['message'] = 'Enter valid mobile number';
        echo json_encode($response);
        exit;
    }

    // Owners typically role_id != 1 (admin). Accept active users by contact.
    $check = db_stmt_fetch_one(
        $con,
        "SELECT `id`, `password`, `contact_number`, `reportPin`, `is_active`, `role_id`
         FROM `users`
         WHERE `contact_number`=? AND `is_active`='1'
         LIMIT 1",
        's',
        $contactNumber
    );

    if ($check === null) {
        $response['message'] = 'licence key expired or user disable. Please contact our customer care or dealer';
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }

    $storedPassword = isset($check['password']) ? (string) $check['password'] : '';
    $hasPassword = $storedPassword !== '' && strpos($storedPassword, '$2') === 0;

    // If a password is set on the account, require it. Otherwise allow mobile-only
    // (legacy owners) but still issue a long-lived token so the app does not re-prompt.
    if ($hasPassword) {
        if ($password === '' || !password_verify($password, $storedPassword)) {
            $response['message'] = 'Invalid mobile number or password';
            echo json_encode($response);
            mysqli_close($con);
            exit;
        }
    }

    $response['status'] = '1';
    $response['message'] = 'Login successfully.';
    $response['userId'] = (string) $check['id'];
    $response['reportPin'] = $check['reportPin'];
    $response['contact_number'] = $check['contact_number'];
    $response['passwordRequired'] = $hasPassword ? '1' : '0';
    auth_token_append_response($con, $response, 'owner', $check['id']);
    if (empty($response['authToken'])) {
        $issued = auth_token_issue($con, 'owner', $check['id'], null, AUTH_TOKEN_TTL_DAYS, null);
        if ($issued !== null) {
            $response['authToken'] = $issued['authToken'];
            $response['tokenExpiresAt'] = $issued['tokenExpiresAt'];
        }
    }

    mysqli_close($con);
    echo json_encode($response);
?>
