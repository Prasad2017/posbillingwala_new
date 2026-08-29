<?php
/**
 * Build error Excel rows for any catalog import type.
 */
class CatalogErrorExporter
{
    public static function buildErrorSheetRows($importType, array $errors)
    {
        $headers = self::headersForType($importType);
        $rows = array($headers);
        foreach ($errors as $err) {
            $rows[] = self::errorToRow($importType, $err);
        }
        return $rows;
    }

    public static function buildErrorSpreadsheet($importType, array $errors)
    {
        if (!class_exists('\PhpOffice\PhpSpreadsheet\Spreadsheet')) {
            return null;
        }
        $spreadsheet = new \PhpOffice\PhpSpreadsheet\Spreadsheet();
        $sheet = $spreadsheet->getActiveSheet();
        $sheet->setTitle('Errors');
        $rows = self::buildErrorSheetRows($importType, $errors);
        $sheet->fromArray($rows, null, 'A1');
        return $spreadsheet;
    }

    private static function headersForType($importType)
    {
        switch ($importType) {
            case 'categories':
                return array('Row Number', 'Category Name', 'Status', 'Error', 'Error Code');
            case 'subcategories':
                return array('Row Number', 'Sub Category Name', 'Category Name', 'Status', 'Error', 'Error Code');
            case 'portions':
                return array('Row Number', 'Portion Name', 'Status', 'Error', 'Error Code');
            case 'products':
            default:
                return array('Row Number', 'Product Name', 'Category', 'Sub Category', 'Portion', 'Error', 'Error Code');
        }
    }

    private static function errorToRow($importType, array $err)
    {
        switch ($importType) {
            case 'categories':
                return array(
                    isset($err['row']) ? $err['row'] : '',
                    isset($err['categoryName']) ? $err['categoryName'] : '',
                    isset($err['status']) ? $err['status'] : '',
                    isset($err['message']) ? $err['message'] : '',
                    isset($err['code']) ? $err['code'] : '',
                );
            case 'subcategories':
                return array(
                    isset($err['row']) ? $err['row'] : '',
                    isset($err['subcategoryName']) ? $err['subcategoryName'] : '',
                    isset($err['categoryName']) ? $err['categoryName'] : '',
                    isset($err['status']) ? $err['status'] : '',
                    isset($err['message']) ? $err['message'] : '',
                    isset($err['code']) ? $err['code'] : '',
                );
            case 'portions':
                return array(
                    isset($err['row']) ? $err['row'] : '',
                    isset($err['portionName']) ? $err['portionName'] : '',
                    isset($err['status']) ? $err['status'] : '',
                    isset($err['message']) ? $err['message'] : '',
                    isset($err['code']) ? $err['code'] : '',
                );
            default:
                return array(
                    isset($err['row']) ? $err['row'] : '',
                    isset($err['productName']) ? $err['productName'] : '',
                    isset($err['category']) ? $err['category'] : '',
                    isset($err['subCategory']) ? $err['subCategory'] : '',
                    isset($err['portion']) ? $err['portion'] : '',
                    isset($err['message']) ? $err['message'] : '',
                    isset($err['code']) ? $err['code'] : '',
                );
        }
    }
}

function catalog_create_validator($con, $customerId, $importType)
{
    switch ($importType) {
        case 'categories':
            return new CatalogCategoryValidator($con, $customerId);
        case 'subcategories':
            return new CatalogSubcategoryValidator($con, $customerId);
        case 'portions':
            return new CatalogPortionValidator($con, $customerId);
        case 'products':
            return new CatalogProductValidator($con, $customerId);
        default:
            return null;
    }
}

function catalog_create_importer($con, $customerId, $dealerId, $importType)
{
    switch ($importType) {
        case 'categories':
            return new CatalogCategoryImporter($con, $customerId, $dealerId);
        case 'subcategories':
            return new CatalogSubcategoryImporter($con, $customerId);
        case 'portions':
            return new CatalogPortionImporter($con, $customerId);
        case 'products':
            return new CatalogProductImporter($con, $customerId, $dealerId);
        default:
            return null;
    }
}

