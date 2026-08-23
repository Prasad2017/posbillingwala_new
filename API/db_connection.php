<?php
/**
 * P5-4 / P5-5: Shared MySQL connection for POS + Owner + Dealer + Admin.
 * Credentials: copy API/db_local.example.php → API/db_local.php (gitignored),
 * or set DB_HOST, DB_USER, DB_PASS, DB_NAME environment variables.
 */

$dbHost = getenv('DB_HOST') ?: 'localhost';
$dbUser = getenv('DB_USER') ?: null;
$dbPass = getenv('DB_PASS') ?: null;
$dbName = getenv('DB_NAME') ?: 'spllmgkn_posbill';

$localConfig = __DIR__ . '/db_local.php';
if (file_exists($localConfig)) {
    require $localConfig;
}

if (!function_exists('getConnection')) {
    function getConnection($server, $username, $password)
    {
        try {
            $con = mysqli_connect($server, $username, $password);
            if ($con != null) {
                return $con;
            }
            return null;
        } catch (Exception $ex) {
            error_log('DB connection error: ' . $ex->getMessage());
            return null;
        }
    }
}

if (!isset($con) || !$con) {
    if ($dbUser === null || $dbUser === '' || $dbPass === null) {
        if (php_sapi_name() !== 'cli') {
            header('Content-Type: application/json');
            http_response_code(503);
            echo json_encode(array(
                'status' => '0',
                'message' => 'Database not configured. Copy API/db_local.example.php to API/db_local.php on the server.',
            ));
        }
        return;
    }

    $con = getConnection($dbHost, $dbUser, $dbPass);
    if ($con && is_object($con)) {
        mysqli_select_db($con, $dbName) or die('Could not select database.');
    }
}
?>
