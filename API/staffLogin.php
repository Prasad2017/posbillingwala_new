<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/auth_tokens.php';
require_once __DIR__ . '/licence_expiry.php';
require_once __DIR__ . '/licence_payload.php';
require_once __DIR__ . '/pos_devices.php';

pos_api_headers();
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    pos_api_json(array('status' => '0', 'message' => 'Use Post Method'));
}

$licenceId = pos_api_post('userId');
$licenceKey = pos_api_post('app_licence_key');
$mobile = pos_normalize_mobile(pos_api_post('mobileNumber'));
$pin = pos_api_post('appLoginPin');
$deviceId = pos_api_post('android_device_id');
if ($deviceId === '') {
    $deviceId = pos_api_post('androidId');
}
$deviceName = pos_api_post('android_device_name');
if ($deviceName === '') {
    $deviceName = pos_api_post('deviceName');
}
$platform = pos_api_post('platform');
$appVersion = pos_api_post('appVersion');
$osVersion = pos_api_post('osVersion');
$coldLogin = false;

if ($licenceId === '' && $licenceKey !== '') {
    $lic = db_stmt_fetch_one($con, 'SELECT `id` FROM `licenses` WHERE `licenseKey`=? LIMIT 1', 's', $licenceKey);
    if ($lic !== null) {
        $licenceId = (string) $lic['id'];
    }
}

if (!pos_mobile_valid($mobile) || !pos_pin_valid_format($pin)) {
    pos_api_json(array('status' => '0', 'message' => 'Enter mobile number and PIN'));
}

pos_schema_ensure($con);

if ($licenceId === '') {
    /* Cold staff login: resolve shop from mobile + PIN (no licence on device yet). */
    $coldLogin = true;
    $candidates = db_stmt_fetch_all(
        $con,
        "SELECT * FROM `pos_staff` WHERE `mobileNumber`=? AND UPPER(`status`)='ACTIVE'",
        's',
        $mobile
    );
    $matches = array();
    foreach ($candidates as $row) {
        if (pos_pin_verify($pin, $row['pinHash'])) {
            $matches[] = $row;
        }
    }
    if (count($matches) === 0) {
        pos_api_json(array('status' => '0', 'message' => 'Invalid mobile number or PIN'));
    }
    if (count($matches) > 1) {
        pos_api_json(array(
            'status' => '0',
            'message' => 'Multiple shops found for this mobile. Use licence login first.',
        ));
    }
    $staff = $matches[0];
    $licenceId = (string) $staff['licenseId'];
    pos_seed_owner_from_licence($con, $licenceId);
} else {
    pos_require_auth($con, $licenceId, array('status' => '0', 'message' => 'Unauthorized — login required'));
    pos_seed_owner_from_licence($con, $licenceId);

    $staff = db_stmt_fetch_one(
        $con,
        'SELECT * FROM `pos_staff` WHERE `licenseId`=? AND `mobileNumber`=? LIMIT 1',
        'is',
        (int) $licenceId,
        $mobile
    );
    if ($staff === null || !pos_pin_verify($pin, $staff['pinHash'])) {
        pos_api_json(array('status' => '0', 'message' => 'Invalid mobile number or PIN'));
    }
    $status = strtoupper((string) $staff['status']);
    if ($status !== 'ACTIVE') {
        pos_api_json(array('status' => '0', 'message' => 'User is ' . strtolower($status)));
    }
}

if (!pos_um_enabled($con, $licenceId)) {
    pos_api_json(array('status' => '0', 'message' => 'User Management is off for this licence'));
}

$check = db_stmt_fetch_one(
    $con,
    "SELECT `licenses`.*, `users`.`shopName`, `users`.`shopImage`, `users`.`reportPin`, `users`.`is_active` AS userActive
     FROM `licenses`
     LEFT JOIN `users` ON `users`.`id` = `licenses`.`userId`
     WHERE `licenses`.`id`=? AND `licenses`.`licenseStatus`='active'",
    's',
    $licenceId
);
if ($check === null || !licence_is_user_active(isset($check['userActive']) ? $check['userActive'] : null)) {
    pos_api_json(array('status' => '0', 'message' => 'Licence expired or user disabled'));
}

