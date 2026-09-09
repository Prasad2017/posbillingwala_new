<?php
/**
 * Shared mess member / payment helpers for Owner, Dealer & Admin remote management.
 * PHP 7.0+ safe. Mess userId = licences.id (licence id).
 */

require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../mess_common_helpers.php';
require_once __DIR__ . '/../catalog/CatalogSimpleXlsx.php';
require_once __DIR__ . '/../catalog/catalog_helpers.php';

if (!function_exists('mess_remote_resolve_licence_ids_for_owner')) {
    /**
     * @return int[]
     */
    function mess_remote_resolve_licence_ids_for_owner($con, $ownerUserId)
    {
        $ownerUserId = (int) $ownerUserId;
        if ($ownerUserId <= 0) {
            return array();
        }
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT id FROM licenses WHERE userId = ?',
            'i',
            $ownerUserId
        );
        $ids = array();
        foreach ($rows as $row) {
            $ids[] = (int) $row['id'];
        }
        return array_values(array_unique($ids));
    }
}

if (!function_exists('mess_remote_resolve_licence_ids_for_dealer_customer')) {
    /**
     * Verify customer belongs to dealer, then return that customer's licence ids.
     * @return int[]|null null when customer not owned by dealer
     */
    function mess_remote_resolve_licence_ids_for_dealer_customer($con, $dealerId, $customerId)
    {
        $dealerId = (int) $dealerId;
        $customerId = (int) $customerId;
        if ($dealerId <= 0 || $customerId <= 0) {
            return null;
        }
        $user = db_stmt_fetch_one(
            $con,
            "SELECT id, dealerId, role_id FROM users WHERE id = ? LIMIT 1",
            'i',
            $customerId
        );
        if ($user === null) {
            return null;
        }
        if ((int) $user['dealerId'] !== $dealerId) {
            return null;
        }
        // role_id 3 = customer
        $role = isset($user['role_id']) ? (string) $user['role_id'] : '';
        if ($role !== '3') {
            return null;
        }

        $rows = db_stmt_fetch_all(
            $con,
            'SELECT id FROM licenses WHERE userId = ?',
            'i',
            $customerId
        );
        $ids = array();
        foreach ($rows as $row) {
            $ids[] = (int) $row['id'];
        }
        return array_values(array_unique($ids));
    }
}


if (!function_exists('mess_remote_resolve_licence_ids_for_admin_customer')) {
    /**
     * Admin: any customer (role_id 3); return that customer's licence ids.
     * @return int[]|null null when customer not found / not a customer
     */
    function mess_remote_resolve_licence_ids_for_admin_customer($con, $customerId)
    {
        $customerId = (int) $customerId;
        if ($customerId <= 0) {
            return null;
        }
        $user = db_stmt_fetch_one(
            $con,
            "SELECT id, role_id FROM users WHERE id = ? LIMIT 1",
            'i',
            $customerId
        );
        if ($user === null) {
            return null;
        }
        $role = isset($user['role_id']) ? (string) $user['role_id'] : '';
        if ($role !== '3') {
            return null;
        }
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT id FROM licenses WHERE userId = ?',
            'i',
            $customerId
        );
        $ids = array();
        foreach ($rows as $row) {
            $ids[] = (int) $row['id'];
        }
        return array_values(array_unique($ids));
    }
}

if (!function_exists('mess_remote_assert_licence_access')) {
    /**
     * @param int[] $allowedLicenceIds
     * @return bool
     */
    function mess_remote_assert_licence_access(array $allowedLicenceIds, $licenceId)
    {
        $licenceId = (int) $licenceId;
        if ($licenceId <= 0) {
            return false;
        }
        foreach ($allowedLicenceIds as $id) {
            if ((int) $id === $licenceId) {
                return true;
            }
        }
        return false;
    }
}

