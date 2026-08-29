<?php

use PhpOffice\PhpSpreadsheet\IOFactory;
use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;

class CatalogExcelService
{
    public static function spreadsheetAvailable()
    {
        return class_exists('\PhpOffice\PhpSpreadsheet\IOFactory') || CatalogSimpleXlsx::isAvailable();
    }

    public static function readImportSheet($filePath, array $preferredSheetNames)
    {
        if (class_exists('\PhpOffice\PhpSpreadsheet\IOFactory')) {
            return self::readImportSheetPhpSpreadsheet($filePath, $preferredSheetNames);
        }
        if (CatalogSimpleXlsx::isAvailable()) {
            $parsed = CatalogSimpleXlsx::readImportRows($filePath, $preferredSheetNames);
            return array(
                'headers' => $parsed['headers'],
                'rows' => $parsed['rows'],
                'sheetName' => $parsed['sheetName'],
            );
        }
        throw new RuntimeException(catalog_error_message('SPREADSHEET_UNAVAILABLE'));
    }

    public static function readProductSheet($filePath)
    {
        return self::readImportSheet($filePath, array('products', 'product'));
    }

    private static function readImportSheetPhpSpreadsheet($filePath, array $preferredSheetNames)
    {
        $spreadsheet = IOFactory::load($filePath);
        $sheet = self::resolveSheetByNames($spreadsheet, $preferredSheetNames);
        $rawRows = $sheet->toArray(null, true, true, false);

        if (empty($rawRows)) {
            return array('headers' => array(), 'rows' => array(), 'sheetName' => $sheet->getTitle());
        }

        $headerRow = array_shift($rawRows);
        $headers = self::normalizeHeaders($headerRow);
        $rows = array();

        foreach ($rawRows as $index => $raw) {
            $assoc = array();
            foreach ($headers as $colIndex => $headerName) {
                if ($headerName === '') {
                    continue;
                }
                $assoc[$headerName] = isset($raw[$colIndex]) ? trim((string) $raw[$colIndex]) : '';
            }
            if (!catalog_row_is_empty($assoc)) {
                $rows[$index + 2] = $assoc;
            }
        }

        return array(
            'headers' => $headers,
            'rows' => $rows,
            'sheetName' => $sheet->getTitle(),
        );
    }

    private static function readProductSheetPhpSpreadsheet($filePath)
    {
        return self::readImportSheetPhpSpreadsheet($filePath, array('products', 'product'));
    }

    private static function resolveSheetByNames(Spreadsheet $spreadsheet, array $preferredNames)
    {
        $normalizedPreferred = array();
        foreach ($preferredNames as $name) {
            $normalizedPreferred[] = strtolower(trim($name));
        }
        foreach ($spreadsheet->getAllSheets() as $sheet) {
            $title = strtolower(trim($sheet->getTitle()));
            if (in_array($title, $normalizedPreferred, true)) {
                return $sheet;
            }
        }
        return $spreadsheet->getSheet(0);
    }

    private static function resolveProductsSheet(Spreadsheet $spreadsheet)
    {
        return self::resolveSheetByNames($spreadsheet, array('products', 'product'));
    }

    public static function mapImportHeaders(array $headerRow, array $columnMap, array $requiredColumns)
    {
        $mappedHeaders = array();
        $errors = array();
        $seenKeys = array();
        $unknownColumns = array();

        foreach ($headerRow as $index => $header) {
            $headerTrim = trim((string) $header);
            if ($headerTrim === '') {
                continue;
            }
            $key = strtolower($headerTrim);
            if (!isset($columnMap[$key])) {
                $unknownColumns[] = $headerTrim;
                continue;
            }
            $internal = $columnMap[$key];
            if (isset($seenKeys[$internal])) {
                $errors[] = array(
                    'row' => 1,
                    'code' => 'INVALID_COLUMN',
                    'message' => 'Duplicate column mapping for ' . $headerTrim . '.',
                    'column' => $headerTrim,
                );
                continue;
            }
            $seenKeys[$internal] = true;
            $mappedHeaders[$index] = $internal;
        }

        foreach ($unknownColumns as $col) {
            $errors[] = array(
                'row' => 1,
                'code' => 'INVALID_COLUMN',
                'message' => 'Unexpected column: "' . $col . '".',
                'column' => $col,
            );
        }

        $labels = array(
            'productName' => 'Product Name',
            'categoryName' => 'Category Name',
            'subcategoryName' => 'Sub Category Name',
            'portionName' => 'Portion Name',
            'category' => 'Category',
        );

        foreach ($requiredColumns as $req) {
            if (!in_array($req, $seenKeys, true)) {
                $label = isset($labels[$req]) ? $labels[$req] : ucfirst($req);
                $errors[] = array(
                    'row' => 1,
                    'code' => 'MISSING_COLUMN',
                    'message' => 'Missing column: ' . $label . '.',
                    'column' => $label,
                );
            }
        }

        return array(
            'mappedHeaders' => $mappedHeaders,
            'errors' => $errors,
        );
    }

    public static function mapImportRow(array $mappedHeaders, array $rawRow)
    {
        $row = array();
        foreach ($mappedHeaders as $index => $key) {
            $row[$key] = isset($rawRow[$index]) ? trim((string) $rawRow[$index]) : '';
        }
        return $row;
    }

    /**
     * @param array<int, string|null> $headerRow
     * @return array<int, string> column index => canonical header
     */
    private static function normalizeHeaders(array $headerRow)
    {
        $headers = array();
        foreach ($headerRow as $index => $cell) {
            $headers[$index] = trim((string) $cell);
        }
        return $headers;
    }

    /**
     * Map header row to internal keys; validate required/allowed columns.
     *
     * @return array{mappedHeaders: array<int, string>, errors: array}
     */
    public static function mapProductHeaders(array $headerRow)
    {
        return self::mapImportHeaders($headerRow, catalog_product_column_map(), catalog_product_required_columns());
    }

    public static function mapProductRow(array $mappedHeaders, array $rawRow)
    {
        return self::mapImportRow($mappedHeaders, $rawRow);
    }

    public static function writeSpreadsheetToFile(Spreadsheet $spreadsheet, $filePath)
    {
        $writer = new Xlsx($spreadsheet);
        $writer->save($filePath);
    }

    public static function streamSpreadsheetDownload(Spreadsheet $spreadsheet, $downloadName)
    {
        if (class_exists('\PhpOffice\PhpSpreadsheet\Writer\Xlsx')) {
            if (ob_get_length()) {
                ob_end_clean();
            }
            header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
            header('Content-Disposition: attachment; filename="' . catalog_sanitize_filename($downloadName) . '"');
            header('Cache-Control: max-age=0');
            $writer = new Xlsx($spreadsheet);
            $writer->save('php://output');
            return;
        }

        throw new RuntimeException(catalog_error_message('SPREADSHEET_UNAVAILABLE'));
    }

    public static function streamSheetsDownload(array $sheets, $downloadName)
    {
        if (class_exists('\PhpOffice\PhpSpreadsheet\Spreadsheet')) {
            $spreadsheet = new Spreadsheet();
            $index = 0;
            foreach ($sheets as $name => $rows) {
                if ($index === 0) {
                    $sheet = $spreadsheet->getActiveSheet();
                } else {
                    $sheet = $spreadsheet->createSheet();
                }
                $sheet->setTitle($name);
                $sheet->fromArray($rows, null, 'A1');
                $index++;
            }
            self::streamSpreadsheetDownload($spreadsheet, $downloadName);
            return;
        }

        CatalogSimpleXlsx::streamWorkbook($sheets, $downloadName);
    }
}
