<?php
/**
 * Copy catalog (categories → subcategories → portions → products → combos)
 * from one userId scope to another (HQ customer id or POS license id).
 */

require_once __DIR__ . '/../db_prepared.php';

if (!function_exists('catalog_push_norm_name')) {
    function catalog_push_norm_name($value)
    {
        return strtolower(trim((string) $value));
    }
}

if (!function_exists('catalog_push_parse_ids')) {
    /**
     * @param string $csv
     * @return int[]
     */
    function catalog_push_parse_ids($csv)
    {
        $ids = array();
        foreach (preg_split('/[,\s]+/', (string) $csv) as $part) {
            $id = (int) $part;
            if ($id > 0) {
                $ids[$id] = $id;
            }
        }
        return array_values($ids);
    }
}

if (!function_exists('catalog_push_product_key')) {
    function catalog_push_product_key(array $row)
    {
        $code = trim((string) (isset($row['productCode']) ? $row['productCode'] : ''));
        if ($code !== '' && $code !== '0') {
            return 'c:' . catalog_push_norm_name($code);
        }
        return 'n:' . catalog_push_norm_name(isset($row['productName']) ? $row['productName'] : '');
    }
}

if (!function_exists('catalog_push_network_id')) {
    function catalog_push_network_id($prefix, $targetUserId)
    {
        return $prefix . (int) $targetUserId . '_' . bin2hex(random_bytes(6));
    }
}

if (!function_exists('catalog_push_has_column')) {
    function catalog_push_has_column($con, $table, $column)
    {
        static $cache = array();
        $key = $table . '.' . $column;
        if (isset($cache[$key])) {
            return $cache[$key];
        }
        $count = db_stmt_scalar_int(
            $con,
            'SELECT COUNT(*) AS c FROM INFORMATION_SCHEMA.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?',
            'ss',
            $table,
            $column
        );
        $cache[$key] = $count > 0;
        return $cache[$key];
    }
}

if (!function_exists('catalog_push_has_table')) {
    function catalog_push_has_table($con, $table)
    {
        static $cache = array();
        if (isset($cache[$table])) {
            return $cache[$table];
        }
        $count = db_stmt_scalar_int(
            $con,
            'SELECT COUNT(*) AS c FROM INFORMATION_SCHEMA.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?',
            's',
            $table
        );
        $cache[$table] = $count > 0;
        return $cache[$table];
    }
}

if (!function_exists('catalog_push_count_products')) {
    function catalog_push_count_products($con, $userId)
    {
        return db_stmt_scalar_int(
            $con,
            'SELECT COUNT(*) AS c FROM `products` WHERE `userId`=? AND IFNULL(`productStatus`,\'active\') <> \'deleted\'',
            'i',
            (int) $userId
        );
    }
}

if (!function_exists('catalog_push_resolve_source')) {
    /**
     * @return array{userId:int,label:string,mode:string}|null
     */
    function catalog_push_resolve_source($con, $ownerUserId, $sourceMode, $sourceBranchId)
    {
        $ownerUserId = (int) $ownerUserId;
        $sourceMode = strtolower(trim((string) $sourceMode));
        if ($sourceMode === '') {
            $sourceMode = 'hq';
        }

        if ($sourceMode === 'branch') {
            $branchId = (int) $sourceBranchId;
            if ($branchId <= 0) {
                return null;
            }
            $lic = db_stmt_fetch_one(
                $con,
                'SELECT `id`, `userId`, `userType`, `userName` FROM `licenses` WHERE `id`=? LIMIT 1',
                'i',
                $branchId
            );
            if ($lic === null || (int) $lic['userId'] !== $ownerUserId) {
                return null;
            }
            require_once __DIR__ . '/../licence_expiry.php';
            $branch = licence_branch_fields($lic);
            return array(
                'userId' => (int) $lic['id'],
                'label' => $branch['branchLabel'],
                'mode' => 'branch',
            );
        }

        $hqCount = catalog_push_count_products($con, $ownerUserId);
        if ($hqCount > 0) {
            return array(
                'userId' => $ownerUserId,
                'label' => 'HQ catalog (Owner app)',
                'mode' => 'hq',
            );
        }

        $main = db_stmt_fetch_one(
            $con,
            'SELECT `id`, `userId`, `userType`, `userName` FROM `licenses`
             WHERE `userId`=? ORDER BY CASE WHEN LOWER(`userType`)=\'owner\' THEN 0 ELSE 1 END, `id` ASC LIMIT 1',
            'i',
            $ownerUserId
        );
        if ($main === null) {
            return array(
                'userId' => $ownerUserId,
                'label' => 'HQ catalog (Owner app)',
                'mode' => 'hq',
            );
        }
        require_once __DIR__ . '/../licence_expiry.php';
        $branch = licence_branch_fields($main);
        return array(
            'userId' => (int) $main['id'],
            'label' => $branch['branchLabel'] . ' (HQ empty — used Main Store)',
            'mode' => 'branch',
        );
    }
}

