<?php
/**
 * PHP 7.0+ compatibility bootstrap for all POS / Owner / Dealer / Admin APIs.
 *
 * Goals:
 * - Require PHP >= 7.0 (reject older runtimes early with JSON).
 * - Keep mysqli behavior identical on PHP 7.x and 8.x (no exception mode).
 * - Provide small polyfills so optional PHP 8 helpers are safe to call.
 *
 * Include this before any DB work (db_connection.php loads it automatically).
 */

if (defined('POS_PHP_COMPAT_LOADED')) {
    return;
}
define('POS_PHP_COMPAT_LOADED', true);

if (version_compare(PHP_VERSION, '7.0.0', '<')) {
    if (php_sapi_name() !== 'cli') {
        header('Content-Type: application/json; charset=utf-8');
        http_response_code(500);
    }
    echo json_encode(array(
        'status' => '0',
        'message' => 'PHP 7.0 or newer is required. Current version: ' . PHP_VERSION,
    ));
    exit(1);
}

/**
 * PHP 8.1+ defaults to MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT (throws).
 * Older PHP returned false. Force OFF so every endpoint works the same way.
 */
if (function_exists('mysqli_report')) {
    mysqli_report(MYSQLI_REPORT_OFF);
}

if (!function_exists('str_contains')) {
    /**
     * @param string $haystack
     * @param string $needle
     * @return bool
     */
    function str_contains($haystack, $needle)
    {
        $haystack = (string) $haystack;
        $needle = (string) $needle;
        if ($needle === '') {
            return true;
        }
        return strpos($haystack, $needle) !== false;
    }
}

if (!function_exists('str_starts_with')) {
    /**
     * @param string $haystack
     * @param string $needle
     * @return bool
     */
    function str_starts_with($haystack, $needle)
    {
        $haystack = (string) $haystack;
        $needle = (string) $needle;
        if ($needle === '') {
            return true;
        }
        return strpos($haystack, $needle) === 0;
    }
}

if (!function_exists('str_ends_with')) {
    /**
     * @param string $haystack
     * @param string $needle
     * @return bool
     */
    function str_ends_with($haystack, $needle)
    {
        $haystack = (string) $haystack;
        $needle = (string) $needle;
        if ($needle === '') {
            return true;
        }
        $len = strlen($needle);
        if ($len === 0) {
            return true;
        }
        return substr($haystack, -$len) === $needle;
    }
}

if (!function_exists('db_safe_query')) {
    /**
     * mysqli_query wrapper that never throws (PHP 7 / 8 safe).
     *
     * @param mysqli $con
     * @param string $sql
     * @return mysqli_result|bool
     */
    function db_safe_query($con, $sql)
    {
        if ($con === null || !($con instanceof mysqli)) {
            return false;
        }
        try {
            $result = mysqli_query($con, $sql);
            return ($result === null) ? false : $result;
        } catch (Throwable $e) {
            return false;
        }
    }
}

?>
