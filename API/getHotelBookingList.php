<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
pos_require_permission($con, $licenceId, 'billing.create');

$rows = db_stmt_fetch_all(
    $con,
    'SELECT `clientId`,`bookingNo`,`roomClientId`,`roomNumber`,`guestName`,`guestMobile`,`guestIdProof`,
            `checkInAt`,`checkOutAt`,`expectedCheckIn`,`expectedCheckOut`,`bookingStatus`,
            `advancePaid`,`roomCharges`,`extraCharges`,`notes`,`createdAt`,`updatedAt`
     FROM `pos_hotel_bookings` WHERE `licenseId`=? ORDER BY `updatedAt` DESC',
    'i',
    (int) $licenceId
);

$list = array();
foreach ($rows as $row) {
    $list[] = array(
        'id' => $row['clientId'],
        'bookingNo' => $row['bookingNo'],
        'roomClientId' => $row['roomClientId'],
        'roomNumber' => $row['roomNumber'],
        'guestName' => $row['guestName'],
        'guestMobile' => $row['guestMobile'],
        'guestIdProof' => $row['guestIdProof'],
        'checkInAt' => $row['checkInAt'],
        'checkOutAt' => $row['checkOutAt'],
        'expectedCheckIn' => $row['expectedCheckIn'],
        'expectedCheckOut' => $row['expectedCheckOut'],
        'bookingStatus' => $row['bookingStatus'],
        'advancePaid' => (float) $row['advancePaid'],
        'roomCharges' => (float) $row['roomCharges'],
        'extraCharges' => (float) $row['extraCharges'],
        'notes' => $row['notes'],
        'createdAt' => $row['createdAt'],
        'updatedAt' => $row['updatedAt'],
    );
}

pos_api_json(array('status' => '1', 'hotelBookingResponse' => $list));
