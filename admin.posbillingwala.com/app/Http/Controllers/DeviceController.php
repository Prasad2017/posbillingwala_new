<?php

namespace App\Http\Controllers;

use App\Services\AdminMetrics;
use Auth;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class DeviceController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
    }

    public function index(Request $request)
    {
        if (Auth::user()->role_id != 1) {
            abort(403);
        }

        $customerId = $request->get('customerId') ? (int) $request->get('customerId') : null;
        $onlineMinutes = 15;
        $devices = [];
        $report = [
            'totalDevices' => 0,
            'activeDevices' => 0,
            'inactiveDevices' => 0,
            'notUsedDevices' => 0,
            'onlineDevices' => 0,
            'offlineDevices' => 0,
            'onlinePercent' => 0,
            'activePercent' => 0,
            'inactivePercent' => 0,
            'notUsedPercent' => 0,
        ];

        try {
            $onlineMinutes = AdminMetrics::onlineMinutes();
        } catch (\Throwable $e) {
            Log::warning('POS Monitoring onlineMinutes failed: ' . $e->getMessage());
        }

        try {
            $devices = AdminMetrics::devices($customerId);
        } catch (\Throwable $e) {
            Log::error('POS Monitoring devices failed: ' . $e->getMessage());
            $devices = [];
        }

        try {
            $report = AdminMetrics::deviceReport();
        } catch (\Throwable $e) {
            Log::error('POS Monitoring report failed: ' . $e->getMessage());
        }

        return view('devices.index', compact('devices', 'report', 'customerId', 'onlineMinutes'));
    }
}
