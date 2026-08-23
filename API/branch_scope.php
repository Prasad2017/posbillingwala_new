<?php
/**
 * Multi-branch / franchise scope helpers.
 *
 * Canonical mapping (backward compatible):
 *   organization_id = users.id (shop owner)
 *   branch_id       = licenses.id (owner = Main Store, franchise = branch)
 *   device_id       = licenses.android_device_id
 *
 * Existing licenseId / userId columns are kept; scope columns are additive.
 */

require_once __DIR__ . '/db_prepared.php';

if (!function_exists('branch_scope_from_license')) {
    /**
     * Resolve org / branch / device for a licence (branch) row.
     *
     * @param mysqli $con
     * @param int|string $licenseId licenses.id
     * @return array|null organizationId, branchId, deviceId, userType, branchLabel
     */
    function branch_scope_from_license($con, $licenseId)
    {
        if ($licenseId === null || $licenseId === '') {
            return null;
        }

        $row = db_stmt_fetch_one(
            $con,
            'SELECT `id`, `userId`, `android_device_id`, `userType`, `userName` FROM `licenses` WHERE `id`=? LIMIT 1',
            'i',
            (int) $licenseId
        );

        if ($row === null) {
            return null;
        }

        require_once __DIR__ . '/licence_expiry.php';
        $branch = licence_branch_fields($row);

        return array(
            'organizationId' => (string) $row['userId'],
            'branchId' => (string) $row['id'],
            'deviceId' => isset($row['android_device_id']) ? trim((string) $row['android_device_id']) : '',
            'userType' => $branch['userType'],
            'branchLabel' => $branch['branchLabel'],
        );
    }
}

if (!function_exists('branch_scope_triplet')) {
    /** @return array organization_id, branch_id, device_id for SQL INSERT/UPDATE */
    function branch_scope_triplet(array $scope)
    {
        return array(
            'organization_id' => isset($scope['organizationId']) ? (int) $scope['organizationId'] : 0,
            'branch_id' => isset($scope['branchId']) ? (int) $scope['branchId'] : 0,
            'device_id' => isset($scope['deviceId']) ? (string) $scope['deviceId'] : '',
        );
    }
}

if (!function_exists('branch_scope_json')) {
    /** Scope fields for API JSON responses. */
    function branch_scope_json(array $scope)
    {
        return array(
            'organizationId' => $scope['organizationId'],
            'branchId' => $scope['branchId'],
            'deviceId' => $scope['deviceId'],
            'branchLabel' => isset($scope['branchLabel']) ? $scope['branchLabel'] : '',
        );
    }
}

if (!function_exists('branch_owner_owns_branch')) {
    /**
     * Owner (users.id) may view any branch in their organization.
     */
    function branch_owner_owns_branch($con, $ownerUserId, $branchId)
    {
        if ($ownerUserId === '' || $branchId === '') {
            return false;
        }

        $orgId = db_stmt_scalar_string(
            $con,
            'SELECT `userId` FROM `licenses` WHERE `id`=? LIMIT 1',
            'i',
            (int) $branchId
        );

        return $orgId !== null && (string) $orgId === (string) $ownerUserId;
    }
}

if (!function_exists('branch_has_cross_access_grant')) {
    /**
     * Explicit grant: source branch POS may read target branch data.
     */
    function branch_has_cross_access_grant($con, $sourceBranchId, $targetBranchId)
    {
        if ($sourceBranchId === '' || $targetBranchId === '' || (string) $sourceBranchId === (string) $targetBranchId) {
            return (string) $sourceBranchId === (string) $targetBranchId;
        }

        $count = db_stmt_scalar_string(
            $con,
            'SELECT COUNT(*) FROM `branch_access_grants`
             WHERE `source_branch_id`=? AND `target_branch_id`=? LIMIT 1',
            'ii',
            (int) $sourceBranchId,
            (int) $targetBranchId
        );

        return $count !== null && (int) $count > 0;
    }
}

if (!function_exists('branch_pos_can_access_branch')) {
    /**
     * POS session (licence id) may access data scoped to targetBranchId.
     */
    function branch_pos_can_access_branch($con, $sessionBranchId, $targetBranchId)
    {
        if ($sessionBranchId === '' || $targetBranchId === '') {
            return false;
        }
        if ((string) $sessionBranchId === (string) $targetBranchId) {
            return true;
        }
        return branch_has_cross_access_grant($con, $sessionBranchId, $targetBranchId);
    }
}

if (!function_exists('branch_pos_require_branch_access')) {
    /**
     * @return array|null scope on success; sets $response error and returns null on deny
     */
    function branch_pos_require_branch_access($con, $sessionBranchId, $targetBranchId, array &$response)
    {
        if (!branch_pos_can_access_branch($con, $sessionBranchId, $targetBranchId)) {
            $response['status'] = '0';
            $response['message'] = 'Cross-branch access denied';
            return null;
        }
        return branch_scope_from_license($con, $targetBranchId);
    }
}

