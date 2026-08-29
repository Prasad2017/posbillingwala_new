<?php

namespace App\Http\Controllers;

use App\Models\User;
use App\Support\CatalogBridge;
use Auth;
use Illuminate\Http\Request;

class CatalogImportExportController extends Controller
{
    private const TYPES = [
        'products' => 'Products',
        'categories' => 'Categories',
        'subcategories' => 'Sub Categories',
        'portions' => 'Portions',
    ];

    public function __construct()
    {
        $this->middleware('auth');
    }

    public function index(Request $request)
    {
        $importType = $this->normalizeType($request->query('type', 'products'));
        $users = $this->customerQuery()->orderBy('name')->get();

        return view('catalog-import-export.index', [
            'users' => $users,
            'importType' => $importType,
            'typeLabel' => self::TYPES[$importType],
            'types' => self::TYPES,
        ]);
    }

    public function validateUpload(Request $request)
    {
        $importType = $this->normalizeType($request->input('import_type', 'products'));

        $request->validate([
            'user_id' => 'required|exists:users,id',
            'import_file' => 'required|file|mimes:xlsx|max:10240',
        ]);

        $customerId = (int) $request->user_id;
        if (!$this->canManageCustomer($customerId)) {
            abort(403);
        }

        CatalogBridge::bootstrap();
        $con = CatalogBridge::connection();
        $actor = CatalogBridge::resolveWebActor();

        $file = $request->file('import_file');
        $result = catalog_run_validate(
            $con,
            $actor['actor_type'],
            $actor['actor_id'],
            (string) $customerId,
            $importType,
            $file->getRealPath(),
            $file->getClientOriginalName(),
            (int) $file->getSize(),
            false
        );

        $body = $result['body'];
        if ($result['httpStatus'] >= 400 || empty($body['success'])) {
            return redirect()
                ->route('catalog-import-export.index', ['type' => $importType])
                ->withInput()
                ->with('error', $body['message'] ?? 'Validation failed.');
        }

        return view('catalog-import-export.preview', [
            'preview' => $body,
            'importType' => $importType,
            'typeLabel' => self::TYPES[$importType],
            'customerId' => $customerId,
        ]);
    }

    public function confirm(Request $request)
    {
        $request->validate([
            'user_id' => 'required|exists:users,id',
            'import_session_id' => 'required|string',
            'import_type' => 'required|string',
        ]);

        $customerId = (int) $request->user_id;
        $importType = $this->normalizeType($request->import_type);

        if (!$this->canManageCustomer($customerId)) {
            abort(403);
        }

        CatalogBridge::bootstrap();
        $con = CatalogBridge::connection();
        $actor = CatalogBridge::resolveWebActor();

        $result = catalog_run_confirm(
            $con,
            $actor['actor_type'],
            $actor['actor_id'],
            (string) $customerId,
            trim($request->import_session_id)
        );

        $body = $result['body'];
        if ($result['httpStatus'] >= 400 || empty($body['success'])) {
            return redirect()
                ->route('catalog-import-export.index', ['type' => $importType])
                ->with('error', $body['message'] ?? 'Import failed.');
        }

        return redirect()
            ->route('catalog-import-export.index', ['type' => $importType])
            ->with('success', $body['message'] ?? 'Import completed.');
    }

    public function export(Request $request)
    {
        $request->validate([
            'user_id' => 'required|exists:users,id',
            'import_type' => 'required|string',
        ]);

        $customerId = (int) $request->user_id;
        $importType = $this->normalizeType($request->import_type);

        if (!$this->canManageCustomer($customerId)) {
            abort(403);
        }

        CatalogBridge::bootstrap();
        $con = CatalogBridge::connection();
        $actor = CatalogBridge::resolveWebActor();

        if (!CatalogBridge::authorizeCustomer($customerId)) {
            abort(403);
        }

        $_GET['customerId'] = (string) $customerId;
        $_GET['type'] = $importType;

        catalog_handle_export($con, $actor['actor_type'], $actor['actor_id']);
        exit;
    }

    public function template(Request $request)
    {
        $request->validate([
            'user_id' => 'required|exists:users,id',
            'import_type' => 'required|string',
        ]);

        $customerId = (int) $request->user_id;
        $importType = $this->normalizeType($request->import_type);

        if (!$this->canManageCustomer($customerId)) {
            abort(403);
        }

        CatalogBridge::bootstrap();
        $con = CatalogBridge::connection();
        $actor = CatalogBridge::resolveWebActor();

        $_GET['customerId'] = (string) $customerId;
        $_GET['type'] = $importType;

        catalog_handle_template($con, $actor['actor_type'], $actor['actor_id']);
        exit;
    }

    public function errorExcel(Request $request)
    {
        $request->validate([
            'user_id' => 'required|exists:users,id',
            'import_session_id' => 'required|string',
        ]);

        $customerId = (int) $request->user_id;
        if (!$this->canManageCustomer($customerId)) {
            abort(403);
        }

        CatalogBridge::bootstrap();
        $con = CatalogBridge::connection();
        $actor = CatalogBridge::resolveWebActor();

        $_GET['customerId'] = (string) $customerId;
        $_GET['importSessionId'] = trim($request->import_session_id);

        catalog_handle_error_excel($con, $actor['actor_type'], $actor['actor_id']);
        exit;
    }

    public function history(Request $request)
    {
        $importType = $request->query('import_type');
        if ($importType !== null && $importType !== '') {
            $importType = $this->normalizeType($importType);
        }

        $users = $this->customerQuery()->orderBy('name')->get();
        $history = [];
        $selectedCustomerId = (int) $request->query('user_id', 0);

        if ($selectedCustomerId > 0 && $this->canManageCustomer($selectedCustomerId)) {
            CatalogBridge::bootstrap();
            $con = CatalogBridge::connection();
            $actor = CatalogBridge::resolveWebActor();
            $history = \CatalogSessionManager::listHistory(
                $con,
                $actor['actor_type'],
                $actor['actor_id'],
                $selectedCustomerId,
                $importType
            );
        }

        return view('catalog-import-export.history', [
            'users' => $users,
            'history' => $history,
            'selectedCustomerId' => $selectedCustomerId,
            'importType' => $importType,
            'types' => self::TYPES,
        ]);
    }

    private function customerQuery()
    {
        $users = User::where('is_active', 1)->where('role_id', 3);
        if (Auth::user()->role_id == 2) {
            $users = $users->where('dealerId', Auth::id());
        }

        return $users;
    }

    private function canManageCustomer(int $customerId): bool
    {
        $customer = User::where('id', $customerId)->where('role_id', 3)->first();
        if ($customer === null) {
            return false;
        }

        if (Auth::user()->role_id == 2 && (int) $customer->dealerId !== (int) Auth::id()) {
            return false;
        }

        return CatalogBridge::authorizeCustomer($customerId);
    }

    private function normalizeType(string $type): string
    {
        $normalized = strtolower(trim($type));

        if (!isset(self::TYPES[$normalized])) {
            abort(404);
        }

        return $normalized;
    }
}
