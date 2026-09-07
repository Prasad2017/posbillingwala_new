<?php
/**
 * Shared company_business_templates ensure / read / upsert for Owner, Admin, Dealer.
 * userId column stores the licence (branch) id — matches POS getBusinessTemplate.php.
 */
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/php_compat.php';

if (!function_exists('business_template_ensure_table')) {
    function business_template_ensure_table($con)
    {
        try {
            db_safe_query(
                $con,
                "CREATE TABLE IF NOT EXISTS `company_business_templates` (
                  `id` INT NOT NULL AUTO_INCREMENT,
                  `userId` VARCHAR(64) NOT NULL,
                  `organization_id` INT NULL DEFAULT NULL,
                  `branch_id` INT NULL DEFAULT NULL,
                  `device_id` VARCHAR(255) NULL DEFAULT NULL,
                  `businessType` VARCHAR(64) NOT NULL DEFAULT 'restaurant',
                  `businessTemplateId` VARCHAR(128) NOT NULL DEFAULT 'restaurant_default',
                  `businessTemplateJson` MEDIUMTEXT NULL,
                  `templateNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
                  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uq_company_biz_template_user` (`userId`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Throwable $e) {
            // ignore — table may already exist from migration
        }
    }
}

if (!function_exists('business_template_response_item')) {
    function business_template_response_item($licenceId, $row = null)
    {
        $item = array();
        $item['licenceId'] = (string) $licenceId;
        if ($row !== null) {
            $item['businessType'] = isset($row['businessType']) ? $row['businessType'] : 'restaurant';
            $item['businessTemplateId'] = isset($row['businessTemplateId'])
                ? $row['businessTemplateId'] : 'restaurant_default';
            $item['businessTemplateJson'] = isset($row['businessTemplateJson'])
                ? $row['businessTemplateJson'] : '';
        } else {
            $item['businessType'] = 'restaurant';
            $item['businessTemplateId'] = 'restaurant_default';
            $item['businessTemplateJson'] = '';
        }
        return $item;
    }
}

if (!function_exists('business_template_fetch')) {
    /**
     * @return array response item (always returns defaults when no row)
     */
    function business_template_fetch($con, $licenceId)
    {
        business_template_ensure_table($con);
        $row = db_stmt_fetch_one(
            $con,
            'SELECT * FROM `company_business_templates` WHERE `userId`=? LIMIT 1',
            's',
            (string) $licenceId
        );
        return business_template_response_item($licenceId, $row);
    }
}

if (!function_exists('business_template_fetch_row')) {
    /**
     * @return array|null raw DB row or null
     */
    function business_template_fetch_row($con, $licenceId)
    {
        business_template_ensure_table($con);
        return db_stmt_fetch_one(
            $con,
            'SELECT * FROM `company_business_templates` WHERE `userId`=? LIMIT 1',
            's',
            (string) $licenceId
        );
    }
}

if (!function_exists('business_template_upsert')) {
    /**
     * @return array {status: '0'|'1', message: string}
     */
    function business_template_upsert(
        $con,
        $licenceId,
        $organizationId,
        $branchId,
        $businessType,
        $businessTemplateId,
        $businessTemplateJson,
        $networkStatus,
        $deviceId = ''
    ) {
        business_template_ensure_table($con);

        $licenceId = (string) $licenceId;
        $businessType = $businessType !== '' ? $businessType : 'restaurant';
        $businessTemplateId = $businessTemplateId !== '' ? $businessTemplateId : 'restaurant_default';
        $businessTemplateJson = $businessTemplateJson !== null ? (string) $businessTemplateJson : '';
        $deviceId = $deviceId !== null ? (string) $deviceId : '';
        $orgId = (int) $organizationId;
        $branchId = (int) $branchId;

        $existing = db_stmt_fetch_one(
            $con,
            'SELECT id FROM `company_business_templates` WHERE `userId`=? LIMIT 1',
            's',
            $licenceId
        );

        if ($existing !== null) {
            $id = (int) $existing['id'];
            $ok = db_stmt_execute(
                $con,
                'UPDATE `company_business_templates` SET
                    `organization_id`=?, `branch_id`=?, `device_id`=?,
                    `businessType`=?, `businessTemplateId`=?, `businessTemplateJson`=?,
                    `templateNetworkStatus`=?
                 WHERE `id`=?',
                'iisssssi',
                $orgId,
                $branchId,
                $deviceId,
                $businessType,
                $businessTemplateId,
                $businessTemplateJson,
                $networkStatus,
                $id
            );
            $out = array(
                'status' => $ok ? '1' : '0',
                'message' => $ok ? 'update successful!' : 'update failed!',
            );
            if ($ok) {
                $out['id'] = $id;
            }
            return $out;
        }

        $insertId = db_stmt_insert_id(
            $con,
            'INSERT INTO `company_business_templates`
            (`userId`, `organization_id`, `branch_id`, `device_id`,
             `businessType`, `businessTemplateId`, `businessTemplateJson`, `templateNetworkStatus`)
             VALUES (?,?,?,?,?,?,?,?)',
            'siisssss',
            $licenceId,
            $orgId,
            $branchId,
            $deviceId,
            $businessType,
            $businessTemplateId,
            $businessTemplateJson,
            $networkStatus
        );
        $out = array(
            'status' => $insertId !== false ? '1' : '0',
            'message' => $insertId !== false ? 'insert successful!' : 'insert failed!',
        );
        if ($insertId !== false) {
            $out['id'] = $insertId;
        }
        return $out;
    }
}

if (!function_exists('business_template_default_licence_modules')) {
    /**
     * Suggested Fast / TakeAway / DineIn / Mess flags for a template.
     * Matches WithTable BusinessTemplateRegistry licence-facing features.
     *
     * @return array{fastBilling:string,takeAway:string,dineIn:string,mess:string}
     */
    function business_template_default_licence_modules($businessType, $businessTemplateId)
    {
        $type = strtolower(trim((string) $businessType));
        $tid = strtolower(trim((string) $businessTemplateId));

        // Mess-focused: fast + mess
        if ($type === 'mess' || $tid === 'mess_focused') {
            return array(
                'fastBilling' => '1',
                'takeAway' => '0',
                'dineIn' => '0',
                'mess' => '1',
            );
        }

        // Full F&B (restaurant / bar / custom starting point)
        if ($type === 'restaurant' || $type === 'bar_restaurant' || $type === 'custom'
            || $tid === 'restaurant_default' || $tid === 'bar_restaurant_default') {
            return array(
                'fastBilling' => '1',
                'takeAway' => '1',
                'dineIn' => '1',
                'mess' => '1',
            );
        }

        // Retail-style + salon-mapped Priority-3: fast billing only
        return array(
            'fastBilling' => '1',
            'takeAway' => '0',
            'dineIn' => '0',
            'mess' => '0',
        );
    }
}

if (!function_exists('business_template_sync_licence_modules')) {
    /**
     * Update licenses.fastBilling / takeAway / dineIn / mess from template defaults.
     *
     * @return bool
     */
    function business_template_sync_licence_modules($con, $licenceId, $businessType, $businessTemplateId)
    {
        $mods = business_template_default_licence_modules($businessType, $businessTemplateId);
        return db_stmt_execute(
            $con,
            'UPDATE `licenses` SET `fastBilling`=?, `takeAway`=?, `dineIn`=?, `mess`=? WHERE `id`=?',
            'iiiii',
            (int) $mods['fastBilling'],
            (int) $mods['takeAway'],
            (int) $mods['dineIn'],
            (int) $mods['mess'],
            (int) $licenceId
        );
    }
}

if (!function_exists('business_template_licence_row')) {
    /**
     * @return array|null licenses row with id, userId
     */
    function business_template_licence_row($con, $licenceId)
    {
        return db_stmt_fetch_one(
            $con,
            'SELECT `id`, `userId` FROM `licenses` WHERE `id`=? LIMIT 1',
            'i',
            (int) $licenceId
        );
    }
}

if (!function_exists('business_template_dealer_owns_licence')) {
    /**
     * @return array|null licence row when dealer owns the customer
     */
    function business_template_dealer_owns_licence($con, $licenceId, $dealerId)
    {
        return db_stmt_fetch_one(
            $con,
            "SELECT l.`id`, l.`userId`
             FROM `licenses` l
             INNER JOIN `users` u ON u.`id` = l.`userId` AND u.`role_id` = '3'
             WHERE l.`id`=? AND u.`dealerId`=?
             LIMIT 1",
            'ii',
            (int) $licenceId,
            (int) $dealerId
        );
    }
}