if (!function_exists('mess_remote_member_row_to_api')) {
    function mess_remote_member_row_to_api(array $row)
    {
        return array(
            'memberId' => (string) $row['id'],
            'licenceId' => (string) $row['userId'],
            'memberName' => isset($row['member_name']) ? $row['member_name'] : '',
            'memberMobileNumber' => isset($row['member_mobile_number']) ? $row['member_mobile_number'] : '',
            'memberAltenetMobileNumber' => isset($row['member_altenet_mobile_number']) ? $row['member_altenet_mobile_number'] : '',
            'memberAlternetMobileNumber' => isset($row['member_altenet_mobile_number']) ? $row['member_altenet_mobile_number'] : '',
            'memberAddress' => isset($row['member_address']) ? $row['member_address'] : '',
            'registrationNo' => isset($row['registration_no']) ? $row['registration_no'] : '',
            'memberType' => isset($row['member_type']) && $row['member_type'] !== '' ? $row['member_type'] : 'student',
            'rollNo' => isset($row['roll_no']) ? $row['roll_no'] : '',
            'college' => isset($row['college']) ? $row['college'] : '',
            'studentYear' => isset($row['student_year']) ? $row['student_year'] : '',
            'company' => isset($row['company']) ? $row['company'] : '',
            'memberStatus' => isset($row['member_status']) ? $row['member_status'] : 'active',
            'memberNetworkStatus' => isset($row['member_network_status']) ? $row['member_network_status'] : '',
        );
    }
}

if (!function_exists('mess_remote_payment_row_to_api')) {
    function mess_remote_payment_row_to_api(array $row)
    {
        return array(
            'paymentId' => (string) $row['payment_id'],
            'licenceId' => (string) $row['userId'],
            'memberId' => isset($row['memberId']) ? (string) $row['memberId'] : '',
            'memberName' => isset($row['memberName']) ? $row['memberName'] : '',
            'paymentMessAmount' => isset($row['paymentMessAmount']) ? $row['paymentMessAmount'] : '',
            'paymentPaidAmount' => isset($row['paymentPaidAmount']) ? $row['paymentPaidAmount'] : '',
            'messTotalDays' => isset($row['messTotalDays']) ? $row['messTotalDays'] : '',
            'paymentDate' => isset($row['paymentDate']) ? $row['paymentDate'] : '',
            'paymentNetworkStatus' => isset($row['paymentNetworkStatus']) ? $row['paymentNetworkStatus'] : '',
            'paymentStatus' => isset($row['paymentStatus']) ? $row['paymentStatus'] : 'active',
        );
    }
}

if (!function_exists('mess_remote_list_members')) {
    /**
     * @param int[] $licenceIds
     * @return array
     */
    function mess_remote_list_members($con, array $licenceIds)
    {
        mess_common_ensure_schema($con);
        $licenceIds = array_values(array_filter(array_map('intval', $licenceIds)));
        if (empty($licenceIds)) {
            return array();
        }
        $placeholders = implode(',', array_fill(0, count($licenceIds), '?'));
        $types = str_repeat('i', count($licenceIds));
        $rows = db_stmt_fetch_all(
            $con,
            "SELECT * FROM mess_member WHERE userId IN ($placeholders) ORDER BY id DESC",
            $types,
            ...$licenceIds
        );
        $out = array();
        foreach ($rows as $row) {
            $out[] = mess_remote_member_row_to_api($row);
        }
        return $out;
    }
}

if (!function_exists('mess_remote_normalize_member_type')) {
    function mess_remote_normalize_member_type($raw)
    {
        $t = strtolower(trim((string) $raw));
        return ($t === 'working') ? 'working' : 'student';
    }
}

if (!function_exists('mess_remote_default_registration')) {
    function mess_remote_default_registration($registrationNo, $memberType, $rollNo, $mobile)
    {
        $registrationNo = mess_normalize_registration($registrationNo);
        if ($registrationNo === '' && $memberType === 'student' && trim((string) $rollNo) !== '') {
            $registrationNo = mess_normalize_registration($rollNo);
        }
        if ($registrationNo === '') {
            $registrationNo = mess_normalize_registration($mobile);
        }
        if ($registrationNo === '') {
            $registrationNo = mess_normalize_mobile($mobile);
        }
        return $registrationNo;
    }
}

