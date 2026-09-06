<?php
/**
 * Public: create (or return existing) meal token for Registration No.
 */
include_once('config.php');
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Content-Type');
header('Access-Control-Allow-Methods: POST, OPTIONS');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

mess_common_ensure_schema($con);
date_default_timezone_set('Asia/Kolkata');

$out = array('success' => false, 'message' => 'Unable to generate your token right now. Please try again.');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($out);
    mysqli_close($con);
    exit;
}

$ip = mess_client_ip();
$t = isset($_POST['t']) ? trim((string) $_POST['t']) : '';
$registrationNo = isset($_POST['registrationNo']) ? trim((string) $_POST['registrationNo']) : '';

if (!mess_rate_limit_allow($con, 'token:' . $ip, 20, 60)) {
    $out['message'] = 'Please try again in a moment.';
    echo json_encode($out);
    mysqli_close($con);
    exit;
}
if ($t !== '' && !mess_rate_limit_allow($con, 'tokenqr:' . $t, 120, 60)) {
    $out['message'] = 'Please try again in a moment.';
    echo json_encode($out);
    mysqli_close($con);
    exit;
}

$qr = ($t !== '') ? mess_get_qr_by_public_token($con, $t) : null;
if ($qr === null) {
    $out['message'] = 'This QR code is not valid.';
    mess_audit($con, null, 'token_qr_invalid', $t, mess_normalize_registration($registrationNo), null, null);
    echo json_encode($out);
    mysqli_close($con);
    exit;
}
if (strtoupper($qr['status']) !== 'ACTIVE') {
    $out['message'] = 'This QR code is currently inactive.';
    mess_audit($con, (int) $qr['userId'], 'token_qr_inactive', $t, mess_normalize_registration($registrationNo), null, null);
    echo json_encode($out);
    mysqli_close($con);
    exit;
}

$userId = (int) $qr['userId'];
$branchId = isset($qr['branch_id']) ? $qr['branch_id'] : null;
$reg = mess_normalize_registration($registrationNo);
$mobileIn = mess_normalize_mobile($registrationNo);
if ($reg === '' && $mobileIn === '') {
    $out['message'] = 'Please enter your mobile number.';
    echo json_encode($out);
    mysqli_close($con);
    exit;
}

$member = mess_find_member_by_registration($con, $userId, $registrationNo, $branchId);
if ($member === null) {
    $out['message'] = 'Mobile number not found. Please check with the mess counter.';
    mess_audit($con, $userId, 'reg_not_found', $t, $reg, null, null);
    echo json_encode($out);
    mysqli_close($con);
    exit;
}

$memberStatus = strtolower(trim((string) $member['member_status']));
if ($memberStatus !== '' && $memberStatus !== 'active' && $memberStatus !== '1') {
    $out['message'] = 'Your membership is currently inactive.';
    mess_audit($con, $userId, 'member_inactive', $t, $reg, null, null);
    echo json_encode($out);
    mysqli_close($con);
    exit;
}

$memberIdForPayment = isset($member['id']) ? (string) $member['id'] : '0';
$memberNameForPayment = isset($member['member_name']) ? (string) $member['member_name'] : '';
$paymentBlockMsg = mess_member_require_current_month_payment(
    $con,
    $userId,
    $memberIdForPayment,
    $memberNameForPayment
);
if ($paymentBlockMsg !== null) {
    $out['message'] = $paymentBlockMsg;
    $out['code'] = 'UNPAID_CURRENT_MONTH';
    mess_audit($con, $userId, 'token_unpaid_month', $t, $reg, null, date('Y-m'));
    echo json_encode($out);
    mysqli_close($con);
    exit;
}

$session = mess_resolve_current_session($con, $userId);
if ($session === null) {
    $out['message'] = 'Token generation is currently closed.';
    mess_audit($con, $userId, 'session_closed', $t, $reg, null, null);
    echo json_encode($out);
    mysqli_close($con);
    exit;
}

$tokenDate = date('Y-m-d');
$memberId = (int) $member['id'];
$sessionId = (int) $session['id'];

$existing = db_stmt_fetch_one(
    $con,
    'SELECT * FROM mess_meal_token WHERE userId = ? AND member_id = ? AND token_date = ? AND session_id = ? LIMIT 1',
    'iisi',
    $userId,
    $memberId,
    $tokenDate,
    $sessionId
);

if ($existing !== null) {
    mess_audit($con, $userId, 'token_duplicate', $t, $reg, $existing['public_id'], null);
    echo json_encode(array(
        'success' => true,
        'alreadyGenerated' => true,
        'message' => 'Your token is already generated.',
        'tokenId' => $existing['public_id'],
        'tokenNumber' => $existing['token_number'],
        'mealSession' => $existing['session_name'],
        'date' => $existing['token_date'],
        'registrationNo' => $existing['registration_no'],
        'status' => $existing['print_status'],
    ));
    mysqli_close($con);
    exit;
}

