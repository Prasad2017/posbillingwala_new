<?php
/**
 * Redact secrets from crash/error log payloads before storage or Admin display.
 * Masks credentials only — does not replace technical error messages with generics.
 */

if (!function_exists('log_sanitize_text')) {
    /**
     * @param string|null $raw
     * @param int $maxBytes
     * @return string
     */
    function log_sanitize_text($raw, $maxBytes = 20480)
    {
        if ($raw === null || $raw === '') {
            return '';
        }
        $text = (string) $raw;

        // Bearer / Authorization headers
        $text = preg_replace('/(Bearer\s+)\S+/i', '$1********', $text);
        $text = preg_replace('/("?(?:authorization|auth[_-]?token|refresh[_-]?token|access[_-]?token)"?\s*[:=]\s*"?)([^"&\s,}]+)/i', '$1********', $text);

        // JSON-style secrets
        $jsonKeys = 'password|mpin|otp|token|authorization|refresh_token|access_token|aadhaar|aadhar|card_number|cardNumber|cvv|secret|api_key|apiKey|app_licence_key|licence_key|license_key';
        $text = preg_replace(
            '/("(?:' . $jsonKeys . ')"\s*:\s*")([^"]*)(")/i',
            '$1******$3',
            $text
        );

        // Form / query secrets
        $formKeys = 'password|mpin|otp|token|authorization|refresh_token|access_token|aadhaar|aadhar|card_number|cvv|secret|api_key|app_licence_key|licence_key|license_key|authToken';
        $text = preg_replace(
            '/((?:' . $formKeys . ')=)([^&\s]*)/i',
            '$1******',
            $text
        );

        if ($maxBytes > 0 && strlen($text) > $maxBytes) {
            $text = substr($text, 0, $maxBytes) . '…[truncated]';
        }
        return $text;
    }
}

if (!function_exists('log_post_str')) {
    /**
     * @param string $key
     * @param int $maxBytes
     * @return string
     */
    function log_post_str($key, $maxBytes = 20480)
    {
        $val = isset($_POST[$key]) ? $_POST[$key] : '';
        return log_sanitize_text($val, $maxBytes);
    }
}

if (!function_exists('log_is_generic_message')) {
    /**
     * Detect forbidden generic replacements that must never overwrite originals.
     * @param string $msg
     * @return bool
     */
    function log_is_generic_message($msg)
    {
        $normalized = strtolower(trim((string) $msg));
        if ($normalized === '') {
            return false;
        }
        $generics = array(
            'something went wrong',
            'unknown error',
            'operation failed',
            'an error occurred',
            'error occurred',
            'failed',
            'unknown',
        );
        return in_array($normalized, $generics, true);
    }
}
