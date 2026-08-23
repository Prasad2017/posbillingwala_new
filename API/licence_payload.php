<?php
/**
 * Production licensing: server-signed license payloads for offline validation.
 * Private key lives ONLY on the server (license_signing_private.pem or env).
 * APK embeds the matching public key in assets/license_signing_public.pem.
 */

require_once __DIR__ . '/licence_expiry.php';

if (!function_exists('licence_payload_offline_grace_days')) {
    /** Max days a device may bill offline after last online refresh. */
    function licence_payload_offline_grace_days()
    {
        return 14;
    }
}

if (!function_exists('licence_payload_version')) {
    function licence_payload_version()
    {
        return 1;
    }
}

if (!function_exists('licence_signing_private_key_path')) {
    function licence_signing_private_key_path()
    {
        $env = getenv('LICENSE_SIGNING_PRIVATE_KEY_PATH');
        if ($env !== false && $env !== '' && is_readable($env)) {
            return $env;
        }
        $local = __DIR__ . '/license_signing_private.pem';
        if (is_readable($local)) {
            return $local;
        }
        return null;
    }
}

if (!function_exists('licence_load_signing_private_key')) {
    /**
     * @return resource|false OpenSSL private key resource
     */
    function licence_load_signing_private_key()
    {
        $path = licence_signing_private_key_path();
        if ($path === null) {
            return false;
        }
        $pem = file_get_contents($path);
        if ($pem === false || trim($pem) === '') {
            return false;
        }
        return openssl_pkey_get_private($pem);
    }
}

if (!function_exists('licence_payload_canonical_json')) {
    /**
     * Deterministic JSON for signing (sorted keys, no whitespace).
     *
     * @param array $payload
     * @return string
     */
    function licence_payload_canonical_json(array $payload)
    {
        ksort($payload);
        return json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    }
}

if (!function_exists('licence_build_payload_data')) {
    /**
     * Build unsigned payload from license + user rows.
     *
     * Hierarchy: organizationId (users.id) → branchId (licenses.id) → deviceId → licenseKey
     *
     * @param mysqli $con
     * @param array  $licenseRow licenses.* (+ optional users join fields)
     * @param string $deviceId   bound android_device_id
     * @return array|null
     */
    function licence_build_payload_data($con, array $licenseRow, $deviceId)
    {
        if (!licence_enforce_expiry($con, $licenseRow)) {
            return null;
        }

        $licenceId = isset($licenseRow['id']) ? (string) $licenseRow['id'] : '';
        $organizationId = isset($licenseRow['userId']) ? (string) $licenseRow['userId'] : '';
        $deviceId = trim((string) $deviceId);
        $licenseKey = isset($licenseRow['licenseKey']) ? (string) $licenseRow['licenseKey'] : '';

        if ($licenceId === '' || $organizationId === '' || $deviceId === '' || $licenseKey === '') {
            return null;
        }

        // Device must match bound device for signed payload
        $boundDevice = isset($licenseRow['android_device_id']) ? trim((string) $licenseRow['android_device_id']) : '';
        if ($boundDevice === '' || $boundDevice !== $deviceId) {
            return null;
        }

        if (licence_is_trial_consumed($licenseRow) && licence_is_trial($licenseRow)) {
            return null;
        }

        $billCount = licence_count_bills($con, $licenceId);
        $isTrial = licence_is_trial($licenseRow);
        $issuedAt = time();
        $graceDays = licence_payload_offline_grace_days();
        $offlineGraceUntil = $issuedAt + ($graceDays * 86400);

        $branch = licence_branch_fields($licenseRow);

        return array(
            'payloadVersion' => licence_payload_version(),
            'organizationId' => $organizationId,
            'branchId' => $licenceId,
            'branchLabel' => $branch['branchLabel'],
            'licenseId' => $licenceId,
            'deviceId' => $deviceId,
            'licenseKey' => $licenseKey,
            'licenseType' => isset($licenseRow['licenseType']) ? (string) $licenseRow['licenseType'] : '',
            'isTrial' => $isTrial ? 1 : 0,
            'trialMaxBills' => licence_trial_max_bills(),
            'trialBillCount' => $billCount,
            'trialConsumed' => licence_is_trial_consumed($licenseRow) ? 1 : 0,
            'expiryDate' => isset($licenseRow['expiryDate']) ? (string) $licenseRow['expiryDate'] : '',
            'issuedAt' => $issuedAt,
            'offlineGraceUntil' => $offlineGraceUntil,
            'fastBilling' => isset($licenseRow['fastBilling']) ? (int) $licenseRow['fastBilling'] : 1,
            'takeAway' => isset($licenseRow['takeAway']) ? (int) $licenseRow['takeAway'] : 1,
            'dineIn' => isset($licenseRow['dineIn']) ? (int) $licenseRow['dineIn'] : 1,
            'mess' => isset($licenseRow['mess']) ? (int) $licenseRow['mess'] : 0,
        );
    }
}

if (!function_exists('licence_sign_payload')) {
    /**
     * @param array $payloadData unsigned payload fields
     * @return array|null ['licensePayload'=>base64json, 'licenseSignature'=>base64sig]
     */
    function licence_sign_payload(array $payloadData)
    {
        $privateKey = licence_load_signing_private_key();
        if ($privateKey === false) {
            return null;
        }

        $canonical = licence_payload_canonical_json($payloadData);
        $signature = '';
        $ok = openssl_sign($canonical, $signature, $privateKey, OPENSSL_ALGO_SHA256);
        openssl_free_key($privateKey);

        if (!$ok || $signature === '') {
            return null;
        }

        return array(
            'licensePayload' => base64_encode($canonical),
            'licenseSignature' => base64_encode($signature),
            'issuedAt' => (string) $payloadData['issuedAt'],
            'offlineGraceUntil' => (string) $payloadData['offlineGraceUntil'],
            'organizationId' => (string) $payloadData['organizationId'],
            'branchId' => (string) $payloadData['branchId'],
            'branchLabel' => (string) $payloadData['branchLabel'],
        );
    }
}

if (!function_exists('licence_append_signed_payload')) {
    /**
     * Adds signed license payload + hierarchy fields to API response.
     *
     * @param mysqli $con
     * @param array  $response
     * @param array  $licenseRow
     * @param string $deviceId
     * @return array
     */
    function licence_append_signed_payload($con, array $response, array $licenseRow, $deviceId)
    {
        $licenseRow = licence_sync_trial_consumed_state($con, $licenseRow);
        $payloadData = licence_build_payload_data($con, $licenseRow, $deviceId);
        if ($payloadData === null) {
            $response['licensePayload'] = '';
            $response['licenseSignature'] = '';
            return $response;
        }

        $signed = licence_sign_payload($payloadData);
        if ($signed === null) {
            $response['licensePayload'] = '';
            $response['licenseSignature'] = '';
            $response['licensePayloadWarning'] = 'Signing key unavailable on server';
            return $response;
        }

        return array_merge($response, $signed);
    }
}

?>
