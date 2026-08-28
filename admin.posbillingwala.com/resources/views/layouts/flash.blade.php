@if ($message = session('success'))
 <div class="alert alert-success border-0 bg-success alert-dismissible fade show">
 	<div class="text-white">{{ $message }}</div>
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
