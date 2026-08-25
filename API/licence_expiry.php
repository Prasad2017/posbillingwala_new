<?php
/**
 * P4-1 / P4-2: Server-authoritative licence helpers.
 * - Expiry: valid while expiryDate >= today.
 * - Trial (Demo): fixed 7 days + max 50 bills — limits live only on the server.
 */

require_once __DIR__ . '/db_prepared.php';

if (!function_exists('licence_lifetime_days')) {
    /** Existing product mapping for Lifetime (~30 years). */
    function licence_lifetime_days()
    {
        return 10958;
    }
}

if (!function_exists('licence_normalize_validity_days')) {
    /**
     * P4-3: Map UI / posted validity labels to day counts.
     * Supports: 7 Days, 6 Months, 1 Year, 3 Years, 5 Years, Lifetime, and raw day numbers.
     *
     * @param string|int $licenseValidity
     * @return string day count suitable for expiryDate math
     */
    function licence_normalize_validity_days($licenseValidity)
    {
        $raw = trim((string) $licenseValidity);
        if ($raw === '') {
            return $raw;
        }

        $lower = strtolower($raw);
        $lower = str_replace(array(' days', ' day'), '', $lower);
        $lower = trim($lower);

        if ($lower === 'lifetime' || $lower === 'life time') {
            return (string) licence_lifetime_days();
        }
        if ($lower === '6 months' || $lower === '6 month' || $lower === '6m') {
            return '183';
        }
        if ($lower === '1 year' || $lower === '1y' || $lower === '12 months') {
            return '365';
        }
        if ($lower === '3 years' || $lower === '3 year' || $lower === '3y' || $lower === '36 months') {
            return '1095';
        }
        if ($lower === '5 years' || $lower === '5 year' || $lower === '5y' || $lower === '60 months') {
            return '1825';
        }
        if ($lower === '7' || $lower === '7 days') {
            return '7';
        }

        // Already numeric (183, 365, 1095, …)
        if (ctype_digit($lower)) {
            return $lower;
        }

        // Fallback: strip non-digits if present (e.g. "183 Days" already partially cleaned)
        if (preg_match('/(\d+)/', $raw, $m)) {
            return $m[1];
        }

        return $raw;
    }
}

if (!function_exists('licence_today')) {
    function licence_today()
    {
        date_default_timezone_set('Asia/Kolkata');
        return date('Y-m-d');
    }
}

if (!function_exists('licence_is_trial_consumed')) {
    /**
     * Trial is consumed when flag set, expired, or bill cap reached.
     *
     * @param array|null $licenseRow
     * @return bool
     */
    function licence_is_trial_consumed($licenseRow)
    {
        if (!is_array($licenseRow) || !licence_is_trial($licenseRow)) {
            return false;
        }
        if (!empty($licenseRow['trialConsumed']) && (int) $licenseRow['trialConsumed'] === 1) {
            return true;
        }
        $expiryDate = isset($licenseRow['expiryDate']) ? $licenseRow['expiryDate'] : null;
        if (!licence_is_date_valid($expiryDate)) {
            return true;
        }
        return false;
    }
}

if (!function_exists('licence_mark_trial_consumed')) {
    /**
     * @param mysqli $con
     * @param string|int $licenceId
     */
    function licence_mark_trial_consumed($con, $licenceId)
    {
        if ($licenceId === null || $licenceId === '') {
            return;
        }
        db_stmt_execute(
            $con,
            'UPDATE `licenses` SET `trialConsumed`=1, `licenseStatus`=\'expire\', `licenseValidity`=\'0\' WHERE `id`=? AND (`licenseType`=\'Demo\' OR `licenseType`=\'Trial\')',
            's',
            (string) $licenceId
        );
    }
}

