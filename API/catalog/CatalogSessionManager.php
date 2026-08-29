<?php

class CatalogSessionManager
{
    public static function generateSessionId()
    {
        try {
            return 'IMP-' . strtoupper(bin2hex(random_bytes(8)));
        } catch (Exception $e) {
            return 'IMP-' . strtoupper(substr(md5(uniqid('', true)), 0, 16));
        }
    }

    public static function createValidatedSession($con, array $data)
    {
        $sessionId = self::generateSessionId();
        $expiresAt = date('Y-m-d H:i:s', strtotime('+' . (int) CATALOG_SESSION_TTL_HOURS . ' hours'));

        $insertId = db_stmt_insert_id(
            $con,
            'INSERT INTO `catalog_import_sessions`
            (`sessionId`, `actorType`, `actorId`, `customerId`, `importType`, `fileName`, `storedFilePath`,
             `totalRows`, `validRows`, `newRows`, `updateRows`, `errorRows`, `status`, `previewJson`, `errorsJson`, `expiresAt`)
             VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
            'ssiisssiiiiissss',
            $sessionId,
            $data['actorType'],
            (int) $data['actorId'],
            (int) $data['customerId'],
            $data['importType'],
            $data['fileName'],
            $data['storedFilePath'],
            (int) $data['totalRows'],
            (int) $data['validRows'],
            (int) $data['newRows'],
            (int) $data['updateRows'],
            (int) $data['errorRows'],
            'validated',
            json_encode($data['previewRows']),
            json_encode($data['errors']),
            $expiresAt
        );

        if ($insertId === false) {
            return null;
        }

        return array(
            'sessionId' => $sessionId,
            'expiresAt' => $expiresAt,
        );
    }

    public static function getSession($con, $sessionId, $actorType, $actorId, $customerId = null)
    {
        $row = db_stmt_fetch_one(
            $con,
            'SELECT * FROM `catalog_import_sessions` WHERE `sessionId`=? LIMIT 1',
            's',
            $sessionId
        );

        if ($row === null) {
            return null;
        }

        if ($row['actorType'] !== $actorType || (int) $row['actorId'] !== (int) $actorId) {
            return null;
        }

        if ($customerId !== null && (int) $row['customerId'] !== (int) $customerId) {
            return null;
        }

        if ($row['status'] === 'expired' || strtotime($row['expiresAt']) < time()) {
            return null;
        }

        return $row;
    }

    public static function markImported($con, $sessionId, $created, $updated, $failed)
    {
        return db_stmt_execute(
            $con,
            'UPDATE `catalog_import_sessions`
             SET `status`=\'imported\', `createdCount`=?, `updatedCount`=?, `failedCount`=?, `confirmedAt`=NOW()
             WHERE `sessionId`=? AND `status`=\'validated\'',
            'iiis',
            (int) $created,
            (int) $updated,
            (int) $failed,
            $sessionId
        );
    }

    public static function markFailed($con, $sessionId)
    {
        return db_stmt_execute(
            $con,
            'UPDATE `catalog_import_sessions` SET `status`=\'failed\' WHERE `sessionId`=?',
            's',
            $sessionId
        );
    }

    public static function listHistory($con, $actorType, $actorId, $customerId, $importType = null, $limit = 50)
    {
        if ($importType !== null && $importType !== '') {
            return db_stmt_fetch_all(
                $con,
                'SELECT `sessionId`, `customerId`, `importType`, `fileName`, `totalRows`, `validRows`,
                        `createdCount`, `updatedCount`, `failedCount`, `errorRows`, `status`, `confirmedAt`, `created_at`
                 FROM `catalog_import_sessions`
                 WHERE `actorType`=? AND `actorId`=? AND `customerId`=? AND `importType`=?
                 ORDER BY `id` DESC LIMIT ?',
                'siisi',
                $actorType,
                (int) $actorId,
                (int) $customerId,
                $importType,
                (int) $limit
            );
        }

        return db_stmt_fetch_all(
            $con,
            'SELECT `sessionId`, `customerId`, `importType`, `fileName`, `totalRows`, `validRows`,
                    `createdCount`, `updatedCount`, `failedCount`, `errorRows`, `status`, `confirmedAt`, `created_at`
             FROM `catalog_import_sessions`
             WHERE `actorType`=? AND `actorId`=? AND `customerId`=?
             ORDER BY `id` DESC LIMIT ?',
            'siii',
            $actorType,
            (int) $actorId,
            (int) $customerId,
            (int) $limit
        );
    }

    public static function recordExport($con, $actorType, $actorId, $customerId, $exportType, $fileName, $rowCount, $status = 'completed')
    {
        return db_stmt_insert_id(
            $con,
            'INSERT INTO `catalog_export_history` (`actorType`, `actorId`, `customerId`, `exportType`, `fileName`, `rowCount`, `status`)
             VALUES (?,?,?,?,?,?,?)',
            'siissis',
            $actorType,
            (int) $actorId,
            (int) $customerId,
            $exportType,
            $fileName,
            (int) $rowCount,
            $status
        );
    }
}
