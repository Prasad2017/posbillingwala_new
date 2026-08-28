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
							<h5 class="mb-0 text-primary">Update Customer Details</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('customers/edit')}}/{{$data->userId}}" enctype="multipart/form-data">
							@csrf
							@if(Auth::user()->role_id  == 1)
							<div class="col-4">
								<label for="inputEmailAddress" class="form-label">Select Dealer</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-user' ></i></span>
									<select class="form-control form-select @error('dealer_id') is-invalid @enderror" name="dealer_id">
										<option value="">select</option>
										@if($dealers)
										@foreach($dealers as $dealer)
										<option value="{{$dealer->id}}" @if(old('dealer_id',$data->dealerId)== $dealer->id) selected @endif>{{$dealer->name}}</option>
										@endforeach
										@endif
									</select>
									@error('dealer_id')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							@else
							<input type="hidden" name="dealer_id" value="{{Auth::user()->id}}">
							@endif
							<input type="hidden" name="licenseId" value="{{$data->licenseId}}">
							<div class="col-4">
								<label for="inputEmailAddress" class="form-label">Customer Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-user' ></i></span>
									<input type="text" name="name" id="inputEmailAddress" placeholder="Customer Name" class="form-control @error('name') is-invalid @enderror border-start-0" value="{{old('name',$data->name)}}">
									@error('name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-4">
								<label for="inputEmailAddress11" class="form-label">Customer Mobile Number</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-phone' ></i></span>
									<input type="text" name="mobile_number" id="inputEmailAddress11" placeholder="Customer Mobile Number" class="form-control @error('mobile_number') is-invalid @enderror border-start-0" value="{{old('mobile_number',$data->contact_number)}}">
									@error('mobile_number')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-4">
								<label for="inputEmailAddress12" class="form-label">Shop Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-building-house' ></i></span>
									<input type="text" name="shop_name" id="inputEmailAddress12" placeholder="Shop Name" class="form-control @error('shop_name') is-invalid @enderror border-start-0" value="{{old('shop_name',$data->shopName)}}">
									@error('shop_name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-4">
								<label for="inputEmailAddress13" class="form-label">Shop Address</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-map-pin' ></i></span>
									<input type="text" name="shop_address" id="inputEmailAddress13" placeholder="Shop Address" class="form-control @error('shop_address') is-invalid @enderror border-start-0" value="{{old('shop_address',$data->address)}}">
									@error('shop_address')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-4">
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
							
							<div class="col-4">
								<label for="inputEmailAddress1" class="form-label">App License Key</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-key' ></i></span>
									<input type="text" class="form-control border-start-0" id="inputEmailAddress1" value="{{$data->licenseKey}}" readonly placeholder="App License Key" name="license_key"/>
								</div>
							</div>
							<div class="col-4">
								<label for="inputEmailAddress14" class="form-label">Select License Key Validity</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-key' ></i></span>
									<select class="form-control form-select" id="inputEmailAddress14" name="license_validity">
										<option value="7" @if(old('license_validity',$data->licenseValidity) == 7) selected @endif>7 days</option>
										<option value="31" @if(old('license_validity',$data->licenseValidity) == 31) selected @endif>1 Month</option>
										<option value="365" @if(old('license_validity',$data->licenseValidity) == 365) selected @endif>1 Year</option>
									</select>
									@error('license_validity')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-4">
								<label for="inputEmailAddress15" class="form-label">Select License Key Type</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-key' ></i></span>
									<select class="form-control form-select" id="inputEmailAddress15" name="license_type">
										<option value="demo" @if(old('license_type',$data->licenseType) == 'demo') selected @endif>Demo</option>
										<option value="regular" @if(old('license_type',$data->licenseType) == 'regular') selected @endif>Regular</option>
										
									</select>
									@error('license_type')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-4">
								<label for="inputEmailAddress16" class="form-label">Select License Key Status</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-key' ></i></span>
									<select class="form-control form-select" id="inputEmailAddress16" name="license_status">
										<option value="active" @if(old('license_status',$data->licenseStatus) == 'active') selected @endif>Active</option>
										<option value="expired" @if(old('license_status',$data->licenseStatus) == 'expired') selected @endif>Expired</option>
										
									</select>
									@error('license_status')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-4">
								<label for="inputEmailAddress17" class="form-label">Select Payment Status</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-money' ></i></span>
									<select class="form-control form-select" id="inputEmailAddress17" name="payment_status">
										<option value="cash" @if(old('payment_status',$data->paymentStatus) == 'cash') selected @endif>Cash</option>
										<option value="online" @if(old('payment_status',$data->paymentStatus) == 'online') selected @endif>Online</option>
										
									</select>
									@error('payment_status')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-4">
								<label for="inputEmailAddress18" class="form-label">Amount</label>
								<div class="input-group">
									<span class="input-group-text bg-transparent"><i class='bx bx-rupee' ></i></span>
									<input type="text" name="amount" id="inputEmailAddress18" placeholder="Amount" class="form-control @error('amount') is-invalid @enderror border-start-0" value="{{old('amount',$data->amount)}}">
									@error('amount')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-4">
								<label for="inputEmailAddress18" class="form-label">Shop Image Preview</label><br>
								<img id="output" @if($data->shopImage!=null) src="{{url('storage/app')}}/{{$data->shopImage}}" @endif style="width: 150px;height: 150px;">
							</div>
							
							<div class="col-12">
								<button type="submit" class="btn btn-primary px-5">Update</button>
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
    function showImagePreview(event) {
        var output2 = document.getElementById('output');
        output2.src = URL.createObjectURL(event.target.files[0]);
        output2.onload = function () {
            URL.revokeObjectURL(output2.src)
        }
    }
</script>
@endsection