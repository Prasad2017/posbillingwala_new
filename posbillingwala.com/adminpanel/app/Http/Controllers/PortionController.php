<?php

namespace App\Http\Controllers;

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
                ->whereIn('portionStatus', ['active', 'inactive']);

            return DataTables::of($data)->make(true);
        }

        return view('portions.all', compact('customer', 'product'));
    }

    public function store(Request $request, $userId, $productId)
    {
        $customer = User::where('id', $userId)->where('role_id', 3)->firstOrFail();
        $this->authorizeCustomer($customer);

        $request->validate([
            'portion_name' => 'required|string|max:64',
            'portion_price' => 'required|numeric|min:0',
            'portion_sort_order' => 'nullable|integer|min:0',
        ]);

        ProductPortion::create([
            'userId' => $userId,
            'productId' => $productId,
            'portionName' => $request->portion_name,
            'portionPrice' => $request->portion_price,
            'portionSortOrder' => $request->portion_sort_order ?? 0,
            'portionNetworkStatus' => substr(md5(uniqid('', true)), 0, 10),
            'portionStatus' => 'active',
        ]);

        return redirect()->back()->with('success', 'Portion added successfully');
    }

    public function deleteRecord($id)
    {
        $data = ProductPortion::findOrFail($id);
        $customer = User::findOrFail($data->userId);
        $this->authorizeCustomer($customer);

        $data->portionStatus = $data->portionStatus === 'active' ? 'inactive' : 'active';
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
