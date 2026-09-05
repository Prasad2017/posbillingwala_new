<?php

namespace App\Http\Controllers;

use Auth;
use Illuminate\Http\Request;

class PushNotificationController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
    }

    private function adminOnly()
    {
        if (Auth::user()->role_id != 1) {
            abort(403);
        }
    }

    /**
     * Resolve Android API root (contains fcm_helper.php).
     * Production: /home/rgusomuk/posbillingwala.com/androidApp/
     */
    private function resolveAndroidAppDir(): ?string
    {
        $candidates = array_filter([
            env('ANDROID_APP_PATH'),
            env('POS_API_PATH'),
            // Production server (posbillingwala.com/androidApp)
            '/home/rgusomuk/posbillingwala.com/androidApp',
            // Local monorepo: admin.posbillingwala.com/../API
            base_path('../API'),
            base_path('API'),
            // Sibling domain folders
            dirname(base_path()) . '/posbillingwala.com/androidApp',
            dirname(base_path()) . '/androidApp',
            base_path('../posbillingwala.com/androidApp'),
            base_path('../../posbillingwala.com/androidApp'),
        ]);

        foreach ($candidates as $dir) {
            if (!is_string($dir) || $dir === '') {
                continue;
            }
            $dir = rtrim(str_replace('\\', '/', $dir), '/');
            if (is_readable($dir . '/fcm_helper.php') && is_readable($dir . '/db_connection.php')) {
                return $dir;
            }
        }

        return null;
    }

    public function index()
    {
        $this->adminOnly();
        return view('push-notifications.index');
    }

    public function send(Request $request)
    {
        $this->adminOnly();

        $request->validate([
            'title' => 'required|string|max:120',
            'message' => 'required|string|max:500',
            'audience' => 'required|in:pos,owner,dealer,admin,all',
            'target' => 'nullable|in:active,all,license_ids',
            'license_ids' => 'nullable|string|max:2000',
            'url' => 'nullable|url|max:500',
            'image_url' => 'nullable|url|max:500',
        ]);

        $apiDir = $this->resolveAndroidAppDir();
        if ($apiDir === null) {
            return back()->with(
                'error',
                'FCM helper not found. On the server, API lives at posbillingwala.com/androidApp/. '
                . 'Set ANDROID_APP_PATH in admin .env to that absolute folder (must contain fcm_helper.php).'
            );
        }

        require_once $apiDir . '/db_connection.php';
        require_once $apiDir . '/fcm_tables.php';
        require_once $apiDir . '/fcm_helper.php';

        if (!isset($con) || !$con) {
            return back()->with('error', 'Database connection unavailable.');
        }

        $extra = array();
        if ($request->filled('url')) {
            $extra['url'] = $request->input('url');
        }
        if ($request->filled('image_url')) {
            $extra['image_url'] = $request->input('image_url');
        }

        $audience = $request->input('audience', 'pos');
        $target = $request->input('target', 'active');
        if (!in_array($audience, array('pos', 'all'), true)) {
            $target = 'active';
        }

        $licenseIds = $target === 'license_ids'
            ? str_replace(' ', '', (string) $request->input('license_ids', ''))
            : '';

        $result = fcm_broadcast_promotional(
            $con,
            $request->input('title'),
            $request->input('message'),
            $target,
            $licenseIds,
            $extra,
            $audience
        );

        mysqli_close($con);

        if (($result['status'] ?? '0') !== '1') {
            return back()->withInput()->with('error', $result['message'] ?? 'Push send failed');
        }

        return back()->with(
            'success',
            'Notification sent to ' . ($result['sent'] ?? '0') . ' device(s). '
            . ($result['failed'] ?? '0') . ' failed, '
            . ($result['skipped'] ?? '0') . ' skipped.'
            . ' Audience: ' . ($result['audience'] ?? $audience) . '.'
        );
    }
}
