<?php

namespace App\Http\Controllers;
use App\Models\User;
use App\Models\License;
use App\Support\BusinessTypes;
use App\Support\LicenceDefaults;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;
use Auth;
use DataTables;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

class CustomerController extends Controller
{
    private function isDealer(): bool
    {
        return (int) Auth::user()->role_id === 2;
    }

    private function assertCustomerAccess(?User $customer): void
    {
        if (!$customer || (int) $customer->role_id !== 3) {
            abort(404, 'Customer Not Found');
        }
        if ($this->isDealer() && (int) $customer->dealerId !== (int) Auth::id()) {
            abort(403, 'You do not have access to this customer.');
        }
    }

    private function resolveExpiryDate(int $validityDays, ?string $currentExpiry = null): string
    {
        // Always compute from today so renewals/edits set a predictable window.
        return date('Y-m-d', strtotime(date('Y-m-d') . ' + ' . max(1, $validityDays) . ' days'));
    }

    public function getAllCustomers(Request $request)
    {
    	if($request->ajax())
    	{
            $hasBusinessType = Schema::hasColumn('licenses', 'businessType');
            $btSelect = $hasBusinessType ? 'licenses.businessType' : "'' as businessType";

    		$data = User::join('licenses','licenses.userId','users.id')->where('licenses.userType','owner')
            ->where('users.role_id',3)
            ->select(
                'users.id as userId',
                'users.name',
                'users.contact_number',
                'users.shopName',
                'users.dealerId',
                'users.is_active',
                'licenses.id as licenseId',
                'licenses.licenseKey',
                'licenses.mpin',
                'licenses.expiryDate',
                'licenses.licenseStatus',
                'licenses.licenseType',
                DB::raw($btSelect)
            );

            if ($this->isDealer()) {
                $data = $data->where('users.dealerId', Auth::id());
            } elseif ($request->dealer_id != null && $request->dealer_id !== '') {
                $data = $data->where('users.dealerId', $request->dealer_id);
            }

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
    	->select('users.*','licenses.*','licenses.id as licenseId','users.id as userId')->first();
    	if($data)
    	{
            $this->assertCustomerAccess(User::find($id));
            $data->mpin = $data->mpin ?: LicenceDefaults::defaultMpin();
            $data->reportPin = $data->reportPin ?? LicenceDefaults::defaultReportPin();
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
    		$storeOps = $this->storeOpsForLicense($data->licenseId ?? $data->id);
    		return view('customers.edit',compact('data','dealers','storeOps'));
    	}
        else
        {
            return abort(404,'Customer Not Found');
        }
    }

    public function addCustomerRecord(Request $request)
    {
        if ($this->isDealer()) {
            $request->merge(['dealer_id' => Auth::id()]);
        }

    	$validated = $request->validate([
            'dealer_id' => 'required',
    		'name' => 'required',
    		'mobile_number' => [
                'required',
                'digits:10',
                Rule::unique('users', 'contact_number')->where(function ($query) {
                    return $query->where('role_id', 3);
                }),
            ],
    		'shop_name' => 'required',
    		'shop_address' => 'required',
    		'license_validity' => 'required',
    		'license_type' => 'required',
    		'license_status' => 'required',
    		'payment_status' => 'required',
    		'amount' => 'required|numeric',
    		'shop_image' => 'required|mimes:jpg,jpeg,png,gif',
            'business_type' => ['nullable', Rule::in(array_merge([''], BusinessTypes::ids()))],
    	]);
    	$expiry_date = $this->resolveExpiryDate((int) $request->license_validity);

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
        $license->licenseKey = LicenceDefaults::generateUniqueKey();
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
        $license->userManagementEnabled = (int) ($request->user_management_enabled ?? 0);
        $license->maxUsers = max(1, (int) ($request->max_users ?? 10));
        $license->maxDevices = max(1, (int) ($request->max_devices ?? 5));
        $license->maxPrinters = max(0, (int) ($request->max_printers ?? 0));
        $license->mpin = LicenceDefaults::defaultMpin();
        $this->applyBusinessType($license, $request);
        $license->save();

        return redirect('customers/edit/' . $data->id)->with([
            'success' => 'Customer registered successfully. Share the credentials below with the customer.',
            'registration_credentials' => [
                'licenseKey' => $license->licenseKey,
                'mpin' => LicenceDefaults::defaultMpin(),
                'reportPin' => LicenceDefaults::defaultReportPin(),
            ],
        ]);
    }

    public function editCustomerRecord(Request $request, $id = null)
    {
        $customerId = $id ?: $request->input('id');
        if ($this->isDealer()) {
            $request->merge(['dealer_id' => Auth::id()]);
        }

        $validated = $request->validate([
            'dealer_id' => 'required',
            'name' => 'required',
            'mobile_number' => [
                'required',
                'digits:10',
                Rule::unique('users', 'contact_number')->ignore($customerId)->where(function ($query) {
                    return $query->where('role_id', 3);
                }),
            ],
            'shop_name' => 'required',
            'shop_address' => 'required',
            'license_validity' => 'required',
            'license_type' => 'required',
            'license_status' => 'required',
            'payment_status' => 'required',
            'amount' => 'required|numeric',
            'shop_image' => 'nullable|mimes:jpg,jpeg,png,gif',
            'business_type' => ['nullable', Rule::in(array_merge([''], BusinessTypes::ids()))],
        ]);

        $data = User::where('id', $customerId)->first();
        $this->assertCustomerAccess($data);

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

        $license = License::where('id', $request->licenseId)->where('userId', $customerId)->first();
        if ($license) {
            $license->licenseValidity = $request->license_validity;
            $license->licenseType = $request->license_type;
            $license->licenseStatus = $request->license_status;
            $license->paymentStatus = $request->payment_status;
            $license->expiryDate = $this->resolveExpiryDate((int) $request->license_validity, (string) $license->expiryDate);
            $license->amount = $request->amount;
            $license->fastBilling = $request->fast_billing ?? $license->fastBilling;
            $license->takeAway = $request->take_away ?? $license->takeAway;
            $license->dineIn = $request->dine_in ?? $license->dineIn;
            $license->mess = $request->mess ?? $license->mess;
            $license->userManagementEnabled = (int) ($request->user_management_enabled ?? $license->userManagementEnabled ?? 0);
            $license->maxUsers = max(1, (int) ($request->max_users ?? $license->maxUsers ?? 10));
            $license->maxDevices = max(1, (int) ($request->max_devices ?? $license->maxDevices ?? 5));
            $license->maxPrinters = max(0, (int) ($request->max_printers ?? $license->maxPrinters ?? 0));
            $this->applyBusinessType($license, $request);
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
                ->where('users.role_id', 3)
                ->select(
                    'licenses.*',
                    'users.name as customerName',
                    'users.shopName',
                    'licenses.userName as branchName'
                );

            if ($this->isDealer()) {
                $data = $data->where('users.dealerId', Auth::id());
            }

            $customerFilter = $request->userId ?: $request->customer_id;
            if ($customerFilter) {
                $data = $data->where('licenses.userId', $customerFilter);
            }

            return DataTables::of($data)->make(true);
        }

        $customers = User::where('is_active', 1)->where('role_id', 3);
        if ($this->isDealer()) {
            $customers = $customers->where('dealerId', Auth::id());
        }
        $customers = $customers->orderBy('name')->get();

        return view('customers.licenses', compact('customers'));
    }

    public function addLicensePage($id)
    {
        $data = User::find($id);
        $this->assertCustomerAccess($data);
        return view('customers.add-license', compact('data'));
    }

    public function addLicenseData(Request $request)
    {
        $customer = User::find($request->id);
        $this->assertCustomerAccess($customer);

        $validated = $request->validate([
            'name' => 'required',
            'user_type' => 'required',
            'license_validity' => 'required',
            'license_type' => 'required',
            'license_status' => 'required',
            'payment_status' => 'required',
            'amount' => 'required|numeric',
            'business_type' => ['nullable', Rule::in(array_merge([''], BusinessTypes::ids()))],
        ]);

        $expiry_date = $this->resolveExpiryDate((int) $request->license_validity);

        $license = new License();
        $license->userId = $request->id;
        $license->licenseKey = LicenceDefaults::generateUniqueKey();
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
        $license->userManagementEnabled = (int) ($request->user_management_enabled ?? 0);
        $license->maxUsers = max(1, (int) ($request->max_users ?? 10));
        $license->maxDevices = max(1, (int) ($request->max_devices ?? 5));
        $license->maxPrinters = max(0, (int) ($request->max_printers ?? 0));
        $license->mpin = LicenceDefaults::defaultMpin();
        $this->applyBusinessType($license, $request);
        $license->save();

        return redirect('customers/edit/'.$request->id)->with([
            'success' => 'App license key generated successfully.',
            'registration_credentials' => [
                'licenseKey' => $license->licenseKey,
                'mpin' => LicenceDefaults::defaultMpin(),
                'reportPin' => LicenceDefaults::defaultReportPin(),
            ],
        ]);
    }

    public function editLicensePage($id)
    {
        $data = License::find($id);
        if (!$data) {
            return abort(404);
        }
        $this->assertCustomerAccess(User::find($data->userId));
        $storeOps = $this->storeOpsForLicense($data->id);
        return view('customers.edit-license', compact('data', 'storeOps'));
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
            'business_type' => ['nullable', Rule::in(array_merge([''], BusinessTypes::ids()))],
        ]);

