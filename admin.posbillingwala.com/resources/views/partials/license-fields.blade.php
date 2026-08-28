@php
    $lv = old('license_validity', isset($license) ? $license->licenseValidity : null);
    $lt = old('license_type', isset($license) ? strtolower((string) $license->licenseType) : 'regular');
    $ls = old('license_status', isset($license) ? $license->licenseStatus : 'active');
    $ps = old('payment_status', isset($license) ? $license->paymentStatus : 'cash');
    $amt = old('amount', isset($license) ? $license->amount : null);
    $fb = old('fast_billing', isset($license) ? $license->fastBilling : 1);
    $di = old('dine_in', isset($license) ? $license->dineIn : 0);
    $ta = old('take_away', isset($license) ? $license->takeAway : 1);

    $regularTiers = ['183', '365', '1095', '1825', '10958'];
    $lvStr = (string) $lv;
    if (!in_array(strtolower($lt), ['demo', 'regular'], true)) {
        $lt = in_array($lvStr, $regularTiers, true) ? 'regular' : 'demo';
    }
    $typeLabel = ucfirst($lt);
@endphp

<div class="col-lg-4">
    <label class="form-label" for="license_validity">License Validity</label>
    <div class="input-group pb-input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-key'></i></span>
        <select class="form-select pb-select-search" id="license_validity" name="license_validity" data-placeholder="Select validity">
            <option value="7" @if($lvStr === '7') selected @endif>7 Days</option>
            <option value="5" @if($lvStr === '5') selected @endif>5 Days (legacy)</option>
            <option value="31" @if($lvStr === '31') selected @endif>1 Month (legacy)</option>
            <option value="183" @if($lvStr === '183') selected @endif>6 Months</option>
            <option value="365" @if($lvStr === '365') selected @endif>1 Year</option>
            <option value="1095" @if($lvStr === '1095') selected @endif>3 Years</option>
            <option value="1825" @if($lvStr === '1825') selected @endif>5 Years</option>
            <option value="10958" @if($lvStr === '10958') selected @endif>Lifetime</option>
        </select>
    </div>
    @error('license_validity')<div class="text-danger small">{{ $message }}</div>@enderror
</div>

<div class="col-lg-4">
    <label class="form-label" for="license_type_display">License Type</label>
    <div class="input-group pb-input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-badge-check'></i></span>
        <input type="text" class="form-control border-start-0" id="license_type_display" value="{{ $typeLabel }}" readonly tabindex="-1" aria-readonly="true">
        <input type="hidden" name="license_type" id="license_type" value="{{ $lt }}">
    </div>
    <small class="text-secondary">Set automatically from validity (same as Dealer app)</small>
</div>

<div class="col-lg-4">
    <label class="form-label" for="license_status">License Status</label>
    <div class="input-group pb-input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-check-circle'></i></span>
        <select class="form-select pb-select-search" id="license_status" name="license_status" data-placeholder="Select status">
            <option value="active" @if(strtolower((string)$ls) === 'active') selected @endif>Active</option>
            <option value="expired" @if(strtolower((string)$ls) === 'expired') selected @endif>Expired</option>
        </select>
    </div>
</div>

<div class="col-lg-4">
    <label class="form-label" for="payment_status">Payment Status</label>
    <div class="input-group pb-input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-money'></i></span>
        <select class="form-select pb-select-search" id="payment_status" name="payment_status" data-placeholder="Select payment">
            <option value="cash" @if(strtolower((string)$ps) === 'cash') selected @endif>Cash</option>
            <option value="online" @if(strtolower((string)$ps) === 'online') selected @endif>Online</option>
        </select>
    </div>
</div>

<div class="col-lg-4">
    <label class="form-label" for="license_amount">Amount</label>
    <div class="input-group pb-input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-rupee'></i></span>
        <input type="text" name="amount" id="license_amount" placeholder="Amount" class="form-control border-start-0" value="{{ $amt }}">
    </div>
</div>

<div class="col-lg-4">
    <label class="form-label" for="fast_billing">Fast Billing</label>
    <div class="input-group pb-input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-receipt'></i></span>
        <select class="form-select pb-select-search" id="fast_billing" name="fast_billing">
            <option value="1" @if((string)$fb === '1') selected @endif>Yes</option>
            <option value="0" @if((string)$fb === '0') selected @endif>No</option>
        </select>
    </div>
</div>

<div class="col-lg-4">
    <label class="form-label" for="dine_in">Dine In (Table wise)</label>
    <div class="input-group pb-input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-restaurant'></i></span>
        <select class="form-select pb-select-search" id="dine_in" name="dine_in">
            <option value="1" @if((string)$di === '1') selected @endif>Yes</option>
            <option value="0" @if((string)$di === '0') selected @endif>No</option>
        </select>
    </div>
</div>

<div class="col-lg-4">
    <label class="form-label" for="take_away">Take Away</label>
    <div class="input-group pb-input-group">
        <span class="input-group-text bg-transparent"><i class='bx bx-package'></i></span>
        <select class="form-select pb-select-search" id="take_away" name="take_away">
            <option value="1" @if((string)$ta === '1') selected @endif>Yes</option>
            <option value="0" @if((string)$ta === '0') selected @endif>No</option>
        </select>
    </div>
</div>

@once
@push('scripts')
<script>
(function () {
    var regularTiers = ['183', '365', '1095', '1825', '10958'];

    window.PB = window.PB || {};
    PB.syncLicenseType = function () {
        var $validity = $('#license_validity');
        if (!$validity.length) return;
        var days = String($validity.val() || '');
        var type = regularTiers.indexOf(days) >= 0 ? 'regular' : 'demo';
        $('#license_type').val(type);
        $('#license_type_display').val(type.charAt(0).toUpperCase() + type.slice(1));
    };

    $(function () {
        $(document).on('change select2:select', '#license_validity', PB.syncLicenseType);
        PB.syncLicenseType();
        if (typeof PB.initForms === 'function') {
            PB.initForms();
        }
    });
})();
</script>
@endpush
@endonce