function catalog_create_exporter($con, $customerId, $importType)
{
    switch ($importType) {
        case 'categories':
            return new CatalogCategoryExporter($con, $customerId);
        case 'subcategories':
            return new CatalogSubcategoryExporter($con, $customerId);
        case 'portions':
            return new CatalogPortionExporter($con, $customerId);
        case 'products':
            return new CatalogProductExporter($con, $customerId);
        default:
            return null;
    }
}

function catalog_type_label($importType)
{
    $config = catalog_import_type_config($importType);
    return $config !== null ? $config['label'] : 'Records';
}

function catalog_resolve_request_import_type($default = 'products')
{
    $type = $default;
    if (isset($_POST['importType'])) {
        $type = trim((string) $_POST['importType']);
    } elseif (isset($_GET['type'])) {
        $type = trim((string) $_GET['type']);
    }
    return catalog_normalize_import_type($type);
}

/**
 * Programmatic validate for web/Laravel callers. Returns ['httpStatus' => int, 'body' => array].
 */
function catalog_run_validate($con, $actorType, $actorId, $customerId, $importType, $sourcePath, $originalFileName, $fileSize, $isUploadedFile = false)
{
    mysqli_query($con, 'SET NAMES utf8');

    if (!CatalogExcelService::spreadsheetAvailable()) {
        return array(
            'httpStatus' => 503,
            'body' => array(
                'success' => false,
                'code' => 'SPREADSHEET_UNAVAILABLE',
                'message' => catalog_error_message('SPREADSHEET_UNAVAILABLE'),
            ),
        );
    }

    $importType = catalog_normalize_import_type($importType);
    if ($importType === null) {
        return array(
            'httpStatus' => 400,
            'body' => array(
                'success' => false,
                'code' => 'INVALID_IMPORT_TYPE',
                'message' => catalog_error_message('INVALID_IMPORT_TYPE'),
            ),
        );
    }

    $typeConfig = catalog_import_type_config($importType);
    $customer = catalog_authorize_customer($con, $actorType, $actorId, $customerId);
    if ($customer === null) {
        return array(
            'httpStatus' => 403,
            'body' => array(
                'success' => false,
                'status' => 'false',
                'message' => catalog_error_message('UNAUTHORIZED_CUSTOMER'),
                'code' => 'UNAUTHORIZED_CUSTOMER',
            ),
        );
    }

    if ($sourcePath === '' || !is_readable($sourcePath)) {
        return array(
            'httpStatus' => 400,
            'body' => array(
                'success' => false,
                'code' => 'INVALID_FILE',
                'message' => catalog_error_message('INVALID_FILE'),
            ),
        );
    }

    if ($fileSize <= 0 || $fileSize > CATALOG_MAX_UPLOAD_BYTES) {
        return array(
            'httpStatus' => 400,
            'body' => array(
                'success' => false,
                'code' => 'INVALID_FILE',
                'message' => 'File is empty or exceeds maximum upload size.',
            ),
        );
    }

    if (strtolower(pathinfo($originalFileName, PATHINFO_EXTENSION)) !== 'xlsx') {
        return array(
            'httpStatus' => 400,
            'body' => array(
                'success' => false,
                'code' => 'INVALID_FILE_TYPE',
                'message' => catalog_error_message('INVALID_FILE_TYPE'),
            ),
        );
    }

    $uploadDir = catalog_storage_dir('uploads');
    $storedName = catalog_sanitize_filename(pathinfo($originalFileName, PATHINFO_FILENAME))
        . '_' . time() . '_' . bin2hex(random_bytes(4)) . '.xlsx';
    $storedPath = $uploadDir . '/' . $storedName;

    $stored = false;
    if ($isUploadedFile) {
        $stored = move_uploaded_file($sourcePath, $storedPath);
    } else {
        $stored = copy($sourcePath, $storedPath);
    }

    if (!$stored) {
        return array(
            'httpStatus' => 500,
            'body' => array(
                'success' => false,
                'code' => 'INVALID_FILE',
                'message' => 'Unable to store uploaded file.',
            ),
        );
    }

    try {
        $parsed = CatalogExcelService::readImportSheet($storedPath, $typeConfig['sheetNames']);
    } catch (Throwable $e) {
        @unlink($storedPath);
        return array(
            'httpStatus' => 400,
            'body' => array(
                'success' => false,
                'code' => 'INVALID_FILE',
                'message' => 'Unable to read Excel file: ' . $e->getMessage(),
            ),
        );
    }

    if (empty($parsed['rows'])) {
        @unlink($storedPath);
        return array(
            'httpStatus' => 400,
            'body' => array(
                'success' => false,
                'code' => 'EMPTY_FILE',
                'message' => catalog_error_message('EMPTY_FILE'),
            ),
        );
    }

    $headerMap = CatalogExcelService::mapImportHeaders(
        $parsed['headers'],
        $typeConfig['columnMap'],
        $typeConfig['required']
    );
    if (!empty($headerMap['errors'])) {
        @unlink($storedPath);
        return array(
            'httpStatus' => 400,
            'body' => array(
                'success' => false,
                'importType' => $importType,
                'summary' => array('total' => 0, 'valid' => 0, 'new' => 0, 'updated' => 0, 'errors' => count($headerMap['errors'])),
                'errors' => $headerMap['errors'],
            ),
        );
    }

    $mappedRows = array();
    foreach ($parsed['rows'] as $rowNum => $rawAssoc) {
        $rawIndexed = array();
        foreach ($parsed['headers'] as $colIndex => $headerLabel) {
            $rawIndexed[$colIndex] = isset($rawAssoc[$headerLabel]) ? $rawAssoc[$headerLabel] : '';
        }
        $mappedRows[$rowNum] = CatalogExcelService::mapImportRow($headerMap['mappedHeaders'], $rawIndexed);
    }

    $validator = catalog_create_validator($con, (int) $customer['id'], $importType);
    if ($validator === null) {
        @unlink($storedPath);
        return array(
            'httpStatus' => 400,
            'body' => array('success' => false, 'message' => 'Unsupported import type.'),
        );
    }

    $result = $validator->validateAll($mappedRows);

    $session = CatalogSessionManager::createValidatedSession($con, array(
        'actorType' => $actorType,
        'actorId' => (int) $actorId,
        'customerId' => (int) $customer['id'],
        'importType' => $importType,
        'fileName' => $originalFileName,
        'storedFilePath' => $storedPath,
        'totalRows' => $result['summary']['total'],
        'validRows' => $result['summary']['valid'],
        'newRows' => $result['summary']['new'],
        'updateRows' => $result['summary']['updated'],
        'errorRows' => $result['summary']['errors'],
        'previewRows' => $result['validRows'],
        'errors' => $result['errors'],
    ));

    if ($session === null) {
        @unlink($storedPath);
        return array(
            'httpStatus' => 500,
            'body' => array('success' => false, 'message' => 'Unable to create import session.'),
        );
    }

    return array(
        'httpStatus' => 200,
        'body' => array(
            'success' => true,
            'status' => 'true',
            'importType' => $importType,
            'importSessionId' => $session['sessionId'],
            'customerId' => (string) $customer['id'],
            'customerName' => $customer['name'],
            'summary' => $result['summary'],
            'errors' => $result['errors'],
            'expiresAt' => $session['expiresAt'],
        ),
    );
}

