<?php

/**
 * Minimal XLSX reader/writer using ZipArchive (fallback when PhpSpreadsheet is unavailable).
 */
class CatalogSimpleXlsx
{
    public static function isAvailable()
    {
        return class_exists('ZipArchive');
    }

    /**
     * @return array{headers: array<int, string>, rows: array<int, array<string, string>>}
     */
    public static function readProductRows($filePath)
    {
        return self::readImportRows($filePath, array('products', 'product'));
    }

    /**
     * @return array{headers: array<int, string>, rows: array<int, array<string, string>>, sheetName: string}
     */
    public static function readImportRows($filePath, array $preferredSheetNames)
    {
        if (!self::isAvailable()) {
            throw new RuntimeException('ZipArchive is not available.');
        }

        $zip = new ZipArchive();
        if ($zip->open($filePath) !== true) {
            throw new RuntimeException('Unable to open XLSX file.');
        }

        $sharedStrings = self::readSharedStrings($zip);
        $sheetPath = self::resolveSheetPath($zip, $preferredSheetNames);
        $sheetXml = $zip->getFromName($sheetPath);
        $zip->close();

        if ($sheetXml === false) {
            throw new RuntimeException('Unable to read worksheet.');
        }

        $parsedRows = self::parseSheetRows($sheetXml, $sharedStrings);
        if (empty($parsedRows)) {
            return array('headers' => array(), 'rows' => array());
        }

        $headerRow = array_shift($parsedRows);
        $headers = array();
        foreach ($headerRow as $colIndex => $value) {
            $headers[$colIndex] = trim((string) $value);
        }

        $rows = array();
        $excelRowNum = 2;
        foreach ($parsedRows as $dataRow) {
            $assoc = array();
            $empty = true;
            foreach ($headers as $colIndex => $headerName) {
                if ($headerName === '') {
                    continue;
                }
                $val = isset($dataRow[$colIndex]) ? trim((string) $dataRow[$colIndex]) : '';
                if ($val !== '') {
                    $empty = false;
                }
                $assoc[$headerName] = $val;
            }
            if (!$empty) {
                $rows[$excelRowNum] = $assoc;
            }
            $excelRowNum++;
        }

        return array('headers' => $headers, 'rows' => $rows);
    }

    public static function writeWorkbook(array $sheets, $filePath)
    {
        if (!self::isAvailable()) {
            throw new RuntimeException('ZipArchive is not available.');
        }

        $zip = new ZipArchive();
        if ($zip->open($filePath, ZipArchive::CREATE | ZipArchive::OVERWRITE) !== true) {
            throw new RuntimeException('Unable to create XLSX file.');
        }

        $sharedStrings = array();
        $sharedIndex = array();

        $sheetEntries = '';
        $sheetOverrides = '';
        $sheetIndex = 1;

        foreach ($sheets as $sheetName => $rows) {
            $sheetXml = self::buildSheetXml($rows, $sharedStrings, $sharedIndex);
            $sheetPath = 'xl/worksheets/sheet' . $sheetIndex . '.xml';
            $zip->addFromString($sheetPath, $sheetXml);
            $sheetEntries .= '<sheet name="' . self::xmlEscape($sheetName) . '" sheetId="' . $sheetIndex . '" r:id="rId' . ($sheetIndex + 1) . '"/>';
            $sheetOverrides .= '<Override PartName="/xl/worksheets/sheet' . $sheetIndex . '.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>';
            $sheetIndex++;
        }

        $zip->addFromString('[Content_Types].xml', '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            . '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
            . '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
            . '<Default Extension="xml" ContentType="application/xml"/>'
            . '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
            . '<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>'
            . '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>'
            . $sheetOverrides
            . '</Types>');

        $zip->addFromString('_rels/.rels', '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            . '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            . '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>'
            . '</Relationships>');

        $rels = '';
        for ($i = 1; $i < $sheetIndex; $i++) {
            $rels .= '<Relationship Id="rId' . ($i + 1) . '" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet' . $i . '.xml"/>';
        }
        $zip->addFromString('xl/_rels/workbook.xml.rels', '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            . '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            . '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>'
            . $rels
            . '</Relationships>');

        $zip->addFromString('xl/workbook.xml', '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            . '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
            . '<sheets>' . $sheetEntries . '</sheets></workbook>');

        $zip->addFromString('xl/sharedStrings.xml', self::buildSharedStringsXml($sharedStrings));
        $zip->addFromString('xl/styles.xml', '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"/>');

        $zip->close();
    }

    public static function streamWorkbook(array $sheets, $downloadName)
    {
        $tmp = tempnam(sys_get_temp_dir(), 'catxlsx');
        self::writeWorkbook($sheets, $tmp);
        if (ob_get_length()) {
            ob_end_clean();
        }
        header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        header('Content-Disposition: attachment; filename="' . catalog_sanitize_filename($downloadName) . '"');
        header('Content-Length: ' . filesize($tmp));
        readfile($tmp);
        @unlink($tmp);
    }

