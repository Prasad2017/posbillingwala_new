@extends('auth.public')
@section('content')
<div class="card" style="box-shadow: 0 2px 6px 0 rgb(15 15 23 / 0%), 0 2px 6px 0 rgb(217 211 211);">
    <div class="card-body">
        <div class="border p-4 rounded">
            <div class="text-center">
                <h3 class="">Sign in</h3>
                <p class="mb-0 text-secondary">Access your POS Billingwala panel</p>
            </div>
            <div class="d-grid gap-2 my-4">
                <a class="btn shadow-sm btn-white" href="{{url('dealer/login')}}">Sign in as Dealer</a>
                <a href="{{url('customer/login')}}" class="btn btn-facebook">Sign in as Customer</a>
            </div>
      <div class="login-separater text-center mb-4"> <span>OR SIGN IN AS ADMIN</span>
        <hr/>
    </div>
    <div class="form-body">
        <form class="row g-3" method="POST" action="{{ route('login') }}">
            @csrf
            <div class="col-12">
                <label for="inputEmailAddress" class="form-label">Email Address</label>
                <input id="email" type="email" class="form-control @error('email') is-invalid @enderror" name="email" value="{{ old('email') }}" required autocomplete="email" placeholder="Email Address" autofocus>

                @error('email')
                <span class="invalid-feedback" role="alert">
                    <strong>{{ $message }}</strong>
                </span>
                @enderror
            </div>
            <div class="col-12">
                <label for="inputChoosePassword" class="form-label">Enter Password</label>
                <div class="input-group" id="show_hide_password">
                    <input id="password" type="password" class="form-control @error('password') is-invalid @enderror" name="password" required autocomplete="current-password" placeholder="Enter Password"><a href="javascript:;" class="input-group-text bg-transparent"><i class='bx bx-hide'></i></a>

                    @error('password')
                    <span class="invalid-feedback" role="alert">
                        <strong>{{ $message }}</strong>
                    </span>
                    @enderror

                </div>
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
