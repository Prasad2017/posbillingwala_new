@extends('layouts.app')
@section('content')
<div class="page-wrapper">
	<div class="page-content">
		@include('layouts.flash')
		<div class="row">
			<div class="col-xl-7">
				<div class="card border-top border-0 border-4 border-primary">
					<div class="card-body p-5">
						<div class="card-title d-flex align-items-center">
							<div><i class="bx bx-list-plus me-1 font-22 text-primary"></i></div>
							<h5 class="mb-0 text-primary">Add Subcategory</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('subcategories/add')}}">
							@csrf
							@if(Auth::user()->role_id==2 || Auth::user()->role_id==1)
							<div class="col-6">
								<label class="form-label">Select Customer</label>
								<div class="input-group">
									<span class="input-group-text bg-transparent"><i class='bx bxs-user'></i></span>
									<select class="form-control form-select" name="user_id" id="user_id" required>
										<option value="">Select</option>
										@foreach($users as $user)
										<option value="{{$user->id}}" @if(old('user_id') == $user->id) selected @endif>{{$user->name}}</option>
										@endforeach
									</select>
								</div>
							</div>
							@endif
							<div class="col-6">
								<label class="form-label">Category</label>
								<div class="input-group">
									<span class="input-group-text bg-transparent"><i class='bx bxs-category'></i></span>
									<select class="form-control form-select" name="category_id" id="category_id" required>
										<option value="">Select Category</option>
										@foreach($categories as $category)
										<option value="{{ $category->categoryId }}"
											data-user="{{ $category->userId }}"
											@if(old('category_id') == $category->categoryId) selected @endif>
											{{ $category->categoryName }}
										</option>
										@endforeach
									</select>
								</div>
								<small class="text-muted">e.g. Veg / Non Veg / Beverage</small>
							</div>
							<div class="col-6">
								<label class="form-label">Subcategory Name</label>
								<div class="input-group">
									<span class="input-group-text bg-transparent"><i class='bx bxs-message'></i></span>
									<input type="text" class="form-control @error('subcategory_name') is-invalid @enderror border-start-0"
										placeholder="e.g. Starter, Main Course"
										name="subcategory_name"
										value="{{old('subcategory_name')}}"
										required />
									@error('subcategory_name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-12">
								<button type="submit" class="btn btn-primary px-5">Add</button>
								<a href="{{url('subcategories/all')}}" class="btn btn-outline-secondary px-4 ms-2">Cancel</a>
							</div>
						</form>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
@if(Auth::user()->role_id==2 || Auth::user()->role_id==1)
<script>
$(document).ready(function(){
	function filterCategoriesByCustomer() {
		var userId = $("#user_id").val();
		$("#category_id option").each(function(){
			var optionUser = $(this).data('user');
			if (!$(this).val()) {
				$(this).show();
				return;
			}
			if (!userId || String(optionUser) === String(userId)) {
				$(this).show();
			} else {
				$(this).hide();
			}
		});
		var selected = $("#category_id option:selected");
		if (selected.val() && selected.is(':hidden')) {
			$("#category_id").val('');
		}
	}
	$("#user_id").on('change', filterCategoriesByCustomer);
	filterCategoriesByCustomer();
});
</script>
@endif
@endsection
