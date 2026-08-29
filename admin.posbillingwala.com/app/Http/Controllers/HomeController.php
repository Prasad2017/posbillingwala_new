<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Auth;
use App\Models\User;
use App\Models\Category;
use App\Models\Company;
use App\Models\Product;
use App\Models\Invoice;
use App\Models\License;
use App\Models\Expense;
use App\Services\AdminMetrics;
use DataTables;

class HomeController extends Controller
{
    /**
     * Create a new controller instance.
     *
     * @return void
     */
    public function __construct()
    {
        $this->middleware('auth');
    }

    /**
     * Show the application dashboard.
     *
     * @return \Illuminate\Contracts\Support\Renderable
     */
    public function index(Request $request)
    {
        $role = Auth::user()->role_id;
        if($role == 1)
        {
            $users = User::where('role_id','!=',1)->get();
            if($request->ajax())
            {
                $licenses = License::join('users','users.id','licenses.userId')->orderBy('licenses.id','DESC')->limit(10)->get();
                return DataTables::of($licenses)->make(true);
            }
            try {
                $kpis = AdminMetrics::dashboard();
                $selectedDate = AdminMetrics::resolveDashboardDate($request->query('date'));
                $filters = AdminMetrics::parseDashboardFilters($request->only(['dealer_id', 'customer_id', 'payment']));
                $dealerSales = AdminMetrics::dealerSales(8, 'month');
                $recentCustomers = AdminMetrics::recentCustomers(8, (int) ($filters['dealer_id'] ?? 0));

                $dateStr = $selectedDate->toDateString();
                $prevDateStr = $selectedDate->copy()->subDay()->toDateString();

                $todayRow = AdminMetrics::salesRow($dateStr, $dateStr, $filters);
                $yesterdayRow = AdminMetrics::salesRow($prevDateStr, $prevDateStr, $filters);
                $itemsToday = AdminMetrics::itemsSoldCount($dateStr, $dateStr, $filters);
                $itemsYesterday = AdminMetrics::itemsSoldCount($prevDateStr, $prevDateStr, $filters);

                $dealers = User::where('role_id', 2)->where('is_active', 1)->orderBy('name')->get(['id', 'name']);
                $customersQuery = User::where('role_id', 3)->where('is_active', 1);
                if ($filters['dealer_id'] > 0) {
                    $customersQuery->where('dealerId', $filters['dealer_id']);
                }
                $customers = $customersQuery->orderBy('name')->get(['id', 'name', 'shopName']);

                $periodLabel = $selectedDate->isToday()
                    ? 'Today, ' . $selectedDate->format('d M Y')
                    : $selectedDate->format('l, d M Y');

                $dashboard = [
                    'selectedDate' => $dateStr,
                    'filters' => $filters,
                    'periodLabel' => $periodLabel,
                    'chartPeriodLabel' => $selectedDate->isToday() ? 'Today' : $selectedDate->format('d M Y'),
                    'totalSales' => $todayRow['total'],
                    'totalBills' => $todayRow['bills'],
                    'totalCustomers' => $kpis['totalCustomer'],
                    'totalDealers' => $kpis['totalDealer'],
                    'itemsSold' => $itemsToday,
                    'totalSalesTrend' => AdminMetrics::trendLabel(AdminMetrics::pctChange($todayRow['total'], $yesterdayRow['total']), true) . ' vs previous day',
                    'totalBillsTrend' => AdminMetrics::trendLabel(AdminMetrics::pctChange($todayRow['bills'], $yesterdayRow['bills']), true) . ' vs previous day',
                    'totalCustomersTrend' => $kpis['totalCustomerTrendLabel'],
                    'itemsSoldTrend' => AdminMetrics::trendLabel(AdminMetrics::pctChange($itemsToday, $itemsYesterday), true) . ' vs previous day',
                    'hourlySales' => AdminMetrics::salesByHour($dateStr, $filters),
                    'topCategories' => AdminMetrics::topSellingCategories($dateStr, $dateStr, 5, $filters),
                    'paymentSummary' => AdminMetrics::paymentSummary($dateStr, $dateStr, $filters),
                    'recentInvoices' => AdminMetrics::recentInvoices(10, '', $dateStr, $filters),
                ];
            } catch (\Throwable $e) {
                \Log::error('Admin dashboard failed: ' . $e->getMessage(), [
                    'trace' => $e->getTraceAsString(),
                ]);
                $kpis = [
                    'greeting' => AdminMetrics::greeting(),
                    'todayLabel' => date('d M Y'),
                    'totalCustomer' => User::where('role_id', 3)->count(),
                    'totalDealer' => User::where('role_id', 2)->count(),
                    'activeCustomer' => 0,
                    'trialCustomer' => 0,
                    'expiredCustomer' => 0,
                    'activeLicenses' => 0,
                    'expiringLicenses' => 0,
                    'expiredLicenses' => 0,
                    'totalBranches' => 0,
                    'totalDevices' => 0,
                    'trialLicenses' => 0,
                    'expiringLicenses7Days' => 0,
                    'trialLicensesExpiringTomorrow' => 0,
                    'customersAddedThisMonth' => 0,
                    'netSales' => 0,
                    'monthSales' => 0,
                    'lastMonthSales' => 0,
                    'allTimeSales' => 0,
                    'todaySales' => 0,
                    'billCount' => 0,
                    'salesSparkline' => array_fill(0, 7, 0),
                    'totalCustomerTrendLabel' => '↑ 0.0%',
                    'activeCustomerTrendLabel' => '↑ 0.0%',
                    'trialCustomerTrendLabel' => '↑ 0.0%',
                    'expiredCustomerTrendLabel' => '↑ 0.0%',
                    'activeLicensesTrendLabel' => '↑ 0.0%',
                    'expiringLicensesTrendLabel' => '↑ 0.0%',
                    'trialLicensesTrendLabel' => '↑ 0.0%',
                    'expiredLicensesTrendLabel' => '↑ 0.0%',
                    'notificationCount' => 0,
                ];
                $dealerSales = ['dealers' => [], 'totalSales' => 0];
                $recentCustomers = [];
                $dashboard = [
                    'selectedDate' => date('Y-m-d'),
                    'filters' => AdminMetrics::parseDashboardFilters([]),
                    'periodLabel' => 'Today, ' . date('d M Y'),
                    'chartPeriodLabel' => 'Today',
                    'totalDealers' => 0,
                    'totalSales' => 0,
                    'totalBills' => 0,
                    'totalCustomers' => 0,
                    'itemsSold' => 0,
                    'totalSalesTrend' => '↑ 0.0% vs yesterday',
                    'totalBillsTrend' => '↑ 0.0% vs yesterday',
                    'totalCustomersTrend' => '↑ 0.0%',
                    'itemsSoldTrend' => '↑ 0.0% vs yesterday',
                    'hourlySales' => [],
                    'topCategories' => [],
                    'paymentSummary' => ['items' => [], 'grandTotal' => 0],
                    'recentInvoices' => [],
                ];
            }
            $dealers = User::where('role_id', 2)->where('is_active', 1)->orderBy('name')->get(['id', 'name']);
            $customers = User::where('role_id', 3)->where('is_active', 1)->orderBy('name')->get(['id', 'name', 'shopName']);
            $filters = $dashboard['filters'] ?? AdminMetrics::parseDashboardFilters([]);
            return view('home', compact('users', 'kpis', 'dealerSales', 'recentCustomers', 'dashboard', 'dealers', 'customers', 'filters'));
        }
        else if($role == 2){
            if ($request->ajax()) {
                $licenses = License::join('users','users.id','licenses.userId')
                    ->where('users.dealerId', Auth::id())
                    ->orderBy('licenses.id','DESC')
                    ->limit(10)
                    ->get();
                return DataTables::of($licenses)->make(true);
            }

            $isDealerDashboard = true;
            $filters = AdminMetrics::parseDashboardFilters($request->only(['customer_id', 'payment']));
            $filters['dealer_id'] = (int) Auth::id();
            if (!empty($filters['customer_id'])) {
                $ownsCustomer = User::where('id', $filters['customer_id'])
                    ->where('role_id', 3)
                    ->where('dealerId', Auth::id())
                    ->exists();
                if (!$ownsCustomer) {
                    $filters['customer_id'] = 0;
                }
            }

            $selectedDate = AdminMetrics::resolveDashboardDate($request->query('date'));
            $dateStr = $selectedDate->toDateString();
            $prevDateStr = $selectedDate->copy()->subDay()->toDateString();
            $todayRow = AdminMetrics::salesRow($dateStr, $dateStr, $filters);
            $yesterdayRow = AdminMetrics::salesRow($prevDateStr, $prevDateStr, $filters);
            $itemsToday = AdminMetrics::itemsSoldCount($dateStr, $dateStr, $filters);
            $itemsYesterday = AdminMetrics::itemsSoldCount($prevDateStr, $prevDateStr, $filters);

            $customers = User::where('role_id', 3)
                ->where('is_active', 1)
                ->where('dealerId', Auth::id())
                ->orderBy('name')
                ->get(['id', 'name', 'shopName']);
            $dealers = collect();
            $dealerSales = ['dealers' => [], 'totalSales' => 0];
            $recentCustomers = AdminMetrics::recentCustomers(8, (int) Auth::id());
            $totalCustomers = User::where('role_id', 3)->where('dealerId', Auth::id())->count();

            $periodLabel = $selectedDate->isToday()
                ? 'Today, ' . $selectedDate->format('d M Y')
                : $selectedDate->format('l, d M Y');

            $dashboard = [
                'selectedDate' => $dateStr,
                'filters' => $filters,
                'periodLabel' => $periodLabel,
                'chartPeriodLabel' => $selectedDate->isToday() ? 'Today' : $selectedDate->format('d M Y'),
                'totalSales' => $todayRow['total'],
                'totalBills' => $todayRow['bills'],
                'totalCustomers' => $totalCustomers,
                'totalDealers' => 0,
                'itemsSold' => $itemsToday,
                'totalSalesTrend' => AdminMetrics::trendLabel(AdminMetrics::pctChange($todayRow['total'], $yesterdayRow['total']), true) . ' vs previous day',
                'totalBillsTrend' => AdminMetrics::trendLabel(AdminMetrics::pctChange($todayRow['bills'], $yesterdayRow['bills']), true) . ' vs previous day',
                'totalCustomersTrend' => 'Your network',
                'itemsSoldTrend' => AdminMetrics::trendLabel(AdminMetrics::pctChange($itemsToday, $itemsYesterday), true) . ' vs previous day',
                'hourlySales' => AdminMetrics::salesByHour($dateStr, $filters),
                'topCategories' => AdminMetrics::topSellingCategories($dateStr, $dateStr, 5, $filters),
                'paymentSummary' => AdminMetrics::paymentSummary($dateStr, $dateStr, $filters),
                'recentInvoices' => AdminMetrics::recentInvoices(10, '', $dateStr, $filters),
            ];
            $kpis = [];
            $users = collect();
            return view('home', compact('users', 'kpis', 'dealerSales', 'recentCustomers', 'dashboard', 'dealers', 'customers', 'filters', 'isDealerDashboard'));
        }
        else
        {

            if($request->ajax())
            {
                $data = Company::join('licenses','companys.licenseId','licenses.id')
                ->join('users','users.id','licenses.userId')
                ->where('users.id',Auth::user()->id)
                ->select('companys.companyName','companys.currencyName','companys.companyAddress','licenses.licenseKey','licenses.id as licenseId')->get();
                if($request->total_sale==1)
                {
                    foreach ($data as $key => $value) {
                        $value['sr'] = ++$key;
                        $value['sale'] = 0;
                        $invoice = Invoice::where('licenseId',$value->licenseId)->sum('totalAmount');
                        $value['sale'] = $invoice;
                    }
                }
                else
                {
                    foreach ($data as $key => $value) {
                        $value['sr'] = ++$key;
                        $value['sale'] = 0;
                        $invoice = Invoice::where('licenseId',$value->licenseId)->whereDate('invoiceDate',date('Y-m-d'))->sum('totalAmount');
                        $value['sale'] = $invoice;
                    }
                }
                
                return DataTables::of($data)->addColumn('sale', function($value){
                    if($value->sale!=0)
                    {
                        return "₹".$value->sale;                        
                    }
                    else
                        return $value->sale;
                })
                ->rawColumns(['sale'])
                ->make(true);
            }
            $categories = Category::where('userId',Auth::user()->id)->get();
            $products = Product::where('userId',Auth::user()->id)->get();
            $expenses = Expense::where('userId',Auth::user()->id)->get();
            return view('customer-home',compact('categories','products','expenses'));
        }
    }
}
