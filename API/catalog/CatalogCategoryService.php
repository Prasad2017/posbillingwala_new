<?php

class CatalogCategoryValidator
{
    private $con;
    private $customerId;
    private $categoriesByName = array();

    public function __construct($con, $customerId)
    {
        $this->con = $con;
        $this->customerId = (int) $customerId;
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT `categoryId`, `categoryName`, `categoryStatus` FROM `categories` WHERE `userId`=?',
            'i',
            $this->customerId
        );
        foreach ($rows as $row) {
            $this->categoriesByName[strtolower(trim($row['categoryName']))] = $row;
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
            $name = trim((string) ($row['categoryName'] ?? ''));
            if ($name !== '') {
                $key = strtolower($name);
                if (isset($fileDup[$key])) {
                    $rowErrors[] = array(
                        'code' => 'DUPLICATE_ROW',
                        'message' => 'Duplicate Category found in rows ' . $fileDup[$key] . ' and ' . $rowNum . '.',
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

            $action = isset($this->categoriesByName[strtolower($name)]) ? 'update' : 'create';
            if ($action === 'create') {
                $newCount++;
            } else {
                $updateCount++;
            }

            $existing = $this->categoriesByName[strtolower($name)];
            $validRows[] = array(
                'row' => $rowNum,
                'action' => $action,
                'data' => $row,
                'resolved' => array(
                    'existingCategoryId' => $action === 'update' ? (int) $existing['categoryId'] : null,
                ),
            );
        }

        return $this->buildResult($rows, $validRows, $errors, $newCount, $updateCount);
    }

    private function validateRow(array $row)
    {
        $errors = array();
        if (trim((string) ($row['categoryName'] ?? '')) === '') {
            $errors[] = array('code' => 'CATEGORY_NAME_REQUIRED', 'message' => 'Category Name is required.');
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
            'categoryName' => isset($row['categoryName']) ? $row['categoryName'] : '',
            'status' => isset($row['status']) ? $row['status'] : '',
        ), $err);
    }

    private function buildResult($rows, $validRows, $errors, $newCount, $updateCount)
    {
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
}

class CatalogCategoryImporter
{
    private $con;
    private $customerId;
    private $dealerId;

    public function __construct($con, $customerId, $dealerId = 0)
    {
        $this->con = $con;
        $this->customerId = (int) $customerId;
        $this->dealerId = (int) $dealerId;
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
                if ($entry['action'] === 'create') {
                    $created++;
                } else {
                    $updated++;
                }
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
        $name = trim((string) $row['categoryName']);
        $status = catalog_normalize_status(trim((string) ($row['status'] ?? '')));
        if ($status === null) {
            $status = 'active';
        }

        if ($entry['action'] === 'update') {
            return db_stmt_execute(
                $this->con,
                'UPDATE `categories` SET `categoryName`=?, `categoryStatus`=? WHERE `categoryId`=? AND `userId`=?',
                'ssii',
                $name,
                $status,
                (int) $entry['resolved']['existingCategoryId'],
                $this->customerId
            );
        }

        $id = db_stmt_insert_id(
            $this->con,
            'INSERT INTO `categories` (`userId`, `dealerId`, `categoryName`, `categoryNetworkStatus`, `categoryStatus`)
             VALUES (?, ?, ?, ?, ?)',
            'iisss',
            $this->customerId,
            $this->dealerId,
            $name,
            catalog_generate_network_status(),
            $status
        );
        return $id !== false;
    }
}

class CatalogCategoryExporter
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
        $rows = array(array('Category Name', 'Status'));
        $data = db_stmt_fetch_all(
            $this->con,
            'SELECT `categoryName`, `categoryStatus` FROM `categories` WHERE `userId`=? ORDER BY `categoryName`',
            'i',
            $this->customerId
        );
        if (empty($data)) {
            $rows[] = array('Veg', 'Active');
            $rows[] = array('Non Veg', 'Active');
            $rows[] = array('Beverages', 'Active');
        } else {
            foreach ($data as $r) {
                $rows[] = array($r['categoryName'], ucfirst(strtolower((string) $r['categoryStatus'])));
            }
        }
        return array(
            'Instructions' => array(
                array('CATEGORY IMPORT TEMPLATE'),
                array(''),
                array('Required: Category Name'),
                array('Optional: Status (Active/Inactive)'),
                array(''),
                array('Rules:'),
                array('1. Do not change column names.'),
                array('2. Existing category names will be updated.'),
            ),
            'Categories' => $rows,
        );
    }

    public function fetchExportRowCount()
    {
        return db_stmt_scalar_int(
            $this->con,
            'SELECT COUNT(*) AS c FROM `categories` WHERE `userId`=?',
            'i',
            $this->customerId
        );
    }
}
