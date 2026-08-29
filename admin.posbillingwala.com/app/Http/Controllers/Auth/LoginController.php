<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Providers\RouteServiceProvider;
use Illuminate\Foundation\Auth\AuthenticatesUsers;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\ValidationException;

class LoginController extends Controller
{
    use AuthenticatesUsers;

    protected $redirectTo = RouteServiceProvider::HOME;

    public function __construct()
    {
        $this->middleware('guest')->except('logout');
    }

    public function login(Request $request)
    {
        $request->validate([
            'login' => 'required|string',
            'password' => 'required|string',
        ]);

        $user = $this->resolveUser(trim($request->input('login')));

        if (!$user || !Hash::check($request->password, $user->password)) {
            throw ValidationException::withMessages([
                'login' => ['These credentials do not match our records.'],
            ]);
        }

        if ((int) $user->is_active !== 1) {
            throw ValidationException::withMessages([
                'login' => ['This account is disabled. Please contact support.'],
            ]);
        }

        if (!in_array((int) $user->role_id, [1, 2], true)) {
            throw ValidationException::withMessages([
                'login' => ['Web login is not available for this account.'],
            ]);
        }

        Auth::login($user, $request->boolean('remember'));
        $request->session()->regenerate();

        return redirect()->intended($this->redirectPath());
    }

    private function resolveUser(string $login): ?User
    {
        $base = User::query()->whereIn('role_id', [1, 2]);

        if (filter_var($login, FILTER_VALIDATE_EMAIL)) {
            return (clone $base)->where('email', $login)->first();
        }

        $digits = preg_replace('/\D+/', '', $login);

        if (preg_match('/^\d{12}$/', $digits)) {
            $byAadhaar = (clone $base)->where('aadhar_number', $digits)->where('role_id', 2)->first();
            if ($byAadhaar) {
                return $byAadhaar;
            }
        }

        if (strlen($digits) >= 10) {
            $mobile = substr($digits, -10);
            $byMobile = (clone $base)->where('role_id', 2)
                ->where(function ($query) use ($mobile, $digits) {
                    $query->where('contact_number', $mobile)
                        ->orWhere('contact_number', $digits);
                })
                ->first();
            if ($byMobile) {
                return $byMobile;
            }
        }

        return (clone $base)->where(function ($query) use ($login) {
            $query->where('email', $login)->orWhere('aadhar_number', $login);
        })->first();
    }

    public function logout(Request $request)
    {
        $this->guard()->logout();
        $request->session()->invalidate();
        $request->session()->regenerateToken();

        return redirect('/login');
    }
}
