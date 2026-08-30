@if ($message = session('success'))
 <div class="alert alert-success border-0 bg-success alert-dismissible fade show">
 	<div class="text-white">{{ $message }}</div>
 	<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
 </div>
@endif

@if ($credentials = session('registration_credentials'))
 <div class="alert alert-info border-0 alert-dismissible fade show">
 	<div>
 		<strong>Customer login credentials (give to customer):</strong>
 		<ul class="mb-0 mt-2">
 			<li><strong>App License Key:</strong> {{ $credentials['licenseKey'] ?? '—' }}</li>
 			<li><strong>PB-PIN (daily login):</strong> {{ $credentials['mpin'] ?? '9082' }}</li>
 			<li><strong>Report PIN:</strong> {{ $credentials['reportPin'] ?? '9082' }}</li>
 		</ul>
 	</div>
 	<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
 </div>
@endif


@if ($message = session('error'))
 <div class="alert alert-danger border-0 bg-danger alert-dismissible fade show">
 	<div class="text-white">{{ $message }}</div>
 	<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
 </div>
@endif


@if ($message = session('warning'))
<div class="alert alert-warning border-0 bg-warning alert-dismissible fade show">
 	<div class="text-white">{{ $message }}</div>
 	<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
 </div>
@endif


@if ($message = session('info'))
<div class="alert alert-info border-0 bg-info alert-dismissible fade show">
 	<div class="text-white">{{ $message }}</div>
 	<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
 </div>
@endif
