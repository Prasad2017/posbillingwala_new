@extends('layouts.app')
@section('content')
<div class="page-wrapper">
	<div class="page-content">

		<div class="row">
			<div class="col-xl-12">

				<div class="card border-top border-0 border-4 border-primary">
					<div class="card-body p-5">
						<div class="card-title d-flex align-items-center">
							<div><i class="bx bx-user-circle me-1 font-22 text-primary"></i>
							</div>
							<h5 class="mb-0 text-primary">Add Customer</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('customers/add')}}" enctype="multipart/form-data">
							@csrf
							@if(Auth::user()->role_id  == 1)
							<div class="col-lg-4">
								<label for="inputEmailAddress" class="form-label">Select Dealer</label>
								<div class="input-group pb-input-group">
									<span class="input-group-text bg-transparent"><i class='bx bxs-user'></i></span>
									<select class="form-select pb-select-search @error('dealer_id') is-invalid @enderror" id="dealer_id" name="dealer_id" data-placeholder="Search dealer…">
										<option value="">Select dealer</option>
										@if($dealers)
										@foreach($dealers as $dealer)
										<option value="{{$dealer->id}}" @if(old('dealer_id')== $dealer->id) selected @endif>{{$dealer->name}}</option>
										@endforeach
										@endif
									</select>
								</div>
								@error('dealer_id')<div class="text-danger small">{{ $message }}</div>@enderror
							</div>
							@else
							<input type="hidden" name="dealer_id" value="{{Auth::user()->id}}">
							@endif
							<div class="col-lg-4">
								<label for="inputEmailAddress" class="form-label">Customer Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-user' ></i></span>
									<input type="text" name="name" id="inputEmailAddress" placeholder="Customer Name" class="form-control @error('name') is-invalid @enderror border-start-0" value="{{old('name')}}">
									@error('name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-lg-4">
								<label for="inputEmailAddress11" class="form-label">Customer Mobile Number</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-phone' ></i></span>
									<input type="text" name="mobile_number" id="inputEmailAddress11" placeholder="Customer Mobile Number" class="form-control @error('mobile_number') is-invalid @enderror border-start-0" value="{{old('mobile_number')}}">
									@error('mobile_number')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-lg-4">
								<label for="inputEmailAddress12" class="form-label">Shop Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-building-house' ></i></span>
									<input type="text" name="shop_name" id="inputEmailAddress12" placeholder="Shop Name" class="form-control @error('shop_name') is-invalid @enderror border-start-0" value="{{old('shop_name')}}">
									@error('shop_name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-lg-4">
								<label for="inputEmailAddress13" class="form-label">Shop Address</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-map-pin' ></i></span>
									<input type="text" name="shop_address" id="inputEmailAddress13" placeholder="Shop Address" class="form-control @error('shop_address') is-invalid @enderror border-start-0" value="{{old('shop_address')}}">
									@error('shop_address')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-lg-4">
								<label for="inputEmailAddress14" class="form-label">Shop Image</label>
								<div class="input-group">
									<input type="file" name="shop_image" id="inputEmailAddress14" placeholder="Shop Image" class="form-control @error('shop_image') is-invalid @enderror border-start-0" value="{{old('shop_image')}}" accept="image/png, image/jpeg" onchange="showImagePreview(event)">
									@error('shop_image')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							
							<div class="col-lg-4">
								<label for="inputEmailAddress1" class="form-label">App License Key</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-key' ></i></span>
									<input type="text" class="form-control border-start-0" id="inputEmailAddress1" readonly placeholder="App License Key" name="license_key"/>
								</div>
							</div>
							@include('partials.license-fields')
							<div class="col-lg-4">
								<label for="inputEmailAddress18" class="form-label">Shop Image Preview</label><br>
								<img id="output" style="width: 150px;height: 150px;">
							</div>
							
							<div class="col-lg-12">
								<button type="submit" class="btn btn-primary px-5">Add</button>
							</div>
						</form>
					</div>
				</div>

			</div>
		</div>
		<!--end row-->
	</div>
</div>
<script type="text/javascript">

	$(document).ready(function(){
		generateString(10);
	});

	const characters ='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

	function generateString(length) {
		let result = '';
		const charactersLength = characters.length;
		for ( let i = 0; i < length; i++ ) {
			result += characters.charAt(Math.floor(Math.random() * charactersLength));
		}

		$("#inputEmailAddress1").val(result);
	}
</script>
<script type="text/javascript">
    function showImagePreview(event) {
        var output2 = document.getElementById('output');
        output2.src = URL.createObjectURL(event.target.files[0]);
        output2.onload = function () {
            URL.revokeObjectURL(output2.src)
        }
    }
</script>
@endsection