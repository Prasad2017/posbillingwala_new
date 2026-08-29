<?php

use PhpOffice\PhpSpreadsheet\Spreadsheet;

class CatalogProductExporter
{
    /** @var mysqli */
    private $con;
    private $customerId;

    public function __construct($con, $customerId)
    {
        $this->con = $con;
        $this->customerId = (int) $customerId;
    }

    public function buildSpreadsheet()
    {
        if (class_exists('\PhpOffice\PhpSpreadsheet\Spreadsheet')) {
            $spreadsheet = new Spreadsheet();
            $this->buildInstructionsSheet($spreadsheet);
            $this->buildProductsSheet($spreadsheet);
            $this->buildReferenceSheets($spreadsheet);
            $spreadsheet->setActiveSheetIndex(1);
            return $spreadsheet;
        }
        return null;
    }

    /**
     * Sheet name => rows (for simple XLSX writer fallback).
     */
    public function buildSheetsArray()
    {
        $sheets = array();
        $sheets['Instructions'] = $this->instructionsRows();
        $sheets['Products'] = $this->productsSheetRows();
        $sheets['Categories'] = $this->categoriesReferenceRows();
        $sheets['Sub Categories'] = $this->subcategoriesReferenceRows();
        $sheets['Portions'] = $this->portionsReferenceRows();
        return $sheets;
    }

    private function instructionsRows()
    {
        return array(
            array('PRODUCT IMPORT TEMPLATE'),
            array(''),
            array('Required:'),
            array('- Product Name'),
            array('- Category'),
            array(''),
            array('Optional:'),
            array('- Sub Category'),
            array('- Portion'),
            array('- Product Code'),
            array('- Price'),
            array('- GST'),
            array('- Unit'),
            array('- Status'),
        );
    }

    private function productsSheetRows()
    {
        $headers = array(
            'Product Name', 'Product Code', 'Category', 'Sub Category', 'Portion',
            'Price', 'GST', 'Unit', 'Status',
        );
        $rows = array($headers);
        $exportRows = $this->fetchExportRows();
        if (empty($exportRows)) {
            $rows[] = array('Veg Thali', 'P001', 'Veg', 'Main Course', 'Full', '120', '5', 'Pcs', 'Active');
            $rows[] = array('Mini Thali', 'P002', 'Veg', 'Main Course', 'Half', '80', '5', 'Pcs', 'Active');
        } else {
            foreach ($exportRows as $r) {
                $rows[] = $r;
            }
        }
        return $rows;
    }

    private function categoriesReferenceRows()
    {
        $rows = array(array('Category Name'));
        $cats = db_stmt_fetch_all(
            $this->con,
            'SELECT `categoryName` FROM `categories` WHERE `userId`=? AND `categoryStatus`=\'active\' ORDER BY `categoryName`',
            'i',
            $this->customerId
        );
        foreach ($cats as $c) {
            $rows[] = array($c['categoryName']);
        }
        return $rows;
    }

    private function subcategoriesReferenceRows()
    {
        $rows = array(array('Category', 'Sub Category Name'));
        $subs = db_stmt_fetch_all(
            $this->con,
            'SELECT c.`categoryName`, ps.`subcategoryName`
             FROM `product_subcategories` ps
             INNER JOIN `categories` c ON c.`categoryId` = ps.`categoryId`
             WHERE ps.`userId`=? AND ps.`subcategoryStatus`=\'active\'
             ORDER BY c.`categoryName`, ps.`subcategoryName`',
            'i',
            $this->customerId
        );
        foreach ($subs as $s) {
            $rows[] = array($s['categoryName'], $s['subcategoryName']);
        }
        return $rows;
    }

    private function portionsReferenceRows()
    {
        $rows = array(array('Portion Name'));
        $portions = db_stmt_fetch_all(
            $this->con,
            'SELECT `portionName` FROM `portion_master` WHERE `userId`=? AND `portionMasterStatus`=\'active\' ORDER BY `portionName`',
            'i',
            $this->customerId
        );
        foreach ($portions as $p) {
            $rows[] = array($p['portionName']);
        }
        return $rows;
    }