if (!function_exists('licence_sync_trial_consumed_state')) {
    /**
     * Mark trial consumed when expired or bill cap reached.
     *
     * @param mysqli $con
     * @param array  $licenseRow
     * @return array refreshed row fields (trialConsumed may change)
     */
    function licence_sync_trial_consumed_state($con, array $licenseRow)
    {
        if (!licence_is_trial($licenseRow)) {
            return $licenseRow;
        }
        $licenceId = isset($licenseRow['id']) ? $licenseRow['id'] : null;
        $expired = !licence_is_date_valid(isset($licenseRow['expiryDate']) ? $licenseRow['expiryDate'] : null);
        $billCap = !licence_trial_allows_new_bill($con, $licenseRow);

        if ($expired || $billCap) {
            licence_mark_trial_consumed($con, $licenceId);
            $licenseRow['trialConsumed'] = 1;
            if ($expired) {
                $licenseRow['licenseStatus'] = 'expire';
            }
        }
        return $licenseRow;
    }
}

if (!function_exists('licence_is_user_active')) {
    /**
     * users.is_active is int(11). mysqli may return int 1 or string "1" — never use === '1'.
     *
     * @param mixed $value users.is_active or aliased userActive
     * @return bool
     */
    function licence_is_user_active($value)
    {
        if ($value === null || $value === false || $value === '') {
            return false;
        }
        if (is_bool($value)) {
            return $value;
        }
        return (int) $value === 1;
    }
}

if (!function_exists('licence_trial_allows_login')) {
    /**
     * Trial licences require prior dealer/admin registration and must not be consumed.
     * Paid / Regular / grandfathered licences always allow login (expiry checked separately).
     *
     * @param mysqli $con
     * @param array  $licenseRow
     * @return bool
     */
    function licence_trial_allows_login($con, array $licenseRow)
    {
        if (!licence_is_trial($licenseRow)) {
            return true;
        }
        // Registration required: license row must belong to an active customer user
        $userId = isset($licenseRow['userId']) ? $licenseRow['userId'] : null;
        if ($userId === null || $userId === '' || (int) $userId <= 0) {
            return false;
        }
        $active = db_stmt_scalar_int(
            $con,
            'SELECT COUNT(*) AS c FROM `users` WHERE `id`=? AND `is_active`=1',
            's',
            (string) $userId
        );
        if ($active < 1) {
            return false;
        }
        $licenseRow = licence_sync_trial_consumed_state($con, $licenseRow);
        return !licence_is_trial_consumed($licenseRow);
    }
}

if (!function_exists('licence_trial_login_block_message')) {
    /**
     * User-facing message when licence_trial_allows_login() fails.
     *
     * @param array $licenseRow
     * @return string
     */
    function licence_trial_login_block_message(array $licenseRow)
    {
        if (licence_is_trial($licenseRow) && licence_is_trial_consumed($licenseRow)) {
            return 'Trial already used. Please upgrade your licence.';
        }
        if (licence_is_trial($licenseRow)) {
            return 'Registration required before trial. Contact your dealer.';
        }
        return 'Customer account inactive or missing. Contact your dealer.';
    }
}

