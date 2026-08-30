<?php
include_once('config.php');
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../licence_expiry.php';
require_once __DIR__ . '/../user_identity.php';
require_once __DIR__ . '/auth_guard.php';

/**
 * Create dealer (role_id=2). Soft-active by default.
 * POST: name, contact_number, address, email, aadhar_number, password (optional)
 */
$response = array('status' => '0', 'message' => 'create failed');

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    admin_require_auth($con);
    mysqli_query($con, 'set names utf8');

    $name = isset($_POST['name']) ? trim($_POST['name']) : '';
    $contact = isset($_POST['contact_number']) ? trim($_POST['contact_number']) : '';
    $address = isset($_POST['address']) ? trim($_POST['address']) : '';
    $email = isset($_POST['email']) ? trim($_POST['email']) : '';
    $aadhaar = isset($_POST['aadhar_number']) ? preg_replace('/\D/', '', trim($_POST['aadhar_number'])) : '';
    $password = isset($_POST['password']) ? (string) $_POST['password'] : '';

    if ($name === '' || $contact === '') {
        $response['message'] = 'Name and mobile are required.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }
    if ($aadhaar !== '' && strlen($aadhaar) !== 12) {
        $response['message'] = 'Aadhaar must be 12 digits.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    if ($aadhaar !== '') {
        if (user_dealer_aadhar_taken($con, $aadhaar)) {
            $response['message'] = 'Aadhaar already registered for another dealer.';
            header('Content-type: application/json; charset=utf-8');
            echo json_encode($response);
            exit;
        }
    }

    $contactDigits = licence_normalize_contact($contact);
    if (strlen($contactDigits) < 10) {
        $response['message'] = 'Please enter a valid 10-digit mobile number.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    if (user_dealer_mobile_taken($con, $contactDigits)) {
        $response['message'] = 'This mobile number is already registered for another dealer.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $hash = $password !== '' ? password_hash($password, PASSWORD_BCRYPT) : password_hash(bin2hex(random_bytes(8)), PASSWORD_BCRYPT);

    $ok = db_stmt_execute(
        $con,
        'INSERT INTO `users`(`role_id`, `name`, `contact_number`, `address`, `email`, `aadhar_number`, `password`, `is_active`)
         VALUES (\'2\', ?, ?, ?, ?, ?, ?, \'1\')',
        'ssssss',
        $name,
        $contactDigits,
        $address,
        $email,
        $aadhaar,
        $hash
    );

    if ($ok) {
        $response['status'] = '1';
        $response['message'] = 'Dealer created successfully.';
        $response['userId'] = (string) mysqli_insert_id($con);
    } else {
        $response['message'] = 'Unable to create dealer.';
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
