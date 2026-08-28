@extends('layouts.app')
@section('content')
<div class="page-wrapper">
	<div class="page-content">
		@include('layouts.flash')
		<div class="row">
			<div class="col-xl-12">

				<div class="card border-top border-0 border-4 border-primary">
					<div class="card-body p-5">
						<div class="card-title d-flex align-items-center">
							<div><i class="bx bx-key me-1 font-22 text-primary"></i>
							</div>
							<h5 class="mb-0 text-primary">Update License Details</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('customers/edit-license')}}/{{$data->id}}" enctype="multipart/form-data">
							@csrf
							
							<div class="col-lg-4">
								<label for="inputEmailAddress" class="form-label">Customer Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-user' ></i></span>
									<input type="text" name="name" id="inputEmailAddress" placeholder="Customer Name" class="form-control @error('name') is-invalid @enderror border-start-0" value="{{old('name',$data->userName)}}">
									@error('name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<input type="hidden" name="user_type" value="{{$data->userType??'franchise'}}">
							<!-- <div class="col-lg-4">
								<label for="inputEmailAddress14" class="form-label">Select Customer Type</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-user' ></i></span>
									<select class="form-control form-select" id="inputEmailAddress14" name="user_type">
										<option value="owner" @if(old('user_type',$data->userType) == 'owner') selected @endif>Owner</option>
										<option value="cashier" @if(old('user_type',$data->userType) == 'cashier') selected @endif>Cashier</option>
									</select>
									@error('user_type')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div> -->

							<div class="col-lg-4">
								<label for="inputEmailAddress1" class="form-label">App License Key</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-key' ></i></span>
									<input type="text" class="form-control border-start-0" id="inputEmailAddress1" value="{{$data->licenseKey}}" readonly placeholder="App License Key" name="license_key"/>
								</div>
							</div>
							@include('partials.license-fields', ['license' => $data])
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

@endsection