if (!function_exists('licence_on_device_bind')) {
    /**
     * First device bind starts trial clock (7 calendar days from bind, not registration).
     * Prevents trial restart after reinstall — state is server-side on licenses row.
     *
     * @param mysqli $con
     * @param string $licenseKey
     * @param string $deviceId
     * @param string $deviceName
     * @return array status, message
     */
    function licence_on_device_bind($con, $licenseKey, $deviceId, $deviceName)
    {
        $response = array('status' => '0', 'message' => 'Device bind failed');

        $row = db_stmt_fetch_one(
            $con,
            'SELECT l.*, u.is_active AS userActive FROM `licenses` l INNER JOIN `users` u ON u.id = l.userId WHERE l.licenseKey=? LIMIT 1',
            's',
            $licenseKey
        );
        if ($row === null) {
            $response['message'] = 'Licence not found';
            return $response;
        }
        if (!licence_is_user_active(isset($row['userActive']) ? $row['userActive'] : null)) {
            // Paid / existing licences must not see the trial registration message
            $response['message'] = licence_is_trial($row)
                ? 'Registration required before trial. Contact your dealer.'
                : 'Customer account inactive. Contact your dealer.';
            return $response;
        }

        $row = licence_sync_trial_consumed_state($con, $row);
        if (licence_is_trial($row) && licence_is_trial_consumed($row)) {
            $response['message'] = 'Trial already used on this licence. Please upgrade to continue.';
            return $response;
        }

        if (!licence_enforce_expiry($con, $row)) {
            $response['message'] = 'Licence expired';
            return $response;
        }

        $today = licence_today();
        $now = date('Y-m-d H:i:s');
        $isFirstBind = empty($row['android_device_id']) || trim((string) $row['android_device_id']) === '';
        $expiryDate = isset($row['expiryDate']) ? $row['expiryDate'] : $today;
        $trialStartedAt = isset($row['trialStartedAt']) ? $row['trialStartedAt'] : null;
        $deviceBoundAt = isset($row['deviceBoundAt']) ? $row['deviceBoundAt'] : null;

        if ($isFirstBind && licence_is_trial($row)) {
            $trialDays = licence_trial_days();
            $expiryDate = date('Y-m-d', strtotime($today . ' +' . $trialDays . ' day'));
            $trialStartedAt = $now;
            $deviceBoundAt = $now;
        } elseif ($isFirstBind) {
            $deviceBoundAt = $now;
        }

        $updated = db_stmt_execute(
            $con,
            'UPDATE `licenses` SET `android_device_id`=?, `android_device_name`=?, `expiryDate`=?, `trialStartedAt`=COALESCE(?, `trialStartedAt`), `deviceBoundAt`=COALESCE(?, `deviceBoundAt`), `licenseStatus`=\'active\' WHERE `licenseKey`=?',
            'ssssss',
            $deviceId,
            $deviceName,
            $expiryDate,
            $trialStartedAt,
            $deviceBoundAt,
            $licenseKey
        );

        if (!$updated) {
            return $response;
        }

        $response['status'] = '1';
        $response['message'] = 'Device bound successfully';
        $response['expiryDate'] = $expiryDate;
        $response['isTrial'] = licence_is_trial($row) ? '1' : '0';
        return $response;
    }
}

if (!function_exists('licence_trial_days')) {
    /** @return int server-only trial length in days */
    function licence_trial_days()
    {
        return 7;
    }
}

if (!function_exists('licence_trial_max_bills')) {
    /** @return int server-only max invoices allowed on a trial/Demo licence */
    function licence_trial_max_bills()
    {
        return 50;
    }
}

if (!function_exists('licence_is_trial')) {
    /**
     * True only for real short free trials.
     * Grandfathers old dealer "Demo" rows that were actually paid / long-validity licences
     * so existing shops are not blocked by trial registration / 7-day / 50-bill gates.
     *
     * @param array|null $licenseRow
     * @return bool
     */
    function licence_is_trial($licenseRow)
    {
        if (!is_array($licenseRow)) {
            return false;
        }
        $type = isset($licenseRow['licenseType']) ? trim($licenseRow['licenseType']) : '';
        if (strcasecmp($type, 'Demo') !== 0 && strcasecmp($type, 'Trial') !== 0) {
            return false;
        }
        // Old paid / long Demo licences (common before P4 trial rules)
        $validity = isset($licenseRow['licenseValidity']) ? (int) $licenseRow['licenseValidity'] : 0;
        if ($validity > licence_trial_days()) {
            return false;
        }
        $amount = isset($licenseRow['amount']) ? (int) $licenseRow['amount'] : 0;
        if ($amount > 0) {
            return false;
        }
        $payment = isset($licenseRow['paymentStatus']) ? trim((string) $licenseRow['paymentStatus']) : '';
        if ($payment !== '' && strcasecmp($payment, 'free') !== 0) {
            return false;
        }
        return true;
    }
}

