<?php

namespace App\Services;

use Illuminate\Support\Facades\DB;
use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;

class CatalogProductExporter
{
    private int $customerId;

    public function __construct(int $customerId)
    {
        $this->customerId = $customerId;
    }

    public function fetchExportRows(): array
    {
        $rows = DB::select(
            'SELECT p.productName, p.productCode, c.categoryName, ps.subcategoryName,
                    pm.portionName, pp.portionPrice, p.productPrice, p.productCGST, p.productSGST,
                    p.productUnit, p.productStatus
             FROM products p
             INNER JOIN categories c ON c.categoryId = p.categoryId
             LEFT JOIN product_subcategories ps ON ps.subcategoryId = p.subcategoryId
             LEFT JOIN product_portions pp ON pp.productId = p.productId AND pp.portionStatus = ?
             LEFT JOIN portion_master pm ON pm.portionMasterId = pp.portionMasterId
             WHERE p.userId = ? AND p.productStatus != ?
             ORDER BY c.categoryName, p.productName, pp.portionSortOrder, pm.portionName',
            ['active', $this->customerId, 'deleted']
        );

        $exportRows = [];
        foreach ($rows as $row) {
            $gst = (int) $row->productCGST + (int) $row->productSGST;
            $price = $row->portionPrice !== null && $row->portionPrice !== ''
                ? $row->portionPrice
                : $row->productPrice;

            $exportRows[] = [
                $row->productName,
                $row->productCode,
                $row->categoryName,
                $row->subcategoryName ?? '',
                $row->portionName ?? '',
                $price,
                $gst,
                $row->productUnit,
                ucfirst(strtolower((string) $row->productStatus)),
            ];
        }

        return $exportRows;
    }

    public function buildSpreadsheet(): Spreadsheet
    {
        $spreadsheet = new Spreadsheet();
        $this->buildInstructionsSheet($spreadsheet);
        $this->buildProductsSheet($spreadsheet);
        $this->buildReferenceSheets($spreadsheet);
        $spreadsheet->setActiveSheetIndex(1);

        return $spreadsheet;
    }

    public function filename(string $customerName): string
    {
        $slug = preg_replace('/[^a-zA-Z0-9_-]+/', '_', trim($customerName)) ?: 'customer';

        return 'Products_' . $slug . '_' . date('Y-m-d') . '.xlsx';
    }

    public function downloadResponse(string $customerName)
    {
        $spreadsheet = $this->buildSpreadsheet();
        $writer = new Xlsx($spreadsheet);
        $filename = $this->filename($customerName);

        return response()->streamDownload(function () use ($writer) {
            $writer->save('php://output');
        }, $filename, [
            'Content-Type' => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        ]);
    }

    private function buildInstructionsSheet(Spreadsheet $spreadsheet): void
    {
        $sheet = $spreadsheet->getActiveSheet();
        $sheet->setTitle('Instructions');
        $sheet->fromArray([
            ['PRODUCT IMPORT TEMPLATE'],
            [''],
            ['Required:'],
            ['- Product Name'],
            ['- Category'],
            [''],
            ['Optional:'],
            ['- Sub Category'],
            ['- Portion'],
            ['- Product Code'],
            ['- Price'],
            ['- GST'],
            ['- Unit'],
            ['- Status'],
            [''],
            ['Rules:'],
            ['1. Do not change column names on the Products sheet.'],
            ['2. Category must already exist.'],
            ['3. Sub Category and Portion are optional.'],
            ['4. Do not enter database IDs.'],
            ['5. Use exact master names.'],
            ['6. Do not add formulas.'],
            ['7. Remove example rows before final import.'],
        ], null, 'A1');
    }

    private function buildProductsSheet(Spreadsheet $spreadsheet): void
    {
        $sheet = $spreadsheet->createSheet();
        $sheet->setTitle('Products');

        $headers = [
            'Product Name', 'Product Code', 'Category', 'Sub Category', 'Portion',
            'Price', 'GST', 'Unit', 'Status',
        ];
        $sheet->fromArray($headers, null, 'A1');

        $exportRows = $this->fetchExportRows();
        if (empty($exportRows)) {
            $examples = [
                ['Veg Thali', 'P001', 'Veg', 'Main Course', 'Full', 120, 5, 'Pcs', 'Active'],
                ['Mini Thali', 'P002', 'Veg', 'Main Course', 'Half', 80, 5, 'Pcs', 'Active'],
                ['Paneer Masala', 'P003', 'Veg', 'Main Course', '', 180, 5, 'Pcs', 'Active'],
                ['Cold Coffee', 'P004', 'Beverages', 'Cold Drinks', '', 90, 5, 'Pcs', 'Active'],
            ];
            $sheet->fromArray($examples, null, 'A2');
        } else {
            $sheet->fromArray($exportRows, null, 'A2');
        }

        foreach (range('A', 'I') as $col) {
            $sheet->getColumnDimension($col)->setAutoSize(true);
        }
    }

    private function buildReferenceSheets(Spreadsheet $spreadsheet): void
    {
        $this->buildSimpleListSheet(
            $spreadsheet,
            'Categories',
            'Category Name',
            DB::table('categories')
                ->where('userId', $this->customerId)
                ->where('categoryStatus', 'active')
                ->orderBy('categoryName')
                ->pluck('categoryName')
                ->all()
        );

        $subs = DB::select(
            'SELECT c.categoryName, ps.subcategoryName
             FROM product_subcategories ps
             INNER JOIN categories c ON c.categoryId = ps.categoryId
             WHERE ps.userId = ? AND ps.subcategoryStatus = ?
             ORDER BY c.categoryName, ps.subcategoryName',
            [$this->customerId, 'active']
        );
        $sheet = $spreadsheet->createSheet();
        $sheet->setTitle('Sub Categories');
        $sheet->fromArray(['Category', 'Sub Category Name'], null, 'A1');
        $subRows = array_map(fn ($s) => [$s->categoryName, $s->subcategoryName], $subs);
        if (!empty($subRows)) {
            $sheet->fromArray($subRows, null, 'A2');
        }

        $this->buildSimpleListSheet(
            $spreadsheet,
            'Portions',
            'Portion Name',
            DB::table('portion_master')
                ->where('userId', $this->customerId)
                ->where('portionMasterStatus', 'active')
                ->orderBy('portionName')
                ->pluck('portionName')
                ->all()
        );
    }

    private function buildSimpleListSheet(Spreadsheet $spreadsheet, string $title, string $headerLabel, array $values): void
    {
        $sheet = $spreadsheet->createSheet();
        $sheet->setTitle($title);
        $sheet->setCellValue('A1', $headerLabel);
        $data = array_map(fn ($v) => [$v], $values);
        if (!empty($data)) {
            $sheet->fromArray($data, null, 'A2');
        }
        $sheet->getColumnDimension('A')->setAutoSize(true);
    }
}