mysqli_begin_transaction($con);
try {
    // Re-check inside transaction
    $existing = db_stmt_fetch_one(
        $con,
        'SELECT * FROM mess_meal_token WHERE userId = ? AND member_id = ? AND token_date = ? AND session_id = ? LIMIT 1 FOR UPDATE',
        'iisi',
        $userId,
        $memberId,
        $tokenDate,
        $sessionId
    );
    if ($existing !== null) {
        mysqli_commit($con);
        echo json_encode(array(
            'success' => true,
            'alreadyGenerated' => true,
            'message' => 'Your token is already generated.',
            'tokenId' => $existing['public_id'],
            'tokenNumber' => $existing['token_number'],
            'mealSession' => $existing['session_name'],
            'date' => $existing['token_date'],
            'registrationNo' => $existing['registration_no'],
            'status' => $existing['print_status'],
        ));
        mysqli_close($con);
        exit;
    }

    $seqInfo = mess_next_token_number($con, $userId, $tokenDate, $session['token_prefix']);
    $publicId = mess_random_token(16);
    $memberName = isset($member['member_name']) ? (string) $member['member_name'] : '';
    $printDeviceId = isset($qr['print_device_id']) ? (string) $qr['print_device_id'] : '';
    $storedReg = mess_normalize_registration(isset($member['registration_no']) ? $member['registration_no'] : '');
    if ($storedReg === '') {
        $storedReg = mess_normalize_mobile(isset($member['member_mobile_number']) ? $member['member_mobile_number'] : '');
    }
    if ($storedReg === '') {
        $storedReg = $mobileIn !== '' ? $mobileIn : $reg;
    }
    // Keep member registration_no in sync with mobile when blank.
    if (mess_normalize_registration(isset($member['registration_no']) ? $member['registration_no'] : '') === ''
        && $storedReg !== '') {
        db_stmt_execute(
            $con,
            'UPDATE mess_member SET registration_no = ? WHERE id = ?',
            'si',
            $storedReg,
            $memberId
        );
    }
    $ok = db_stmt_execute(
        $con,
        'INSERT INTO mess_meal_token
         (public_id, userId, organization_id, branch_id, qr_id, session_id, session_name, member_id, registration_no, member_name, token_number, token_date, token_seq, print_status, print_device_id)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, \'PRINT_PENDING\', ?)',
        'siiiiisissssis',
        $publicId,
        $userId,
        isset($qr['organization_id']) ? (int) $qr['organization_id'] : 0,
        isset($qr['branch_id']) ? (int) $qr['branch_id'] : 0,
        (int) $qr['id'],
        $sessionId,
        $session['session_name'],
        $memberId,
        $storedReg,
        $memberName,
        $seqInfo['token_number'],
        $tokenDate,
        (int) $seqInfo['seq'],
        $printDeviceId
    );

    if (!$ok) {
        // Unique race: fetch existing
        mysqli_rollback($con);
        $existing = db_stmt_fetch_one(
            $con,
            'SELECT * FROM mess_meal_token WHERE userId = ? AND member_id = ? AND token_date = ? AND session_id = ? LIMIT 1',
            'iisi',
            $userId,
            $memberId,
            $tokenDate,
            $sessionId
        );
        if ($existing) {
            echo json_encode(array(
                'success' => true,
                'alreadyGenerated' => true,
                'message' => 'Your token is already generated.',
                'tokenId' => $existing['public_id'],
                'tokenNumber' => $existing['token_number'],
                'mealSession' => $existing['session_name'],
                'date' => $existing['token_date'],
                'registrationNo' => $existing['registration_no'],
                'status' => $existing['print_status'],
            ));
            mysqli_close($con);
            exit;
        }
        echo json_encode($out);
        mysqli_close($con);
        exit;
    }

    mysqli_commit($con);
} catch (Exception $e) {
    mysqli_rollback($con);
    echo json_encode($out);
    mysqli_close($con);
    exit;
}

$tokenRow = db_stmt_fetch_one(
    $con,
    'SELECT * FROM mess_meal_token WHERE public_id = ? LIMIT 1',
    's',
    $publicId
);

if ($tokenRow) {
    mess_notify_pos_token_created($con, $tokenRow);
    mess_audit($con, $userId, 'token_created', $t, $storedReg, $publicId, $seqInfo['token_number']);
}

echo json_encode(array(
    'success' => true,
    'alreadyGenerated' => false,
    'message' => 'Token generated',
    'tokenId' => $publicId,
    'tokenNumber' => $seqInfo['token_number'],
    'mealSession' => $session['session_name'],
    'date' => $tokenDate,
    'registrationNo' => $storedReg,
    'status' => 'CREATED',
));
mysqli_close($con);