    private static function readSharedStrings(ZipArchive $zip)
    {
        $xml = $zip->getFromName('xl/sharedStrings.xml');
        if ($xml === false) {
            return array();
        }
        $doc = simplexml_load_string($xml);
        if ($doc === false) {
            return array();
        }
        $strings = array();
        foreach ($doc->si as $si) {
            if (isset($si->t)) {
                $strings[] = (string) $si->t;
            } elseif (isset($si->r)) {
                $text = '';
                foreach ($si->r as $run) {
                    $text .= (string) $run->t;
                }
                $strings[] = $text;
            } else {
                $strings[] = '';
            }
        }
        return $strings;
    }

    private static function resolveSheetPath(ZipArchive $zip, array $preferredNames)
    {
        $normalizedPreferred = array();
        foreach ($preferredNames as $name) {
            $normalizedPreferred[] = strtolower(trim($name));
        }

        $workbookXml = $zip->getFromName('xl/workbook.xml');
        if ($workbookXml !== false) {
            $doc = simplexml_load_string($workbookXml);
            if ($doc !== false && isset($doc->sheets->sheet)) {
                $index = 1;
                foreach ($doc->sheets->sheet as $sheet) {
                    $name = strtolower(trim((string) $sheet['name']));
                    if (in_array($name, $normalizedPreferred, true)) {
                        return 'xl/worksheets/sheet' . $index . '.xml';
                    }
                    $index++;
                }
            }
        }
        return 'xl/worksheets/sheet1.xml';
    }

    private static function parseSheetRows($sheetXml, array $sharedStrings)
    {
        $doc = simplexml_load_string($sheetXml);
        if ($doc === false || !isset($doc->sheetData->row)) {
            return array();
        }

        $rows = array();
        foreach ($doc->sheetData->row as $row) {
            $rowData = array();
            foreach ($row->c as $cell) {
                $ref = (string) $cell['r'];
                $colLetters = preg_replace('/[0-9]+/', '', $ref);
                $colIndex = self::columnLettersToIndex($colLetters);
                $type = (string) $cell['t'];
                $value = isset($cell->v) ? (string) $cell->v : '';
                if ($type === 's') {
                    $idx = (int) $value;
                    $value = isset($sharedStrings[$idx]) ? $sharedStrings[$idx] : '';
                }
                $rowData[$colIndex] = $value;
            }
            if (!empty($rowData)) {
                ksort($rowData);
                $rows[] = $rowData;
            }
        }
        return $rows;
    }

    private static function columnLettersToIndex($letters)
    {
        $letters = strtoupper($letters);
        $index = 0;
        $len = strlen($letters);
        for ($i = 0; $i < $len; $i++) {
            $index = $index * 26 + (ord($letters[$i]) - 64);
        }
        return $index - 1;
    }

    private static function indexToColumnLetters($index)
    {
        $index++;
        $letters = '';
        while ($index > 0) {
            $mod = ($index - 1) % 26;
            $letters = chr(65 + $mod) . $letters;
            $index = (int) (($index - $mod) / 26);
        }
        return $letters;
    }

    private static function buildSharedStringsXml(array $sharedStrings)
    {
        $xml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>';
        $xml .= '<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="' . count($sharedStrings) . '" uniqueCount="' . count($sharedStrings) . '">';
        foreach ($sharedStrings as $str) {
            $xml .= '<si><t>' . self::xmlEscape($str) . '</t></si>';
        }
        $xml .= '</sst>';
        return $xml;
    }

    private static function buildSheetXml(array $rows, array &$sharedStrings, array &$sharedIndex)
    {
        $xml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>';
        $xml .= '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>';
        $rowNum = 1;
        foreach ($rows as $row) {
            $xml .= '<row r="' . $rowNum . '">';
            $colNum = 0;
            foreach ($row as $cellValue) {
                $colRef = self::indexToColumnLetters($colNum) . $rowNum;
                $strVal = (string) $cellValue;
                if (!isset($sharedIndex[$strVal])) {
                    $sharedIndex[$strVal] = count($sharedStrings);
                    $sharedStrings[] = $strVal;
                }
                $idx = $sharedIndex[$strVal];
                $xml .= '<c r="' . $colRef . '" t="s"><v>' . $idx . '</v></c>';
                $colNum++;
            }
            $xml .= '</row>';
            $rowNum++;
        }
        $xml .= '</sheetData></worksheet>';
        return $xml;
    }

    private static function xmlEscape($value)
    {
        return htmlspecialchars((string) $value, ENT_XML1 | ENT_QUOTES, 'UTF-8');
    }
}
