<?php
/**
 * Shared auth for HTTP cron endpoints.
 *
 * Default: allow plain URL hits (cPanel / hosting "Fetch URL" cron).
 * Harden by setting in db_local.php:
 *   $cronRequireSecret = true;
 *   $cronSecret = 'long-random-string';
 *
 * When required, pass secret via:
 *   Header: X-Cron-Secret: …
 *   or ?secret=… / ?key=…
 * CLI (`php expireLicenses.php`) always allowed.
 */

if (!function_exists('cron_auth_configured_secret')) {
    function cron_auth_configured_secret()
    {
        if (isset($GLOBALS['cronSecret']) && is_string($GLOBALS['cronSecret']) && $GLOBALS['cronSecret'] !== '') {
            return $GLOBALS['cronSecret'];
        }
        $envSecret = getenv('CRON_SECRET');
        if (is_string($envSecret) && $envSecret !== '') {
            return $envSecret;
        }
        return '';
    }
}

if (!function_exists('cron_auth_provided_secret')) {
    function cron_auth_provided_secret()
    {
        if (!empty($_SERVER['HTTP_X_CRON_SECRET'])) {
            return trim((string) $_SERVER['HTTP_X_CRON_SECRET']);
        }
        if (isset($_GET['secret'])) {
            return trim((string) $_GET['secret']);
        }
        if (isset($_GET['key'])) {
            return trim((string) $_GET['key']);
        }
        if (isset($_POST['secret'])) {
            return trim((string) $_POST['secret']);
        }
        if (isset($_POST['key'])) {
            return trim((string) $_POST['key']);
        }
        return '';
    }
}

if (!function_exists('cron_auth_require_secret')) {
    function cron_auth_require_secret()
    {
        if (isset($GLOBALS['cronRequireSecret'])) {
            return (bool) $GLOBALS['cronRequireSecret'];
        }
        /* Env override: CRON_REQUIRE_SECRET=1 */
        $env = getenv('CRON_REQUIRE_SECRET');
        if ($env === '1' || strtolower((string) $env) === 'true') {
            return true;
        }
        return false;
    }
}

if (!function_exists('cron_auth_guard')) {
    /**
     * @param array $response Base JSON response (status/message).
     * @return bool true = continue; false = already exited with error JSON.
     */
    function cron_auth_guard(array &$response)
    {
        if (PHP_SAPI === 'cli') {
            return true;
        }

        if (!cron_auth_require_secret()) {
            return true;
        }

        $configuredSecret = cron_auth_configured_secret();
        if ($configuredSecret === '') {
            http_response_code(503);
            $response['status'] = '0';
            $response['message'] = 'Cron secret required but not configured. Set $cronSecret in db_local.php or CRON_SECRET env.';
            echo json_encode($response);
            return false;
        }

        $provided = cron_auth_provided_secret();
        if ($provided === '' || !hash_equals($configuredSecret, $provided)) {
            http_response_code(401);
            $response['status'] = '0';
            $response['message'] = 'Unauthorized';
            echo json_encode($response);
            return false;
        }

        return true;
    }
}
