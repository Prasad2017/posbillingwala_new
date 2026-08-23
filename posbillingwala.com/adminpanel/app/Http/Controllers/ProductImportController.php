<?php

namespace App\Http\Controllers;

use App\Models\Category;
use App\Models\Product;
use App\Models\User;
use Auth;
use Illuminate\Http\Request;
use PhpOffice\PhpSpreadsheet\IOFactory;

class ProductImportController extends Controller
{
    public function index()
    {
        $users = User::where('is_active', 1)->where('role_id', 3);
        if (Auth::user()->role_id == 2) {
            $users = $users->where('dealerId', Auth::id());
        }
        $users = $users->orderBy('name')->get();

        return view('product-import.index', compact('users'));
    }

    public function import(Request $request)
    {
        $request->validate([
            'user_id' => 'required|exists:users,id',
            'import_file' => 'required|file|mimes:csv,txt,xlsx,xls|max:10240',
        ]);

        $customer = User::where('id', $request->user_id)->where('role_id', 3)->firstOrFail();
        if (Auth::user()->role_id == 2 && (int) $customer->dealerId !== (int) Auth::id()) {
            abort(403);
        }

        $file = $request->file('import_file');
        $extension = strtolower($file->getClientOriginalExtension());

        try {
            $rows = in_array($extension, ['xlsx', 'xls'], true)
                ? $this->readSpreadsheetRows($file->getRealPath())
                : $this->readCsvRows($file->getRealPath());
        } catch (\Throwable $e) {
            return redirect()->back()->with('error', 'Unable to read file: ' . $e->getMessage());
        }

        if (empty($rows)) {
            return redirect()->back()->with('error', 'No product rows found in the uploaded file.');
        }

        $imported = 0;
        $updated = 0;
        $failed = 0;

        foreach ($rows as $row) {
            $result = $this->importRow($customer, $row);
            if ($result === 'imported') {
                $imported++;
            } elseif ($result === 'updated') {
                $updated++;
            } else {
                $failed++;
            }
        }

        return redirect()->back()->with(
            'success',
            "Import complete: {$imported} added, {$updated} updated" . ($failed ? ", {$failed} skipped" : '')
        );
    }

    private function readCsvRows(string $path): array
    {
        $handle = fopen($path, 'r');
        if (!$handle) {
            throw new \RuntimeException('Unable to open CSV file.');
        }

        $rows = [];
        $line = 0;
        while (($row = fgetcsv($handle)) !== false) {
            $line++;
            if ($line === 1 && $this->looksLikeHeaderRow($row)) {
                continue;
            }
            $normalized = $this->normalizeRow($row);
            if ($normalized !== null) {
                $rows[] = $normalized;
            }
        }
        fclose($handle);

        return $rows;
    }

    private function readSpreadsheetRows(string $path): array
    {
        $spreadsheet = IOFactory::load($path);
        $sheet = $spreadsheet->getActiveSheet();
        $rawRows = $sheet->toArray(null, true, true, false);

        $rows = [];
        foreach ($rawRows as $index => $row) {
            if ($index === 0 && $this->looksLikeHeaderRow($row)) {
                continue;
            }
            $normalized = $this->normalizeRow($row);
            if ($normalized !== null) {
                $rows[] = $normalized;
            }
        }

        return $rows;
    }

    private function looksLikeHeaderRow(array $row): bool
    {
        $first = strtolower(trim((string) ($row[0] ?? '')));
        return in_array($first, ['product', 'product name', 'productname'], true);
    }

    private function normalizeRow(array $row): ?array
    {
        $normalized = [
            'product' => trim((string) ($row[0] ?? '')),
            'category' => trim((string) ($row[1] ?? '')),
            'unit' => trim((string) ($row[2] ?? '')),
            'price' => trim((string) ($row[3] ?? '0')),
            'cgst' => trim((string) ($row[4] ?? '0')),
            'sgst' => trim((string) ($row[5] ?? '0')),
        ];

        if ($normalized['product'] === '' && $normalized['category'] === '') {
            return null;
        }

        return $normalized;
    }

    private function importRow(User $customer, array $row): string
    {
        $productName = $row['product'];
        $categoryName = $row['category'];
        $unit = $row['unit'];
        $price = $row['price'] === '' ? '0' : $row['price'];
        $cgst = $row['cgst'] === '' ? '0' : $row['cgst'];
        $sgst = $row['sgst'] === '' ? '0' : $row['sgst'];

        if ($productName === '' || $categoryName === '') {
            return 'failed';
        }

        $category = Category::where('userId', $customer->id)
            ->where('categoryName', $categoryName)
            ->first();

        if (!$category) {
            $category = Category::create([
                'userId' => $customer->id,
                'dealerId' => $customer->dealerId ?? 0,
                'categoryName' => $categoryName,
                'categoryNetworkStatus' => substr(md5(uniqid('', true)), 0, 10),
                'categoryStatus' => 'active',
            ]);
        }

        $product = Product::where('userId', $customer->id)
            ->where('productName', $productName)
            ->first();

        if ($product) {
            $product->categoryId = $category->categoryId;
            $product->productPrice = $price;
            $product->productUnit = $unit;
            $product->productCGST = $cgst;
            $product->productSGST = $sgst;
            $product->save();

            return 'updated';
        }

        Product::create([
            'userId' => $customer->id,
            'dealerId' => $customer->dealerId ?? 0,
            'categoryId' => $category->categoryId,
            'productName' => $productName,
            'productPrice' => $price,
            'productUnit' => $unit,
            'productCGST' => $cgst,
            'productSGST' => $sgst,
            'productNetworkStatus' => substr(md5(uniqid('', true)), 0, 10),
            'productStatus' => 'active',
        ]);

        return 'imported';
    }
}
