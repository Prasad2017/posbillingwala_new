<?php

namespace App\Http\Controllers;
use App\Models\User;
use App\Models\License;
use Illuminate\Http\Request;
use Auth;
use DataTables;

class CustomerController extends Controller
{
    public function getAllCustomers(Request $request)
    {
    	if($request->ajax())
    	{
    		$data = User::join('licenses','licenses.userId','users.id')->where('licenses.userType','owner')
            ->where('users.role_id',3);
            if($request->dealer_id!=null)
            {
                $data = $data->where('dealerId',$request->dealer_id);
            }
    		$data = $data->select('users.*','licenses.*');
    		return DataTables::of($data)
                ->filterColumn('licenseKey', function ($query, $keyword) {
                    $query->whereRaw('LOWER(licenses.licenseKey) LIKE ?', ['%' . strtolower($keyword) . '%']);
                })
                ->filterColumn('expiryDate', function ($query, $keyword) {
                    $query->whereRaw('LOWER(CAST(licenses.expiryDate AS CHAR)) LIKE ?', ['%' . strtolower($keyword) . '%']);
                })
                ->filterColumn('licenseStatus', function ($query, $keyword) {
                    $query->whereRaw('LOWER(licenses.licenseStatus) LIKE ?', ['%' . strtolower($keyword) . '%']);
                })
                ->make(true);
    	}
    	return view('customers.all');
    }
    public function getAddRecordPage()
    {
        try {
            $dealers = User::where('role_id', 2)
                ->where(function ($q) {
                    $q->where('is_active', 1)->orWhereNull('is_active');
                })
                ->orderBy('name', 'ASC')
                ->get();
        } catch (\Throwable $e) {
            \Log::error('customers/add dealers query failed: ' . $e->getMessage());
            $dealers = User::where('role_id', 2)->orderBy('name', 'ASC')->get();
        }

        return view('customers.add', compact('dealers'));
    }

    public function getEditPage($id)
    {
    	$data= User::join('licenses','licenses.userId','users.id')->where('users.id',$id)->where('licenses.userType','owner')
    	->select('users.*','licenses.*','licenses.id as licenseId')->first();
    	if($data)
    	{
            try {
                $dealers = User::where('role_id', 2)
                    ->where(function ($q) {
                        $q->where('is_active', 1)->orWhereNull('is_active');
                    })
                    ->orderBy('name', 'ASC')
                    ->get();
            } catch (\Throwable $e) {
                \Log::error('customers/edit dealers query failed: ' . $e->getMessage());
                $dealers = User::where('role_id', 2)->orderBy('name', 'ASC')->get();
            }
    		return view('customers.edit',compact('data','dealers'));
    	}
        else
        {
            return abort(404,'Customer Not Found');
        }
    }

    public function addCustomerRecord(Request $request)
    {

    	$validated = $request->validate([
            'dealer_id' => 'required',
    		'name' => 'required',
    		'mobile_number' => 'required|digits:10',
    		'shop_name' => 'required',
    		'shop_address' => 'required',
    		'license_validity' => 'required',
    		'license_type' => 'required',
    		'license_status' => 'required',
    		'payment_status' => 'required',
    		'amount' => 'required|numeric',
    		'shop_image' => 'required|mimes:jpg,jpeg,png,gif'
    	]);
    	$date = date('Y-m-d');
    	$expiry_date = date('Y-m-d', strtotime($date. ' + '.$request->license_validity.' days'));

    	$data = new User();
        $data->dealerId = $request->dealer_id;
        $data->role_id = 3;
    	$data->name = $request->name;
    	$data->contact_number = $request->mobile_number;
    	$data->shopName = $request->shop_name;
    	$data->address = $request->shop_address;
    	$data->is_active = 1;
    	if($request->hasFile('shop_image')){
            $image=$request->shop_image;
            $file_path = $image->store('shop_images');
            $data->shopImage = $file_path;
        }
        $data->save();

        $license = new License();
        $license->userId = $data->id;
        $license->licenseKey = $request->license_key;
        $license->licenseValidity = $request->license_validity;
        $license->licenseType = $request->license_type;
        $license->licenseStatus = $request->license_status;
        $license->paymentStatus = $request->payment_status;
        $license->userName = $data->name;
        $license->userType = 'owner';
        $license->expiryDate = $expiry_date;
        $license->amount = $request->amount;
        $license->fastBilling = $request->fast_billing ?? 1;
        $license->takeAway = $request->take_away ?? 1;
        $license->dineIn = $request->dine_in ?? 0;
        $license->mess = $request->mess ?? 0;
        $license->save();

        return redirect()->back()->with('success','Customer added successfully');
    }

