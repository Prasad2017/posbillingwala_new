<?php
/**
 * Load PhpSpreadsheet autoloader from API vendor or Laravel admin vendor.
 */
if (!defined('CATALOG_VENDOR_LOADED')) {
    define('CATALOG_VENDOR_LOADED', true);

    $candidates = array(
        __DIR__ . '/../vendor/autoload.php',
        __DIR__ . '/../../admin.posbillingwala.com/vendor/autoload.php',
    );

    foreach ($candidates as $autoload) {
        if (file_exists($autoload)) {
            require_once $autoload;
            break;
        }
    }
}

require_once __DIR__ . '/catalog_constants.php';
require_once __DIR__ . '/catalog_helpers.php';
require_once __DIR__ . '/CatalogSimpleXlsx.php';
require_once __DIR__ . '/catalog_auth.php';
require_once __DIR__ . '/CatalogExcelService.php';
require_once __DIR__ . '/CatalogSessionManager.php';
require_once __DIR__ . '/CatalogProductValidator.php';
require_once __DIR__ . '/CatalogProductImporter.php';
require_once __DIR__ . '/CatalogProductExporter.php';
require_once __DIR__ . '/CatalogCategoryService.php';
require_once __DIR__ . '/CatalogSubcategoryService.php';
require_once __DIR__ . '/CatalogPortionService.php';
require_once __DIR__ . '/catalog_handlers.php';
