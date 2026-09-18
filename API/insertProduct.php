<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    mysqli_query($con, 'set names utf8');

    $userId = isset($_POST['userId']) ? $_POST['userId'] : '';
    $__postedUserId = $userId;
    pos_require_auth($con, $__postedUserId, isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized'));
    require_once __DIR__ . '/pos_staff.php';
    pos_require_permission($con, $userId, 'product.create');

    $categoryName = isset($_POST['categoryName']) ? trim((string)$_POST['categoryName']) : '';
    $postedCategoryId = isset($_POST['categoryId']) ? trim((string)$_POST['categoryId']) : '';
    $productName = isset($_POST['productName']) ? trim((string)$_POST['productName']) : '';
    $productCode = isset($_POST['productCode']) ? trim((string)$_POST['productCode']) : '';
    $productUnit = isset($_POST['productUnit']) ? trim((string)$_POST['productUnit']) : '';
    $productPrice = isset($_POST['productPrice']) ? $_POST['productPrice'] : '0';
    $productMrp = isset($_POST['productMrp']) ? $_POST['productMrp'] : '0';
    if (!is_numeric($productMrp) || (float)$productMrp <= 0) {
        $productMrp = $productPrice;
    }
    $productCGST = isset($_POST['productCGST']) ? $_POST['productCGST'] : '0';
    $productSGST = isset($_POST['productSGST']) ? $_POST['productSGST'] : '0';
    $productNetworkStatus = isset($_POST['productNetworkStatus']) ? trim((string)$_POST['productNetworkStatus']) : '';
    $productDeletedStatus = isset($_POST['productDeletedStatus']) ? $_POST['productDeletedStatus'] : '0';
    $subcategoryId = isset($_POST['subcategoryId']) ? trim((string)$_POST['subcategoryId']) : '';
    $subcategoryNetworkStatus = isset($_POST['subcategoryNetworkStatus']) ? trim((string)$_POST['subcategoryNetworkStatus']) : '';
    if ($subcategoryId === '' || $subcategoryId === '0' || $subcategoryId === 'null') {
        $subcategoryId = '';
    }

    $openPrice = isset($_POST['openPrice']) ? strtolower(trim((string)$_POST['openPrice'])) : 'off';
    if ($openPrice === 'on' || $openPrice === '1' || $openPrice === 'true' || $openPrice === 'yes') {
        $openPrice = 'on';
    } else {
        $openPrice = 'off';
    }

    $priceIncludesGst = isset($_POST['priceIncludesGst']) ? strtolower(trim((string)$_POST['priceIncludesGst'])) : '0';
    if ($priceIncludesGst === '1' || $priceIncludesGst === 'on' || $priceIncludesGst === 'true' || $priceIncludesGst === 'yes') {
        $priceIncludesGst = '1';
    } else {
        $priceIncludesGst = '0';
    }

    // Ensure optional columns exist.
    static $colsEnsured = false;
    if (!$colsEnsured) {
        require_once __DIR__ . '/php_compat.php';
        foreach (array(
            'productMrp' => "ALTER TABLE `products` ADD COLUMN `productMrp` DECIMAL(16,2) NOT NULL DEFAULT 0 AFTER `productPrice`",
            'openPrice' => "ALTER TABLE `products` ADD COLUMN `openPrice` VARCHAR(10) NOT NULL DEFAULT 'off' AFTER `productPrice`",
            'priceIncludesGst' => "ALTER TABLE `products` ADD COLUMN `priceIncludesGst` VARCHAR(10) NOT NULL DEFAULT '0' AFTER `openPrice`",
            'subcategoryId' => "ALTER TABLE `products` ADD COLUMN `subcategoryId` INT UNSIGNED NULL DEFAULT NULL AFTER `categoryId`",
            'productImage' => "ALTER TABLE `products` ADD COLUMN `productImage` MEDIUMTEXT NULL",
        ) as $name => $ddl) {
            $col = db_safe_query($con, "SHOW COLUMNS FROM `products` LIKE '" . $name . "'");
            if ($col && mysqli_num_rows($col) === 0) {
                db_safe_query($con, $ddl);
            }
            if ($col) {
                mysqli_free_result($col);
            }
        }
        $colsEnsured = true;
    }

    $hasProductImage = array_key_exists('productImage', $_POST);
    $productImage = $hasProductImage ? trim((string)$_POST['productImage']) : null;
    if ($productImage !== null && $productImage === '') {
        $productImage = null;
    }

    if ($productDeletedStatus == '1') {
        $productDeletedStatus = 'deactive';
    } else {
        $productDeletedStatus = 'active';
    }

    date_default_timezone_set('Asia/Kolkata');

    $userIdEsc = mysqli_real_escape_string($con, (string)$userId);
    $categoryNameEsc = mysqli_real_escape_string($con, $categoryName);
    $productNameEsc = mysqli_real_escape_string($con, $productName);
    $productCodeEsc = mysqli_real_escape_string($con, $productCode);
    $productUnitEsc = mysqli_real_escape_string($con, $productUnit);
    $productPriceEsc = mysqli_real_escape_string($con, (string)$productPrice);
    $productMrpEsc = mysqli_real_escape_string($con, (string)$productMrp);
    $productCGSTEsc = mysqli_real_escape_string($con, (string)$productCGST);
    $productSGSTEsc = mysqli_real_escape_string($con, (string)$productSGST);
    $openPriceEsc = mysqli_real_escape_string($con, $openPrice);
    $priceIncludesGstEsc = mysqli_real_escape_string($con, $priceIncludesGst);
    $productNetworkEsc = mysqli_real_escape_string($con, $productNetworkStatus);
    $productStatusEsc = mysqli_real_escape_string($con, $productDeletedStatus);
    $subcategoryEsc = mysqli_real_escape_string($con, $subcategoryId);

    /*
     * Resolve category reliably for offline-first sync:
     * 1) Match by categoryName for this user (local ids often differ from server)
     * 2) Else use posted categoryId if it belongs to this user
     * Never save a product without a resolved category.
     */
    $resolvedCategoryId = 0;
    if ($categoryName !== '') {
        $byName = mysqli_query(
            $con,
            "SELECT `categoryId`, `categoryName` FROM `categories`
             WHERE `userId`='$userIdEsc' AND `categoryName`='$categoryNameEsc'
             LIMIT 1"
        );
        if ($byName && ($rowByName = mysqli_fetch_assoc($byName))) {
            $resolvedCategoryId = (int)$rowByName['categoryId'];
        }
        if ($byName) {
            mysqli_free_result($byName);
        }
    }
    if ($resolvedCategoryId <= 0 && $postedCategoryId !== '' && $postedCategoryId !== '0' && ctype_digit((string)$postedCategoryId)) {
        $byId = mysqli_query(
            $con,
            "SELECT `categoryId`, `categoryName` FROM `categories`
             WHERE `userId`='$userIdEsc' AND `categoryId`='" . mysqli_real_escape_string($con, $postedCategoryId) . "'
             LIMIT 1"
        );
        if ($byId && ($rowById = mysqli_fetch_assoc($byId))) {
            $resolvedCategoryId = (int)$rowById['categoryId'];
            if ($categoryName === '' && !empty($rowById['categoryName'])) {
                $categoryName = $rowById['categoryName'];
                $categoryNameEsc = mysqli_real_escape_string($con, $categoryName);
            }
        }
        if ($byId) {
            mysqli_free_result($byId);
        }
    }

    if ($resolvedCategoryId <= 0) {
        $response['status'] = '0';
        $response['message'] = 'category not found — select a valid category';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $categoryIdEsc = (string)$resolvedCategoryId;
    if ($subcategoryNetworkStatus !== '') {
        $subNetEsc = mysqli_real_escape_string($con, $subcategoryNetworkStatus);
        $bySubNet = mysqli_query(
            $con,
            "SELECT `subcategoryId` FROM `product_subcategories`
             WHERE `userId`='$userIdEsc' AND `subcategoryNetworkStatus`='$subNetEsc'
             LIMIT 1"
        );
        if ($bySubNet && ($rowSubNet = mysqli_fetch_assoc($bySubNet))) {
            $subcategoryId = (string)$rowSubNet['subcategoryId'];
        }
        if ($bySubNet) {
            mysqli_free_result($bySubNet);
        }
    }
    if ($subcategoryId !== '') {
        $subEscCheck = mysqli_real_escape_string($con, $subcategoryId);
        $ownedSub = mysqli_query(
            $con,
            "SELECT `subcategoryId` FROM `product_subcategories`
             WHERE `userId`='$userIdEsc' AND `subcategoryId`='$subEscCheck'
             LIMIT 1"
        );
        if (!$ownedSub || !mysqli_fetch_assoc($ownedSub)) {
            $subcategoryId = '';
        }
        if ($ownedSub) {
            mysqli_free_result($ownedSub);
        }
    }
    $subcategoryEsc = mysqli_real_escape_string($con, $subcategoryId);
    $subSqlSet = ($subcategoryId !== '')
        ? ", `subcategoryId`='$subcategoryEsc'"
        : ", `subcategoryId`=NULL";
    $imageSql = '';
    if ($hasProductImage) {
        if ($productImage === null) {
            $imageSql = ", `productImage`=NULL";
        } else {
            $imageEsc = mysqli_real_escape_string($con, $productImage);
            $imageSql = ", `productImage`='$imageEsc'";
        }
    }

    $existing = null;
    if ($productNetworkStatus !== '') {
        $find = mysqli_query(
            $con,
            "SELECT * FROM `products`
             WHERE `userId`='$userIdEsc' AND `productNetworkStatus`='$productNetworkEsc'
             LIMIT 1"
        );
        if ($find) {
            $existing = mysqli_fetch_assoc($find);
            mysqli_free_result($find);
        }
    }

    if ($existing) {
        $productIdEsc = mysqli_real_escape_string($con, (string)$existing['productId']);
        $sql = "UPDATE `products` SET
                  `categoryId`='$categoryIdEsc',
                  `productCode`='$productCodeEsc',
                  `productName`='$productNameEsc',
                  `productPrice`='$productPriceEsc',
                  `productMrp`='$productMrpEsc',
                  `productUnit`='$productUnitEsc',
                  `productCGST`='$productCGSTEsc',
                  `productSGST`='$productSGSTEsc',
                  `openPrice`='$openPriceEsc',
                  `priceIncludesGst`='$priceIncludesGstEsc',
                  `productStatus`='$productStatusEsc'
                  $subSqlSet
                  $imageSql
                WHERE `productId`='$productIdEsc' AND `userId`='$userIdEsc'";
        if (mysqli_query($con, $sql)) {
            $response['status'] = '1';
            $response['message'] = 'update successful!';
            $response['categoryId'] = $resolvedCategoryId;
            $response['categoryName'] = $categoryName;
        } else {
            $response['status'] = '0';
            $response['message'] = 'update failed!';
        }
    } else {
        $subCol = ($subcategoryId !== '') ? ', `subcategoryId`' : '';
        $subVal = ($subcategoryId !== '') ? ", '$subcategoryEsc'" : '';
        $imageCol = '';
        $imageVal = '';
        if ($hasProductImage && $productImage !== null) {
            $imageEsc = mysqli_real_escape_string($con, $productImage);
            $imageCol = ', `productImage`';
            $imageVal = ", '$imageEsc'";
        }
        $sql = "INSERT INTO `products`(
                    `userId`, `categoryId`, `productCode`, `productName`, `productPrice`, `productMrp`,
                    `productUnit`, `productCGST`, `productSGST`, `openPrice`, `priceIncludesGst`,
                    `productNetworkStatus`, `productStatus`$subCol$imageCol
                ) VALUES (
                    '$userIdEsc', '$categoryIdEsc', '$productCodeEsc', '$productNameEsc', '$productPriceEsc',
                    '$productMrpEsc', '$productUnitEsc', '$productCGSTEsc', '$productSGSTEsc',
                    '$openPriceEsc', '$priceIncludesGstEsc', '$productNetworkEsc', '$productStatusEsc'
                    $subVal$imageVal
                )";
        if (mysqli_query($con, $sql)) {
            $response['status'] = '1';
            $response['message'] = 'insert successful!';
            $response['categoryId'] = $resolvedCategoryId;
            $response['categoryName'] = $categoryName;
            $response['productId'] = mysqli_insert_id($con);
        } else {
            $response['status'] = '0';
            $response['message'] = 'insert failed!';
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
