<?php

namespace App\Http\Controllers;

use Auth;

class UsersController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
    }

    public function hub()
    {
        if (Auth::user()->role_id != 1) {
            abort(403);
        }

        return view('users.hub');
    }
}