$check = licence_sync_trial_consumed_state($con, $check);
if (!licence_trial_allows_login($con, $check)) {
    pos_api_json(array('status' => '0', 'message' => licence_trial_login_block_message($check)));
}
if (!licence_enforce_expiry($con, $check)) {
    pos_api_json(array('status' => '0', 'message' => 'Licence expired or user disabled'));
}

if ($deviceId === '') {
    pos_api_json(array('status' => '0', 'message' => 'Device id required'));
}

if (!pos_device_authorized($con, $check, $deviceId)) {
    if (!pos_device_can_bind_additional($con, $licenceId, $deviceId)) {
        pos_api_json(array(
            'status' => '0',
            'message' => 'Device limit reached. Ask owner to free a device slot.',
        ));
    }
    $reg = pos_device_register(
        $con,
        $licenceId,
        $deviceId,
        $deviceName !== '' ? $deviceName : 'Staff device',
        $platform !== '' ? $platform : 'ANDROID',
        $appVersion,
        $osVersion
    );
    if (empty($reg['ok'])) {
        pos_api_json(array(
            'status' => '0',
            'message' => isset($reg['message']) ? $reg['message'] : 'Unable to register device',
        ));
    }
}

$overrides = pos_load_staff_overrides($con, $staff['id']);
$effective = pos_effective_permissions($con, $staff['role'], $overrides);
$um = pos_licence_um_row($con, $licenceId);

db_stmt_execute(
    $con,
    'UPDATE `pos_staff` SET `lastLoginAt`=? WHERE `id`=?',
    'si',
    date('Y-m-d H:i:s'),
    (int) $staff['id']
);

$sessionId = sprintf(
    '%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
    mt_rand(0, 0xffff),
    mt_rand(0, 0xffff),
    mt_rand(0, 0xffff),
    mt_rand(0, 0x0fff) | 0x4000,
    mt_rand(0, 0x3fff) | 0x8000,
    mt_rand(0, 0xffff),
    mt_rand(0, 0xffff),
    mt_rand(0, 0xffff)
);
db_stmt_insert_id(
    $con,
    'INSERT INTO `pos_sessions` (`sessionId`, `staffId`, `deviceId`, `licenseId`, `loginAt`, `lastActivityAt`, `status`)
     VALUES (?, ?, ?, ?, ?, ?, \'ACTIVE\')',
    'sisiss',
    $sessionId,
    (int) $staff['id'],
    $deviceId,
    (int) $licenceId,
    date('Y-m-d H:i:s'),
    date('Y-m-d H:i:s')
);
pos_audit($con, $licenceId, 'User Login', 'staff', $staff['id'], $staff['id']);

$response = array(
    'status' => '1',
    'message' => 'Login successfully.',
    'staffSessionId' => $sessionId,
    'permissionVersion' => (string) $um['permissionVersion'],
    'staff' => pos_staff_public($staff, $effective, $overrides),
    'coldLogin' => $coldLogin ? '1' : '0',
);

if ($coldLogin) {
    $response['licenceId'] = $check['id'];
    $response['ownerId'] = $check['userId'];
    $response['userName'] = $check['userName'];
    $response['shopName'] = $check['shopName'];
    $response['shopImage'] = $check['shopImage'];
    $response['reportPin'] = $check['reportPin'];
    $response['fastBilling'] = $check['fastBilling'];
    $response['takeAway'] = $check['takeAway'];
    $response['dineIn'] = $check['dineIn'];
    $response['mess'] = $check['mess'];
    $response['licenceKey'] = $check['licenseKey'];
    $response['mpin'] = $check['mpin'];
    $response['licence_key_reg_date'] = $check['created_at'];
    $response['licence_key_expire_date'] = $check['expiryDate'];
    $response['totalSaleData'] = $check['total_sale_data'];
    $response['todaySaleData'] = $check['today_sale_data'];
    $response = licence_append_trial_response($con, $response, $check);
    $response = licence_append_user_management_response($con, $response, $check['id']);
    $response = licence_append_signed_payload($con, $response, $check, $deviceId);
    auth_token_append_response($con, $response, 'pos_licence', $check['id'], $deviceId, $check['expiryDate']);
    require_once __DIR__ . '/pos_presence.php';
    licence_touch_last_login($con, (int) $check['id']);
}

pos_api_json($response);
