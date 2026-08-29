<?php

namespace App\Http\Controllers;

use App\Models\PortionMaster;
use App\Models\ProductPortion;
use App\Models\User;
use Auth;
use DataTables;
use Illuminate\Http\Request;

/**
 * Portion Master — name only. Price is set on Product Portions.
 */
class PortionMasterController extends Controller
{
    public function getAll(Request $request)
    {
        if ($request->ajax()) {
            $data = PortionMaster::query()
                ->leftJoin('users', 'users.id', '=', 'portion_master.userId')
                ->whereIn('portion_master.portionMasterStatus', ['active', 'inactive', 'deactive'])
                ->select('portion_master.*', 'users.name as customerName', 'users.shopName');

            if ($request->customer_id) {
                $data = $data->where('portion_master.userId', $request->customer_id);
            } elseif (Auth::user()->role_id == 3) {
                $data = $data->where('portion_master.userId', Auth::user()->id);
            } elseif (Auth::user()->role_id == 2) {
                $customerIds = User::where('dealerId', Auth::id())->where('role_id', 3)->pluck('id');
                $data = $data->whereIn('portion_master.userId', $customerIds);
            }

            $data = $data->orderBy('portionName');

            return DataTables::of($data)->make(true);
        }

        $customers = User::where('is_active', 1)->where('role_id', 3);
        if (Auth::user()->role_id == 2) {
            $customers = $customers->where('dealerId', Auth::id());
        }
        $customers = $customers->orderBy('name')->get();

        return view('portion-masters.list', compact('customers'));
    }

    public function getAddPage()
    {
        $users = User::where('is_active', 1)->where('role_id', 3);
        if (Auth::user()->role_id == 2) {
            $users = $users->where('dealerId', Auth::user()->id);
        }
        $users = $users->get();

        return view('portion-masters.add', compact('users'));
    }

    public function store(Request $request)
    {
        $request->validate([
            'portion_name' => 'required|string|max:64',
        ]);

        if (Auth::user()->role_id == 1 || Auth::user()->role_id == 2) {
            $request->validate(['user_id' => 'required|integer']);
            $userId = (int) $request->user_id;
            if (Auth::user()->role_id == 2) {
                $customer = User::where('id', $userId)->where('dealerId', Auth::id())->firstOrFail();
            }
        } else {
            $userId = (int) Auth::user()->id;
        }

        $name = trim($request->portion_name);
        $exists = PortionMaster::where('userId', $userId)
            ->whereRaw('LOWER(TRIM(portionName)) = ?', [strtolower($name)])
            ->where('portionMasterStatus', 'active')
            ->exists();
        if ($exists) {
            return redirect()->back()->with('error', 'Portion name already exists for this customer');
        }

        PortionMaster::create([
            'userId' => $userId,
            'portionName' => $name,
            'portionMasterNetworkStatus' => substr(md5(uniqid('pm', true)), 0, 16),
            'portionMasterStatus' => 'active',
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        return redirect('portion-masters/all')->with('success', 'Portion Master added (name only)');
    }

    public function toggle($id)
    {
        $data = PortionMaster::findOrFail($id);
        $customer = User::findOrFail($data->userId);
        $this->authorizeCustomer($customer);

        if ($data->portionMasterStatus === 'active') {
            $inUse = ProductPortion::where('portionMasterId', $data->portionMasterId)
                ->whereIn('portionStatus', ['active', null, ''])
                ->count();
            if ($inUse > 0) {
                return redirect()->back()->with('error', 'Cannot deactivate — used by ' . $inUse . ' product portion(s). Remove those first.');
            }
            $data->portionMasterStatus = 'inactive';
        } else {
            $data->portionMasterStatus = 'active';
        }
        $data->updated_at = now();
        $data->save();

        return redirect()->back()->with('success', 'Portion Master status updated');
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