if (!function_exists('catalog_push_copy_to_user')) {
    /**
     * Upsert catalog from $sourceUserId into $targetUserId (POS license id or HQ).
     *
     * @return array
     */
    function catalog_push_copy_to_user($con, $sourceUserId, $targetUserId)
    {
        $sourceUserId = (int) $sourceUserId;
        $targetUserId = (int) $targetUserId;
        $stats = array(
            'categoriesCopied' => 0,
            'categoriesUpdated' => 0,
            'subcategoriesCopied' => 0,
            'subcategoriesUpdated' => 0,
            'portionsCopied' => 0,
            'portionsUpdated' => 0,
            'productsCopied' => 0,
            'productsUpdated' => 0,
            'combosCopied' => 0,
            'combosUpdated' => 0,
        );

        if ($sourceUserId === $targetUserId) {
            return $stats;
        }

        $categoryMap = catalog_push_copy_categories($con, $sourceUserId, $targetUserId, $stats);
        $subMap = catalog_push_copy_subcategories($con, $sourceUserId, $targetUserId, $categoryMap, $stats);
        $portionMasterMap = catalog_push_copy_portion_master($con, $sourceUserId, $targetUserId, $stats);
        $productMap = catalog_push_copy_products($con, $sourceUserId, $targetUserId, $categoryMap, $subMap, $stats);
        catalog_push_copy_product_portions($con, $sourceUserId, $targetUserId, $productMap, $portionMasterMap, $stats);
        catalog_push_copy_combos($con, $sourceUserId, $targetUserId, $productMap, $stats);

        return $stats;
    }
}

if (!function_exists('catalog_push_copy_categories')) {
    function catalog_push_copy_categories($con, $sourceUserId, $targetUserId, array &$stats)
    {
        $hasFood = catalog_push_has_column($con, 'categories', 'foodTypeId');
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `categories` WHERE `userId`=?',
            'i',
            $sourceUserId
        );
        $existing = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `categories` WHERE `userId`=?',
            'i',
            $targetUserId
        );
        $byName = array();
        foreach ($existing as $row) {
            $byName[catalog_push_norm_name($row['categoryName'])] = $row;
        }
        $map = array();
        foreach ($rows as $row) {
            $name = (string) $row['categoryName'];
            $key = catalog_push_norm_name($name);
            $status = isset($row['categoryStatus']) ? $row['categoryStatus'] : 'active';
            $foodTypeId = ($hasFood && isset($row['foodTypeId']) && $row['foodTypeId'] !== '') ? (int) $row['foodTypeId'] : null;
            if (isset($byName[$key])) {
                $id = (int) $byName[$key]['categoryId'];
                if ($hasFood) {
                    db_stmt_execute(
                        $con,
                        'UPDATE `categories` SET `categoryStatus`=?, `foodTypeId`=? WHERE `categoryId`=? AND `userId`=?',
                        'siii',
                        $status,
                        $foodTypeId,
                        $id,
                        $targetUserId
                    );
                } else {
                    db_stmt_execute(
                        $con,
                        'UPDATE `categories` SET `categoryStatus`=? WHERE `categoryId`=? AND `userId`=?',
                        'sii',
                        $status,
                        $id,
                        $targetUserId
                    );
                }
                $stats['categoriesUpdated']++;
            } else {
                $net = isset($row['categoryNetworkStatus']) && $row['categoryNetworkStatus'] !== ''
                    ? catalog_push_network_id('cat', $targetUserId)
                    : catalog_push_network_id('cat', $targetUserId);
                $dealerId = isset($row['dealerId']) ? $row['dealerId'] : null;
                if ($hasFood) {
                    $id = db_stmt_insert_id(
                        $con,
                        'INSERT INTO `categories` (`userId`, `dealerId`, `categoryName`, `foodTypeId`, `categoryNetworkStatus`, `categoryStatus`, `created_at`, `updated_at`)
                         VALUES (?,?,?,?,?,?,NOW(),NOW())',
                        'iissss',
                        $targetUserId,
                        $dealerId,
                        $name,
                        $foodTypeId,
                        $net,
                        $status
                    );
                } else {
                    $id = db_stmt_insert_id(
                        $con,
                        'INSERT INTO `categories` (`userId`, `dealerId`, `categoryName`, `categoryNetworkStatus`, `categoryStatus`, `created_at`, `updated_at`)
                         VALUES (?,?,?,?,?,NOW(),NOW())',
                        'iisss',
                        $targetUserId,
                        $dealerId,
                        $name,
                        $net,
                        $status
                    );
                }
                if ($id === false) {
                    continue;
                }
                $stats['categoriesCopied']++;
            }
            $map[(int) $row['categoryId']] = (int) $id;
        }
        return $map;
    }
}

