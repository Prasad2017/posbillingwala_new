<?php

class CatalogProductImporter
{
    /** @var mysqli */
    private $con;
    private $customerId;
    private $dealerId;

    public function __construct($con, $customerId, $dealerId = 0)
    {
        $this->con = $con;
        $this->customerId = (int) $customerId;
        $this->dealerId = (int) $dealerId;
    }

    /**
     * Import validated preview rows inside a transaction.
     *
     * @param array $validRows from validator
     * @return array{created: int, updated: int, failed: int}
     */
    public function importRows(array $validRows)
    {
        $created = 0;
        $updated = 0;
        $failed = 0;

        mysqli_begin_transaction($this->con);

        try {
            foreach ($validRows as $entry) {
                $ok = $this->importSingleRow($entry);
                if (!$ok) {
                    $failed++;
                    continue;
                }
                if ($entry['action'] === 'create') {
                    $created++;
                } else {
                    $updated++;
                }
            }

            if ($failed > 0) {
                mysqli_rollback($this->con);
                return array('created' => 0, 'updated' => 0, 'failed' => count($validRows));
            }

            mysqli_commit($this->con);
        } catch (Exception $e) {
            mysqli_rollback($this->con);
            return array('created' => 0, 'updated' => 0, 'failed' => count($validRows));
        }

        return array('created' => $created, 'updated' => $updated, 'failed' => 0);
    }

    private function importSingleRow(array $entry)
    {
        $row = $entry['data'];
        $resolved = $entry['resolved'];

        $productName = trim((string) $row['productName']);
        $productCode = trim((string) ($row['productCode'] ?? ''));
        $price = trim((string) ($row['price'] ?? ''));
        $priceVal = ($price === '' || !is_numeric($price)) ? 0.0 : (float) $price;
        $unit = trim((string) ($row['unit'] ?? ''));
        if ($unit === '') {
            $unit = 'Pcs';
        }

        $gstSplit = catalog_split_gst(trim((string) ($row['gst'] ?? '')));
        $cgst = $gstSplit !== null ? $gstSplit['cgst'] : 0;
        $sgst = $gstSplit !== null ? $gstSplit['sgst'] : 0;

        $statusRaw = trim((string) ($row['status'] ?? ''));
        $status = catalog_normalize_status($statusRaw);
        if ($status === null) {
            $status = 'active';
        }

        $categoryId = (int) $resolved['categoryId'];
        $subcategoryId = $resolved['subcategoryId'];
        $productId = $resolved['existingProductId'];

        if ($productId !== null) {
            return $this->updateProduct($productId, $categoryId, $subcategoryId, $productCode, $productName, $priceVal, $unit, $cgst, $sgst, $status, $row, $resolved);
        }

        return $this->createProduct($categoryId, $subcategoryId, $productCode, $productName, $priceVal, $unit, $cgst, $sgst, $status, $row, $resolved);
    }

