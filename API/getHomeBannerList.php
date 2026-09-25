<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';

header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'bannerResponse' => array(),
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

$userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
pos_require_auth($con, $userId, $response);

mysqli_query($con, 'set names utf8');
mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_home_banners` (
    `bannerId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `image_path` VARCHAR(255) NOT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0,
    `is_active` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`bannerId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

$banners = array();
$sql = "SELECT `bannerId`, `image_url`, `sort_order`
        FROM `pos_home_banners`
        WHERE `is_active` = 1 AND `image_url` <> ''
        ORDER BY `sort_order` ASC, `bannerId` ASC";
if ($result = mysqli_query($con, $sql)) {
    while ($row = mysqli_fetch_assoc($result)) {
        $banners[] = array(
            'bannerId' => (string) $row['bannerId'],
            'imageUrl' => $row['image_url'],
            'sortOrder' => (string) $row['sort_order'],
        );
    }
}

$response['status'] = 'true';
$response['bannerResponse'] = $banners;

mysqli_close($con);
echo json_encode($response);
?>
