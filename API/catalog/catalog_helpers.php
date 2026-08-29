<?php

function catalog_json_response(array $payload, $httpCode = 200)
{
    if (!headers_sent()) {
        header('Content-Type: application/json; charset=utf-8');
        if ($httpCode !== 200) {
            http_response_code((int) $httpCode);
        }
    }
    echo json_encode($payload);
}

function catalog_sanitize_filename($name)
{
    $name = preg_replace('/[^A-Za-z0-9._-]+/', '_', (string) $name);
    $name = trim($name, '._-');
    return $name !== '' ? $name : 'catalog';
}

function catalog_generate_network_status()
{
    try {
        return substr(bin2hex(random_bytes(5)), 0, 10);
    } catch (Exception $e) {
        return substr(md5(uniqid('', true)), 0, 10);
    }
}

function catalog_normalize_status($value)
{
    $value = strtolower(trim((string) $value));
    if ($value === '') {
        return 'active';
    }
    if (in_array($value, array('active', 'inactive', '1', 'yes', 'enabled'), true)) {
        return 'active';
    }
    if (in_array($value, array('inactive', '0', 'no', 'disabled'), true)) {
        return 'inactive';
    }
    return null;
}

function catalog_split_gst($gstValue)
{
    if ($gstValue === '' || $gstValue === null) {
        return array('cgst' => 0, 'sgst' => 0);
    }
    if (!is_numeric($gstValue)) {
        return null;
    }
    $gst = (float) $gstValue;
    if ($gst < 0 || $gst > 100) {
        return null;
    }
    $half = $gst / 2.0;
    return array(
        'cgst' => (int) round($half),
        'sgst' => (int) round($half),
    );
}

function catalog_row_is_empty(array $row)
{
    foreach ($row as $value) {
        if (trim((string) $value) !== '') {
            return false;
        }
    }
    return true;
}

function catalog_storage_dir($subdir = '')
{
    $base = __DIR__ . '/storage';
    if ($subdir !== '') {
        $base .= '/' . trim($subdir, '/');
    }
    if (!is_dir($base)) {
        @mkdir($base, 0755, true);
    }
    return $base;
}

function catalog_customer_display_name($con, $customerId)
{
    $name = db_stmt_scalar_string(
        $con,
        'SELECT `name` FROM `users` WHERE `id`=? AND `role_id`=3 LIMIT 1',
        'i',
        (int) $customerId
    );
    return $name !== null ? $name : 'Customer';
}

function catalog_export_filename($customerName, $type, $extension = 'xlsx')
{
    $safe = catalog_sanitize_filename(str_replace(' ', '_', $customerName));
    $date = date('Y-m-d');
    return $safe . '_' . ucfirst($type) . '_' . $date . '.' . $extension;
}
