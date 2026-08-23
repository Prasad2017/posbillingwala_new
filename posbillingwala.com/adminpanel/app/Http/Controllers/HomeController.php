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
            return view('home',compact('users'));
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