if (!function_exists('licence_apply_trial_validity')) {
    /**
     * Forces Demo/Trial licences to the server trial day count.
     *
     * @param string $licenseType
     * @param string|int $licenseValidity
     * @return string|int
     */
    function licence_apply_trial_validity($licenseType, $licenseValidity)
    {
        $licenseValidity = licence_normalize_validity_days($licenseValidity);
        if (strcasecmp(trim((string) $licenseType), 'Demo') === 0
            || strcasecmp(trim((string) $licenseType), 'Trial') === 0) {
            return (string) licence_trial_days();
        }
        return $licenseValidity;
    }
}

if (!function_exists('licence_count_bills')) {
    /**
     * @param mysqli $con
     * @param string|int $licenceId licenses.id (POS userId / licenseId on invoice)
     * @return int
     */
    function licence_count_bills($con, $licenceId)
    {
        if ($licenceId === null || $licenceId === '') {
            return 0;
        }
        return db_stmt_scalar_int(
            $con,
            'SELECT COUNT(*) AS billCount FROM `invoice` WHERE `licenseId` = ?',
            's',
            (string) $licenceId
        );
    }
}

if (!function_exists('licence_trial_allows_new_bill')) {
    /**
     * Regular licences always allowed. Trial blocked at max bill count.
     *
     * @param mysqli $con
     * @param array $licenseRow
     * @return bool
     */
    function licence_trial_allows_new_bill($con, $licenseRow)
    {
        if (!licence_is_trial($licenseRow)) {
            return true;
        }
        $id = isset($licenseRow['id']) ? $licenseRow['id'] : null;
        return licence_count_bills($con, $id) < licence_trial_max_bills();
    }
}

if (!function_exists('licence_append_trial_response')) {
    /**
     * Adds trial metadata to login/check responses (APK must not hard-code limits).
     *
     * @param mysqli $con
     * @param array $response
     * @param array $licenseRow
     * @return array
     */
    function licence_append_trial_response($con, $response, $licenseRow)
    {
        if (!is_array($response)) {
            $response = array();
        }
        $isTrial = licence_is_trial($licenseRow);
        $licenceId = isset($licenseRow['id']) ? $licenseRow['id'] : '';
        $billCount = licence_count_bills($con, $licenceId);

        $response['licenseType'] = isset($licenseRow['licenseType']) ? $licenseRow['licenseType'] : '';
        $response['isTrial'] = $isTrial ? '1' : '0';
        $response['trialDays'] = (string) licence_trial_days();
        $response['trialMaxBills'] = (string) licence_trial_max_bills();
        $response['trialBillCount'] = (string) $billCount;
        $response['trialBillsRemaining'] = $isTrial
            ? (string) max(0, licence_trial_max_bills() - $billCount)
            : '';
        $response['trialConsumed'] = licence_is_trial_consumed($licenseRow) ? '1' : '0';

        $branch = licence_branch_fields($licenseRow);
        $response['organizationId'] = isset($licenseRow['userId']) ? (string) $licenseRow['userId'] : '';
        $response['branchId'] = $licenceId !== '' ? (string) $licenceId : '';
        $response['branchLabel'] = $branch['branchLabel'];

        return $response;
    }
}

if (!function_exists('licence_is_date_valid')) {
    /**
     * @param string|null $expiryDate Y-m-d from licenses.expiryDate
     * @param string|null $today      Y-m-d; defaults to Asia/Kolkata today
     * @return bool true when licence is still within validity window
     */
    function licence_is_date_valid($expiryDate, $today = null)
    {
        if ($today === null) {
            $today = licence_today();
        }
        if ($expiryDate === null || $expiryDate === '' || $expiryDate === '0000-00-00') {
            return false;
        }
        return strtotime($expiryDate) >= strtotime($today);
    }
}

if (!function_exists('licence_mark_expired')) {
    /**
     * @param mysqli $con
     * @param string|int $licenceId
     */
    function licence_mark_expired($con, $licenceId)
    {
        if ($licenceId === null || $licenceId === '') {
            return;
        }
        db_stmt_execute(
            $con,
            "UPDATE `licenses` SET `licenseValidity`='0', `licenseStatus`='expire' WHERE `id` = ?",
            's',
            (string) $licenceId
        );
    }
}