if (!function_exists('catalog_push_copy_subcategories')) {
    function catalog_push_copy_subcategories($con, $sourceUserId, $targetUserId, array $categoryMap, array &$stats)
    {
        if (!catalog_push_has_table($con, 'product_subcategories')) {
            return array();
        }
        $hasSort = catalog_push_has_column($con, 'product_subcategories', 'subcategorySortOrder');
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `product_subcategories` WHERE `userId`=?',
            'i',
            $sourceUserId
        );
        $existing = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `product_subcategories` WHERE `userId`=?',
            'i',
            $targetUserId
        );
        $byKey = array();
        foreach ($existing as $row) {
            $byKey[(int) $row['categoryId'] . ':' . catalog_push_norm_name($row['subcategoryName'])] = $row;
        }
        $map = array();
        foreach ($rows as $row) {
            $srcCat = (int) $row['categoryId'];
            if (!isset($categoryMap[$srcCat])) {
                continue;
            }
            $tgtCat = $categoryMap[$srcCat];
            $name = (string) $row['subcategoryName'];
            $lookup = $tgtCat . ':' . catalog_push_norm_name($name);
            $status = isset($row['subcategoryStatus']) ? $row['subcategoryStatus'] : 'active';
            $sort = $hasSort && isset($row['subcategorySortOrder']) ? (int) $row['subcategorySortOrder'] : 0;
            if (isset($byKey[$lookup])) {
                $id = (int) $byKey[$lookup]['subcategoryId'];
                if ($hasSort) {
                    db_stmt_execute(
                        $con,
                        'UPDATE `product_subcategories` SET `subcategoryStatus`=?, `subcategorySortOrder`=? WHERE `subcategoryId`=? AND `userId`=?',
                        'siii',
                        $status,
                        $sort,
                        $id,
                        $targetUserId
                    );
                } else {
                    db_stmt_execute(
                        $con,
                        'UPDATE `product_subcategories` SET `subcategoryStatus`=? WHERE `subcategoryId`=? AND `userId`=?',
                        'sii',
                        $status,
                        $id,
                        $targetUserId
                    );
                }
                $stats['subcategoriesUpdated']++;
            } else {
                $net = catalog_push_network_id('sub', $targetUserId);
                if ($hasSort) {
                    $id = db_stmt_insert_id(
                        $con,
                        'INSERT INTO `product_subcategories` (`userId`, `categoryId`, `subcategoryName`, `subcategoryNetworkStatus`, `subcategoryStatus`, `subcategorySortOrder`, `created_at`, `updated_at`)
                         VALUES (?,?,?,?,?,?,NOW(),NOW())',
                        'iisssi',
                        $targetUserId,
                        $tgtCat,
                        $name,
                        $net,
                        $status,
                        $sort
                    );
                } else {
                    $id = db_stmt_insert_id(
                        $con,
                        'INSERT INTO `product_subcategories` (`userId`, `categoryId`, `subcategoryName`, `subcategoryNetworkStatus`, `subcategoryStatus`, `created_at`, `updated_at`)
                         VALUES (?,?,?,?,?,NOW(),NOW())',
                        'iisss',
                        $targetUserId,
                        $tgtCat,
                        $name,
                        $net,
                        $status
                    );
                }
                if ($id === false) {
                    continue;
                }
                $stats['subcategoriesCopied']++;
            }
            $map[(int) $row['subcategoryId']] = (int) $id;
        }
        return $map;
    }
}