        $license = License::find($request->id);
        if (!$license) {
            return redirect('customers/all-license')->with('error', 'License not found');
        }
        $this->assertCustomerAccess(User::find($license->userId));

        $license->licenseValidity = $request->license_validity;
        $license->licenseType = $request->license_type;
        $license->licenseStatus = $request->license_status;
        $license->paymentStatus = $request->payment_status;
        $license->expiryDate = $this->resolveExpiryDate((int) $request->license_validity, (string) $license->expiryDate);
        $license->amount = $request->amount;
        $license->userName = $request->name;
        $license->userType = $request->user_type;
        $license->fastBilling = $request->fast_billing ?? $license->fastBilling;
        $license->takeAway = $request->take_away ?? $license->takeAway;
        $license->dineIn = $request->dine_in ?? $license->dineIn;
        $license->mess = $request->mess ?? $license->mess;
        $license->userManagementEnabled = (int) ($request->user_management_enabled ?? $license->userManagementEnabled ?? 0);
        $license->maxUsers = max(1, (int) ($request->max_users ?? $license->maxUsers ?? 10));
        $license->maxDevices = max(1, (int) ($request->max_devices ?? $license->maxDevices ?? 5));
        $license->maxPrinters = max(0, (int) ($request->max_printers ?? $license->maxPrinters ?? 0));
        $this->applyBusinessType($license, $request);
        $license->save();