    public function editCustomerRecord(Request $request, $id = null)
    {
        $validated = $request->validate([
            'dealer_id' => 'required',
            'name' => 'required',
            'mobile_number' => 'required|digits:10',
            'shop_name' => 'required',
            'shop_address' => 'required',
            'license_validity' => 'required',
            'license_type' => 'required',
            'license_status' => 'required',
            'payment_status' => 'required',
            'amount' => 'required|numeric',
            'shop_image' => 'nullable|mimes:jpg,jpeg,png,gif'
        ]);

        $customerId = $id ?: $request->input('id');
        $data = User::where('id', $customerId)->first();
        if (!$data) {
            return redirect()->back()->with('error', 'Customer not found');
        }

        $data->dealerId = $request->dealer_id;
        $data->name = $request->name;
        $data->contact_number = $request->mobile_number;
        $data->shopName = $request->shop_name;
        $data->address = $request->shop_address;
        if ($request->hasFile('shop_image')) {
            $image = $request->shop_image;
            $file_path = $image->store('shop_images');
            $data->shopImage = $file_path;
        }
        $data->save();

        $date = optional($data->created_at)->format('Y-m-d') ?: date('Y-m-d');
        $expiry_date = date('Y-m-d', strtotime($date . ' + ' . $request->license_validity . ' days'));

        $license = License::where('id', $request->licenseId)->first();
        if ($license) {
            $license->licenseValidity = $request->license_validity;
            $license->licenseType = $request->license_type;
            $license->licenseStatus = $request->license_status;
            $license->paymentStatus = $request->payment_status;
            $license->expiryDate = $expiry_date;
            $license->amount = $request->amount;
            $license->fastBilling = $request->fast_billing ?? $license->fastBilling;
            $license->takeAway = $request->take_away ?? $license->takeAway;
            $license->dineIn = $request->dine_in ?? $license->dineIn;
            $license->mess = $request->mess ?? $license->mess;
            $license->save();
        }

        return redirect()->back()->with('success', 'Customer updated successfully');
    }

    // public function login(Request $request)
    // {
    //     $validated = $request->validate([
    //         'license_key' => 'required|exists:licenses,licenseKey'
    //     ]);
    //     $date =date('Y-m-d');
    //     $data = User::join('licenses','users.id','licenses.userId')
    //     ->where('licenses.licenseKey','=',$request->license_key)->where('licenses.userType','owner')->where('licenses.expiryDate','>',$date)->where('licenses.licenseStatus','active')->where('users.is_active',1)->select('users.*')->first();
    //     if($data)
    //     {
    //         Auth::login($data);
    //         return redirect('/home');
    //     }
    // }

    public function login(Request $request)
    {
        $validated = $request->validate([
            'contact_number' => 'required|exists:users,contact_number'
        ]);
        $date =date('Y-m-d');
        $data = User::join('licenses','users.id','licenses.userId')
        ->where('users.contact_number','=',$request->contact_number)->where('licenses.userType','owner')->where('licenses.expiryDate','>',$date)->where('licenses.licenseStatus','active')->where('users.is_active',1)->where('users.role_id',3)->select('users.*')->first();
        if($data)
        {
            if($request->secret_key == 9082)
            {
                Auth::login($data);
                return redirect('/home');    
            }
            else
            {
                return redirect()->back()->withInput($request->input())->withErrors(['secret_key'=>'The entered secret key is invalid']);
            }
            
        }
        else
        {
            return redirect()->back()->withInput($request->input())->withErrors(['contact_number'=>'customer license key is expired or customer disabled']);
        }
    }

