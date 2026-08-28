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
    public function getAllSubcategories(Request $request)
    {
        if ($request->ajax()) {
            $data = ProductSubcategory::leftJoin('categories', 'categories.categoryId', '=', 'product_subcategories.categoryId')
                ->whereIn('product_subcategories.subcategoryStatus', ['active', 'inactive'])
                ->select(
                    'product_subcategories.*',
                    'categories.categoryName',
                    'categories.dealerId'
                );

            if ($request->customer_id) {
                $data = $data->where('product_subcategories.userId', $request->customer_id);
            } elseif (Auth::user()->role_id == 3) {
                $data = $data->where('product_subcategories.userId', Auth::user()->id);
            } elseif (Auth::user()->role_id == 2) {
                $data = $data->where('categories.dealerId', Auth::user()->id);
            }

            return DataTables::of($data)->make(true);
        }

        $customers = User::where('is_active', 1)->where('role_id', 3);
        if (Auth::user()->role_id == 2) {
            $customers = $customers->where('dealerId', Auth::id());
        }
        $customers = $customers->orderBy('name')->get();

        return view('subcategories.list', compact('customers'));
    }

    public function getAddRecordPage()
    {
        $users = User::where('is_active', 1)->where('role_id', 3);
        if (Auth::user()->role_id == 2) {
            $users = $users->where('dealerId', Auth::user()->id);
        }
        $users = $users->get();

        $categories = Category::whereIn('categoryStatus', ['active', 'inactive']);
        if (Auth::user()->role_id == 3) {
            $categories = $categories->where('userId', Auth::user()->id);
        } elseif (Auth::user()->role_id == 2) {
            $categories = $categories->where('dealerId', Auth::user()->id);
        }
        $categories = $categories->orderBy('categoryName')->get();

        return view('subcategories.add', compact('users', 'categories'));
    }

    public function addSubcategoryRecord(Request $request)
    {
        $request->validate([
            'subcategory_name' => 'required|string|max:255',
            'category_id' => 'required|integer',
        ]);

        $category = Category::where('categoryId', $request->category_id)->firstOrFail();

        if (Auth::user()->role_id == 1 || Auth::user()->role_id == 2) {
            $request->validate(['user_id' => 'required|integer']);
            $userId = (int) $request->user_id;
            if ((int) $category->userId !== $userId) {
                return redirect()->back()->with('error', 'Selected category does not belong to this customer');
            }
            if (Auth::user()->role_id == 2 && (int) $category->dealerId !== (int) Auth::id()) {
                abort(403);
            }
        } else {
            $userId = (int) Auth::user()->id;
            if ((int) $category->userId !== $userId) {
                abort(403);
            }
        }

        $data = new ProductSubcategory();
        $data->userId = $userId;
        $data->categoryId = $category->categoryId;
        $data->subcategoryName = $request->subcategory_name;
        $data->subcategoryNetworkStatus = substr(md5(uniqid('', true)), 0, 10);
        $data->subcategoryStatus = 'active';
        $data->save();

        return redirect('subcategories/all')->with('success', 'Subcategory added successfully');
    }

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

        $data = new ProductSubcategory();
        $data->userId = $userId;
        $data->categoryId = $categoryId;
        $data->subcategoryName = $request->subcategory_name;
        $data->subcategoryNetworkStatus = substr(md5(uniqid('', true)), 0, 10);
        $data->subcategoryStatus = 'active';
        $data->save();

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
