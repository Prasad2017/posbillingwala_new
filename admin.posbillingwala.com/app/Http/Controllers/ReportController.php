<?php

namespace App\Http\Controllers;

use App\Services\AdminMetrics;
use Auth;

class ReportController extends Controller
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

    public function hub()
    {
        $this->adminOnly();
        return view('reports.hub');
    }

    public function customers()
    {
        $this->adminOnly();
        $data = AdminMetrics::customerReport();
        return view('reports.customers', compact('data'));
    }

    public function licenses()
    {
        $this->adminOnly();
        $data = AdminMetrics::licenseReport();
        return view('reports.licenses', compact('data'));
    }

    public function dealers()
    {
        $this->adminOnly();
        try {
            $status = AdminMetrics::dealerReport();
            $sales = AdminMetrics::dealerSales(20, 'month');
            $data = array_merge($status, $sales);
        } catch (\Throwable $e) {
            \Log::error('Dealer report failed: ' . $e->getMessage());
            $data = [
                'totalDealer' => 0,
                'activeDealer' => 0,
                'inactiveDealer' => 0,
                'totalCustomers' => 0,
                'activePercent' => 0,
                'inactivePercent' => 0,
                'growthBars' => [],
                'dealers' => [],
                'totalSales' => 0,
            ];
        }
        return view('reports.dealers', compact('data'));
    }

    public function branches()
    {
        $this->adminOnly();
        $data = AdminMetrics::branchReport();
        return view('reports.branches', compact('data'));
    }

    public function devices()
    {
        $this->adminOnly();
        $data = AdminMetrics::deviceReport();
        return view('reports.devices', compact('data'));
    }
}