if (!function_exists('branch_owner_require_branch_access')) {
    /**
     * @return bool
     */
    function branch_owner_require_branch_access($con, $ownerUserId, $targetBranchId, array &$response)
    {
        if (!branch_owner_owns_branch($con, $ownerUserId, $targetBranchId)) {
            $response['status'] = '0';
            $response['message'] = 'Branch not in your organization';
            return false;
        }
        return true;
    }
}

if (!function_exists('branch_resolve_pos_session')) {
    /**
     * Resolve authenticated POS branch id from token or posted userId.
     *
     * @param mysqli $con
     * @param string $postedUserId
     * @return string|null licence id or null when token invalid
     */
    function branch_resolve_pos_session($con, $postedUserId)
    {
        require_once __DIR__ . '/auth_tokens.php';
        return auth_pos_licence_id_from_request($con, $postedUserId);
    }
}

if (!function_exists('branch_enforce_pos_scope_match')) {
    /**
     * When client posts scope ids, they must match the authenticated session (if sent).
     */
    function branch_enforce_pos_scope_match($sessionScope, $postedOrgId, $postedBranchId, $postedDeviceId)
    {
        if ($sessionScope === null) {
            return true;
        }
        if ($postedOrgId !== null && $postedOrgId !== '' && (string) $postedOrgId !== (string) $sessionScope['organizationId']) {
            return false;
        }
        if ($postedBranchId !== null && $postedBranchId !== '' && (string) $postedBranchId !== (string) $sessionScope['branchId']) {
            return false;
        }
        if ($postedDeviceId !== null && $postedDeviceId !== '' && $sessionScope['deviceId'] !== ''
            && (string) $postedDeviceId !== (string) $sessionScope['deviceId']) {
            return false;
        }
        return true;
    }
}

if (!function_exists('branch_append_scope_to_invoice_row')) {
    function branch_append_scope_to_invoice_row(array &$getdata, array $row)
    {
        $orgId = isset($row['organization_id']) ? $row['organization_id'] : null;
        $branchId = isset($row['branch_id']) ? $row['branch_id'] : (isset($row['licenseId']) ? $row['licenseId'] : null);
        $deviceId = isset($row['device_id']) ? $row['device_id'] : '';

        if ($orgId !== null && $orgId !== '') {
            $getdata['organizationId'] = (string) $orgId;
        }
        if ($branchId !== null && $branchId !== '') {
            $getdata['branchId'] = (string) $branchId;
        }
        if ($deviceId !== null && $deviceId !== '') {
            $getdata['deviceId'] = (string) $deviceId;
        }
    }
}

if (!function_exists('branch_pos_prepare_write')) {
    /**
     * Authenticate POS write: resolve licence id + scope; reject spoofed scope fields.
     *
     * @return array|null licenseId, scope, triplet
     */
    function branch_pos_prepare_write($con, $postedUserId, array &$response)
    {
        $licenseId = branch_resolve_pos_session($con, $postedUserId);
        if ($licenseId === null || $licenseId === '') {
            $response['status'] = '0';
            $response['message'] = 'Invalid or expired auth token';
            return null;
        }

        $scope = branch_scope_from_license($con, $licenseId);
        if ($scope === null) {
            $response['status'] = '0';
            $response['message'] = 'Invalid licence';
            return null;
        }

        $postedOrg = isset($_POST['organizationId']) ? $_POST['organizationId'] : '';
        $postedBranch = isset($_POST['branchId']) ? $_POST['branchId'] : '';
        $postedDevice = isset($_POST['deviceId']) ? $_POST['deviceId'] : '';

        if (!branch_enforce_pos_scope_match($scope, $postedOrg, $postedBranch, $postedDevice)) {
            $response['status'] = '0';
            $response['message'] = 'Scope mismatch — record must match logged-in branch';
            return null;
        }

        return array(
            'licenseId' => $licenseId,
            'scope' => $scope,
            'triplet' => branch_scope_triplet($scope),
        );
    }
}

if (!function_exists('branch_pos_prepare_read')) {
    /**
     * Authenticate POS read for a target branch (own branch or explicit grant).
     *
     * @return array|null licenseId (session), scope for target
     */
    function branch_pos_prepare_read($con, $postedUserId, $requestedBranchId, array &$response)
    {
        $sessionBranchId = branch_resolve_pos_session($con, $postedUserId);
        if ($sessionBranchId === null || $sessionBranchId === '') {
            $response['status'] = '0';
            $response['message'] = 'Invalid or expired auth token';
            return null;
        }

        $targetBranchId = ($requestedBranchId !== null && $requestedBranchId !== '')
            ? $requestedBranchId
            : $sessionBranchId;

        $scope = branch_pos_require_branch_access($con, $sessionBranchId, $targetBranchId, $response);
        if ($scope === null) {
            return null;
        }

        return array(
            'sessionBranchId' => $sessionBranchId,
            'targetBranchId' => (string) $targetBranchId,
            'scope' => $scope,
        );
    }
}

?>
