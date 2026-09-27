<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
$actor = pos_require_permission($con, $licenceId, 'billing.create');
$orgId = enterprise_org_id($con, $licenceId);

$clientId = pos_api_post('clientId');
$roomNumber = pos_api_post('roomNumber');
if ($clientId === '' || $roomNumber === '') {
    pos_api_json(array('status' => '0', 'message' => 'clientId and roomNumber are required'));
}

$floor = pos_api_post('floor');
$wing = pos_api_post('wing');
$roomTypeClientId = pos_api_post('roomTypeClientId');
$roomTypeName = pos_api_post('roomTypeName');
$roomStatus = strtoupper(pos_api_post('roomStatus', 'AVAILABLE'));
$notes = pos_api_post('notes');

$existing = db_stmt_fetch_one(
    $con,
    'SELECT `id` FROM `pos_hotel_rooms` WHERE `licenseId`=? AND `clientId`=? LIMIT 1',
    'is',
    (int) $licenceId,
    $clientId
);

if ($existing !== null) {
    db_stmt_execute(
        $con,
        'UPDATE `pos_hotel_rooms` SET `roomNumber`=?, `floor`=?, `wing`=?, `roomTypeClientId`=?,
         `roomTypeName`=?, `roomStatus`=?, `notes`=? WHERE `licenseId`=? AND `clientId`=?',
        'sssssssis',
        $roomNumber, $floor, $wing, $roomTypeClientId, $roomTypeName, $roomStatus, $notes,
        (int) $licenceId, $clientId
    );
} else {
    db_stmt_execute(
        $con,
        'INSERT INTO `pos_hotel_rooms`
         (`organization_id`,`licenseId`,`clientId`,`roomNumber`,`floor`,`wing`,`roomTypeClientId`,
          `roomTypeName`,`roomStatus`,`notes`)
         VALUES (?,?,?,?,?,?,?,?,?,?)',
        'iissssssss',
        $orgId, (int) $licenceId, $clientId, $roomNumber, $floor, $wing,
        $roomTypeClientId, $roomTypeName, $roomStatus, $notes
    );
}

pos_audit($con, $licenceId, 'HotelRoom Saved', 'hotel_room', $clientId, isset($actor['id']) ? $actor['id'] : 0, array('roomNumber' => $roomNumber));
pos_api_json(array('status' => '1', 'message' => 'Room saved', 'clientId' => $clientId));
