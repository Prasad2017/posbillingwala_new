<?php

namespace App\Http\Controllers;

use App\Services\AdminBranding;
use Auth;
use Hash;
use Illuminate\Http\Request;

class SettingsController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
    }

    private function adminOnly(): void
    {
        if (Auth::user()->role_id != 1) {
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
        $rules = [
            'name' => 'required|string|max:120',
            'contact_number' => 'nullable|digits:10',
            'address' => 'nullable|string|max:500',
            'shopName' => 'nullable|string|max:255',
        ];

        if ((int) $user->role_id === 1) {
            $rules['email'] = 'required|email|max:190|unique:users,email,' . $user->id;
        } else {
            $rules['email'] = 'nullable|email|max:190|unique:users,email,' . $user->id;
        }

        $validated = $request->validate($rules);

        $user->name = $validated['name'];
        $user->email = $validated['email'] ?? null;
        $user->contact_number = $validated['contact_number'] ?? null;
        $user->address = $validated['address'] ?? null;
        if (array_key_exists('shopName', $validated)) {
            $user->shopName = $validated['shopName'] ?? null;
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
}