if (!function_exists('catalog_push_copy_portion_master')) {
    function catalog_push_copy_portion_master($con, $sourceUserId, $targetUserId, array &$stats)
    {
        if (!catalog_push_has_table($con, 'portion_master')) {
            return array();
        }
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `portion_master` WHERE `userId`=?',
            'i',
            $sourceUserId
        );
        $existing = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `portion_master` WHERE `userId`=?',
            'i',
            $targetUserId
        );
        $byName = array();
        foreach ($existing as $row) {
            $byName[catalog_push_norm_name($row['portionName'])] = $row;
        }
        $map = array();
        foreach ($rows as $row) {
            $name = (string) $row['portionName'];
            $key = catalog_push_norm_name($name);
            $status = isset($row['portionMasterStatus']) ? $row['portionMasterStatus'] : 'active';
            if (isset($byName[$key])) {
                $id = (int) $byName[$key]['portionMasterId'];
                db_stmt_execute(
                    $con,
                    'UPDATE `portion_master` SET `portionMasterStatus`=? WHERE `portionMasterId`=? AND `userId`=?',
                    'sii',
                    $status,
                    $id,
                    $targetUserId
                );
                $stats['portionsUpdated']++;
            } else {
                $net = catalog_push_network_id('pm', $targetUserId);
                $id = db_stmt_insert_id(
                    $con,
                    'INSERT INTO `portion_master` (`userId`, `portionName`, `portionMasterNetworkStatus`, `portionMasterStatus`, `created_at`, `updated_at`)
                     VALUES (?,?,?,?,NOW(),NOW())',
                    'isss',
                    $targetUserId,
                    $name,
                    $net,
                    $status
                );
                if ($id === false) {
                    continue;
                }
                $stats['portionsCopied']++;
            }
            $map[(int) $row['portionMasterId']] = (int) $id;
        }
        return $map;
    }
}

