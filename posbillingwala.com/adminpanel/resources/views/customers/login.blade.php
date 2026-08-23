@extends('auth.public')
@section('content')
<div class="card" style="box-shadow: 0 2px 6px 0 rgb(15 15 23 / 0%), 0 2px 6px 0 rgb(217 211 211);">
    <div class="card-body">
        <div class="border p-4 rounded">
            <div class="text-center">
                <h3 class="">Sign in</h3>
                <p class="mb-0 text-secondary">Customer access for POS Billingwala</p>
            </div>
      <div class="login-separater text-center mb-4"> <span>SIGN IN AS CUSTOMER</span>
        <hr/>
    </div>
    <div class="form-body">
        <form class="row g-3" method="POST" action="{{ route('customer.login') }}">
            @csrf
            <div class="col-12">
                <label for="contact_number" class="form-label">Enter Contact Number</label>
                <input id="contact_number" type="text" class="form-control @error('contact_number') is-invalid @enderror" name="contact_number" value="{{ old('contact_number') }}" required autocomplete="contact_number" placeholder="Contact Number" autofocus>

                @error('contact_number')
                <span class="invalid-feedback" role="alert">
                    <strong>{{ $message }}</strong>
                </span>
                @enderror
            </div>
            <div class="col-12">
                <label for="secret_key" class="form-label">Enter Secret Key</label>
                <input id="secret_key" type="text" class="form-control @error('secret_key') is-invalid @enderror" name="secret_key" value="{{ old('secret_key') }}" required autocomplete="off" placeholder="Secret Key" autofocus>

                @error('secret_key')
                <span class="invalid-feedback" role="alert">
                    <strong>{{ $message }}</strong>
                </span>
                @enderror
            </div>
            <div class="col-12">
                <div class="d-grid">
                    <button type="submit" class="btn btn-primary"><i class="bx bxs-lock-open"></i>Sign in</button>
                </div>
            </div>
        </form>
    </div>
</div>
</div>
</div>
@endsection