function catalog_handle_validate($con, $actorType, $actorId)
{
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        catalog_json_response(array('success' => false, 'message' => 'Use POST'), 405);
        return;
    }

    $importType = catalog_resolve_request_import_type('products');
    $customerId = isset($_POST['customerId']) ? trim($_POST['customerId']) : '';
    if ($customerId === '' && isset($_POST['userId'])) {
        $customerId = trim($_POST['userId']);
    }

    if (!isset($_FILES['import_file']) || !is_uploaded_file($_FILES['import_file']['tmp_name'])) {
        catalog_json_response(array(
            'success' => false,
            'code' => 'INVALID_FILE',
            'message' => catalog_error_message('INVALID_FILE'),
        ), 400);
        return;
    }

    $file = $_FILES['import_file'];
    $result = catalog_run_validate(
        $con,
        $actorType,
        $actorId,
        $customerId,
        $importType,
        $file['tmp_name'],
        $file['name'],
        (int) $file['size'],
        true
    );
    catalog_json_response($result['body'], $result['httpStatus']);
}

/**
 * Programmatic confirm for web/Laravel callers. Returns ['httpStatus' => int, 'body' => array].
 */
function catalog_run_confirm($con, $actorType, $actorId, $customerId, $sessionId)
{
    mysqli_query($con, 'SET NAMES utf8');

    if ($sessionId === '') {
        return array(
            'httpStatus' => 400,
            'body' => array(
                'success' => false,
                'code' => 'SESSION_NOT_FOUND',
                'message' => catalog_error_message('SESSION_NOT_FOUND'),
            ),
        );
    }

    if (catalog_authorize_customer($con, $actorType, $actorId, $customerId) === null) {
        return array(
            'httpStatus' => 403,
            'body' => array(
                'success' => false,
                'status' => 'false',
                'message' => catalog_error_message('UNAUTHORIZED_CUSTOMER'),
                'code' => 'UNAUTHORIZED_CUSTOMER',
            ),
        );
    }

    $session = CatalogSessionManager::getSession($con, $sessionId, $actorType, $actorId, (int) $customerId);
    if ($session === null) {
        return array(
            'httpStatus' => 404,
            'body' => array(
                'success' => false,
                'code' => 'SESSION_NOT_FOUND',
                'message' => catalog_error_message('SESSION_NOT_FOUND'),
            ),
        );
    }

    if ($session['status'] === 'imported') {
        return array(
            'httpStatus' => 409,
            'body' => array(
                'success' => false,
                'code' => 'SESSION_ALREADY_IMPORTED',
                'message' => catalog_error_message('SESSION_ALREADY_IMPORTED'),
            ),
        );
    }

    if ((int) $session['validRows'] <= 0) {
        return array(
            'httpStatus' => 400,
            'body' => array('success' => false, 'message' => 'No valid rows to import.'),
        );
    }

    $previewRows = json_decode($session['previewJson'], true);
    if (!is_array($previewRows)) {
        return array(
            'httpStatus' => 500,
            'body' => array('success' => false, 'message' => 'Invalid import session data.'),
        );
    }

    $importType = $session['importType'];
    $customer = db_stmt_fetch_one(
        $con,
        'SELECT `id`, `dealerId` FROM `users` WHERE `id`=? LIMIT 1',
        'i',
        (int) $session['customerId']
    );
    $dealerId = $customer !== null ? (int) $customer['dealerId'] : 0;

    $importer = catalog_create_importer($con, (int) $session['customerId'], $dealerId, $importType);
    if ($importer === null) {
        return array(
            'httpStatus' => 400,
            'body' => array('success' => false, 'message' => 'Unsupported import type.'),
        );
    }

    $importResult = $importer->importRows($previewRows);

    if ($importResult['failed'] > 0) {
        CatalogSessionManager::markFailed($con, $sessionId);
        return array(
            'httpStatus' => 500,
            'body' => array(
                'success' => false,
                'code' => 'IMPORT_FAILED',
                'message' => catalog_error_message('IMPORT_FAILED'),
            ),
        );
    }

    CatalogSessionManager::markImported(
        $con,
        $sessionId,
        $importResult['created'],
        $importResult['updated'],
        0
    );

    $label = catalog_type_label($importType);
    return array(
        'httpStatus' => 200,
        'body' => array(
            'success' => true,
            'status' => 'true',
            'importType' => $importType,
            'importSessionId' => $sessionId,
            'summary' => array(
                'total' => (int) $session['validRows'],
                'created' => $importResult['created'],
                'updated' => $importResult['updated'],
                'failed' => 0,
            ),
            'message' => ($importResult['created'] + $importResult['updated']) . ' ' . strtolower($label) . ' imported successfully.',
        ),
    );
}

