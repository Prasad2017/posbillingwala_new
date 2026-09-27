<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
$actor = pos_require_permission($con, $licenceId, 'billing.create');
$orgId = enterprise_org_id($con, $licenceId);

$clientId = pos_api_post('clientId');
$bookingNo = pos_api_post('bookingNo');
$guestName = pos_api_post('guestName');
if ($clientId === '' || $guestName === '') {
    pos_api_json(array('status' => '0', 'message' => 'clientId and guestName are required'));
}
if ($bookingNo === '') {
    $bookingNo = 'BK-' . date('ymdHis');
}

$roomClientId = pos_api_post('roomClientId');
$roomNumber = pos_api_post('roomNumber');
$guestMobile = pos_api_post('guestMobile');
$guestIdProof = pos_api_post('guestIdProof');
$checkInAt = pos_api_post('checkInAt');
$checkOutAt = pos_api_post('checkOutAt');
$expectedCheckIn = pos_api_post('expectedCheckIn');
$expectedCheckOut = pos_api_post('expectedCheckOut');
$bookingStatus = strtoupper(pos_api_post('bookingStatus', 'RESERVED'));
$advancePaid = (float) pos_api_post('advancePaid', '0');
$roomCharges = (float) pos_api_post('roomCharges', '0');
$extraCharges = (float) pos_api_post('extraCharges', '0');
$notes = pos_api_post('notes');

$existing = db_stmt_fetch_one(
    $con,
    'SELECT `id` FROM `pos_hotel_bookings` WHERE `licenseId`=? AND `clientId`=? LIMIT 1',
    'is',
    (int) $licenceId,
    $clientId
);

if ($existing !== null) {
    db_stmt_execute(
        $con,
        'UPDATE `pos_hotel_bookings` SET `bookingNo`=?, `roomClientId`=?, `roomNumber`=?, `guestName`=?,
         `guestMobile`=?, `guestIdProof`=?, `checkInAt`=?, `checkOutAt`=?, `expectedCheckIn`=?,
         `expectedCheckOut`=?, `bookingStatus`=?, `advancePaid`=?, `roomCharges`=?, `extraCharges`=?,
         `notes`=? WHERE `licenseId`=? AND `clientId`=?',
        'sssssssssssdddsis',
        $bookingNo, $roomClientId, $roomNumber, $guestName, $guestMobile, $guestIdProof,
        $checkInAt === '' ? null : $checkInAt,
        $checkOutAt === '' ? null : $checkOutAt,
        $expectedCheckIn === '' ? null : $expectedCheckIn,
        $expectedCheckOut === '' ? null : $expectedCheckOut,
        $bookingStatus, $advancePaid, $roomCharges, $extraCharges, $notes,
        (int) $licenceId, $clientId
    );
} else {
    db_stmt_execute(
        $con,
        'INSERT INTO `pos_hotel_bookings`
         (`organization_id`,`licenseId`,`clientId`,`bookingNo`,`roomClientId`,`roomNumber`,`guestName`,
          `guestMobile`,`guestIdProof`,`checkInAt`,`checkOutAt`,`expectedCheckIn`,`expectedCheckOut`,
          `bookingStatus`,`advancePaid`,`roomCharges`,`extraCharges`,`notes`)
         VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
        'iissssssssssssddds',
        $orgId, (int) $licenceId, $clientId, $bookingNo, $roomClientId, $roomNumber, $guestName,
        $guestMobile, $guestIdProof,
        $checkInAt === '' ? null : $checkInAt,
        $checkOutAt === '' ? null : $checkOutAt,
        $expectedCheckIn === '' ? null : $expectedCheckIn,
        $expectedCheckOut === '' ? null : $expectedCheckOut,
        $bookingStatus, $advancePaid, $roomCharges, $extraCharges, $notes
    );
}

/* Keep room board status in sync for check-in / check-out. */
if ($roomClientId !== '') {
    $roomStatus = 'RESERVED';
    if ($bookingStatus === 'CHECKED_IN') {
        $roomStatus = 'OCCUPIED';
    } elseif ($bookingStatus === 'CHECKED_OUT' || $bookingStatus === 'CANCELLED') {
        $roomStatus = 'AVAILABLE';
    } elseif ($bookingStatus === 'CLEANING') {
        $roomStatus = 'CLEANING';
    }
    db_stmt_execute(
        $con,
        'UPDATE `pos_hotel_rooms` SET `roomStatus`=? WHERE `licenseId`=? AND `clientId`=?',
        'sis',
        $roomStatus,
        (int) $licenceId,
        $roomClientId
    );
}

pos_audit($con, $licenceId, 'HotelBooking Saved', 'hotel_booking', $clientId, isset($actor['id']) ? $actor['id'] : 0, array(
    'bookingNo' => $bookingNo,
    'status' => $bookingStatus,
));
pos_api_json(array('status' => '1', 'message' => 'Booking saved', 'clientId' => $clientId, 'bookingNo' => $bookingNo));
