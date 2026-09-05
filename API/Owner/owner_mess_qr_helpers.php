<?php
/**
 * Owner Mess QR helpers — licence must belong to authenticated owner.
 * POS print device id is never overwritten when Owner omits android_device_id.
 */
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../mess_common_helpers.php';
require_once __DIR__ . '/../db_prepared.php';

if (!function_exists('owner_mess_qr_resolve_licence')) {
    /**
     * @return array{ownerId:string,licenceId:int,licence:array}|null
     */
    function owner_mess_qr_resolve_licence($con, $postedOwnerUserId, $licenceId, array &$response)
    {
        owner_require_auth($con, $response);
        $ownerId = owner_resolve_user_id($con, $postedOwnerUserId);
        if ($ownerId === null) {
            $response['message'] = 'Invalid or expired auth token';
            return null;
        }
        $licenceId = (int) $licenceId;
        if ($licenceId <= 0) {
            $response['message'] = 'Select an outlet / licence';
            return null;
        }
        $lic = db_stmt_fetch_one(
            $con,
            'SELECT * FROM licenses WHERE id = ? AND userId = ? LIMIT 1',
            'ii',
            $licenceId,
            (int) $ownerId
        );
        if ($lic === null) {
            $response['message'] = 'Outlet not found for this owner';
            return null;
        }
        return array(
            'ownerId' => (string) $ownerId,
            'licenceId' => $licenceId,
            'licence' => $lic,
        );
    }
}

if (!function_exists('owner_mess_qr_payload')) {
    function owner_mess_qr_payload($row)
    {
        if ($row === null) {
            return null;
        }
        return array(
            'publicToken' => $row['public_token'],
            'status' => $row['status'],
            'qrUrl' => mess_public_qr_url($row['public_token']),
            'messLabel' => $row['mess_label'],
            'branchLabel' => $row['branch_label'],
            'printDeviceId' => $row['print_device_id'],
            'createdAt' => isset($row['created_at']) ? $row['created_at'] : '',
            'licenceId' => (string) $row['userId'],
        );
    }
}
