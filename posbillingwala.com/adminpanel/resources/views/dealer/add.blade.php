@extends('layouts.app')
@section('content')
<div class="page-wrapper">
	<div class="page-content">

		<div class="row">
			<div class="col-xl-7">

				<div class="card border-top border-0 border-4 border-primary">
					<div class="card-body p-5">
						<div class="card-title d-flex align-items-center">
							<div><i class="bx bxs-user me-1 font-22 text-primary"></i>
							</div>
							<h5 class="mb-0 text-primary">Dealer Registration</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('dealer/add')}}">
							@csrf
							<div class="col-md-6">
								<label for="inputLastName1" class="form-label">Full Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-user'></i></span>
									<input type="text" class="form-control @error('name') is-invalid @enderror border-start-0" name="name" id="inputLastName1" value="{{ old('name') }}" placeholder="Full Name" />
									@error('name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-md-6">
								<label for="inputPhoneNo" class="form-label">Phone No</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-microphone' ></i></span>
									<input type="text" class="form-control @error('contact_number') is-invalid @enderror border-start-0" id="inputPhoneNo" maxlength="10" placeholder="Phone No" value="{{ old('contact_number') }}" name="contact_number" />
									@error('contact_number')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-12">
								<label for="inputEmailAddress" class="form-label">Aadhar Number</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-message' ></i></span>
									<input type="text" class="form-control @error('aadhar_number') is-invalid @enderror border-start-0" id="inputEmailAddress" placeholder="Aadhar Number" name="aadhar_number" maxlength="12" value="{{old('aadhar_number')}}" />
									@error('aadhar_number')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-12">
								<label for="inputChoosePassword" class="form-label">Choose Password</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-lock-open' ></i></span>
									<input type="text" class="form-control @error('password') is-invalid @enderror border-start-0" id="inputChoosePassword" placeholder="Choose Password"  name="password" value="{{old('password')}}" />
									@error('password')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-12">
								<label for="inputConfirmPassword" class="form-label">Confirm Password</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-lock' ></i></span>
									<input type="text" class="form-control border-start-0" id="inputConfirmPassword" name="password_confirmation" placeholder="Confirm Password" />
								</div>
							</div>
							<div class="col-12">
								<label for="inputAddress3" class="form-label">Address</label>
								<textarea class="form-control @error('address') is-invalid @enderror" id="inputAddress3" placeholder="Enter Address" name="address" rows="3">{{old('address')}}</textarea>
								@error('address')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
							</div>

							<div class="col-12">
								<button type="submit" class="btn btn-primary px-5">Register</button>
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