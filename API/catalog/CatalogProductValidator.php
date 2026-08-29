<?php

class CatalogProductValidator
{
    /** @var mysqli */
    private $con;
    private $customerId;

    /** @var array<string, array> */
    private $categories = array();
    /** @var array<string, array> categoryName|subName => subcategory */
    private $subcategories = array();
    /** @var array<string, array> portionName lower => row */
    private $portionMasters = array();
    /** @var array<string, array> productCode lower => product */
    private $productsByCode = array();
    /** @var array<string, array> productName lower => product */
    private $productsByName = array();

    public function __construct($con, $customerId)
    {
        $this->con = $con;
        $this->customerId = (int) $customerId;
        $this->loadReferenceData();
    }

    private function loadReferenceData()
    {
        $uid = $this->customerId;

        $cats = db_stmt_fetch_all(
            $this->con,
            'SELECT `categoryId`, `categoryName`, `categoryStatus` FROM `categories` WHERE `userId`=?',
            'i',
            $uid
        );
        foreach ($cats as $cat) {
            $key = strtolower(trim($cat['categoryName']));
            $this->categories[$key] = $cat;
        }

        $subs = db_stmt_fetch_all(
            $this->con,
            'SELECT ps.`subcategoryId`, ps.`subcategoryName`, ps.`categoryId`, c.`categoryName`
             FROM `product_subcategories` ps
             INNER JOIN `categories` c ON c.`categoryId` = ps.`categoryId`
             WHERE ps.`userId`=?',
            'i',
            $uid
        );
        foreach ($subs as $sub) {
            $key = strtolower(trim($sub['categoryName'])) . '|' . strtolower(trim($sub['subcategoryName']));
            $this->subcategories[$key] = $sub;
        }

        $portions = db_stmt_fetch_all(
            $this->con,
            'SELECT `portionMasterId`, `portionName`, `portionMasterStatus` FROM `portion_master` WHERE `userId`=?',
            'i',
            $uid
        );
        foreach ($portions as $p) {
            $this->portionMasters[strtolower(trim($p['portionName']))] = $p;
        }

        $products = db_stmt_fetch_all(
            $this->con,
            'SELECT `productId`, `productCode`, `productName`, `categoryId`, `subcategoryId`, `productPrice`,
                    `productUnit`, `productCGST`, `productSGST`, `productStatus`, `productNetworkStatus`
             FROM `products` WHERE `userId`=?',
            'i',
            $uid
        );
        foreach ($products as $prod) {
            $code = strtolower(trim((string) $prod['productCode']));
            $name = strtolower(trim((string) $prod['productName']));
            if ($code !== '') {
                $this->productsByCode[$code] = $prod;
            }
            if ($name !== '') {
                $this->productsByName[$name] = $prod;
            }
        }
    }

    /**
     * Validate all rows; detect duplicate rows in file.
     *
     * @param array<int, array<string, string>> $rows keyed by excel row number
     * @return array{validRows: array, errors: array, summary: array}
     */
    public function validateAll(array $rows)
    {
        $validRows = array();
        $errors = array();
        $fileDuplicateTracker = array();
        $newCount = 0;
        $updateCount = 0;

        foreach ($rows as $rowNum => $row) {
            $rowErrors = $this->validateRow($row, $rowNum);

            $dupKey = $this->duplicateKey($row);
            if ($dupKey !== null) {
                if (isset($fileDuplicateTracker[$dupKey])) {
                    $rowErrors[] = array(
                        'row' => $rowNum,
                        'code' => 'DUPLICATE_ROW',
                        'message' => 'Duplicate Product Code found in rows ' . $fileDuplicateTracker[$dupKey] . ' and ' . $rowNum . '.',
                    );
                } else {
                    $fileDuplicateTracker[$dupKey] = $rowNum;
                }
            }

            if (!empty($rowErrors)) {
                foreach ($rowErrors as $err) {
                    $errors[] = $this->enrichError($rowNum, $row, $err);
                }
                continue;
            }

            $action = $this->resolveAction($row);
            if ($action === 'create') {
                $newCount++;
            } else {
                $updateCount++;
            }

            $resolved = $this->resolveReferences($row);
            $validRows[] = array(
                'row' => $rowNum,
                'action' => $action,
                'data' => $row,
                'resolved' => $resolved,
            );
        }

        $total = count($rows);
        $valid = count($validRows);
        $errorCount = count($errors);

        return array(
            'validRows' => $validRows,
            'errors' => $errors,
            'summary' => array(
                'total' => $total,
                'valid' => $valid,
                'new' => $newCount,
                'updated' => $updateCount,
                'errors' => $errorCount,
            ),
        );
    }

    private function duplicateKey(array $row)
    {
        $code = trim((string) ($row['productCode'] ?? ''));
        if ($code !== '') {
            return 'code:' . strtolower($code);
        }
        $name = trim((string) ($row['productName'] ?? ''));
        if ($name !== '') {
            return 'name:' . strtolower($name);
        }
        return null;
    }