if (!function_exists('mess_remote_save_member')) {
    /**
     * Upsert member for a licence.
     * @return array{status:string,message:string,memberId?:string,registrationNo?:string}
     */
    function mess_remote_save_member($con, $licenceId, array $fields)
    {
        mess_common_ensure_schema($con);
        $licenceId = (int) $licenceId;
        if ($licenceId <= 0) {
            return array('status' => '0', 'message' => 'Invalid licenceId');
        }

        $memberName = isset($fields['memberName']) ? trim((string) $fields['memberName']) : '';
        $memberMobileNumber = isset($fields['memberMobileNumber']) ? trim((string) $fields['memberMobileNumber']) : '';
        $memberAltenetMobileNumber = isset($fields['memberAltenetMobileNumber'])
            ? trim((string) $fields['memberAltenetMobileNumber'])
            : (isset($fields['memberAlternetMobileNumber']) ? trim((string) $fields['memberAlternetMobileNumber']) : '');
        $memberAddress = isset($fields['memberAddress']) ? trim((string) $fields['memberAddress']) : '';
        $memberStatus = isset($fields['memberStatus']) ? trim((string) $fields['memberStatus']) : 'active';
        if ($memberStatus === '') {
            $memberStatus = 'active';
        }
        $memberType = mess_remote_normalize_member_type(isset($fields['memberType']) ? $fields['memberType'] : 'student');
        $rollNo = isset($fields['rollNo']) ? trim((string) $fields['rollNo']) : '';
        $college = isset($fields['college']) ? trim((string) $fields['college']) : '';
        $studentYear = isset($fields['studentYear']) ? trim((string) $fields['studentYear']) : '';
        $company = isset($fields['company']) ? trim((string) $fields['company']) : '';
        $registrationNo = mess_remote_default_registration(
            isset($fields['registrationNo']) ? $fields['registrationNo'] : '',
            $memberType,
            $rollNo,
            $memberMobileNumber
        );

        if ($memberName === '' || $memberMobileNumber === '') {
            return array('status' => '0', 'message' => 'Name and Mobile are required');
        }

        $memberId = isset($fields['memberId']) ? (int) $fields['memberId'] : 0;
        $networkStatus = isset($fields['memberNetworkStatus']) ? trim((string) $fields['memberNetworkStatus']) : '';

        $existing = null;
        if ($memberId > 0) {
            $existing = db_stmt_fetch_one(
                $con,
                'SELECT * FROM mess_member WHERE id = ? AND userId = ? LIMIT 1',
                'ii',
                $memberId,
                $licenceId
            );
        }
        if ($existing === null && $networkStatus !== '') {
            $existing = db_stmt_fetch_one(
                $con,
                'SELECT * FROM mess_member WHERE userId = ? AND member_network_status = ? LIMIT 1',
                'is',
                $licenceId,
                $networkStatus
            );
        }

        if ($existing !== null) {
            $memberId = (int) $existing['id'];
            if ($registrationNo === '') {
                $registrationNo = !empty($existing['registration_no'])
                    ? mess_normalize_registration($existing['registration_no'])
                    : mess_normalize_mobile($memberMobileNumber);
            }
            if ($registrationNo === '') {
                $registrationNo = 'REG-' . str_pad((string) $memberId, 4, '0', STR_PAD_LEFT);
            }
            $ok = db_stmt_execute(
                $con,
                'UPDATE mess_member SET member_name = ?, member_mobile_number = ?, member_altenet_mobile_number = ?, member_address = ?, registration_no = ?, member_type = ?, roll_no = ?, college = ?, student_year = ?, company = ?, member_status = ? WHERE id = ? AND userId = ?',
                'sssssssssssii',
                $memberName,
                $memberMobileNumber,
                $memberAltenetMobileNumber,
                $memberAddress,
                $registrationNo,
                $memberType,
                $rollNo,
                $college,
                $studentYear,
                $company,
                $memberStatus,
                $memberId,
                $licenceId
            );
            return array(
                'status' => $ok ? '1' : '0',
                'message' => $ok ? 'update successful!' : 'update failed!',
                'memberId' => (string) $memberId,
                'registrationNo' => $registrationNo,
            );
        }

        if ($networkStatus === '') {
            $networkStatus = 'REMOTE-' . strtoupper(bin2hex(random_bytes(8)));
        }
        $ok = db_stmt_execute(
            $con,
            'INSERT INTO mess_member (userId, member_name, member_mobile_number, member_altenet_mobile_number, member_address, registration_no, member_type, roll_no, college, student_year, company, member_status, member_network_status)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
            'issssssssssss',
            $licenceId,
            $memberName,
            $memberMobileNumber,
            $memberAltenetMobileNumber,
            $memberAddress,
            $registrationNo !== '' ? $registrationNo : null,
            $memberType,
            $rollNo,
            $college,
            $studentYear,
            $company,
            $memberStatus !== '' ? $memberStatus : 'active',
            $networkStatus
        );
        if (!$ok) {
            return array('status' => '0', 'message' => 'insert failed!');
        }
        $memberId = (int) mysqli_insert_id($con);
        if ($registrationNo === '') {
            $registrationNo = mess_normalize_mobile($memberMobileNumber);
            if ($registrationNo === '') {
                $registrationNo = 'REG-' . str_pad((string) $memberId, 4, '0', STR_PAD_LEFT);
            }
            db_stmt_execute(
                $con,
                'UPDATE mess_member SET registration_no = ? WHERE id = ?',
                'si',
                $registrationNo,
                $memberId
            );
        }
        return array(
            'status' => '1',
            'message' => 'insert successful!',
            'memberId' => (string) $memberId,
            'registrationNo' => $registrationNo,
            'memberNetworkStatus' => $networkStatus,
        );
    }
}