if (!function_exists('catalog_push_copy_products')) {
    function catalog_push_copy_products($con, $sourceUserId, $targetUserId, array $categoryMap, array $subMap, array &$stats)
    {
        $hasOpen = catalog_push_has_column($con, 'products', 'openPrice');
        $hasSub = catalog_push_has_column($con, 'products', 'subcategoryId');
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `products` WHERE `userId`=?',
            'i',
            $sourceUserId
        );
        $existing = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `products` WHERE `userId`=?',
            'i',
            $targetUserId
        );
        $byKey = array();
        foreach ($existing as $row) {
            $k = catalog_push_product_key($row);
            if ($k !== 'c:' && $k !== 'n:') {
                $byKey[$k] = $row;
            }
        }
        $map = array();
        foreach ($rows as $row) {
            $srcCat = (int) $row['categoryId'];
            if (!isset($categoryMap[$srcCat])) {
                continue;
            }
            $tgtCat = $categoryMap[$srcCat];
            $tgtSub = null;
            if ($hasSub && isset($row['subcategoryId']) && $row['subcategoryId'] !== '' && isset($subMap[(int) $row['subcategoryId']])) {
                $tgtSub = $subMap[(int) $row['subcategoryId']];
            }
            $key = catalog_push_product_key($row);
            $code = (string) $row['productCode'];
            $name = (string) $row['productName'];
            $unit = (string) $row['productUnit'];
            $price = (string) $row['productPrice'];
            $cgst = isset($row['productCGST']) ? (string) $row['productCGST'] : '0';
            $sgst = isset($row['productSGST']) ? (string) $row['productSGST'] : '0';
            $status = isset($row['productStatus']) ? $row['productStatus'] : 'active';
            $open = $hasOpen && isset($row['openPrice']) ? $row['openPrice'] : 'off';
            $dealerId = isset($row['dealerId']) ? $row['dealerId'] : null;

            if (isset($byKey[$key])) {
                $id = (int) $byKey[$key]['productId'];
                if ($hasOpen && $hasSub) {
                    db_stmt_execute(
                        $con,
                        'UPDATE `products` SET `categoryId`=?, `subcategoryId`=?, `productCode`=?, `productName`=?, `productUnit`=?, `productPrice`=?, `productCGST`=?, `productSGST`=?, `openPrice`=?, `productStatus`=?, `updated_at`=NOW()
                         WHERE `productId`=? AND `userId`=?',
                        'iissssssssii',
                        $tgtCat,
                        $tgtSub,
                        $code,
                        $name,
                        $unit,
                        $price,
                        $cgst,
                        $sgst,
                        $open,
                        $status,
                        $id,
                        $targetUserId
                    );
                } elseif ($hasSub) {
                    db_stmt_execute(
                        $con,
                        'UPDATE `products` SET `categoryId`=?, `subcategoryId`=?, `productCode`=?, `productName`=?, `productUnit`=?, `productPrice`=?, `productCGST`=?, `productSGST`=?, `productStatus`=?, `updated_at`=NOW()
                         WHERE `productId`=? AND `userId`=?',
                        'iisssssssii',
                        $tgtCat,
                        $tgtSub,
                        $code,
                        $name,
                        $unit,
                        $price,
                        $cgst,
                        $sgst,
                        $status,
                        $id,
                        $targetUserId
                    );
                } else {
                    db_stmt_execute(
                        $con,
                        'UPDATE `products` SET `categoryId`=?, `productCode`=?, `productName`=?, `productUnit`=?, `productPrice`=?, `productCGST`=?, `productSGST`=?, `productStatus`=?, `updated_at`=NOW()
                         WHERE `productId`=? AND `userId`=?',
                        'isssssssii',
                        $tgtCat,
                        $code,
                        $name,
                        $unit,
                        $price,
                        $cgst,
                        $sgst,
                        $status,
                        $id,
                        $targetUserId
                    );
                }
                $stats['productsUpdated']++;
            } else {
                $net = catalog_push_network_id('prd', $targetUserId);
                if ($hasOpen && $hasSub) {
                    $id = db_stmt_insert_id(
                        $con,
                        'INSERT INTO `products` (`userId`, `dealerId`, `categoryId`, `subcategoryId`, `productCode`, `productName`, `productUnit`, `productPrice`, `productCGST`, `productSGST`, `openPrice`, `productNetworkStatus`, `productStatus`, `created_at`, `updated_at`)
                         VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())',
                        'iiiisssssssss',
                        $targetUserId,
                        $dealerId,
                        $tgtCat,
                        $tgtSub,
                        $code,
                        $name,
                        $unit,
                        $price,
                        $cgst,
                        $sgst,
                        $open,
                        $net,
                        $status
                    );
                } elseif ($hasSub) {
                    $id = db_stmt_insert_id(
                        $con,
                        'INSERT INTO `products` (`userId`, `dealerId`, `categoryId`, `subcategoryId`, `productCode`, `productName`, `productUnit`, `productPrice`, `productCGST`, `productSGST`, `productNetworkStatus`, `productStatus`, `created_at`, `updated_at`)
                         VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())',
                        'iiiissssssss',
                        $targetUserId,
                        $dealerId,
                        $tgtCat,
                        $tgtSub,
                        $code,
                        $name,
                        $unit,
                        $price,
                        $cgst,
                        $sgst,
                        $net,
                        $status
                    );
                } else {
                    $id = db_stmt_insert_id(
                        $con,
                        'INSERT INTO `products` (`userId`, `dealerId`, `categoryId`, `productCode`, `productName`, `productUnit`, `productPrice`, `productCGST`, `productSGST`, `productNetworkStatus`, `productStatus`, `created_at`, `updated_at`)
                         VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())',
                        'iiissssssss',
                        $targetUserId,
                        $dealerId,
                        $tgtCat,
                        $code,
                        $name,
                        $unit,
                        $price,
                        $cgst,
                        $sgst,
                        $net,
                        $status
                    );
                }
                if ($id === false) {
                    continue;
                }
                $stats['productsCopied']++;
            }
            $map[(int) $row['productId']] = (int) $id;
        }
        return $map;
    }
}