function catalog_handle_confirm($con, $actorType, $actorId)
{
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        catalog_json_response(array('success' => false, 'message' => 'Use POST'), 405);
        return;
    }

    $sessionId = isset($_POST['importSessionId']) ? trim($_POST['importSessionId']) : '';
    $customerId = isset($_POST['customerId']) ? trim($_POST['customerId']) : '';
    if ($customerId === '' && isset($_POST['userId'])) {
        $customerId = trim($_POST['userId']);
    }

    $result = catalog_run_confirm($con, $actorType, $actorId, $customerId, $sessionId);
    catalog_json_response($result['body'], $result['httpStatus']);
}

function catalog_handle_error_excel($con, $actorType, $actorId)
{
    mysqli_query($con, 'SET NAMES utf8');

    $sessionId = isset($_GET['importSessionId']) ? trim($_GET['importSessionId']) : '';
    $customerId = isset($_GET['customerId']) ? trim($_GET['customerId']) : '';
    if ($customerId === '' && isset($_GET['userId'])) {
        $customerId = trim($_GET['userId']);
    }

    if ($sessionId === '') {
        catalog_json_response(array('success' => false, 'message' => 'importSessionId required'), 400);
        return;
    }

    catalog_require_customer($con, $actorType, $actorId, $customerId);

    $session = CatalogSessionManager::getSession($con, $sessionId, $actorType, $actorId, (int) $customerId);
    if ($session === null) {
        catalog_json_response(array('success' => false, 'message' => 'Session not found'), 404);
        return;
    }

    $errors = json_decode($session['errorsJson'], true);
    if (!is_array($errors) || empty($errors)) {
        catalog_json_response(array('success' => false, 'message' => 'No errors for this session'), 404);
        return;
    }

    $importType = $session['importType'];
    $fileName = 'Import_Errors_' . $sessionId . '.xlsx';

    try {
        $spreadsheet = CatalogErrorExporter::buildErrorSpreadsheet($importType, $errors);
        if ($spreadsheet !== null) {
            CatalogExcelService::streamSpreadsheetDownload($spreadsheet, $fileName);
        } else {
            CatalogExcelService::streamSheetsDownload(
                array('Errors' => CatalogErrorExporter::buildErrorSheetRows($importType, $errors)),
                $fileName
            );
        }
    } catch (Throwable $e) {
        catalog_json_response(array('success' => false, 'message' => $e->getMessage()), 500);
    }
}