if (!function_exists('licence_enforce_expiry')) {
    /**
     * @param mysqli $con
     * @param array $licenseRow
     * @return bool
     */
    function licence_enforce_expiry($con, $licenseRow)
    {
        if (!is_array($licenseRow)) {
            return false;
        }
        $expiryDate = isset($licenseRow['expiryDate']) ? $licenseRow['expiryDate'] : null;
        if (licence_is_date_valid($expiryDate)) {
            return true;
        }
        if (isset($licenseRow['id'])) {
            licence_mark_expired($con, $licenseRow['id']);
        }
        return false;
    }
}

if (!function_exists('licence_load_by_id')) {
    /**
     * @param mysqli $con
     * @param string|int $licenceId
     * @return array|null
     */
    function licence_load_by_id($con, $licenceId)
    {
        if ($licenceId === null || $licenceId === '') {
            return null;
        }
        return db_stmt_fetch_one(
            $con,
            'SELECT * FROM `licenses` WHERE `id` = ? LIMIT 1',
            's',
            (string) $licenceId
        );
    }
}

if (!function_exists('licence_renewal_base_date')) {
    /**
     * P4-4: Renew from remaining paid window when still active; otherwise from today.
     *
     * @param string|null $currentExpiryDate
     * @param string|null $today
     * @return string Y-m-d
     */
    function licence_renewal_base_date($currentExpiryDate, $today = null)
    {
        if ($today === null) {
            $today = licence_today();
        }
        if ($currentExpiryDate !== null
            && $currentExpiryDate !== ''
            && $currentExpiryDate !== '0000-00-00'
            && strtotime($currentExpiryDate) >= strtotime($today)) {
            return $currentExpiryDate;
        }
        return $today;
    }
}

if (!function_exists('licence_compute_renewal_expiry')) {
    /**
     * @param string|null $currentExpiryDate
     * @param string|int $validityDays
     * @param string|null $today
     * @return string|null Y-m-d
     */
    function licence_compute_renewal_expiry($currentExpiryDate, $validityDays, $today = null)
    {
        $days = (int) licence_normalize_validity_days($validityDays);
        if ($days <= 0) {
            return null;
        }
        $base = licence_renewal_base_date($currentExpiryDate, $today);
        return date('Y-m-d', strtotime($base . ' +' . $days . ' day'));
    }
}