if (!function_exists('catalog_push_copy_product_portions')) {
    function catalog_push_copy_product_portions($con, $sourceUserId, $targetUserId, array $productMap, array $portionMasterMap, array &$stats)
    {
        if (!catalog_push_has_table($con, 'product_portions')) {
            return;
        }
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `product_portions` WHERE `userId`=?',
            'i',
            $sourceUserId
        );
        foreach ($rows as $row) {
            $srcPid = (int) $row['productId'];
            if (!isset($productMap[$srcPid])) {
                continue;
            }
            $tgtPid = $productMap[$srcPid];
            $tgtPmid = null;
            if (isset($row['portionMasterId']) && $row['portionMasterId'] !== '' && isset($portionMasterMap[(int) $row['portionMasterId']])) {
                $tgtPmid = $portionMasterMap[(int) $row['portionMasterId']];
            }
            $name = (string) $row['portionName'];
            $price = (string) $row['portionPrice'];
            $sort = isset($row['portionSortOrder']) ? (int) $row['portionSortOrder'] : 0;
            $status = isset($row['portionStatus']) ? $row['portionStatus'] : 'active';

            $existing = null;
            if ($tgtPmid !== null) {
                $existing = db_stmt_fetch_one(
                    $con,
                    'SELECT `portionId` FROM `product_portions` WHERE `userId`=? AND `productId`=? AND `portionMasterId`=? LIMIT 1',
                    'iii',
                    $targetUserId,
                    $tgtPid,
                    $tgtPmid
                );
            }
            if ($existing === null) {
                $existing = db_stmt_fetch_one(
                    $con,
                    'SELECT `portionId` FROM `product_portions` WHERE `userId`=? AND `productId`=? AND LOWER(`portionName`)=? LIMIT 1',
                    'iis',
                    $targetUserId,
                    $tgtPid,
                    catalog_push_norm_name($name)
                );
            }
            if ($existing !== null) {
                db_stmt_execute(
                    $con,
                    'UPDATE `product_portions` SET `portionName`=?, `portionPrice`=?, `portionSortOrder`=?, `portionStatus`=?, `portionMasterId`=?, `updated_at`=NOW()
                     WHERE `portionId`=?',
                    'ssisii',
                    $name,
                    $price,
                    $sort,
                    $status,
                    $tgtPmid,
                    (int) $existing['portionId']
                );
            } else {
                $net = catalog_push_network_id('pp', $targetUserId);
                db_stmt_insert_id(
                    $con,
                    'INSERT INTO `product_portions` (`userId`, `productId`, `portionMasterId`, `portionName`, `portionPrice`, `portionSortOrder`, `portionNetworkStatus`, `portionStatus`, `created_at`, `updated_at`)
                     VALUES (?,?,?,?,?,?,?,?,NOW(),NOW())',
                    'iiississ',
                    $targetUserId,
                    $tgtPid,
                    $tgtPmid,
                    $name,
                    $price,
                    $sort,
                    $net,
                    $status
                );
            }
        }
    }
}

