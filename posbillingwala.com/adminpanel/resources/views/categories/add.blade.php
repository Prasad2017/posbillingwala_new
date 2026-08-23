@extends('layouts.app')
@section('content')
<div class="page-wrapper">
	<div class="page-content">

		<div class="row">
			<div class="col-xl-7">

				<div class="card border-top border-0 border-4 border-primary">
					<div class="card-body p-5">
						<div class="card-title d-flex align-items-center">
							<div><i class="bx bxs-category me-1 font-22 text-primary"></i>
							</div>
							<h5 class="mb-0 text-primary">Add Category</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('categories/add')}}">
							@csrf
							@if(Auth::user()->role_id==2 || Auth::user()->role_id==1)
							<div class="col-6">
								<label for="inputEmailAddress" class="form-label">Select Customer</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-user' ></i></span>
									<select class="form-control form-select" name="user_id">
										<option value="0">Select</option>
										@foreach($users as $user)
										<option value="{{$user->id}}" @if(old('user_id') == $user->id) selected @endif>{{$user->name}}</option>
										@endforeach
									</select>
									@error('user_id')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							@endif
							<div class="col-6">
								<label for="inputEmailAddress4" class="form-label">Category Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-message' ></i></span>
									<input type="text" class="form-control @error('category_name') is-invalid @enderror border-start-0" id="inputEmailAddress4" placeholder="Category Name" name="category_name" value="{{old('category_name')}}" />
									@error('category_name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-6">
								<label class="form-label">Food Type</label>
								<div class="input-group">
									<span class="input-group-text bg-transparent"><i class='bx bx-food-menu'></i></span>
									<select class="form-control form-select" name="food_type_id">
										<option value="">Select (optional)</option>
										@foreach($foodTypes as $foodType)
										<option value="{{ $foodType->foodTypeId }}" @if(old('food_type_id') == $foodType->foodTypeId) selected @endif>{{ $foodType->foodTypeName }}</option>
										@endforeach
									</select>
								</div>
							</div>
							
							<div class="col-12">
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

@endsection