if (!function_exists('mess_remote_list_payments')) {
    /**
     * @param int[] $licenceIds
     * @param int|null $memberId
     */
    function mess_remote_list_payments($con, array $licenceIds, $memberId = null)
    {
        mess_common_ensure_schema($con);
        $licenceIds = array_values(array_filter(array_map('intval', $licenceIds)));
        if (empty($licenceIds)) {
            return array();
        }
        $placeholders = implode(',', array_fill(0, count($licenceIds), '?'));
        $types = str_repeat('i', count($licenceIds));
        $params = $licenceIds;
        $sql = "SELECT * FROM mess_member_payment WHERE userId IN ($placeholders)";
        if ($memberId !== null && (int) $memberId > 0) {
            $sql .= ' AND CAST(memberId AS CHAR) = ?';
            $types .= 's';
            $params[] = (string) ((int) $memberId);
        }
        $sql .= ' ORDER BY payment_id DESC';
        $rows = db_stmt_fetch_all($con, $sql, $types, ...$params);
        $out = array();
        foreach ($rows as $row) {
            $out[] = mess_remote_payment_row_to_api($row);
        }
        return $out;
    }
}

if (!function_exists('mess_remote_save_payment')) {
    /**
     * @return array{status:string,message:string,paymentId?:string}
     */
    function mess_remote_save_payment($con, $licenceId, array $fields)
    {
        mess_common_ensure_schema($con);
        $licenceId = (int) $licenceId;
        if ($licenceId <= 0) {
            return array('status' => '0', 'message' => 'Invalid licenceId');
        }

        $memberId = isset($fields['memberId']) ? trim((string) $fields['memberId']) : '';
        $memberName = isset($fields['memberName']) ? trim((string) $fields['memberName']) : '';
        $paymentMessAmount = isset($fields['paymentMessAmount']) ? trim((string) $fields['paymentMessAmount']) : '';
        $paymentPaidAmount = isset($fields['paymentPaidAmount']) ? trim((string) $fields['paymentPaidAmount']) : '';
        $messTotalDays = isset($fields['messTotalDays']) ? trim((string) $fields['messTotalDays']) : '';
        $paymentDate = isset($fields['paymentDate']) ? trim((string) $fields['paymentDate']) : '';
        $paymentStatus = isset($fields['paymentStatus']) ? trim((string) $fields['paymentStatus']) : 'active';
        $paymentNetworkStatus = isset($fields['paymentNetworkStatus']) ? trim((string) $fields['paymentNetworkStatus']) : '';
        $paymentId = isset($fields['paymentId']) ? (int) $fields['paymentId'] : 0;

        if ($paymentMessAmount === '' || $paymentPaidAmount === '') {
            return array('status' => '0', 'message' => 'Mess Amount and Paid Amount are required');
        }
        if ($paymentDate === '') {
            date_default_timezone_set('Asia/Kolkata');
            $paymentDate = date('Y-m-d');
        }
        if ($memberName === '' && $memberId !== '' && $memberId !== '0') {
            $m = db_stmt_fetch_one(
                $con,
                'SELECT member_name FROM mess_member WHERE id = ? AND userId = ? LIMIT 1',
                'ii',
                (int) $memberId,
                $licenceId
            );
            if ($m !== null) {
                $memberName = $m['member_name'];
            }
        }

        $existing = null;
        if ($paymentId > 0) {
            $existing = db_stmt_fetch_one(
                $con,
                'SELECT * FROM mess_member_payment WHERE payment_id = ? AND userId = ? LIMIT 1',
                'ii',
                $paymentId,
                $licenceId
            );
        }
        if ($existing === null && $paymentNetworkStatus !== '') {
            $existing = db_stmt_fetch_one(
                $con,
                'SELECT * FROM mess_member_payment WHERE userId = ? AND paymentNetworkStatus = ? LIMIT 1',
                'is',
                $licenceId,
                $paymentNetworkStatus
            );
        }

        if ($existing !== null) {
            $paymentId = (int) $existing['payment_id'];
            $ok = db_stmt_execute(
                $con,
                'UPDATE mess_member_payment SET memberId = ?, memberName = ?, paymentMessAmount = ?, paymentPaidAmount = ?, messTotalDays = ?, paymentDate = ?, paymentStatus = ? WHERE payment_id = ? AND userId = ?',
                'sssssssii',
                $memberId,
                $memberName,
                $paymentMessAmount,
                $paymentPaidAmount,
                $messTotalDays,
                $paymentDate,
                $paymentStatus !== '' ? $paymentStatus : 'active',
                $paymentId,
                $licenceId
            );
            return array(
                'status' => $ok ? '1' : '0',
                'message' => $ok ? 'update successful!' : 'update failed!',
                'paymentId' => (string) $paymentId,
            );
        }

        if ($paymentNetworkStatus === '') {
            $paymentNetworkStatus = 'REMOTE-PAY-' . strtoupper(bin2hex(random_bytes(8)));
        }
        $ok = db_stmt_execute(
            $con,
            'INSERT INTO mess_member_payment (userId, memberId, memberName, paymentMessAmount, paymentPaidAmount, messTotalDays, paymentDate, paymentNetworkStatus, paymentStatus)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
            'issssssss',
            $licenceId,
            $memberId,
            $memberName,
            $paymentMessAmount,
            $paymentPaidAmount,
            $messTotalDays,
            $paymentDate,
            $paymentNetworkStatus,
            $paymentStatus !== '' ? $paymentStatus : 'active'
        );
        if (!$ok) {
            return array('status' => '0', 'message' => 'insert failed!');
        }
        return array(
            'status' => '1',
            'message' => 'insert successful!',
            'paymentId' => (string) mysqli_insert_id($con),
            'paymentNetworkStatus' => $paymentNetworkStatus,
        );
    }
}

