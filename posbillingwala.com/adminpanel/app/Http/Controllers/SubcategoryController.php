<?php

namespace App\Http\Controllers;

use App\Models\Category;
use App\Models\ProductSubcategory;
use App\Models\User;
use Auth;
use DataTables;
use Illuminate\Http\Request;

class SubcategoryController extends Controller
{
    public function index(Request $request, $userId, $categoryId)
    {
        $customer = User::where('id', $userId)->where('role_id', 3)->firstOrFail();
        $category = Category::where('categoryId', $categoryId)->where('userId', $userId)->firstOrFail();
        $this->authorizeCustomer($customer);

        if ($request->ajax()) {
            $data = ProductSubcategory::where('userId', $userId)
                ->where('categoryId', $categoryId)
                ->whereIn('subcategoryStatus', ['active', 'inactive']);

            return DataTables::of($data)->make(true);
        }

        return view('subcategories.all', compact('customer', 'category'));
    }

    public function store(Request $request, $userId, $categoryId)
    {
        $customer = User::where('id', $userId)->where('role_id', 3)->firstOrFail();
        $this->authorizeCustomer($customer);

        $request->validate([
            'subcategory_name' => 'required|string|max:255',
        ]);

        ProductSubcategory::create([
            'userId' => $userId,
            'categoryId' => $categoryId,
            'subcategoryName' => $request->subcategory_name,
            'subcategoryNetworkStatus' => substr(md5(uniqid('', true)), 0, 10),
            'subcategoryStatus' => 'active',
        ]);

        return redirect()->back()->with('success', 'Subcategory added successfully');
    }

    public function deleteRecord($id)
    {
        $data = ProductSubcategory::findOrFail($id);
        $customer = User::findOrFail($data->userId);
        $this->authorizeCustomer($customer);

        $data->subcategoryStatus = $data->subcategoryStatus === 'active' ? 'inactive' : 'active';
        $data->save();

        return redirect()->back()->with('success', 'Subcategory status updated');
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