    public function fetchExportRows()
    {
        $rows = db_stmt_fetch_all(
            $this->con,
            'SELECT p.`productName`, p.`productCode`, c.`categoryName`, ps.`subcategoryName`,
                    pm.`portionName`, pp.`portionPrice`, p.`productPrice`, p.`productCGST`, p.`productSGST`,
                    p.`productUnit`, p.`productStatus`
             FROM `products` p
             INNER JOIN `categories` c ON c.`categoryId` = p.`categoryId`
             LEFT JOIN `product_subcategories` ps ON ps.`subcategoryId` = p.`subcategoryId`
             LEFT JOIN `product_portions` pp ON pp.`productId` = p.`productId` AND pp.`portionStatus` = \'active\'
             LEFT JOIN `portion_master` pm ON pm.`portionMasterId` = pp.`portionMasterId`
             WHERE p.`userId`=? AND p.`productStatus` != \'deleted\'
             ORDER BY c.`categoryName`, p.`productName`, pp.`portionSortOrder`, pm.`portionName`',
            'i',
            $this->customerId
        );

        $exportRows = array();
        foreach ($rows as $row) {
            $gst = (int) $row['productCGST'] + (int) $row['productSGST'];
            $price = $row['portionPrice'] !== null && $row['portionPrice'] !== ''
                ? $row['portionPrice']
                : $row['productPrice'];

            $exportRows[] = array(
                $row['productName'],
                $row['productCode'],
                $row['categoryName'],
                $row['subcategoryName'] ?? '',
                $row['portionName'] ?? '',
                $price,
                $gst,
                $row['productUnit'],
                ucfirst(strtolower((string) $row['productStatus'])),
            );
        }

        return $exportRows;
    }

    private function buildInstructionsSheet(Spreadsheet $spreadsheet)
    {
        $sheet = $spreadsheet->getActiveSheet();
        $sheet->setTitle('Instructions');

        $lines = array(
            array('PRODUCT IMPORT TEMPLATE'),
            array(''),
            array('Required:'),
            array('- Product Name'),
            array('- Category'),
            array(''),
            array('Optional:'),
            array('- Sub Category'),
            array('- Portion'),
            array('- Product Code'),
            array('- Price'),
            array('- GST'),
            array('- Unit'),
            array('- Status'),
            array(''),
            array('Rules:'),
            array('1. Do not change column names on the Products sheet.'),
            array('2. Category must already exist.'),
            array('3. Sub Category and Portion are optional.'),
            array('4. Do not enter database IDs.'),
            array('5. Use exact master names.'),
            array('6. Do not add formulas.'),
            array('7. Remove example rows before final import.'),
        );

        $sheet->fromArray($lines, null, 'A1');
    }

    private function buildProductsSheet(Spreadsheet $spreadsheet)
    {
        $sheet = $spreadsheet->createSheet();
        $sheet->setTitle('Products');

        $headers = array(
            'Product Name', 'Product Code', 'Category', 'Sub Category', 'Portion',
            'Price', 'GST', 'Unit', 'Status',
        );
        $sheet->fromArray($headers, null, 'A1');

        $exportRows = $this->fetchExportRows();
        if (empty($exportRows)) {
            $examples = array(
                array('Veg Thali', 'P001', 'Veg', 'Main Course', 'Full', 120, 5, 'Pcs', 'Active'),
                array('Mini Thali', 'P002', 'Veg', 'Main Course', 'Half', 80, 5, 'Pcs', 'Active'),
                array('Paneer Masala', 'P003', 'Veg', 'Main Course', '', 180, 5, 'Pcs', 'Active'),
                array('Cold Coffee', 'P004', 'Beverages', 'Cold Drinks', '', 90, 5, 'Pcs', 'Active'),
            );
            $sheet->fromArray($examples, null, 'A2');
        } else {
            $sheet->fromArray($exportRows, null, 'A2');
        }

        foreach (range('A', 'I') as $col) {
            $sheet->getColumnDimension($col)->setAutoSize(true);
        }
    }