if (!function_exists('catalog_push_copy_combos')) {
    function catalog_push_copy_combos($con, $sourceUserId, $targetUserId, array $productMap, array &$stats)
    {
        if (!catalog_push_has_table($con, 'combos')) {
            return;
        }
        $rows = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `combos` WHERE `userId`=?',
            'i',
            $sourceUserId
        );
        $comboMap = array();
        foreach ($rows as $row) {
            $name = (string) $row['comboName'];
            $code = isset($row['comboCode']) ? (string) $row['comboCode'] : '';
            $existing = null;
            if ($code !== '') {
                $existing = db_stmt_fetch_one(
                    $con,
                    'SELECT `comboId` FROM `combos` WHERE `userId`=? AND `comboCode`=? LIMIT 1',
                    'is',
                    $targetUserId,
                    $code
                );
            }
            if ($existing === null) {
                $existing = db_stmt_fetch_one(
                    $con,
                    'SELECT `comboId` FROM `combos` WHERE `userId`=? AND LOWER(`comboName`)=? LIMIT 1',
                    'is',
                    $targetUserId,
                    catalog_push_norm_name($name)
                );
            }
            $price = (string) $row['comboPrice'];
            $cgst = isset($row['comboCGST']) ? (string) $row['comboCGST'] : '';
            $sgst = isset($row['comboSGST']) ? (string) $row['comboSGST'] : '';
            $withGst = isset($row['comboWithGSTPrice']) ? (string) $row['comboWithGSTPrice'] : '';
            $active = isset($row['comboActiveStatus']) ? (string) $row['comboActiveStatus'] : '1';
            $status = isset($row['comboStatus']) ? $row['comboStatus'] : 'active';
            $sort = isset($row['comboSortOrder']) ? (int) $row['comboSortOrder'] : 0;

            if ($existing !== null) {
                $id = (int) $existing['comboId'];
                db_stmt_execute(
                    $con,
                    'UPDATE `combos` SET `comboName`=?, `comboCode`=?, `comboPrice`=?, `comboCGST`=?, `comboSGST`=?, `comboWithGSTPrice`=?, `comboActiveStatus`=?, `comboStatus`=?, `comboSortOrder`=?, `updated_at`=NOW()
                     WHERE `comboId`=?',
                    'ssssssssii',
                    $name,
                    $code,
                    $price,
                    $cgst,
                    $sgst,
                    $withGst,
                    $active,
                    $status,
                    $sort,
                    $id
                );
                $stats['combosUpdated']++;
            } else {
                $net = catalog_push_network_id('cmb', $targetUserId);
                $id = db_stmt_insert_id(
                    $con,
                    'INSERT INTO `combos` (`userId`, `comboName`, `comboCode`, `comboPrice`, `comboCGST`, `comboSGST`, `comboWithGSTPrice`, `comboActiveStatus`, `comboNetworkStatus`, `comboStatus`, `comboSortOrder`, `created_at`, `updated_at`)
                     VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())',
                    'isssssssssi',
                    $targetUserId,
                    $name,
                    $code,
                    $price,
                    $cgst,
                    $sgst,
                    $withGst,
                    $active,
                    $net,
                    $status,
                    $sort
                );
                if ($id === false) {
                    continue;
                }
                $stats['combosCopied']++;
            }
            $comboMap[(int) $row['comboId']] = (int) $id;
        }

        if (!catalog_push_has_table($con, 'combo_items') || empty($comboMap)) {
            return;
        }
        $items = db_stmt_fetch_all(
            $con,
            'SELECT * FROM `combo_items` WHERE `userId`=?',
            'i',
            $sourceUserId
        );
        foreach ($items as $item) {
            $srcCombo = (int) $item['comboId'];
            if (!isset($comboMap[$srcCombo])) {
                continue;
            }
            $tgtCombo = $comboMap[$srcCombo];
            $tgtPid = null;
            if (isset($item['productId']) && $item['productId'] !== '' && isset($productMap[(int) $item['productId']])) {
                $tgtPid = $productMap[(int) $item['productId']];
            }
            $qty = isset($item['comboItemQuantity']) ? (string) $item['comboItemQuantity'] : '1';
            $sort = isset($item['comboItemSortOrder']) ? (int) $item['comboItemSortOrder'] : 0;
            $status = isset($item['comboItemStatus']) ? $item['comboItemStatus'] : 'active';

            $existingItem = null;
            if ($tgtPid !== null) {
                $existingItem = db_stmt_fetch_one(
                    $con,
                    'SELECT `comboItemId` FROM `combo_items` WHERE `userId`=? AND `comboId`=? AND `productId`=? LIMIT 1',
                    'iii',
                    $targetUserId,
                    $tgtCombo,
                    $tgtPid
                );
            }
            if ($existingItem !== null) {
                db_stmt_execute(
                    $con,
                    'UPDATE `combo_items` SET `comboItemQuantity`=?, `comboItemSortOrder`=?, `comboItemStatus`=?, `updated_at`=NOW()
                     WHERE `comboItemId`=?',
                    'sisi',
                    $qty,
                    $sort,
                    $status,
                    (int) $existingItem['comboItemId']
                );
            } else {
                $net = catalog_push_network_id('ci', $targetUserId);
                db_stmt_insert_id(
                    $con,
                    'INSERT INTO `combo_items` (`userId`, `comboId`, `productId`, `comboItemQuantity`, `comboItemSortOrder`, `comboItemNetworkStatus`, `comboItemStatus`, `created_at`, `updated_at`)
                     VALUES (?,?,?,?,?,?,?,NOW(),NOW())',
                    'iiisiss',
                    $targetUserId,
                    $tgtCombo,
                    $tgtPid,
                    $qty,
                    $sort,
                    $net,
                    $status
                );
            }
        }
    }
}
