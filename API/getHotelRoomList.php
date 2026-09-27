<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
pos_require_permission($con, $licenceId, 'billing.create');

$rows = db_stmt_fetch_all(
    $con,
    'SELECT `clientId`,`roomNumber`,`floor`,`wing`,`roomTypeClientId`,`roomTypeName`,`roomStatus`,`notes`,
            `createdAt`,`updatedAt`
     FROM `pos_hotel_rooms` WHERE `licenseId`=? ORDER BY `roomNumber` ASC',
    'i',
    (int) $licenceId
);

$list = array();
foreach ($rows as $row) {
    $list[] = array(
        'id' => $row['clientId'],
        'roomNumber' => $row['roomNumber'],
        'floor' => $row['floor'],
        'wing' => $row['wing'],
        'roomTypeClientId' => $row['roomTypeClientId'],
        'roomTypeName' => $row['roomTypeName'],
        'roomStatus' => $row['roomStatus'],
        'notes' => $row['notes'],
        'createdAt' => $row['createdAt'],
        'updatedAt' => $row['updatedAt'],
    );
}

pos_api_json(array('status' => '1', 'hotelRoomResponse' => $list));