    private function validateRow(array $row, $rowNum)
    {
        $errors = array();

        $productName = trim((string) ($row['productName'] ?? ''));
        $category = trim((string) ($row['category'] ?? ''));

        if ($productName === '') {
            $errors[] = array('code' => 'PRODUCT_NAME_REQUIRED', 'message' => 'Product Name is required.');
        }
        if ($category === '') {
            $errors[] = array('code' => 'CATEGORY_REQUIRED', 'message' => 'Category is required.');
        } elseif (!isset($this->categories[strtolower($category)])) {
            $errors[] = array(
                'code' => 'CATEGORY_NOT_FOUND',
                'message' => 'Category "' . $category . '" not found.',
            );
        }

        $subCategory = trim((string) ($row['subCategory'] ?? ''));
        if ($subCategory !== '' && $category !== '') {
            $subKey = strtolower($category) . '|' . strtolower($subCategory);
            if (!isset($this->subcategories[$subKey])) {
                $catExists = isset($this->categories[strtolower($category)]);
                if ($catExists) {
                    $errors[] = array(
                        'code' => 'SUBCATEGORY_NOT_FOUND',
                        'message' => 'Sub Category "' . $subCategory . '" not found under Category "' . $category . '".',
                    );
                } else {
                    $errors[] = array(
                        'code' => 'SUBCATEGORY_CATEGORY_MISMATCH',
                        'message' => 'Sub Category "' . $subCategory . '" does not belong to Category "' . $category . '".',
                    );
                }
            }
        }

        $portion = trim((string) ($row['portion'] ?? ''));
        if ($portion !== '' && !isset($this->portionMasters[strtolower($portion)])) {
            $errors[] = array(
                'code' => 'PORTION_NOT_FOUND',
                'message' => 'Portion "' . $portion . '" not found.',
            );
        }

        $price = trim((string) ($row['price'] ?? ''));
        if ($price !== '' && !is_numeric($price)) {
            $errors[] = array('code' => 'INVALID_PRICE', 'message' => 'Invalid price value.');
        }

        $gst = trim((string) ($row['gst'] ?? ''));
        if ($gst !== '') {
            $split = catalog_split_gst($gst);
            if ($split === null) {
                $errors[] = array('code' => 'INVALID_GST', 'message' => 'Invalid GST value.');
            }
        }

        $status = trim((string) ($row['status'] ?? ''));
        if ($status !== '' && catalog_normalize_status($status) === null) {
            $errors[] = array('code' => 'INVALID_STATUS', 'message' => 'Invalid status value.');
        }

        return $errors;
    }

    private function resolveAction(array $row)
    {
        $code = trim((string) ($row['productCode'] ?? ''));
        if ($code !== '') {
            return isset($this->productsByCode[strtolower($code)]) ? 'update' : 'create';
        }
        $name = trim((string) ($row['productName'] ?? ''));
        return isset($this->productsByName[strtolower($name)]) ? 'update' : 'create';
    }

    private function resolveReferences(array $row)
    {
        $category = trim((string) $row['category']);
        $cat = $this->categories[strtolower($category)];

        $resolved = array(
            'categoryId' => (int) $cat['categoryId'],
            'subcategoryId' => null,
            'portionMasterId' => null,
            'existingProductId' => null,
        );

        $subCategory = trim((string) ($row['subCategory'] ?? ''));
        if ($subCategory !== '') {
            $subKey = strtolower($category) . '|' . strtolower($subCategory);
            if (isset($this->subcategories[$subKey])) {
                $resolved['subcategoryId'] = (int) $this->subcategories[$subKey]['subcategoryId'];
            }
        }

        $portion = trim((string) ($row['portion'] ?? ''));
        if ($portion !== '') {
            $pm = $this->portionMasters[strtolower($portion)];
            $resolved['portionMasterId'] = (int) $pm['portionMasterId'];
        }

        $code = trim((string) ($row['productCode'] ?? ''));
        if ($code !== '' && isset($this->productsByCode[strtolower($code)])) {
            $resolved['existingProductId'] = (int) $this->productsByCode[strtolower($code)]['productId'];
        } else {
            $name = trim((string) ($row['productName'] ?? ''));
            if ($name !== '' && isset($this->productsByName[strtolower($name)])) {
                $resolved['existingProductId'] = (int) $this->productsByName[strtolower($name)]['productId'];
            }
        }

        return $resolved;
    }

    private function enrichError($rowNum, array $row, array $err)
    {
        return array_merge(array(
            'row' => $rowNum,
            'productName' => isset($row['productName']) ? $row['productName'] : '',
            'category' => isset($row['category']) ? $row['category'] : '',
            'subCategory' => isset($row['subCategory']) ? $row['subCategory'] : '',
            'portion' => isset($row['portion']) ? $row['portion'] : '',
        ), $err);
    }
}
