@extends('auth.public')
@section('content')
<div class="pb-login-card">
    <div class="pb-login-card-inner">
        @if(session('info'))
        <div class="alert alert-info py-2 px-3 mb-3" role="alert">{{ session('info') }}</div>
        @endif
        <div class="text-center mb-4">
            <h3 class="pb-login-title">Sign in</h3>
            <p class="pb-login-subtitle mb-0">Access your POS Billingwala panel</p>
        </div>

        <form class="row g-3" method="POST" action="{{ route('login') }}">
            @csrf
            <div class="col-12">
                <label for="login" class="form-label">Email or Aadhar Number</label>
                <input id="login" type="text" class="form-control pb-field @error('login') is-invalid @enderror" name="login" value="{{ old('login', old('email', old('aadhar_number'))) }}" required autocomplete="username" placeholder="Admin email or dealer Aadhar number" autofocus>

                @error('login')
                <span class="invalid-feedback" role="alert"><strong>{{ $message }}</strong></span>
                @enderror
            </div>
            <div class="col-12">
                <label for="password" class="form-label">Enter Password</label>
                <div class="input-group pb-password-toggle" id="show_hide_password">
                    <input id="password" type="password" class="form-control pb-field @error('password') is-invalid @enderror" name="password" required autocomplete="current-password" placeholder="Enter Password">
                    <button type="button" class="input-group-text bg-transparent border-start-0" aria-label="Toggle password visibility">
                        <i class='bx bx-hide'></i>
                    </button>
                    @error('password')
                    <span class="invalid-feedback d-block" role="alert"><strong>{{ $message }}</strong></span>
                    @enderror
                </div>
            </div>
            <div class="col-12">
                <button type="submit" class="btn btn-primary w-100 pb-login-submit">
                    <i class='bx bxs-lock-open'></i> Sign in
                </button>
            </div>
        </form>
    </div>
</div>
@endsection

@push('scripts')
<script>
$(function () {
    $('#show_hide_password button').on('click', function () {
        var $input = $('#password');
        var $icon = $(this).find('i');
        if ($input.attr('type') === 'password') {
            $input.attr('type', 'text');
            $icon.removeClass('bx-hide').addClass('bx-show');
        } else {
            $input.attr('type', 'password');
            $icon.removeClass('bx-show').addClass('bx-hide');
        }
    });
});
</script>
@endpush
