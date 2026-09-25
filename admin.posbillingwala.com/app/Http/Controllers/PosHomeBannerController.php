<?php

namespace App\Http\Controllers;

use App\Services\AdminTables;
use App\Services\WebsiteMedia;
use Auth;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class PosHomeBannerController extends Controller
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
    }

    public function index()
    {
        $this->adminOnly();
        $banners = DB::table('pos_home_banners')
            ->orderBy('sort_order')
            ->orderBy('bannerId')
            ->get();

        return view('settings.pos-home-banners', compact('banners'));
    }

    public function store(Request $request)
    {
        $this->adminOnly();
        $request->validate([
            'banners' => 'required|array|min:1|max:8',
            'banners.*' => 'required|image|mimes:jpeg,jpg,png,webp|max:2048',
        ]);

        $next = (int) DB::table('pos_home_banners')->max('sort_order');
        $saved = 0;
        foreach ($request->file('banners') as $file) {
            $next++;
            $path = WebsiteMedia::save($file, 'pos-banners', 'banner_' . time() . '_' . $next);
            $url = WebsiteMedia::url($path);
            if (!$url) {
                continue;
            }
            if (!str_starts_with($url, 'http://') && !str_starts_with($url, 'https://')) {
                $url = url($url);
            }
            DB::table('pos_home_banners')->insert([
                'image_path' => $path,
                'image_url' => $url,
                'sort_order' => $next,
                'is_active' => 1,
                'created_at' => now(),
            ]);
            $saved++;
        }

        return redirect('settings/pos-banners')->with(
            'success',
            $saved === 1 ? '1 banner uploaded' : $saved . ' banners uploaded'
        );
    }

    public function toggle($id)
    {
        $this->adminOnly();
        $row = DB::table('pos_home_banners')->where('bannerId', $id)->first();
        if (!$row) {
            abort(404);
        }
        DB::table('pos_home_banners')->where('bannerId', $id)->update([
            'is_active' => $row->is_active ? 0 : 1,
        ]);

        return redirect('settings/pos-banners');
    }

    public function move($id, $direction)
    {
        $this->adminOnly();
        $rows = DB::table('pos_home_banners')->orderBy('sort_order')->orderBy('bannerId')->get()->values();
        $index = $rows->search(fn ($row) => (int) $row->bannerId === (int) $id);
        if ($index === false) {
            abort(404);
        }
        $swapWith = $direction === 'up' ? $index - 1 : $index + 1;
        if ($swapWith < 0 || $swapWith >= $rows->count()) {
            return redirect('settings/pos-banners');
        }
        $current = $rows[$index];
        $other = $rows[$swapWith];
        DB::table('pos_home_banners')->where('bannerId', $current->bannerId)->update(['sort_order' => $other->sort_order]);
        DB::table('pos_home_banners')->where('bannerId', $other->bannerId)->update(['sort_order' => $current->sort_order]);
        if ((int) $current->sort_order === (int) $other->sort_order) {
            DB::table('pos_home_banners')->where('bannerId', $current->bannerId)->update(['sort_order' => $swapWith]);
            DB::table('pos_home_banners')->where('bannerId', $other->bannerId)->update(['sort_order' => $index]);
        }

        return redirect('settings/pos-banners');
    }

    public function destroy($id)
    {
        $this->adminOnly();
        $row = DB::table('pos_home_banners')->where('bannerId', $id)->first();
        if (!$row) {
            abort(404);
        }
        WebsiteMedia::delete($row->image_path);
        DB::table('pos_home_banners')->where('bannerId', $id)->delete();

        return redirect('settings/pos-banners')->with('success', 'Banner removed');
    }
}