if (!function_exists('licence_same_key_upgrade')) {
    /**
     * P4-4: Upgrade/renew an existing licence row without rotating the key or clearing device bind.
     * Never updates licenseKey, android_device_id, android_device_name, or mpin.
     *
     * @param mysqli $con
     * @param string|int $licensesId
     * @param string|int $licenseValidity
     * @param string $licenseType
     * @param string|int $amount
     * @return array response with status, message, licenseKey, expiryDate, deviceBound
     */
    function licence_same_key_upgrade($con, $licensesId, $licenseValidity, $licenseType, $amount)
    {
        $response = array(
            'status' => '0',
            'message' => 'update failed...',
            'licenseKey' => '',
            'expiryDate' => '',
            'deviceBound' => '0',
        );

        $row = licence_load_by_id($con, $licensesId);
        if ($row === null) {
            $response['message'] = 'Licence not found';
            return $response;
        }

        $licenseValidity = licence_apply_trial_validity($licenseType, $licenseValidity);
        $expiryDate = licence_compute_renewal_expiry(
            isset($row['expiryDate']) ? $row['expiryDate'] : null,
            $licenseValidity
        );
        if ($expiryDate === null) {
            $response['message'] = 'Invalid licence validity';
            return $response;
        }

        $licenseStatus = licence_is_date_valid($expiryDate) ? 'active' : 'expire';
        $paymentStatus = (strcasecmp(trim((string) $licenseType), 'Demo') === 0
            || strcasecmp(trim((string) $licenseType), 'Trial') === 0)
            ? ''
            : (isset($row['paymentStatus']) && $row['paymentStatus'] !== '' ? $row['paymentStatus'] : 'cash');

        $licensesIdEsc = mysqli_real_escape_string($con, (string) $licensesId);
        $licenseValidityEsc = mysqli_real_escape_string($con, (string) $licenseValidity);
        $licenseTypeEsc = mysqli_real_escape_string($con, (string) $licenseType);
        $amountEsc = mysqli_real_escape_string($con, (string) $amount);
        $expiryDateEsc = mysqli_real_escape_string($con, (string) $expiryDate);
        $licenseStatusEsc = mysqli_real_escape_string($con, (string) $licenseStatus);
        $paymentStatusEsc = mysqli_real_escape_string($con, (string) $paymentStatus);

        // Clear trialConsumed when upgrading off trial so old Demo→Regular shops can log in
        $clearTrial = '';
        if (strcasecmp(trim((string) $licenseType), 'Demo') !== 0
            && strcasecmp(trim((string) $licenseType), 'Trial') !== 0) {
            $clearTrial = ', `trialConsumed`=0';
        }

        // Explicit column list — never touch licenseKey / android_device_* / mpin
        $updated = db_stmt_execute(
            $con,
            'UPDATE `licenses` SET `licenseValidity`=?, `licenseType`=?, `amount`=?, `expiryDate`=?, `licenseStatus`=?, `paymentStatus`=?' . $clearTrial . ' WHERE `id`=?',
            'sssssss',
            $licenseValidityEsc,
            $licenseTypeEsc,
            $amountEsc,
            $expiryDateEsc,
            $licenseStatusEsc,
            $paymentStatusEsc,
            $licensesIdEsc
        );

        if (!$updated) {
            $response['message'] = 'update failed...';
            return $response;
        }

        $deviceBound = isset($row['android_device_id']) && $row['android_device_id'] !== null && $row['android_device_id'] !== '';
        $licenseKey = isset($row['licenseKey']) ? $row['licenseKey'] : '';

        $response['status'] = '1';
        $response['licenseKey'] = $licenseKey;
        $response['expiryDate'] = $expiryDate;
        $response['deviceBound'] = $deviceBound ? '1' : '0';
        $response['message'] = $deviceBound
            ? "Licence renewed. Same key kept — device stays bound. New expiry: $expiryDate"
            : "Licence renewed. Same key kept (not yet bound to a device). New expiry: $expiryDate";

        return $response;
    }
}

if (!function_exists('licence_branch_label')) {
    /**
     * P4-5: Human-readable branch label for owner vs franchise licences.
     *
     * @param string|null $userType owner|franchise
     * @param string|null $userName branch/store name on licenses.userName
     * @return string
     */
    function licence_branch_label($userType, $userName)
    {
        if (strcasecmp(trim((string) $userType), 'owner') === 0) {
            return 'Main Store';
        }
        $name = trim((string) $userName);
        return $name !== '' ? 'Franchise: ' . $name : 'Franchise Branch';
    }
}

if (!function_exists('licence_format_currency_name')) {
    /** @param string|null $currencyName */
    function licence_format_currency_name($currencyName)
    {
        if ($currencyName === 'Dinar: Ø¯.Ùƒ') {
            return 'د.ك';
        }
        if ($currencyName === 'Rupee: â‚¹') {
            return '₹';
        }
        if ($currencyName === 'Cent: Â¢') {
            return '¢';
        }
        if ($currencyName === 'Pound: Â£') {
            return '£';
        }
        if ($currencyName === 'Yen: Â¥') {
            return '¥';
        }
        if ($currencyName === 'French Franc: â‚£') {
            return '₣';
        }
        if ($currencyName === 'Euro: â‚¬') {
            return '€';
        }
        return '₹';
    }
}