    private function buildReferenceSheets(Spreadsheet $spreadsheet)
    {
        $this->buildSimpleListSheet($spreadsheet, 'Categories', 'Category Name', array(
            db_stmt_fetch_all(
                $this->con,
                'SELECT `categoryName` FROM `categories` WHERE `userId`=? AND `categoryStatus`=\'active\' ORDER BY `categoryName`',
                'i',
                $this->customerId
            ),
            'categoryName',
        ));

        $subs = db_stmt_fetch_all(
            $this->con,
            'SELECT c.`categoryName`, ps.`subcategoryName`
             FROM `product_subcategories` ps
             INNER JOIN `categories` c ON c.`categoryId` = ps.`categoryId`
             WHERE ps.`userId`=? AND ps.`subcategoryStatus`=\'active\'
             ORDER BY c.`categoryName`, ps.`subcategoryName`',
            'i',
            $this->customerId
        );
        $sheet = $spreadsheet->createSheet();
        $sheet->setTitle('Sub Categories');
        $sheet->fromArray(array('Category', 'Sub Category Name'), null, 'A1');
        $subRows = array();
        foreach ($subs as $s) {
            $subRows[] = array($s['categoryName'], $s['subcategoryName']);
        }
        if (!empty($subRows)) {
            $sheet->fromArray($subRows, null, 'A2');
        }

        $this->buildSimpleListSheet($spreadsheet, 'Portions', 'Portion Name', array(
            db_stmt_fetch_all(
                $this->con,
                'SELECT `portionName` FROM `portion_master` WHERE `userId`=? AND `portionMasterStatus`=\'active\' ORDER BY `portionName`',
                'i',
                $this->customerId
            ),
            'portionName',
        ));
    }

    private function buildSimpleListSheet(Spreadsheet $spreadsheet, $title, $headerLabel, array $config)
    {
        list($rows, $field) = $config;
        $sheet = $spreadsheet->createSheet();
        $sheet->setTitle($title);
        $sheet->setCellValue('A1', $headerLabel);
        $data = array();
        foreach ($rows as $r) {
            $data[] = array($r[$field]);
        }
        if (!empty($data)) {
            $sheet->fromArray($data, null, 'A2');
        }
        $sheet->getColumnDimension('A')->setAutoSize(true);
    }

    public static function buildErrorSpreadsheet(array $errors)
    {
        if (class_exists('\PhpOffice\PhpSpreadsheet\Spreadsheet')) {
            $spreadsheet = new Spreadsheet();
            $sheet = $spreadsheet->getActiveSheet();
            $sheet->setTitle('Errors');

            $headers = array('Row Number', 'Product Name', 'Category', 'Sub Category', 'Portion', 'Error', 'Error Code');
            $sheet->fromArray($headers, null, 'A1');

            $rows = array();
            foreach ($errors as $err) {
                $rows[] = array(
                    isset($err['row']) ? $err['row'] : '',
                    isset($err['productName']) ? $err['productName'] : '',
                    isset($err['category']) ? $err['category'] : '',
                    isset($err['subCategory']) ? $err['subCategory'] : '',
                    isset($err['portion']) ? $err['portion'] : '',
                    isset($err['message']) ? $err['message'] : '',
                    isset($err['code']) ? $err['code'] : '',
                );
            }
            if (!empty($rows)) {
                $sheet->fromArray($rows, null, 'A2');
            }
            return $spreadsheet;
        }
        return null;
    }

    public static function buildErrorSheetRows(array $errors)
    {
        $rows = array(array('Row Number', 'Product Name', 'Category', 'Sub Category', 'Portion', 'Error', 'Error Code'));
        foreach ($errors as $err) {
            $rows[] = array(
                isset($err['row']) ? $err['row'] : '',
                isset($err['productName']) ? $err['productName'] : '',
                isset($err['category']) ? $err['category'] : '',
                isset($err['subCategory']) ? $err['subCategory'] : '',
                isset($err['portion']) ? $err['portion'] : '',
                isset($err['message']) ? $err['message'] : '',
                isset($err['code']) ? $err['code'] : '',
            );
        }
        return $rows;
    }
}
