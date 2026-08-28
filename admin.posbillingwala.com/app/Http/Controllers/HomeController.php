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
                $dealerSales = AdminMetrics::dealerSales(5, 'month');
                $recentCustomers = AdminMetrics::recentCustomers(5);
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
            }
            return view('home', compact('users', 'kpis', 'dealerSales', 'recentCustomers'));
        }
        else if($role == 2){
            $customers = User::where('role_id', 3);
            if ($request->ajax()) {
                $licenses = License::join('users','users.id','licenses.userId')
                    ->where('users.dealerId', Auth::id())
                    ->orderBy('licenses.id','DESC')
                    ->limit(10)
                    ->get();
                return DataTables::of($licenses)->make(true);
            }
            $customers = $customers->where('dealerId', Auth::id())->get();
            return view('dealer-home', compact('customers'));
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
