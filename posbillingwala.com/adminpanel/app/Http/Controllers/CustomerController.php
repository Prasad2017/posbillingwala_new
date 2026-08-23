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
    		return DataTables::of($data)->make(true);
    	}
    	return view('customers.all');
    }
    public function getAddRecordPage()
    {
        $dealers = User::where('role_id',2)->where('is_active',1)->orderBy('name','ASC')->get();
    	return view('customers.add',compact('dealers'));
    }

    public function getEditPage($id)
    {
    	$data= User::join('licenses','licenses.userId','users.id')->where('users.id',$id)->where('licenses.userType','owner')
    	->select('users.*','licenses.*','licenses.id as licenseId')->first();
    	if($data)
    	{
            $dealers = User::where('role_id',2)->where('is_active',1)->orderBy('name','ASC')->get();
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
        $license->save();

        return redirect()->back()->with('success','Customer added successfully');
    }

    public function editCustomerRecord(Request $request)
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

        $date = date('Y-m-d');
        $expiry_date = date('Y-m-d', strtotime($date. ' + '.$request->license_validity.' days'));

        $data = User::where('id',$request->id)->first();
        if($data)
        {
            $data->dealerId = $request->dealer_id;
            $data->name = $request->name;
            $data->contact_number = $request->mobile_number;
            $data->shopName = $request->shop_name;
            $data->address = $request->shop_address;
            if($request->hasFile('shop_image')){
                $image=$request->shop_image;
                $file_path = $image->store('shop_images');
                $data->shopImage = $file_path;
            }
            $data->save();    
        }

        $date = $data->created_at->format('Y-m-d');
        $expiry_date = date('Y-m-d', strtotime($date. ' + '.$request->license_validity.' days'));

        $license = License::where('id',$request->licenseId)->first();
        if($license)
        {
            $license->licenseValidity = $request->license_validity;
            $license->licenseType = $request->license_type;
            $license->licenseStatus = $request->license_status;
            $license->paymentStatus = $request->payment_status;
            $license->expiryDate = $expiry_date;
            $license->amount = $request->amount;
            $license->save();
        }
        

        return redirect()->back()->with('success','Customer updated successfully');
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
        $data = License::where('userId',$request->userId);
        return DataTables::of($data)->make(true);

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
        $license->branchName = $request->branch_name;
        $license->expiryDate = $expiry_date;
        $license->amount = $request->amount;
        $license->fastBilling = $request->fast_billing;
        $license->takeAway = $request->take_away;
        $license->dineIn = $request->dine_in;
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
            $license->fastBilling = $request->fast_billing;
            $license->takeAway = $request->take_away;
            $license->dineIn = $request->dine_in;
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
