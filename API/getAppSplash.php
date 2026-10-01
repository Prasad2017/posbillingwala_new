<?php
include_once('config.php');

header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'imageUrl' => '',
    'legacyUrl' => '',
    'images' => array(
        'mobilePortrait' => '',
        'mobileLandscape' => '',
        'tabletPortrait' => '',
        'tabletLandscape' => '',
        'webPortrait' => '',
        'webLandscape' => '',
    ),
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

$slotToKey = array(
    'mobile_portrait' => 'mobilePortrait',
    'mobile_landscape' => 'mobileLandscape',
    'tablet_portrait' => 'tabletPortrait',
    'tablet_landscape' => 'tabletLandscape',
    'web_portrait' => 'webPortrait',
    'web_landscape' => 'webLandscape',
);

mysqli_query($con, 'set names utf8');
mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_app_splash` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `slot` VARCHAR(40) NOT NULL DEFAULT 'legacy',
    `image_path` VARCHAR(255) NULL,
    `image_url` VARCHAR(500) NULL,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `pos_app_splash_slot_unique` (`slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

$col = mysqli_query($con, "SHOW COLUMNS FROM `pos_app_splash` LIKE 'slot'");
if ($col && mysqli_num_rows($col) === 0) {
    mysqli_query(
        $con,
        "ALTER TABLE `pos_app_splash` ADD COLUMN `slot` VARCHAR(40) NOT NULL DEFAULT 'legacy' AFTER `id`"
    );
}

$idx = mysqli_query($con, "SHOW INDEX FROM `pos_app_splash` WHERE Key_name = 'pos_app_splash_slot_unique'");
if ($idx && mysqli_num_rows($idx) === 0) {
    mysqli_query(
        $con,
        "DELETE t1 FROM `pos_app_splash` t1
         INNER JOIN `pos_app_splash` t2 ON t1.`slot` = t2.`slot` AND t1.`id` < t2.`id`"
    );
    mysqli_query(
        $con,
        "ALTER TABLE `pos_app_splash` ADD UNIQUE KEY `pos_app_splash_slot_unique` (`slot`)"
    );
}

$legacyUrl = '';
$sql = "SELECT `slot`, `image_url`
        FROM `pos_app_splash`
        WHERE `image_url` IS NOT NULL AND `image_url` <> ''";
if ($result = mysqli_query($con, $sql)) {
    while ($row = mysqli_fetch_assoc($result)) {
        $slot = trim((string) ($row['slot'] ?? 'legacy'));
        $url = trim((string) ($row['image_url'] ?? ''));
        if ($url === '') {
            continue;
        }
        if ($slot === '' || $slot === 'legacy') {
            $legacyUrl = $url;
            continue;
        }
        if (isset($slotToKey[$slot])) {
            $response['images'][$slotToKey[$slot]] = $url;
        }
    }
}

$fallback = $legacyUrl;
if ($fallback === '') {
    foreach ($response['images'] as $url) {
        if ($url !== '') {
            $fallback = $url;
            break;
        }
    }
}

$response['status'] = 'true';
$response['legacyUrl'] = $legacyUrl;
$response['imageUrl'] = $fallback;

mysqli_close($con);
echo json_encode($response);
?>
