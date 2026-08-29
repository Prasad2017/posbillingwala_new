<?php

namespace App\Http\Controllers;

use App\Models\User;
use App\Services\CatalogProductExporter;
use Auth;
use Illuminate\Http\Request;

class ProductExportController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
    }

    public function index()
    {
        $users = User::where('is_active', 1)->where('role_id', 3);
        if (Auth::user()->role_id == 2) {
            $users = $users->where('dealerId', Auth::id());
        }
        $users = $users->orderBy('name')->get();

        return view('product-export.index', compact('users'));
    }

    public function export(Request $request)
    {
        $request->validate([
            'user_id' => 'required|exists:users,id',
        ]);

        $customer = User::where('id', $request->user_id)->where('role_id', 3)->firstOrFail();
        if (Auth::user()->role_id == 2 && (int) $customer->dealerId !== (int) Auth::id()) {
            abort(403);
        }

        $exporter = new CatalogProductExporter((int) $customer->id);
        $label = $customer->shopName ?: $customer->name ?: 'customer';

        return $exporter->downloadResponse($label);
    }
}
