<?php

namespace App\Http\Controllers;

use App\Models\CrashLog;
use App\Models\ErrorLog;
use App\Services\AdminMetrics;
use App\Services\AdminTables;
use Auth;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Schema;

class CrashController extends Controller
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
        AdminTables::ensure();
    }

    public function index(Request $request)
    {
        $this->adminOnly();
        $q = trim((string) $request->get('q', ''));
        $app = trim((string) $request->get('app', ''));
        $status = trim((string) $request->get('status', ''));

        $query = CrashLog::query();
        if ($q !== '') {
            $query->where(function ($w) use ($q) {
                $w->where('error_title', 'like', '%' . $q . '%')
                    ->orWhere('error_class', 'like', '%' . $q . '%');
            });
        }
        if ($app !== '' && strtolower($app) !== 'all') {
            $query->where('app_name', $app);
        }
        if ($status !== '' && strtolower($status) !== 'all') {
            $query->where('status', $status);
        }
        $crashes = $query->orderByDesc('id')->limit(200)->get();
        $analytics = AdminMetrics::crashAnalytics();
        $total = CrashLog::count();
        $affected = CrashLog::query()->select('user_id')->distinct()->count('user_id');
        $resolved = CrashLog::whereRaw("LOWER(status)='resolved'")->count();
        $errorCount = Schema::hasTable('error_logs') ? ErrorLog::count() : 0;

        return view('crashes.index', compact(
            'crashes', 'analytics', 'q', 'app', 'status', 'total', 'affected', 'resolved', 'errorCount'
        ));
    }

    public function show($id)
    {
        $this->adminOnly();
        $crash = CrashLog::findOrFail($id);
        return view('crashes.show', compact('crash'));
    }

    public function updateStatus(Request $request, $id)
    {
        $this->adminOnly();
        $crash = CrashLog::findOrFail($id);
        $crash->status = $request->get('status', 'Resolved');
        $crash->updated_at = now();
        $crash->save();
        return redirect()->back()->with('success', 'Crash status updated');
    }

    public function errors()
    {
        $this->adminOnly();
        $logs = Schema::hasTable('error_logs')
            ? ErrorLog::orderByDesc('last_seen_at')->limit(500)->get()
            : collect();
        return view('crashes.errors', compact('logs'));
    }

    public function errorShow($id)
    {
        $this->adminOnly();
        if (!Schema::hasTable('error_logs')) {
            abort(404);
        }
        $log = ErrorLog::findOrFail($id);
        return view('crashes.error-show', compact('log'));
    }

    public function resolveError(Request $request, $id)
    {
        $this->adminOnly();
        if (!Schema::hasTable('error_logs')) {
            abort(404);
        }
        $log = ErrorLog::findOrFail($id);
        $log->resolution_notes = $request->get('resolution_notes');
        $log->resolved_at = now();
        $log->resolved_by = Auth::user()->name ?: 'Admin';
        $log->save();
        return redirect()->back()->with('success', 'Error marked resolved');
    }

    public function analytics()
    {
        $this->adminOnly();
        $data = AdminMetrics::crashAnalytics();
        return view('crashes.analytics', compact('data'));
    }
}