function catalog_handle_template($con, $actorType, $actorId)
{
    mysqli_query($con, 'SET NAMES utf8');

    $importType = catalog_resolve_request_import_type('products');
    if ($importType === null) {
        catalog_json_response(array(
            'success' => false,
            'code' => 'INVALID_IMPORT_TYPE',
            'message' => catalog_error_message('INVALID_IMPORT_TYPE'),
        ), 400);
        return;
    }

    $typeConfig = catalog_import_type_config($importType);

    $customerId = isset($_GET['customerId']) ? trim($_GET['customerId']) : '';
    if ($customerId === '' && isset($_GET['userId'])) {
        $customerId = trim($_GET['userId']);
    }

    $customer = catalog_require_customer($con, $actorType, $actorId, $customerId);
    $exporter = catalog_create_exporter($con, (int) $customer['id'], $importType);
    if ($exporter === null) {
        catalog_json_response(array('success' => false, 'message' => 'Unsupported export type.'), 400);
        return;
    }

    try {
        $fileName = catalog_export_filename($customer['name'], $typeConfig['templateLabel']);
        if ($importType === 'products') {
            $productExporter = $exporter;
            $spreadsheet = $productExporter->buildSpreadsheet();
            if ($spreadsheet !== null) {
                CatalogExcelService::streamSpreadsheetDownload($spreadsheet, $fileName);
            } else {
                CatalogExcelService::streamSheetsDownload($productExporter->buildSheetsArray(), $fileName);
            }
        } else {
            CatalogExcelService::streamSheetsDownload($exporter->buildSheetsArray(), $fileName);
        }
    } catch (Throwable $e) {
        catalog_json_response(array('success' => false, 'message' => $e->getMessage()), 500);
    }
}

