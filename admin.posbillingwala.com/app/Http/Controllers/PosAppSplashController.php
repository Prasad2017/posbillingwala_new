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
    /** Screen slots the POS app picks from window size and orientation. */
    public const SLOTS = [
        'mobile_portrait' => [
            'label' => 'Mobile portrait',
            'hint' => 'Phones, upright. Recommended 1080×1920 (9:16).',
            'ratio' => '9 / 16',
            'previewWidth' => '140px',
        ],
        'mobile_landscape' => [
            'label' => 'Mobile landscape',
            'hint' => 'Phones, sideways. Recommended 1920×1080 (16:9).',
            'ratio' => '16 / 9',
            'previewWidth' => '240px',
        ],
        'tablet_portrait' => [
            'label' => 'Tablet portrait',
            'hint' => 'Tablets, upright. Recommended 1536×2048 (3:4).',
            'ratio' => '3 / 4',
            'previewWidth' => '180px',
        ],
        'tablet_landscape' => [
            'label' => 'Tablet landscape',
            'hint' => 'Tablets, sideways. Recommended 2048×1536 (4:3).',
            'ratio' => '4 / 3',
            'previewWidth' => '260px',
        ],
        'web_portrait' => [
            'label' => 'Web portrait',
            'hint' => 'Desktop window taller than it is wide (width about 1200px and up). Recommended 1440×1920.',
            'ratio' => '3 / 4',
            'previewWidth' => '180px',
        ],
        'web_landscape' => [
            'label' => 'Web landscape',
            'hint' => 'Wide desktop browser (width about 1200px and up). Recommended 1920×1080 (16:9).',
            'ratio' => '16 / 9',
            'previewWidth' => '280px',
        ],
    ];

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
        if (!Schema::hasTable('pos_app_splash')) {
            Schema::create('pos_app_splash', function ($table) {
                $table->increments('id');
                $table->string('slot', 40)->default('legacy');
                $table->string('image_path', 255)->nullable();
                $table->string('image_url', 500)->nullable();
                $table->dateTime('updated_at')->useCurrent();
                $table->unique('slot', 'pos_app_splash_slot_unique');
            });

            return;
        }

        if (!Schema::hasColumn('pos_app_splash', 'slot')) {
            Schema::table('pos_app_splash', function ($table) {
                $table->string('slot', 40)->default('legacy');
            });
        }

        $index = DB::select("SHOW INDEX FROM pos_app_splash WHERE Key_name = 'pos_app_splash_slot_unique'");
        if (count($index) > 0) {
            return;
        }

        $dupes = DB::table('pos_app_splash')
            ->select('slot', DB::raw('MAX(id) as keep_id'))
            ->groupBy('slot')
            ->havingRaw('COUNT(*) > 1')
            ->get();
        foreach ($dupes as $dupe) {
            DB::table('pos_app_splash')
                ->where('slot', $dupe->slot)
                ->where('id', '!=', $dupe->keep_id)
                ->delete();
        }

        DB::statement('ALTER TABLE pos_app_splash ADD UNIQUE KEY pos_app_splash_slot_unique (slot)');
    }

    public function index()
    {
        $this->adminOnly();
        $rows = DB::table('pos_app_splash')->orderByDesc('id')->get()->keyBy('slot');
        $slots = self::SLOTS;
        $legacy = $rows->get('legacy');

        return view('settings.pos-app-splash', compact('rows', 'slots', 'legacy'));
    }

    public function store(Request $request)
    {
        $this->adminOnly();
        $request->validate([
            'slot' => 'required|in:' . implode(',', array_keys(self::SLOTS)),
            'splash' => 'required|image|mimes:jpeg,jpg,png,webp|max:6144',
        ]);

        $slot = (string) $request->input('slot');
        $file = $request->file('splash');
        $path = WebsiteMedia::save($file, 'pos-splash', 'splash_' . $slot . '_' . time());
        $url = $this->absoluteUrl(WebsiteMedia::url($path));
        if (!$url) {
            return redirect('settings/pos-splash')->withErrors([
                'splash' => 'Could not save splash image',
            ]);
        }

        $existing = DB::table('pos_app_splash')->where('slot', $slot)->first();
        if ($existing) {
            WebsiteMedia::delete($existing->image_path);
            DB::table('pos_app_splash')->where('id', $existing->id)->update([
                'image_path' => $path,
                'image_url' => $url,
                'updated_at' => now(),
            ]);
        } else {
            DB::table('pos_app_splash')->insert([
                'slot' => $slot,
                'image_path' => $path,
                'image_url' => $url,
                'updated_at' => now(),
            ]);
        }

        $label = self::SLOTS[$slot]['label'];

        return redirect('settings/pos-splash')->with(
            'success',
            $label . ' splash saved — the POS app uses it on the next launch for that screen'
        );
    }

    public function destroy(Request $request)
    {
        $this->adminOnly();
        $slot = (string) $request->input('slot', '');
        $allowed = array_merge(array_keys(self::SLOTS), ['legacy', 'all']);
        if (!in_array($slot, $allowed, true)) {
            return redirect('settings/pos-splash')->withErrors([
                'splash' => 'Unknown splash slot',
            ]);
        }

        $query = DB::table('pos_app_splash');
        if ($slot !== 'all') {
            $query->where('slot', $slot);
        }
        foreach ($query->get() as $existing) {
            WebsiteMedia::delete($existing->image_path);
            DB::table('pos_app_splash')->where('id', $existing->id)->delete();
        }

        $message = $slot === 'all'
            ? 'All splash images removed — the app will use the built-in logo'
            : 'Splash image removed';

        return redirect('settings/pos-splash')->with('success', $message);
    }

    private function absoluteUrl(?string $url): ?string
    {
        if (!$url) {
            return null;
        }
        if (!str_starts_with($url, 'http://') && !str_starts_with($url, 'https://')) {
            return url($url);
        }

        return $url;
    }
}
