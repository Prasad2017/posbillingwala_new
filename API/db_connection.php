<?php
/**
 * P5-4 / P5-5: Shared MySQL connection for POS + Owner + Dealer + Admin.
 * Credentials: copy API/db_local.example.php → API/db_local.php (gitignored),
 * or set DB_HOST, DB_USER, DB_PASS, DB_NAME environment variables.
 *
 * Requires PHP >= 7.0 (enforced by php_compat.php).
 */

require_once __DIR__ . '/php_compat.php';

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
            $con = @mysqli_connect($server, $username, $password);
            if ($con != null) {
                return $con;
            }
            return null;
        } catch (Throwable $ex) {
            error_log('DB connection error: ' . $ex->getMessage());
            return null;
        }
    }
}

if (!function_exists('db_json_fatal')) {
    /**
     * @param string $message
     * @param int $httpCode
     */
    function db_json_fatal($message, $httpCode = 503)
    {
        if (php_sapi_name() !== 'cli') {
            header('Content-Type: application/json; charset=utf-8');
            http_response_code((int) $httpCode);
        }
        echo json_encode(array(
            'status' => '0',
            'message' => (string) $message,
        ));
        exit;
    }
}

if (!isset($con) || !$con) {
    if ($dbUser === null || $dbUser === '' || $dbPass === null) {
        if (php_sapi_name() !== 'cli') {
            header('Content-Type: application/json; charset=utf-8');
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
        $selected = false;
        try {
            $selected = @mysqli_select_db($con, $dbName);
        } catch (Throwable $e) {
            $selected = false;
        }
        if (!$selected) {
            db_json_fatal('Could not select database.');
        }
        // Re-assert after connect (some hosts reset driver flags).
        if (function_exists('mysqli_report')) {
            mysqli_report(MYSQLI_REPORT_OFF);
        }
    } else {
        db_json_fatal('Could not connect to database.');
    }
}
?>
