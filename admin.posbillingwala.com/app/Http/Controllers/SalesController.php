<?php

namespace App\Http\Controllers;

use App\Models\Invoice;
use App\Models\InvoiceFinalProduct;
use App\Services\AdminMetrics;
use Illuminate\Http\Request;
use Auth;

class SalesController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
    }

    public function dashboard()
    {
        if (Auth::user()->role_id != 1) {
            abort(403);
        }
        $data = AdminMetrics::salesDashboard();
        return view('sales.dashboard', compact('data'));
    }

    public function overview(Request $request)
    {
        if (Auth::user()->role_id != 1) {
            abort(403);
        }
        $data = AdminMetrics::salesOverview($request->get('month'));
        return view('sales.overview', compact('data'));
    }

    public function invoices(Request $request)
    {
        if (Auth::user()->role_id != 1) {
            abort(403);
        }
        $q = trim((string) $request->get('q', ''));
        $filters = AdminMetrics::parseDashboardFilters($request->only(['dealer_id', 'customer_id', 'payment']));
        $selectedDate = trim((string) $request->get('date', ''));
        if ($selectedDate !== '') {
            $selectedDate = AdminMetrics::resolveDashboardDate($selectedDate)->toDateString();
        }

        $invoices = AdminMetrics::recentInvoices(100, $q, $selectedDate !== '' ? $selectedDate : null, $filters);
        $total = 0;
        foreach ($invoices as $inv) {
            $total += (float) $inv->totalAmount;
        }

        $summaryFrom = $selectedDate !== '' ? $selectedDate : AdminMetrics::today()->toDateString();
        $paymentSummary = $selectedDate !== ''
            ? AdminMetrics::paymentSummary($summaryFrom, $summaryFrom, $filters)
            : AdminMetrics::paymentSummaryFromInvoices($invoices);

        $dealers = \App\Models\User::where('role_id', 2)->where('is_active', 1)->orderBy('name')->get(['id', 'name']);
        $customersQuery = \App\Models\User::where('role_id', 3)->where('is_active', 1);
        if ($filters['dealer_id'] > 0) {
            $customersQuery->where('dealerId', $filters['dealer_id']);
        }
        $customers = $customersQuery->orderBy('name')->get(['id', 'name', 'shopName']);

        return view('sales.invoices', compact(
            'invoices',
            'q',
            'total',
            'filters',
            'selectedDate',
            'paymentSummary',
            'dealers',
            'customers'
        ));
    }

    public function invoiceDetails($id)
    {
        if (Auth::user()->role_id != 1) {
            abort(403);
        }
        $invoice = Invoice::query()
            ->from('invoice as i')
            ->join('licenses as l', 'l.id', '=', 'i.licenseId')
            ->join('users as u', 'u.id', '=', 'l.userId')
            ->where('i.invoiceId', $id)
            ->select('i.*', 'u.name as ownerName', 'u.shopName')
            ->first();
        if (!$invoice) {
            abort(404, 'Invoice not found');
        }
        $items = InvoiceFinalProduct::where('invoiceNumber', $invoice->invoiceNumber)->get();
        return view('sales.invoice-details', compact('invoice', 'items'));
    }
}
