<?php

namespace App\Http\Controllers;

use App\Services\AdminTables;
use App\Services\WebsiteMedia;
use Auth;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

class PosAppSplashController extends Controller
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
        AdminTables::ensureWebsite();
        $this->ensureTable();
    }

    private function ensureTable(): void
    {
        if (Schema::hasTable('pos_app_splash')) {
            return;
        }
        Schema::create('pos_app_splash', function ($table) {
            $table->increments('id');
            $table->string('image_path', 255)->nullable();
            $table->string('image_url', 500)->nullable();
            $table->dateTime('updated_at')->useCurrent();
        });
    }

    public function index()
    {
        $this->adminOnly();
        $splash = DB::table('pos_app_splash')->orderByDesc('id')->first();

        return view('settings.pos-app-splash', compact('splash'));
    }

    public function store(Request $request)
    {
        $this->adminOnly();
        $request->validate([
            'splash' => 'required|image|mimes:jpeg,jpg,png,webp|max:4096',
        ]);

        $file = $request->file('splash');
        $path = WebsiteMedia::save($file, 'pos-splash', 'splash_' . time());
        $url = WebsiteMedia::url($path);
        if (!$url) {
            return redirect('settings/pos-splash')->withErrors([
                'splash' => 'Could not save splash image',
            ]);
        }
        if (!str_starts_with($url, 'http://') && !str_starts_with($url, 'https://')) {
            $url = url($url);
        }

        $existing = DB::table('pos_app_splash')->orderByDesc('id')->first();
        if ($existing) {
            WebsiteMedia::delete($existing->image_path);
            DB::table('pos_app_splash')->where('id', $existing->id)->update([
                'image_path' => $path,
                'image_url' => $url,
                'updated_at' => now(),
            ]);
        } else {
            DB::table('pos_app_splash')->insert([
                'image_path' => $path,
                'image_url' => $url,
                'updated_at' => now(),
            ]);
        }

        return redirect('settings/pos-splash')->with(
            'success',
            'Splash screen updated — POS app will use this image on next launch'
        );
    }

    public function destroy()
    {
        $this->adminOnly();
        $existing = DB::table('pos_app_splash')->orderByDesc('id')->first();
        if ($existing) {
            WebsiteMedia::delete($existing->image_path);
            DB::table('pos_app_splash')->where('id', $existing->id)->delete();
        }

        return redirect('settings/pos-splash')->with('success', 'Splash screen removed — app will use default art');
    }
}
