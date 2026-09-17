<?php
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/pos_schema.php';
require_once __DIR__ . '/pos_permissions.php';
require_once __DIR__ . '/pos_staff.php';
require_once __DIR__ . '/pos_devices.php';
require_once __DIR__ . '/pos_print_ops.php';
require_once __DIR__ . '/pos_audit.php';

if (!function_exists('pos_api_headers')) {
    function pos_api_headers()
    {
        header('Access-Control-Allow-Origin: *');
        header('Access-Control-Allow-Headers: Authorization, Content-Type, Accept, X-Requested-With, X-Pos-Staff-Id');
        header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
        header('Content-Type: application/json; charset=utf-8');
        if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
            http_response_code(204);
            exit;
        }
    }
}

if (!function_exists('pos_api_json')) {
    function pos_api_json(array $response)
    {
        echo json_encode($response);
        exit;
    }
}

if (!function_exists('pos_api_post')) {
    function pos_api_post($key, $default = '')
    {
        return isset($_POST[$key]) ? trim((string) $_POST[$key]) : $default;
    }
}

if (!function_exists('pos_api_require_licence')) {
    function pos_api_require_licence($con)
    {
        pos_api_headers();
        if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
            pos_api_json(array('status' => '0', 'message' => 'Use Post Method'));
        }
        $userId = pos_api_post('userId');
        $licenceId = pos_require_auth($con, $userId, array('status' => '0', 'message' => 'Unauthorized'));
        pos_schema_ensure($con);
        return $licenceId;
    }
}
