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
            'target' => 'required|in:active,all,license_ids',
            'license_ids' => 'nullable|string|max:2000',
            'url' => 'nullable|url|max:500',
            'image_url' => 'nullable|url|max:500',
        ]);

        $apiPath = base_path('../API/fcm_helper.php');
        if (!is_readable($apiPath)) {
            return back()->with('error', 'FCM helper not found on server. Deploy API/fcm_helper.php.');
        }

        require_once base_path('../API/db_connection.php');
        require_once base_path('../API/fcm_tables.php');
        require_once $apiPath;

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

        $licenseIds = $request->input('target') === 'license_ids'
            ? str_replace(' ', '', (string) $request->input('license_ids', ''))
            : '';

        $result = fcm_broadcast_promotional(
            $con,
            $request->input('title'),
            $request->input('message'),
            $request->input('target'),
            $licenseIds,
            $extra
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
        );
    }
}
