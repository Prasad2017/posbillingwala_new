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
        $invoices = AdminMetrics::recentInvoices(100, $q);
        $total = 0;
        foreach ($invoices as $inv) {
            $total += (float) $inv->totalAmount;
        }
        return view('sales.invoices', compact('invoices', 'q', 'total'));
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
