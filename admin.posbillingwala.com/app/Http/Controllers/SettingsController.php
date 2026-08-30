<?php

namespace App\Http\Controllers;

use App\Services\AdminBranding;
use Auth;
use Hash;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Schema;
use Illuminate\Validation\Rule;

class SettingsController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
    }

    private function adminOnly(): void
    {
        if ((int) Auth::user()->role_id !== 1) {
            abort(403);
        }
    }

    public function hub()
    {
        $user = Auth::user();

        return view('settings.hub', compact('user'));
    }

    public function profile()
    {
        $user = Auth::user();

        return view('settings.profile', compact('user'));
    }

    public function updateProfile(Request $request)
    {
        $user = Auth::user();
        $roleId = (int) $user->role_id;

        $rules = [
            'name' => 'required|string|max:120',
            'contact_number' => 'nullable|digits:10',
        ];

        if ($this->usersHasColumn('address')) {
            $rules['address'] = 'nullable|string|max:500';
        }

        if ($roleId === 1) {
            $rules['email'] = 'required|email|max:190|unique:users,email,' . $user->id;
        } else {
            $rules['email'] = 'nullable|email|max:190|unique:users,email,' . $user->id;
        }

        if ($roleId === 2) {
            $rules['contact_number'] = [
                'required',
                'digits:10',
                Rule::unique('users', 'contact_number')->ignore($user->id)->where(function ($query) {
                    return $query->where('role_id', 2);
                }),
            ];
            if ($this->usersHasColumn('address')) {
                $rules['address'] = 'required|string|max:500';
            }
            if ($this->usersHasColumn('aadhar_number')) {
                $rules['aadhar_number'] = [
                    'required',
                    'digits:12',
                    Rule::unique('users', 'aadhar_number')->ignore($user->id)->where(function ($query) {
                        return $query->where('role_id', 2);
                    }),
                ];
            }
        }

        if (($roleId === 2 || $roleId === 3) && $this->usersHasColumn('shopName')) {
            $rules['shopName'] = 'nullable|string|max:255';
        }

        $validated = $request->validate($rules);

        $user->name = $validated['name'];
        $user->email = $validated['email'] ?? null;

        if ($this->usersHasColumn('contact_number')) {
            $user->contact_number = $validated['contact_number'] ?? null;
        }
        if ($this->usersHasColumn('address') && array_key_exists('address', $validated)) {
            $user->address = $validated['address'] ?? null;
        }
        if ($this->usersHasColumn('shopName') && array_key_exists('shopName', $validated)) {
            $user->shopName = $validated['shopName'] ?? null;
        }
        if ($roleId === 2 && $this->usersHasColumn('aadhar_number') && array_key_exists('aadhar_number', $validated)) {
            $user->aadhar_number = $validated['aadhar_number'];
        }

        $user->save();

        return redirect('settings/profile')->with('success', 'Profile updated successfully');
    }

    public function password()
    {
        return view('settings.password');
    }

    public function updatePassword(Request $request)
    {
        $request->validate([
            'current_password' => 'required',
            'password' => 'required|string|min:8|confirmed',
        ]);

        $user = Auth::user();
        if (!Hash::check($request->current_password, $user->password)) {
            return redirect()->back()->withErrors(['current_password' => 'Current password is incorrect'])->withInput();
        }

        $user->password = Hash::make($request->password);
        $user->save();

        return redirect('settings/password')->with('success', 'Password changed successfully');
    }

    public function logo()
    {
        $this->adminOnly();

        return view('settings.logo', [
            'logoUrl' => AdminBranding::logoUrl(),
            'hasCustom' => AdminBranding::hasCustomLogo(),
        ]);
    }

    public function updateLogo(Request $request)
    {
        $this->adminOnly();
        $request->validate([
            'logo' => 'required|image|mimes:jpeg,jpg,png,webp,svg|max:2048',
        ]);

        AdminBranding::saveLogo($request->file('logo'));

        return redirect('settings/logo')->with('success', 'Admin logo updated successfully');
    }

    public function favicon()
    {
        $this->adminOnly();

        return view('settings.favicon', [
            'faviconUrl' => AdminBranding::faviconUrl(),
            'hasCustom' => AdminBranding::hasCustomFavicon(),
        ]);
    }

    public function updateFavicon(Request $request)
    {
        $this->adminOnly();
        $request->validate([
            'favicon' => 'required|file|mimes:jpeg,jpg,png,webp,ico,svg|max:512',
        ]);

        AdminBranding::saveFavicon($request->file('favicon'));

        return redirect('settings/favicon')->with('success', 'Favicon updated successfully');
    }

    private function usersHasColumn(string $column): bool
    {
        static $cache = [];

        if (!array_key_exists($column, $cache)) {
            $cache[$column] = Schema::hasColumn('users', $column);
        }

        return $cache[$column];
    }
}