        return redirect('customers/edit/'.$license->userId)->with('success','App license key updated successfully');
    }

    public function deleteLicenseData($id)
    {
        $data = License::find($id);
        if (!$data) {
            return redirect('customers/all-license')->with('error', 'License not found');
        }
        $this->assertCustomerAccess(User::find($data->userId));
        if (strtolower((string) $data->licenseStatus) === 'active') {
            $data->licenseStatus = 'expired';
        } else {
            $data->licenseStatus = 'active';
        }
        $data->save();
        return redirect('customers/edit/'.$data->userId)->with('success','App license key status changed successfully');
    }

    private function applyBusinessType(License $license, Request $request): void
    {
        if (!Schema::hasColumn('licenses', 'businessType')) {
            return;
        }
        $license->businessType = BusinessTypes::normalize($request->input('business_type'));
    }

    private function storeOpsForLicense($licenseId)
    {
        $empty = array(
            'staff' => collect(),
            'devices' => collect(),
            'printers' => collect(),
            'routes' => collect(),
            'billPaper' => null,
            'kotPaper' => null,
        );
        if (empty($licenseId)) {
            return $empty;
        }
        try {
            $billPaper = null;
            $kotPaper = null;
            if (Schema::hasTable('company_printer_setting')) {
                $row = DB::table('company_printer_setting')->where('licenseId', $licenseId)->first();
                if ($row) {
                    $billPaper = $row->paperSize ?? null;
                    $kotPaper = $row->kotPaperSize ?? $billPaper;
                }
            }
            return array(
                'staff' => DB::table('pos_staff')->where('licenseId', $licenseId)
                    ->orderBy('name')->get(['id', 'name', 'mobileNumber', 'role', 'status', 'lastLoginAt']),
                'devices' => DB::table('pos_devices')->where('licenseId', $licenseId)
                    ->orderByDesc('lastSeenAt')->get(['deviceId', 'deviceName', 'platform', 'status', 'isPrintHost', 'lastSeenAt']),
                'printers' => DB::table('store_printers')->where('licenseId', $licenseId)
                    ->orderBy('printerName')->get(
                        Schema::hasColumn('store_printers', 'paperSize')
                            ? ['id', 'printerName', 'connectionType', 'paperSize', 'purpose', 'enabled', 'status']
                            : ['id', 'printerName', 'connectionType', 'purpose', 'enabled', 'status']
                    ),
                'routes' => DB::table('printer_routes')->where('licenseId', $licenseId)
                    ->orderBy('id')->get(['printerId', 'documentType', 'foodTypeCode', 'categoryId']),
                'billPaper' => $billPaper,
                'kotPaper' => $kotPaper,
            );
        } catch (\Throwable $e) {
            return $empty;
        }
    }
}
