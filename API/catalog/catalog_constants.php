<?php

if (!defined('CATALOG_IMPORT_TYPES')) {
    define('CATALOG_IMPORT_TYPES', array('products', 'categories', 'subcategories', 'portions'));
}

if (!defined('CATALOG_SESSION_TTL_HOURS')) {
    define('CATALOG_SESSION_TTL_HOURS', 24);
}

if (!defined('CATALOG_MAX_UPLOAD_BYTES')) {
    define('CATALOG_MAX_UPLOAD_BYTES', 15 * 1024 * 1024);
}

/**
 * Product sheet column definitions (canonical header => internal key).
 */
function catalog_product_column_map()
{
    return array(
        'product name' => 'productName',
        'product' => 'productName',
        'product code' => 'productCode',
        'category' => 'category',
        'sub category' => 'subCategory',
        'subcategory' => 'subCategory',
        'portion' => 'portion',
        'price' => 'price',
        'gst' => 'gst',
        'unit' => 'unit',
        'status' => 'status',
    );
}

function catalog_product_required_columns()
{
    return array('productName', 'category');
}

function catalog_product_allowed_columns()
{
    return array(
        'productName',
        'productCode',
        'category',
        'subCategory',
        'portion',
        'price',
        'gst',
        'unit',
        'status',
    );
}

function catalog_category_column_map()
{
    return array(
        'category name' => 'categoryName',
        'category' => 'categoryName',
        'status' => 'status',
    );
}

function catalog_category_required_columns()
{
    return array('categoryName');
}

function catalog_subcategory_column_map()
{
    return array(
        'sub category name' => 'subcategoryName',
        'subcategory name' => 'subcategoryName',
        'sub category' => 'subcategoryName',
        'subcategory' => 'subcategoryName',
        'category name' => 'categoryName',
        'category' => 'categoryName',
        'status' => 'status',
    );
}

function catalog_subcategory_required_columns()
{
    return array('subcategoryName', 'categoryName');
}

function catalog_portion_column_map()
{
    return array(
        'portion name' => 'portionName',
        'portion' => 'portionName',
        'status' => 'status',
    );
}

function catalog_portion_required_columns()
{
    return array('portionName');
}

/**
 * @return array|null
 */
function catalog_import_type_config($type)
{
    $type = strtolower(trim((string) $type));
    $map = array(
        'products' => array(
            'sheetNames' => array('products', 'product'),
            'columnMap' => catalog_product_column_map(),
            'required' => catalog_product_required_columns(),
            'label' => 'Products',
            'templateLabel' => 'Products_Template',
            'exportLabel' => 'Products',
        ),
        'categories' => array(
            'sheetNames' => array('categories', 'category'),
            'columnMap' => catalog_category_column_map(),
            'required' => catalog_category_required_columns(),
            'label' => 'Categories',
            'templateLabel' => 'Categories_Template',
            'exportLabel' => 'Categories',
        ),
        'subcategories' => array(
            'sheetNames' => array('sub categories', 'subcategories', 'subcategory'),
            'columnMap' => catalog_subcategory_column_map(),
            'required' => catalog_subcategory_required_columns(),
            'label' => 'SubCategories',
            'templateLabel' => 'SubCategories_Template',
            'exportLabel' => 'SubCategories',
        ),
        'portions' => array(
            'sheetNames' => array('portions', 'portion'),
            'columnMap' => catalog_portion_column_map(),
            'required' => catalog_portion_required_columns(),
            'label' => 'Portions',
            'templateLabel' => 'Portions_Template',
            'exportLabel' => 'Portions',
        ),
    );
    return isset($map[$type]) ? array_merge(array('type' => $type), $map[$type]) : null;
}

function catalog_normalize_import_type($type)
{
    $config = catalog_import_type_config($type);
    return $config !== null ? $config['type'] : null;
}

function catalog_error_message($code)
{
    $messages = array(
        'CATEGORY_REQUIRED' => 'Category is required.',
        'CATEGORY_NOT_FOUND' => 'Category was not found.',
        'CATEGORY_NAME_REQUIRED' => 'Category Name is required.',
        'SUBCATEGORY_NOT_FOUND' => 'Sub Category was not found.',
        'SUBCATEGORY_NAME_REQUIRED' => 'Sub Category Name is required.',
        'SUBCATEGORY_CATEGORY_MISMATCH' => 'Sub Category does not belong to the selected Category.',
        'PORTION_NOT_FOUND' => 'Portion was not found.',
        'PORTION_NAME_REQUIRED' => 'Portion Name is required.',
        'PRODUCT_NAME_REQUIRED' => 'Product Name is required.',
        'DUPLICATE_PRODUCT_CODE' => 'Duplicate Product Code in catalog.',
        'DUPLICATE_CATEGORY' => 'Duplicate Category in catalog.',
        'DUPLICATE_SUBCATEGORY' => 'Duplicate Sub Category in catalog.',
        'DUPLICATE_PORTION' => 'Duplicate Portion in catalog.',
        'DUPLICATE_ROW' => 'Duplicate row in uploaded file.',
        'INVALID_PRICE' => 'Invalid price value.',
        'INVALID_GST' => 'Invalid GST value.',
        'INVALID_STATUS' => 'Invalid status value.',
        'INVALID_IMPORT_TYPE' => 'Invalid import type.',
        'INVALID_CUSTOMER' => 'Invalid customer.',
        'UNAUTHORIZED_CUSTOMER' => 'You are not authorized for this customer.',
        'INVALID_FILE' => 'Invalid file upload.',
        'INVALID_COLUMN' => 'Unexpected column in Excel file.',
        'MISSING_COLUMN' => 'Required column is missing.',
        'INVALID_FILE_TYPE' => 'Please upload a valid Excel (.xlsx) file.',
        'EMPTY_FILE' => 'No data rows found in Excel file.',
        'SESSION_NOT_FOUND' => 'Import session not found or expired.',
        'SESSION_ALREADY_IMPORTED' => 'This import was already confirmed.',
        'IMPORT_FAILED' => 'Import failed. No changes were saved.',
        'SPREADSHEET_UNAVAILABLE' => 'Excel library is not available on the server.',
    );

    return isset($messages[$code]) ? $messages[$code] : 'Validation error.';
}
