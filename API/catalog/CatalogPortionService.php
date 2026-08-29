<?php

class CatalogPortionValidator
{
    private $con;
    private $customerId;
    private $portionsByName = array();

    public function __construct($con, $customerId)
    {
        $this->con = $con;
        $this->customerId = (int) $customerId;
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT `portionMasterId`, `portionName`, `portionMasterStatus` FROM `portion_master` WHERE `userId`=?',
            'i',
            $this->customerId
        );
        foreach ($rows as $row) {
            $this->portionsByName[strtolower(trim($row['portionName']))] = $row;
        }
    }

    public function validateAll(array $rows)
    {
        $validRows = array();
        $errors = array();
        $fileDup = array();
        $newCount = 0;
        $updateCount = 0;

        foreach ($rows as $rowNum => $row) {
            $rowErrors = $this->validateRow($row);
            $name = trim((string) ($row['portionName'] ?? ''));
            if ($name !== '') {
                $key = strtolower($name);
                if (isset($fileDup[$key])) {
                    $rowErrors[] = array(
                        'code' => 'DUPLICATE_ROW',
                        'message' => 'Duplicate Portion found in rows ' . $fileDup[$key] . ' and ' . $rowNum . '.',
                    );
                } else {
                    $fileDup[$key] = $rowNum;
                }
            }

            if (!empty($rowErrors)) {
                foreach ($rowErrors as $err) {
                    $errors[] = $this->enrichError($rowNum, $row, $err);
                }
                continue;
            }

            $action = isset($this->portionsByName[strtolower($name)]) ? 'update' : 'create';
            $action === 'create' ? $newCount++ : $updateCount++;

            $existing = isset($this->portionsByName[strtolower($name)]) ? $this->portionsByName[strtolower($name)] : null;
            $validRows[] = array(
                'row' => $rowNum,
                'action' => $action,
                'data' => $row,
                'resolved' => array(
                    'existingPortionMasterId' => $existing !== null ? (int) $existing['portionMasterId'] : null,
                ),
            );
        }

        return array(
            'validRows' => $validRows,
            'errors' => $errors,
            'summary' => array(
                'total' => count($rows),
                'valid' => count($validRows),
                'new' => $newCount,
                'updated' => $updateCount,
                'errors' => count($errors),
            ),
        );
    }

    private function validateRow(array $row)
    {
        $errors = array();
        if (trim((string) ($row['portionName'] ?? '')) === '') {
            $errors[] = array('code' => 'PORTION_NAME_REQUIRED', 'message' => 'Portion Name is required.');
        }
        $status = trim((string) ($row['status'] ?? ''));
        if ($status !== '' && catalog_normalize_status($status) === null) {
            $errors[] = array('code' => 'INVALID_STATUS', 'message' => 'Invalid status value.');
        }
        return $errors;
    }

    private function enrichError($rowNum, array $row, array $err)
    {
        return array_merge(array(
            'row' => $rowNum,
            'portionName' => isset($row['portionName']) ? $row['portionName'] : '',
            'status' => isset($row['status']) ? $row['status'] : '',
        ), $err);
    }
}

class CatalogPortionImporter
{
    private $con;
    private $customerId;

    public function __construct($con, $customerId)
    {
        $this->con = $con;
        $this->customerId = (int) $customerId;
    }

    public function importRows(array $validRows)
    {
        $created = 0;
        $updated = 0;
        mysqli_begin_transaction($this->con);
        try {
            foreach ($validRows as $entry) {
                if (!$this->importSingle($entry)) {
                    mysqli_rollback($this->con);
                    return array('created' => 0, 'updated' => 0, 'failed' => count($validRows));
                }
                $entry['action'] === 'create' ? $created++ : $updated++;
            }
            mysqli_commit($this->con);
        } catch (Exception $e) {
            mysqli_rollback($this->con);
            return array('created' => 0, 'updated' => 0, 'failed' => count($validRows));
        }
        return array('created' => $created, 'updated' => $updated, 'failed' => 0);
    }

    private function importSingle(array $entry)
    {
        $row = $entry['data'];
        $name = trim((string) $row['portionName']);
        $status = catalog_normalize_status(trim((string) ($row['status'] ?? '')));
        if ($status === null) {
            $status = 'active';
        }

        if ($entry['action'] === 'update') {
            $masterId = (int) $entry['resolved']['existingPortionMasterId'];
            $ok = db_stmt_execute(
                $this->con,
                'UPDATE `portion_master` SET `portionName`=?, `portionMasterStatus`=? WHERE `portionMasterId`=? AND `userId`=?',
                'ssii',
                $name,
                $status,
                $masterId,
                $this->customerId
            );
            if ($ok) {
                db_stmt_execute(
                    $this->con,
                    'UPDATE `product_portions` SET `portionName`=? WHERE `portionMasterId`=? AND `userId`=?',
                    'sii',
                    $name,
                    $masterId,
                    $this->customerId
                );
            }
            return $ok;
        }

        $id = db_stmt_insert_id(
            $this->con,
            'INSERT INTO `portion_master` (`userId`, `portionName`, `portionMasterNetworkStatus`, `portionMasterStatus`)
             VALUES (?, ?, ?, ?)',
            'isss',
            $this->customerId,
            $name,
            catalog_generate_network_status(),
            $status
        );
        return $id !== false;
    }
}

class CatalogPortionExporter
{
    private $con;
    private $customerId;

    public function __construct($con, $customerId)
    {
        $this->con = $con;
        $this->customerId = (int) $customerId;
    }

    public function buildSheetsArray()
    {
        $rows = array(array('Portion Name', 'Status'));
        $data = db_stmt_fetch_all(
            $this->con,
            'SELECT `portionName`, `portionMasterStatus` FROM `portion_master` WHERE `userId`=? ORDER BY `portionName`',
            'i',
            $this->customerId
        );
        if (empty($data)) {
            $rows[] = array('Full', 'Active');
            $rows[] = array('Half', 'Active');
            $rows[] = array('Quarter', 'Active');
        } else {
            foreach ($data as $r) {
                $rows[] = array($r['portionName'], ucfirst(strtolower((string) $r['portionMasterStatus'])));
            }
        }
        return array(
            'Instructions' => array(
                array('PORTION MASTER IMPORT TEMPLATE'),
                array(''),
                array('Required: Portion Name'),
                array('Optional: Status'),
                array(''),
                array('IMPORTANT: Portion master does NOT include price.'),
                array('Product pricing is configured on products.'),
            ),
            'Portions' => $rows,
        );
    }

    public function fetchExportRowCount()
    {
        return db_stmt_scalar_int(
            $this->con,
            'SELECT COUNT(*) AS c FROM `portion_master` WHERE `userId`=?',
            'i',
            $this->customerId
        );
    }
}