function catalog_handle_export($con, $actorType, $actorId)
{
    mysqli_query($con, 'SET NAMES utf8');

    $importType = catalog_resolve_request_import_type('products');
    if ($importType === null) {
        catalog_json_response(array(
            'success' => false,
            'code' => 'INVALID_IMPORT_TYPE',
            'message' => catalog_error_message('INVALID_IMPORT_TYPE'),
        ), 400);
        return;
    }

    $typeConfig = catalog_import_type_config($importType);

    $customerId = isset($_GET['customerId']) ? trim($_GET['customerId']) : '';
    if ($customerId === '' && isset($_GET['userId'])) {
        $customerId = trim($_GET['userId']);
    }

    $customer = catalog_require_customer($con, $actorType, $actorId, $customerId);
    $exporter = catalog_create_exporter($con, (int) $customer['id'], $importType);
    if ($exporter === null) {
        catalog_json_response(array('success' => false, 'message' => 'Unsupported export type.'), 400);
        return;
    }

    try {
        $rowCount = method_exists($exporter, 'fetchExportRowCount')
            ? $exporter->fetchExportRowCount()
            : (method_exists($exporter, 'fetchExportRows') ? count($exporter->fetchExportRows()) : 0);
        $fileName = catalog_export_filename($customer['name'], $typeConfig['exportLabel']);

        CatalogSessionManager::recordExport(
            $con,
            $actorType,
            (int) $actorId,
            (int) $customer['id'],
            $importType,
            $fileName,
            (int) $rowCount
        );

        if ($importType === 'products') {
            $spreadsheet = $exporter->buildSpreadsheet();
            if ($spreadsheet !== null) {
                CatalogExcelService::streamSpreadsheetDownload($spreadsheet, $fileName);
            } else {
                CatalogExcelService::streamSheetsDownload($exporter->buildSheetsArray(), $fileName);
            }
        } else {
            CatalogExcelService::streamSheetsDownload($exporter->buildSheetsArray(), $fileName);
        }
    } catch (Throwable $e) {
        CatalogSessionManager::recordExport(
            $con,
            $actorType,
            (int) $actorId,
            (int) $customer['id'],
            $importType,
            '',
            0,
            'failed'
        );
        catalog_json_response(array('success' => false, 'message' => $e->getMessage()), 500);
    }
}

function catalog_handle_history($con, $actorType, $actorId)
{
    mysqli_query($con, 'SET NAMES utf8');

    $customerId = isset($_GET['customerId']) ? trim($_GET['customerId']) : '';
    if ($customerId === '' && isset($_GET['userId'])) {
        $customerId = trim($_GET['userId']);
    }

    catalog_require_customer($con, $actorType, $actorId, $customerId);

    $importType = isset($_GET['importType']) ? catalog_normalize_import_type(trim($_GET['importType'])) : null;

    $history = CatalogSessionManager::listHistory($con, $actorType, (int) $actorId, (int) $customerId, $importType);

    catalog_json_response(array(
        'success' => true,
        'status' => 'true',
        'importType' => $importType,
        'history' => $history,
    ));
}

// Backward-compatible aliases
function catalog_products_handle_validate($con, $actorType, $actorId) { catalog_handle_validate($con, $actorType, $actorId); }
function catalog_products_handle_confirm($con, $actorType, $actorId) { catalog_handle_confirm($con, $actorType, $actorId); }
function catalog_products_handle_error_excel($con, $actorType, $actorId) { catalog_handle_error_excel($con, $actorType, $actorId); }
function catalog_products_handle_template($con, $actorType, $actorId) { catalog_handle_template($con, $actorType, $actorId); }
function catalog_products_handle_export($con, $actorType, $actorId) { catalog_handle_export($con, $actorType, $actorId); }
function catalog_products_handle_history($con, $actorType, $actorId) { catalog_handle_history($con, $actorType, $actorId); }
