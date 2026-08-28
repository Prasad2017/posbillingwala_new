<?php

namespace App\Http\Controllers;

use App\Models\PortionMaster;
use App\Models\Product;
use App\Models\ProductPortion;
use App\Models\User;
use Auth;
use DataTables;
use Illuminate\Http\Request;

class PortionController extends Controller
{
    public function index(Request $request, $userId, $productId)
    {
        $customer = User::where('id', $userId)->where('role_id', 3)->firstOrFail();
        $product = Product::where('productId', $productId)->where('userId', $userId)->firstOrFail();
        $this->authorizeCustomer($customer);

        if ($request->ajax()) {
            $data = ProductPortion::where('userId', $userId)
                ->where('productId', $productId)
                ->whereIn('portionStatus', ['active', 'inactive', 'deactive']);

            return DataTables::of($data)->make(true);
        }

        $portionMasters = PortionMaster::where('userId', $userId)
            ->where('portionMasterStatus', 'active')
            ->orderBy('portionName')
            ->get();

        return view('portions.all', compact('customer', 'product', 'portionMasters'));
    }

    public function store(Request $request, $userId, $productId)
    {
        $customer = User::where('id', $userId)->where('role_id', 3)->firstOrFail();
        $this->authorizeCustomer($customer);

        $request->validate([
            'portion_master_id' => 'nullable|integer',
            'portion_name' => 'nullable|string|max:64',
            'portion_price' => 'required|numeric|min:0',
            'portion_sort_order' => 'nullable|integer|min:0',
        ]);

        $master = null;
        if ($request->filled('portion_master_id')) {
            $master = PortionMaster::where('portionMasterId', $request->portion_master_id)
                ->where('userId', $userId)
                ->where('portionMasterStatus', 'active')
                ->first();
        }

        $portionName = trim((string) $request->portion_name);
        if ($master === null && $portionName !== '') {
            $master = PortionMaster::where('userId', $userId)
                ->whereRaw('LOWER(TRIM(portionName)) = ?', [strtolower($portionName)])
                ->where('portionMasterStatus', 'active')
                ->first();
            if ($master === null) {
                $master = PortionMaster::create([
                    'userId' => $userId,
                    'portionName' => $portionName,
                    'portionMasterNetworkStatus' => substr(md5(uniqid('web', true)), 0, 16),
                    'portionMasterStatus' => 'active',
                    'created_at' => now(),
                    'updated_at' => now(),
                ]);
            }
        }

        if ($master === null) {
            return redirect()->back()->with('error', 'Select or enter a Portion Master name.');
        }

        $existing = ProductPortion::where('productId', $productId)
            ->where('portionMasterId', $master->portionMasterId)
            ->first();

        if ($existing) {
            $existing->portionName = $master->portionName;
            $existing->portionPrice = $request->portion_price;
            $existing->portionSortOrder = $request->portion_sort_order ?? 0;
            $existing->portionStatus = 'active';
            $existing->updated_at = now();
            $existing->save();
            return redirect()->back()->with('success', 'Portion price updated for this product');
        }

        ProductPortion::create([
            'userId' => $userId,
            'productId' => $productId,
            'portionMasterId' => $master->portionMasterId,
            'portionName' => $master->portionName,
            'portionPrice' => $request->portion_price,
            'portionSortOrder' => $request->portion_sort_order ?? 0,
            'portionNetworkStatus' => substr(md5(uniqid('', true)), 0, 10),
            'portionStatus' => 'active',
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        return redirect()->back()->with('success', 'Product portion added successfully');
    }

    public function deleteRecord($id)
    {
        $data = ProductPortion::findOrFail($id);
        $customer = User::findOrFail($data->userId);
        $this->authorizeCustomer($customer);

        $data->portionStatus = in_array($data->portionStatus, ['active'], true) ? 'inactive' : 'active';
        $data->save();

        return redirect()->back()->with('success', 'Portion status updated');
    }

    private function authorizeCustomer(User $customer): void
    {
        $role = Auth::user()->role_id;
        if ($role == 2 && (int) $customer->dealerId !== (int) Auth::id()) {
            abort(403);
        }
        if ($role == 3 && (int) $customer->id !== (int) Auth::id()) {
            abort(403);
        }
        if (!in_array($role, [1, 2, 3], true)) {
            abort(403);
        }
    }
}