if (!function_exists('licence_branch_fields')) {
    /**
     * @param array $licenseRow licenses (+ optional companys join) row
     * @return array userType, userName, branchLabel
     */
    function licence_branch_fields($licenseRow)
    {
        $userType = isset($licenseRow['userType']) ? $licenseRow['userType'] : '';
        $userName = isset($licenseRow['userName']) ? $licenseRow['userName'] : '';
        return array(
            'userType' => $userType,
            'userName' => $userName,
            'branchLabel' => licence_branch_label($userType, $userName),
        );
    }
}

if (!function_exists('licence_store_sales')) {
    /**
     * @param mysqli $con
     * @param string|int $licenseId
     * @param string|null $today Y-m-d
     * @return array totalSale, todaySale
     */
    function licence_store_sales($con, $licenseId, $today = null)
    {
        if ($today === null) {
            $today = licence_today();
        }

        $totalSale = db_stmt_scalar_string(
            $con,
            'SELECT SUM(`totalAmount`) AS `totalSale` FROM `invoice` WHERE `licenseId` = ?',
            's',
            (string) $licenseId
        );
        $todaySale = db_stmt_scalar_string(
            $con,
            'SELECT SUM(`totalAmount`) AS `todaySale` FROM `invoice` WHERE `licenseId` = ? AND `invoiceDate` LIKE CONCAT(\'%\', ?, \'%\')',
            'ss',
            (string) $licenseId,
            (string) $today
        );

        return array(
            'totalSale' => ($totalSale !== null && $totalSale !== '') ? $totalSale : '0',
            'todaySale' => ($todaySale !== null && $todaySale !== '') ? $todaySale : '0',
        );
    }
}

if (!function_exists('licence_normalize_contact')) {
    /**
     * Strip non-digits; keep last 10 digits for Indian mobile numbers.
     *
     * @param string|null $contact
     * @return string
     */
    function licence_normalize_contact($contact)
    {
        $digits = preg_replace('/\D+/', '', (string) $contact);
        if (strlen($digits) > 10) {
            $digits = substr($digits, -10);
        }
        return $digits;
    }
}

if (!function_exists('licence_generate_unique_key')) {
    /**
     * Secure unique licence key. Format: BW-XXXX-XXXX-XXXX (uppercase alnum).
     * Legacy callers may pass $length; when $length <= 12 the BW format is used.
     *
     * @param mysqli $con
     * @param int $length unused for BW format (kept for backward compatibility)
     * @return string|null
     */
    function licence_generate_unique_key($con, $length = 10)
    {
        $chars = '0123456789ABCDEFGHJKLMNPQRSTUVWXYZ'; // no I/O to reduce confusion
        $maxIndex = strlen($chars) - 1;
        for ($attempt = 0; $attempt < 40; $attempt++) {
            $parts = array();
            for ($p = 0; $p < 3; $p++) {
                $segment = '';
                for ($i = 0; $i < 4; $i++) {
                    $segment .= $chars[random_int(0, $maxIndex)];
                }
                $parts[] = $segment;
            }
            $key = 'BW-' . implode('-', $parts);
            $exists = db_stmt_scalar_int(
                $con,
                'SELECT COUNT(*) AS c FROM `licenses` WHERE `licenseKey`=?',
                's',
                $key
            );
            if ($exists < 1) {
                return $key;
            }
        }
        return null;
    }
}

if (!function_exists('licence_contact_has_trial')) {
    /**
     * True when this mobile number already has a Demo/Trial customer licence.
     *
     * @param mysqli $con
     * @param string $contactDigits normalized 10-digit contact
     * @return bool
     */
    function licence_contact_has_trial($con, $contactDigits)
    {
        if ($contactDigits === '') {
            return false;
        }
        $count = db_stmt_scalar_int(
            $con,
            "SELECT COUNT(*) AS c FROM `users` u
             INNER JOIN `licenses` l ON l.userId = u.id
             WHERE u.role_id='3'
               AND (l.licenseType='Demo' OR l.licenseType='Trial')
               AND (
                 u.contact_number = ?
                 OR u.contact_number LIKE CONCAT('%', ?)
               )",
            'ss',
            $contactDigits,
            $contactDigits
        );
        return $count > 0;
    }
}