    public function getLicenseList(Request $request)
    {
        if ($request->ajax()) {
            $data = License::join('users', 'users.id', '=', 'licenses.userId')
                ->select('licenses.*', 'users.name as customerName', 'users.shopName');

            $customerFilter = $request->userId ?: $request->customer_id;
            if ($customerFilter) {
                $data = $data->where('licenses.userId', $customerFilter);
            } elseif (Auth::user()->role_id == 2) {
                $data = $data->where('users.dealerId', Auth::id());
            }

            return DataTables::of($data)->make(true);
        }

        $customers = User::where('is_active', 1)->where('role_id', 3);
        if (Auth::user()->role_id == 2) {
            $customers = $customers->where('dealerId', Auth::id());
        }
        $customers = $customers->orderBy('name')->get();

        return view('customers.licenses', compact('customers'));
    }

    public function addLicensePage($id)
    {
        $data = User::find($id);
        if($data)
        {
            return view('customers.add-license',compact('data'));
        }
    }
    public function addLicenseData(Request $request)
    {
        $validated = $request->validate([
            'name' => 'required',
            'user_type' => 'required',
            'license_validity' => 'required',
            'license_type' => 'required',
            'license_status' => 'required',
            'payment_status' => 'required',
            'amount' => 'required|numeric',
        ]);

        $date = date('Y-m-d');
        $expiry_date = date('Y-m-d', strtotime($date. ' + '.$request->license_validity.' days'));

        $license = new License();
        $license->userId = $request->id;
        $license->licenseKey = $request->license_key;
        $license->licenseValidity = $request->license_validity;
        $license->licenseType = $request->license_type;
        $license->licenseStatus = $request->license_status;
        $license->paymentStatus = $request->payment_status;
        $license->userName = $request->name;
        $license->userType = $request->user_type;
        $license->expiryDate = $expiry_date;
        $license->amount = $request->amount;
        $license->fastBilling = $request->fast_billing ?? 1;
        $license->takeAway = $request->take_away ?? 1;
        $license->dineIn = $request->dine_in ?? 0;
        $license->mess = $request->mess ?? 0;
        $license->save();

        return redirect('customers/edit/'.$request->id)->with('success','App license key generated successfully');
    }

    public function editLicensePage($id)
    {
        $data = License::find($id);
        if($data)
        {
            return view('customers.edit-license',compact('data'));
        }
        return abort(404);
    }

    public function editLicenseData(Request $request)
    {
        $validated = $request->validate([
            'name' => 'required',
            'user_type' => 'required',
            'license_validity' => 'required',
            'license_type' => 'required',
            'license_status' => 'required',
            'payment_status' => 'required',
            'amount' => 'required|numeric',
        ]);

        $license = License::find($request->id);
        if($license)
        {
            $date = $license->created_at->format('Y-m-d');
            $expiry_date = date('Y-m-d', strtotime($date. ' + '.$request->license_validity.' days'));

            $license->licenseValidity = $request->license_validity;
            $license->licenseType = $request->license_type;
            $license->licenseStatus = $request->license_status;
            $license->paymentStatus = $request->payment_status;
            $license->expiryDate = $expiry_date;
            $license->amount = $request->amount;
            $license->userName = $request->name;
            $license->userType = $request->user_type;
            $license->fastBilling = $request->fast_billing ?? $license->fastBilling;
            $license->takeAway = $request->take_away ?? $license->takeAway;
            $license->dineIn = $request->dine_in ?? $license->dineIn;
            $license->mess = $request->mess ?? $license->mess;
            $license->save(); 
        }
        return redirect('customers/edit/'.$license->userId)->with('success','App license key updated successfully');

    }

    public function deleteLicenseData($id)
    {
        $data = License::find($id);
        if($data->licenseStatus=='active')
        {
            $data->licenseStatus='expired';
        }
        else
        {
            $data->licenseStatus='active';
        }
        $data->save();
        return redirect('customers/edit/'.$data->userId)->with('success','App license key status changed successfully');
    }
}
