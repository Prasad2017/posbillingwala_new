@extends('layouts.app')
@section('content')
<div class="page-wrapper">
	<div class="page-content">

		<div class="row">
			<div class="col-xl-10">

				<div class="card border-top border-0 border-4 border-primary">
					<div class="card-body p-5">
						<div class="card-title d-flex align-items-center">
							<div><i class="bx bx-message-square-edit me-1 font-22 text-primary"></i>
							</div>
							<h5 class="mb-0 text-primary">Add Product</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('products/add')}}">
							@csrf
							@if(Auth::user()->role_id==2 || Auth::user()->role_id==1)
							<div class="col-4">
								<label for="inputEmailAddress" class="form-label">Select Customer</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-user' ></i></span>
									<select class="form-control form-select" name="user_id" onchange="getCategories(this.value)">
										<option value="0">Select</option>
										@foreach($users as $user)
										<option value="{{$user->id}}" @if(old('user_id', $selectedUserId ?? null) == $user->id) selected @endif>{{$user->name}}</option>
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
							<div class="col-4">
								<label for="inputEmailAd" class="form-label">Select Category</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-category' ></i></span>
									<select class="form-control form-select @error('category_id') is-invalid @enderror" name="category_id" id="category_id" onchange="getSubcategories()">
										<option value="">select</option>
										@foreach($categories as $category)
										<option value="{{$category->categoryId}}" @if(old('category_id') == $category->categoryId) selected @endif>{{$category->categoryName}}</option>
										@endforeach
									</select>
									@error('category_id')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-4">
								<label class="form-label">Select Subcategory</label>
								<div class="input-group">
									<span class="input-group-text bg-transparent"><i class='bx bx-list-ul'></i></span>
									<select class="form-control form-select" name="subcategory_id" id="subcategory_id">
										<option value="">select</option>
									</select>
								</div>
							</div>
							<div class="col-4">
								<label for="inputEmailAddress" class="form-label">Product Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-message' ></i></span>
									<input type="text" class="form-control @error('product_name') is-invalid @enderror border-start-0" id="inputEmailAddress" placeholder="Product Name" name="product_name" value="{{old('product_name')}}" />
									@error('product_name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-4">
								<label for="inputPrice" class="form-label">Product Price</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-money' ></i></span>
									<input type="text" class="form-control @error('price') is-invalid @enderror border-start-0" id="inputPrice" placeholder="Product Price" name="price" value="{{old('price')}}" />
									@error('price')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-4">
								<label for="inputUnit" class="form-label">Select Product Unit</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-bar-chart-alt-2' ></i></span>
									<select id="inputUnit" class="form-control form-select @error('unit_id') is-invalid @enderror" name="unit_id">
										<option value="">select</option>
										@foreach($units as $unit)
										<option value="{{$unit->name}}"  @if(old('unit_id') == $unit->name) selected @endif>{{$unit->name}}</option>
										@endforeach
									</select>
									@error('unit_id')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-4">
								<label for="cgst" class="form-label">CGST %</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-rupee' ></i></span>
									<input type="text" class="form-control @error('cgst') is-invalid @enderror border-start-0" id="cgst" placeholder="CGST (Optional)" name="cgst" value="{{old('cgst')}}" />
									@error('cgst')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-4">
								<label for="sgst" class="form-label">SGST %</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-rupee' ></i></span>
									<input type="text" class="form-control @error('sgst') is-invalid @enderror border-start-0" id="sgst" placeholder="SGST (Optional)" name="sgst" value="{{old('sgst')}}" />
									@error('sgst')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
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

<script type="text/javascript">
@if(Auth::user()->role_id==2)
	function getCategories(user_id)
	{
		getCategoriesRecord(user_id);
	}
@else
	$(document).ready(function(){
		getCategoriesRecord("{{Auth::user()->id}}");
	});
@endif
@if(!empty($selectedUserId))
	$(document).ready(function(){
		getCategoriesRecord("{{ $selectedUserId }}");
	});
@endif
</script>

<script type="text/javascript">
	function getSubcategories()
	{
		var userId = $("select[name='user_id']").val() || "{{ Auth::user()->id }}";
		var categoryId = $("#category_id").val();
		if(userId && categoryId){
			$.ajax({
				url:"{{url('products/get-subcategories')}}",
				type:"GET",
				data:{user_id:userId, category_id:categoryId},
				success:function(data){ $("#subcategory_id").html(data); }
			});
		}
	}
	function getCategoriesRecord(user_id)
	{
		if(user_id.length!=0)
		{
			$.ajax({
				url:"{{url('products/get-categories')}}",
				type:"GET",
				data:{user_id:user_id},
				success:function(data)
				{
					$("#category_id").html(data);
				}
			});
		}
	}
</script>
@endsection