if (!function_exists('mess_remote_member_headers')) {
    function mess_remote_member_headers()
    {
        return array(
            'Name', 'Mobile', 'Alt Mobile', 'Address', 'Type', 'Roll No',
            'College', 'Year', 'Company', 'Registration No', 'Status',
        );
    }
}

if (!function_exists('mess_remote_payment_headers')) {
    function mess_remote_payment_headers()
    {
        return array(
            'Mobile', 'Member Name', 'Mess Amount', 'Paid Amount',
            'Mess Days', 'Payment Date', 'Status',
        );
    }
}

if (!function_exists('mess_remote_template_sheets')) {
    function mess_remote_template_sheets()
    {
        return array(
            'Members' => array(mess_remote_member_headers()),
            'Payments' => array(mess_remote_payment_headers()),
        );
    }
}

if (!function_exists('mess_remote_export_xlsx')) {
    /**
     * Write export workbook to temp file and return path, or stream if $stream=true.
     * @param int[] $licenceIds
     * @return string|void file path when not streaming
     */
    function mess_remote_export_xlsx($con, array $licenceIds, $stream = true, $downloadName = 'mess_members.xlsx')
    {
        mess_common_ensure_schema($con);
        $members = mess_remote_list_members($con, $licenceIds);
        $payments = mess_remote_list_payments($con, $licenceIds, null);

        // Map memberId -> mobile for payments sheet
        $mobileByMemberId = array();
        foreach ($members as $m) {
            $mobileByMemberId[$m['memberId']] = $m['memberMobileNumber'];
        }

        $memberRows = array(mess_remote_member_headers());
        foreach ($members as $m) {
            $memberRows[] = array(
                $m['memberName'],
                $m['memberMobileNumber'],
                $m['memberAltenetMobileNumber'],
                $m['memberAddress'],
                $m['memberType'],
                $m['rollNo'],
                $m['college'],
                $m['studentYear'],
                $m['company'],
                $m['registrationNo'],
                $m['memberStatus'],
            );
        }

        $paymentRows = array(mess_remote_payment_headers());
        foreach ($payments as $p) {
            $mobile = '';
            if (isset($mobileByMemberId[$p['memberId']])) {
                $mobile = $mobileByMemberId[$p['memberId']];
            }
            $paymentRows[] = array(
                $mobile,
                $p['memberName'],
                $p['paymentMessAmount'],
                $p['paymentPaidAmount'],
                $p['messTotalDays'],
                $p['paymentDate'],
                $p['paymentStatus'],
            );
        }

        $sheets = array(
            'Members' => $memberRows,
            'Payments' => $paymentRows,
        );

        if ($stream) {
            if (method_exists('CatalogSimpleXlsx', 'streamWorkbook')) {
                CatalogSimpleXlsx::streamWorkbook($sheets, $downloadName);
                return;
            }
            $tmp = tempnam(sys_get_temp_dir(), 'messxlsx');
            CatalogSimpleXlsx::writeWorkbook($sheets, $tmp);
            if (ob_get_length()) {
                ob_end_clean();
            }
            header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
            header('Content-Disposition: attachment; filename="' . catalog_sanitize_filename($downloadName) . '"');
            header('Content-Length: ' . filesize($tmp));
            readfile($tmp);
            @unlink($tmp);
            return;
        }

        $tmp = tempnam(sys_get_temp_dir(), 'messxlsx');
        CatalogSimpleXlsx::writeWorkbook($sheets, $tmp);
        return $tmp;
    }
}