    private function createProduct($categoryId, $subcategoryId, $productCode, $productName, $price, $unit, $cgst, $sgst, $status, array $row, array $resolved)
    {
        $networkStatus = catalog_generate_network_status();

        $dealerId = $this->dealerId > 0 ? $this->dealerId : 0;

        if ($subcategoryId !== null) {
            $productId = db_stmt_insert_id(
                $this->con,
                'INSERT INTO `products` (`userId`, `dealerId`, `categoryId`, `subcategoryId`, `productCode`, `productName`,
                 `productPrice`, `productUnit`, `productCGST`, `productSGST`, `productNetworkStatus`, `productStatus`)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?,?)',
                'iiiissdsiiss',
                $this->customerId,
                $dealerId,
                $categoryId,
                (int) $subcategoryId,
                $productCode,
                $productName,
                $price,
                $unit,
                $cgst,
                $sgst,
                $networkStatus,
                $status
            );
        } else {
            $productId = db_stmt_insert_id(
                $this->con,
                'INSERT INTO `products` (`userId`, `dealerId`, `categoryId`, `productCode`, `productName`,
                 `productPrice`, `productUnit`, `productCGST`, `productSGST`, `productNetworkStatus`, `productStatus`)
                 VALUES (?,?,?,?,?,?,?,?,?,?,?)',
                'iiissdsiiss',
                $this->customerId,
                $dealerId,
                $categoryId,
                $productCode,
                $productName,
                $price,
                $unit,
                $cgst,
                $sgst,
                $networkStatus,
                $status
            );
        }

        if ($productId === false) {
            return false;
        }

        return $this->upsertProductPortion((int) $productId, $row, $resolved, $price);
    }

    private function updateProduct($productId, $categoryId, $subcategoryId, $productCode, $productName, $price, $unit, $cgst, $sgst, $status, array $row, array $resolved)
    {
        if ($subcategoryId !== null) {
            $ok = db_stmt_execute(
                $this->con,
                'UPDATE `products` SET `categoryId`=?, `subcategoryId`=?, `productCode`=?, `productName`=?, `productPrice`=?,
                 `productUnit`=?, `productCGST`=?, `productSGST`=?, `productStatus`=? WHERE `productId`=? AND `userId`=?',
                'iissdsiiisii',
                $categoryId,
                (int) $subcategoryId,
                $productCode,
                $productName,
                $price,
                $unit,
                $cgst,
                $sgst,
                $status,
                (int) $productId,
                $this->customerId
            );
        } else {
            $ok = db_stmt_execute(
                $this->con,
                'UPDATE `products` SET `categoryId`=?, `subcategoryId`=NULL, `productCode`=?, `productName`=?, `productPrice`=?,
                 `productUnit`=?, `productCGST`=?, `productSGST`=?, `productStatus`=? WHERE `productId`=? AND `userId`=?',
                'issdsiiisii',
                $categoryId,
                $productCode,
                $productName,
                $price,
                $unit,
                $cgst,
                $sgst,
                $status,
                (int) $productId,
                $this->customerId
            );
        }

        if (!$ok) {
            return false;
        }

        if (trim((string) ($row['price'] ?? '')) !== '') {
            db_stmt_execute(
                $this->con,
                'UPDATE `products` SET `productPrice`=? WHERE `productId`=? AND `userId`=?',
                'dii',
                $price,
                (int) $productId,
                $this->customerId
            );
        }

        return $this->upsertProductPortion((int) $productId, $row, $resolved, $price);
    }

    private function upsertProductPortion($productId, array $row, array $resolved, $price)
    {
        $portionMasterId = $resolved['portionMasterId'];
        if ($portionMasterId === null) {
            return true;
        }

        $portionName = trim((string) ($row['portion'] ?? ''));
        $portionPrice = trim((string) ($row['price'] ?? ''));
        $priceVal = ($portionPrice !== '' && is_numeric($portionPrice)) ? (float) $portionPrice : $price;

        $existing = db_stmt_fetch_one(
            $this->con,
            'SELECT `portionId` FROM `product_portions` WHERE `productId`=? AND `portionMasterId`=? LIMIT 1',
            'ii',
            $productId,
            (int) $portionMasterId
        );

        if ($existing !== null) {
            return db_stmt_execute(
                $this->con,
                'UPDATE `product_portions` SET `portionName`=?, `portionPrice`=?, `portionStatus`=\'active\'
                 WHERE `portionId`=? AND `userId`=?',
                'sdii',
                $portionName,
                $priceVal,
                (int) $existing['portionId'],
                $this->customerId
            );
        }

        $networkStatus = catalog_generate_network_status();
        $insertId = db_stmt_insert_id(
            $this->con,
            'INSERT INTO `product_portions` (`userId`, `productId`, `portionMasterId`, `portionName`, `portionPrice`,
             `portionSortOrder`, `portionNetworkStatus`, `portionStatus`)
             VALUES (?,?,?,?,?,?,?,?)',
            'iiisdiss',
            $this->customerId,
            $productId,
            (int) $portionMasterId,
            $portionName,
            $priceVal,
            0,
            $networkStatus,
            'active'
        );

        return $insertId !== false;
    }
}
