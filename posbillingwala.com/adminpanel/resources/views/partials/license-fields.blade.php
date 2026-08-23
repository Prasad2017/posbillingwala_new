@php
    $lv = old('license_validity', isset($license) ? $license->licenseValidity : null);
    $lt = old('license_type', isset($license) ? $license->licenseType : 'regular');
    $ls = old('license_status', isset($license) ? $license->licenseStatus : 'active');
    $ps = old('payment_status', isset($license) ? $license->paymentStatus : 'cash');
    $amt = old('amount', isset($license) ? $license->amount : null);
    $fb = old('fast_billing', isset($license) ? $license->fastBilling : 1);
    $di = old('dine_in', isset($license) ? $license->dineIn : 0);
    $ta = old('take_away', isset($license) ? $license->takeAway : 1);
@endphp
<div class="col-lg-4">
    <label class="form-label">License Validity</label>
    <div class="input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-key'></i></span>
        <select class="form-control form-select" name="license_validity">
            <option value="7" @if($lv == 7) selected @endif>7 Days</option>
            <option value="183" @if($lv == 183) selected @endif>6 Months</option>
            <option value="365" @if($lv == 365) selected @endif>1 Year</option>
            <option value="1095" @if($lv == 1095) selected @endif>3 Years</option>
            <option value="1825" @if($lv == 1825) selected @endif>5 Years</option>
            <option value="10958" @if($lv == 10958) selected @endif>Lifetime</option>
        </select>
    </div>
</div>
<div class="col-lg-4">
    <label class="form-label">License Type</label>
    <div class="input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-key'></i></span>
        <select class="form-control form-select" name="license_type">
            <option value="demo" @if($lt == 'demo') selected @endif>Demo</option>
            <option value="regular" @if($lt == 'regular') selected @endif>Regular</option>
        </select>
    </div>
</div>
<div class="col-lg-4">
    <label class="form-label">License Status</label>
    <div class="input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-key'></i></span>
        <select class="form-control form-select" name="license_status">
            <option value="active" @if($ls == 'active') selected @endif>Active</option>
            <option value="expired" @if($ls == 'expired') selected @endif>Expired</option>
        </select>
    </div>
</div>
<div class="col-lg-4">
    <label class="form-label">Payment Status</label>
    <div class="input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-money'></i></span>
        <select class="form-control form-select" name="payment_status">
            <option value="cash" @if($ps == 'cash') selected @endif>Cash</option>
            <option value="online" @if($ps == 'online') selected @endif>Online</option>
        </select>
    </div>
</div>
<div class="col-lg-4">
    <label class="form-label">Amount</label>
    <div class="input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-rupee'></i></span>
        <input type="text" name="amount" placeholder="Amount" class="form-control border-start-0" value="{{ $amt }}">
    </div>
</div>
<div class="col-lg-4">
    <label class="form-label">Fast Billing</label>
    <div class="input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-receipt'></i></span>
        <select class="form-control form-select" name="fast_billing">
            <option value="1" @if($fb == 1) selected @endif>Yes</option>
            <option value="0" @if($fb == 0) selected @endif>No</option>
        </select>
    </div>
</div>
<div class="col-lg-4">
    <label class="form-label">Dine In (Table wise)</label>
    <div class="input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-restaurant'></i></span>
        <select class="form-control form-select" name="dine_in">
            <option value="1" @if($di == 1) selected @endif>Yes</option>
            <option value="0" @if($di == 0) selected @endif>No</option>
        </select>
    </div>
</div>
<div class="col-lg-4">
    <label class="form-label">Take Away</label>
    <div class="input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-package'></i></span>
        <select class="form-control form-select" name="take_away">
            <option value="1" @if($ta == 1) selected @endif>Yes</option>
            <option value="0" @if($ta == 0) selected @endif>No</option>
        </select>
    </div>
</div>
