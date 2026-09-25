<?php
include_once('config.php');

header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'imageUrl' => '',
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

mysqli_query($con, 'set names utf8');
mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_app_splash` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `image_path` VARCHAR(255) NULL,
    `image_url` VARCHAR(500) NULL,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

$imageUrl = '';
$sql = "SELECT `image_url`
        FROM `pos_app_splash`
        WHERE `image_url` IS NOT NULL AND `image_url` <> ''
        ORDER BY `id` DESC
        LIMIT 1";
if ($result = mysqli_query($con, $sql)) {
    if ($row = mysqli_fetch_assoc($result)) {
        $imageUrl = trim((string) $row['image_url']);
    }
}

$response['status'] = 'true';
$response['imageUrl'] = $imageUrl;

mysqli_close($con);
echo json_encode($response);
?>
