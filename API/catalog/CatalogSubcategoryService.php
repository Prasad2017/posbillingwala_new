<?php

class CatalogSubcategoryValidator
{
    private $con;
    private $customerId;
    private $categoriesByName = array();
    private $subcategoriesByKey = array();

    public function __construct($con, $customerId)
    {
        $this->con = $con;
        $this->customerId = (int) $customerId;

        $cats = db_stmt_fetch_all(
            $con,
            'SELECT `categoryId`, `categoryName` FROM `categories` WHERE `userId`=?',
            'i',
            $this->customerId
        );
        foreach ($cats as $cat) {
            $this->categoriesByName[strtolower(trim($cat['categoryName']))] = $cat;
        }

        $subs = db_stmt_fetch_all(
            $con,
            'SELECT ps.`subcategoryId`, ps.`subcategoryName`, c.`categoryName`
             FROM `product_subcategories` ps
             INNER JOIN `categories` c ON c.`categoryId` = ps.`categoryId`
             WHERE ps.`userId`=?',
            'i',
            $this->customerId
        );
        foreach ($subs as $sub) {
            $key = strtolower(trim($sub['categoryName'])) . '|' . strtolower(trim($sub['subcategoryName']));
            $this->subcategoriesByKey[$key] = $sub;
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
            $dupKey = $this->duplicateKey($row);
            if ($dupKey !== null) {
                if (isset($fileDup[$dupKey])) {
                    $rowErrors[] = array(
                        'code' => 'DUPLICATE_ROW',
                        'message' => 'Duplicate Sub Category found in rows ' . $fileDup[$dupKey] . ' and ' . $rowNum . '.',
                    );
                } else {
                    $fileDup[$dupKey] = $rowNum;
                }
            }

            if (!empty($rowErrors)) {
                foreach ($rowErrors as $err) {
                    $errors[] = $this->enrichError($rowNum, $row, $err);
                }
                continue;
            }

            $catName = trim((string) $row['categoryName']);
            $subName = trim((string) $row['subcategoryName']);
            $key = strtolower($catName) . '|' . strtolower($subName);
            $action = isset($this->subcategoriesByKey[$key]) ? 'update' : 'create';
            $action === 'create' ? $newCount++ : $updateCount++;

            $cat = $this->categoriesByName[strtolower($catName)];
            $validRows[] = array(
                'row' => $rowNum,
                'action' => $action,
                'data' => $row,
                'resolved' => array(
                    'categoryId' => (int) $cat['categoryId'],
                    'existingSubcategoryId' => $action === 'update'
                        ? (int) $this->subcategoriesByKey[$key]['subcategoryId'] : null,
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

    private function duplicateKey(array $row)
    {
        $cat = trim((string) ($row['categoryName'] ?? ''));
        $sub = trim((string) ($row['subcategoryName'] ?? ''));
        if ($cat === '' || $sub === '') {
            return null;
        }
        return strtolower($cat) . '|' . strtolower($sub);
    }

    private function validateRow(array $row)
    {
        $errors = array();
        $sub = trim((string) ($row['subcategoryName'] ?? ''));
        $cat = trim((string) ($row['categoryName'] ?? ''));

        if ($sub === '') {
            $errors[] = array('code' => 'SUBCATEGORY_NAME_REQUIRED', 'message' => 'Sub Category Name is required.');
        }
        if ($cat === '') {
            $errors[] = array('code' => 'CATEGORY_REQUIRED', 'message' => 'Category Name is required.');
        } elseif (!isset($this->categoriesByName[strtolower($cat)])) {
            $errors[] = array(
                'code' => 'CATEGORY_NOT_FOUND',
                'message' => 'Category "' . $cat . '" not found.',
            );
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
            'subcategoryName' => isset($row['subcategoryName']) ? $row['subcategoryName'] : '',
            'categoryName' => isset($row['categoryName']) ? $row['categoryName'] : '',
            'status' => isset($row['status']) ? $row['status'] : '',
        ), $err);
    }
}

class CatalogSubcategoryImporter
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
        $name = trim((string) $row['subcategoryName']);
        $categoryId = (int) $entry['resolved']['categoryId'];
        $status = catalog_normalize_status(trim((string) ($row['status'] ?? '')));
        if ($status === null) {
            $status = 'active';
        }

        if ($entry['action'] === 'update') {
            return db_stmt_execute(
                $this->con,
                'UPDATE `product_subcategories` SET `categoryId`=?, `subcategoryName`=?, `subcategoryStatus`=? WHERE `subcategoryId`=? AND `userId`=?',
                'issii',
                $categoryId,
                $name,
                $status,
                (int) $entry['resolved']['existingSubcategoryId'],
                $this->customerId
            );
        }

        $id = db_stmt_insert_id(
            $this->con,
            'INSERT INTO `product_subcategories` (`userId`, `categoryId`, `subcategoryName`, `subcategoryNetworkStatus`, `subcategoryStatus`)
             VALUES (?, ?, ?, ?, ?)',
            'iisss',
            $this->customerId,
            $categoryId,
            $name,
            catalog_generate_network_status(),
            $status
        );
        return $id !== false;
    }
}

class CatalogSubcategoryExporter
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
        $rows = array(array('Sub Category Name', 'Category Name', 'Status'));
        $data = db_stmt_fetch_all(
            $this->con,
            'SELECT ps.`subcategoryName`, c.`categoryName`, ps.`subcategoryStatus`
             FROM `product_subcategories` ps
             INNER JOIN `categories` c ON c.`categoryId` = ps.`categoryId`
             WHERE ps.`userId`=? ORDER BY c.`categoryName`, ps.`subcategoryName`',
            'i',
            $this->customerId
        );
        if (empty($data)) {
            $rows[] = array('Main Course', 'Veg', 'Active');
            $rows[] = array('Snacks', 'Veg', 'Active');
            $rows[] = array('Cold Drinks', 'Beverages', 'Active');
        } else {
            foreach ($data as $r) {
                $rows[] = array($r['subcategoryName'], $r['categoryName'], ucfirst(strtolower((string) $r['subcategoryStatus'])));
            }
        }

        $catRows = array(array('Category Name'));
        $cats = db_stmt_fetch_all(
            $this->con,
            'SELECT `categoryName` FROM `categories` WHERE `userId`=? AND `categoryStatus`=\'active\' ORDER BY `categoryName`',
            'i',
            $this->customerId
        );
        foreach ($cats as $c) {
            $catRows[] = array($c['categoryName']);
        }

        return array(
            'Instructions' => array(
                array('SUB CATEGORY IMPORT TEMPLATE'),
                array(''),
                array('Required: Sub Category Name, Category Name'),
                array('Optional: Status'),
                array('Category must already exist.'),
            ),
            'Sub Categories' => $rows,
            'Categories' => $catRows,
        );
    }

    public function fetchExportRowCount()
    {
        return db_stmt_scalar_int(
            $this->con,
            'SELECT COUNT(*) AS c FROM `product_subcategories` WHERE `userId`=?',
            'i',
            $this->customerId
        );
    }
}