if (!function_exists('licence_register_trial_customer')) {
    /**
     * Self-service POS signup: create customer user + 7-day Demo licence.
     * Trial clock still starts on first device bind (licence_on_device_bind).
     *
     * @param mysqli $con
     * @param string $name
     * @param string $contactNumber
     * @param string $address
     * @param string $shopName
     * @return array status, message, licenceKey, licenceId, trialDays, trialMaxBills
     */
    function licence_register_trial_customer($con, $name, $contactNumber, $address, $shopName)
    {
        $response = array(
            'status' => '0',
            'message' => 'We could not complete your registration. Please try again.',
            'licenceKey' => '',
            'licenceId' => '',
            'trialDays' => (string) licence_trial_days(),
            'trialMaxBills' => (string) licence_trial_max_bills(),
        );

        $name = trim((string) $name);
        $address = trim((string) $address);
        $shopName = trim((string) $shopName);
        $contactDigits = licence_normalize_contact($contactNumber);

        if ($name === '' || $address === '' || $shopName === '') {
            $response['message'] = 'Please enter your name, mobile number, shop name, and address.';
            return $response;
        }
        if (strlen($contactDigits) < 10) {
            $response['message'] = 'Please enter a valid 10-digit mobile number.';
            return $response;
        }

        if (licence_contact_has_trial($con, $contactDigits)) {
            $response['message'] = 'This mobile number is already registered. Please log in with your licence key, or call +91 89831 49299 for help.';
            return $response;
        }

        $licenseKey = licence_generate_unique_key($con, 10);
        if ($licenseKey === null) {
            $response['message'] = 'Something went wrong while creating your licence. Please try again.';
            return $response;
        }

        $licenseType = 'Demo';
        $licenseValidity = licence_apply_trial_validity($licenseType, licence_trial_days());
        $today = licence_today();
        $expiryDate = date('Y-m-d', strtotime($today . ' +' . (int) $licenseValidity . ' day'));
        $contactStored = $contactDigits;
        $mpin = '';

        $customerId = db_stmt_insert_id(
            $con,
            "INSERT INTO `users`(`role_id`, `name`, `contact_number`, `address`, `is_active`, `shopName`)
             VALUES ('3', ?, ?, ?, '1', ?)",
            'ssss',
            $name,
            $contactStored,
            $address,
            $shopName
        );
        if ($customerId === false || (int) $customerId < 1) {
            $response['message'] = 'We could not create your account. Please try again.';
            return $response;
        }

        $licenceId = db_stmt_insert_id(
            $con,
            "INSERT INTO `licenses`(
                `userId`, `licenseKey`, `licenseValidity`, `licenseType`, `licenseStatus`,
                `expiryDate`, `paymentStatus`, `amount`, `userType`, `userName`, `mpin`,
                `fastBilling`, `takeAway`, `dineIn`, `mess`
             ) VALUES (?, ?, ?, ?, 'active', ?, '', '0', 'owner', ?, ?, '1', '1', '1', '0')",
            'issssss',
            (string) $customerId,
            $licenseKey,
            (string) $licenseValidity,
            $licenseType,
            $expiryDate,
            $name,
            $mpin
        );
        if ($licenceId === false || (int) $licenceId < 1) {
            db_stmt_execute($con, 'DELETE FROM `users` WHERE `id`=?', 's', (string) $customerId);
            $response['message'] = 'We could not activate your free account. Please try again.';
            return $response;
        }

        $response['status'] = '1';
        $response['message'] = 'Your free account is ready! Log in with your licence key to start using the app.';
        $response['licenceKey'] = $licenseKey;
        $response['licenceId'] = (string) $licenceId;
        return $response;
    }
}
?>