if (!function_exists('mess_remote_find_member_by_mobile')) {
    function mess_remote_find_member_by_mobile($con, $licenceId, $mobile)
    {
        $mobile = mess_normalize_mobile($mobile);
        if ($mobile === '') {
            return null;
        }
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM mess_member WHERE userId = ?',
            'i',
            (int) $licenceId
        );
        foreach ($rows as $row) {
            $m1 = mess_normalize_mobile(isset($row['member_mobile_number']) ? $row['member_mobile_number'] : '');
            $m2 = mess_normalize_mobile(isset($row['member_altenet_mobile_number']) ? $row['member_altenet_mobile_number'] : '');
            if ($m1 === $mobile || $m2 === $mobile) {
                return $row;
            }
        }
        return null;
    }
}

if (!function_exists('mess_remote_import_xlsx')) {
    /**
     * Import Members then Payments for a single licence.
     * @return array
     */
    function mess_remote_import_xlsx($con, $licenceId, $filePath)
    {
        mess_common_ensure_schema($con);
        $licenceId = (int) $licenceId;
        $result = array(
            'status' => '0',
            'message' => '',
            'membersImported' => 0,
            'membersUpdated' => 0,
            'paymentsImported' => 0,
            'errors' => array(),
        );

        if ($licenceId <= 0 || !is_file($filePath)) {
            $result['message'] = 'Invalid licence or file';
            return $result;
        }

        try {
            $memberSheet = CatalogSimpleXlsx::readImportRows($filePath, array('Members', 'members', 'Member'));
        } catch (Exception $e) {
            $result['message'] = 'Unable to read Members sheet: ' . $e->getMessage();
            return $result;
        }

        // Normalize header labels (trim + collapse spaces) for robust matching
        $normalizedHeaders = array();
        foreach ($memberSheet['headers'] as $colIndex => $headerName) {
            $label = trim(preg_replace('/\s+/', ' ', (string) $headerName));
            $normalizedHeaders[$colIndex] = $label;
            $memberSheet['headers'][$colIndex] = $label;
        }
        // Remap row keys to normalized header names
        foreach ($memberSheet['rows'] as $excelRow => $assoc) {
            $mapped = array();
            foreach ($assoc as $key => $val) {
                $mapped[trim(preg_replace('/\s+/', ' ', (string) $key))] = $val;
            }
            $memberSheet['rows'][$excelRow] = $mapped;
        }

        $requiredMemberHeaders = array('Name', 'Mobile');
        foreach ($requiredMemberHeaders as $h) {
            if (!in_array($h, $normalizedHeaders, true)) {
                $result['errors'][] = array('row' => 1, 'sheet' => 'Members', 'message' => "Missing header: $h");
            }
        }
        if (!empty($result['errors'])) {
            $found = array_values(array_filter($normalizedHeaders, function ($h) {
                return $h !== '';
            }));
            $result['message'] = 'Invalid Members sheet headers. Found: '
                . (empty($found) ? '(none — file may use unsupported Excel format)' : implode(', ', $found))
                . '. Required: Name, Mobile';
            return $result;
        }

        foreach ($memberSheet['rows'] as $excelRow => $assoc) {
            $name = isset($assoc['Name']) ? trim($assoc['Name']) : '';
            $mobileRaw = isset($assoc['Mobile']) ? trim($assoc['Mobile']) : '';
            $mobile = mess_normalize_mobile($mobileRaw);
            if ($name === '' || $mobile === '') {
                $result['errors'][] = array('row' => $excelRow, 'sheet' => 'Members', 'message' => 'Name and Mobile required');
                continue;
            }
            if (strlen($mobile) !== 10) {
                $result['errors'][] = array('row' => $excelRow, 'sheet' => 'Members', 'message' => 'Mobile must be 10 digits');
                continue;
            }

            $typeRaw = isset($assoc['Type']) ? $assoc['Type'] : 'student';
            $memberType = mess_remote_normalize_member_type($typeRaw);
            $rollNo = isset($assoc['Roll No']) ? trim($assoc['Roll No']) : '';
            $reg = isset($assoc['Registration No']) ? trim($assoc['Registration No']) : '';
            $status = isset($assoc['Status']) ? trim($assoc['Status']) : 'active';
            if ($status === '') {
                $status = 'active';
            }

            $existing = mess_remote_find_member_by_mobile($con, $licenceId, $mobile);
            $fields = array(
                'memberName' => $name,
                'memberMobileNumber' => $mobile,
                'memberAltenetMobileNumber' => isset($assoc['Alt Mobile']) ? mess_normalize_mobile($assoc['Alt Mobile']) : '',
                'memberAddress' => isset($assoc['Address']) ? trim($assoc['Address']) : '',
                'memberType' => $memberType,
                'rollNo' => $rollNo,
                'college' => isset($assoc['College']) ? trim($assoc['College']) : '',
                'studentYear' => isset($assoc['Year']) ? trim($assoc['Year']) : '',
                'company' => isset($assoc['Company']) ? trim($assoc['Company']) : '',
                'registrationNo' => $reg,
                'memberStatus' => $status,
            );
            if ($existing !== null) {
                $fields['memberId'] = $existing['id'];
                $fields['memberNetworkStatus'] = isset($existing['member_network_status']) ? $existing['member_network_status'] : '';
            }

            $save = mess_remote_save_member($con, $licenceId, $fields);
            if ($save['status'] === '1') {
                if ($existing !== null) {
                    $result['membersUpdated']++;
                } else {
                    $result['membersImported']++;
                }
            } else {
                $result['errors'][] = array(
                    'row' => $excelRow,
                    'sheet' => 'Members',
                    'message' => isset($save['message']) ? $save['message'] : 'Save failed',
                );
            }
        }

        // Payments sheet (optional)
        try {
            $paymentSheet = CatalogSimpleXlsx::readImportRows($filePath, array('Payments', 'payments', 'Payment'));
            $hasPaymentHeaders = in_array('Mobile', $paymentSheet['headers'], true)
                && in_array('Mess Amount', $paymentSheet['headers'], true)
                && in_array('Paid Amount', $paymentSheet['headers'], true);

            if ($hasPaymentHeaders) {
                foreach ($paymentSheet['rows'] as $excelRow => $assoc) {
                    $mobile = mess_normalize_mobile(isset($assoc['Mobile']) ? $assoc['Mobile'] : '');
                    $messAmt = isset($assoc['Mess Amount']) ? trim($assoc['Mess Amount']) : '';
                    $paidAmt = isset($assoc['Paid Amount']) ? trim($assoc['Paid Amount']) : '';
                    if ($mobile === '' || $messAmt === '' || $paidAmt === '') {
                        $result['errors'][] = array(
                            'row' => $excelRow,
                            'sheet' => 'Payments',
                            'message' => 'Mobile, Mess Amount, Paid Amount required',
                        );
                        continue;
                    }
                    $member = mess_remote_find_member_by_mobile($con, $licenceId, $mobile);
                    if ($member === null) {
                        $result['errors'][] = array(
                            'row' => $excelRow,
                            'sheet' => 'Payments',
                            'message' => 'No member found for mobile ' . $mobile,
                        );
                        continue;
                    }
                    $memberName = isset($assoc['Member Name']) && trim($assoc['Member Name']) !== ''
                        ? trim($assoc['Member Name'])
                        : $member['member_name'];
                    $savePay = mess_remote_save_payment($con, $licenceId, array(
                        'memberId' => (string) $member['id'],
                        'memberName' => $memberName,
                        'paymentMessAmount' => $messAmt,
                        'paymentPaidAmount' => $paidAmt,
                        'messTotalDays' => isset($assoc['Mess Days']) ? trim($assoc['Mess Days']) : '',
                        'paymentDate' => isset($assoc['Payment Date']) ? trim($assoc['Payment Date']) : '',
                        'paymentStatus' => isset($assoc['Status']) && trim($assoc['Status']) !== ''
                            ? trim($assoc['Status']) : 'active',
                    ));
                    if ($savePay['status'] === '1') {
                        $result['paymentsImported']++;
                    } else {
                        $result['errors'][] = array(
                            'row' => $excelRow,
                            'sheet' => 'Payments',
                            'message' => isset($savePay['message']) ? $savePay['message'] : 'Payment save failed',
                        );
                    }
                }
            }
        } catch (Exception $e) {
            // Payments sheet optional — ignore missing sheet
        }

        $result['status'] = '1';
        $errorCount = isset($result['errors']) ? count($result['errors']) : 0;
        $errorSuffix = $errorCount > 0 ? (' (' . $errorCount . ' row errors)') : '';
        $result['message'] = sprintf(
            'Imported %d members, updated %d, payments %d%s',
            $result['membersImported'],
            $result['membersUpdated'],
            $result['paymentsImported'],
            $errorSuffix
        );
        return $result;
    }
}

if (!function_exists('mess_remote_stream_template')) {
    function mess_remote_stream_template($downloadName = 'mess_member_template.xlsx')
    {
        $sheets = mess_remote_template_sheets();
        if (method_exists('CatalogSimpleXlsx', 'streamWorkbook')) {
            CatalogSimpleXlsx::streamWorkbook($sheets, $downloadName);
            return;
        }
        $tmp = tempnam(sys_get_temp_dir(), 'messtpl');
        CatalogSimpleXlsx::writeWorkbook($sheets, $tmp);
        if (ob_get_length()) {
            ob_end_clean();
        }
        header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        header('Content-Disposition: attachment; filename="' . catalog_sanitize_filename($downloadName) . '"');
        header('Content-Length: ' . filesize($tmp));
        readfile($tmp);
        @unlink($tmp);
    }
}
