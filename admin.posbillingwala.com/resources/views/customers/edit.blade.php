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
							<div><i class="bx bx-user-circle me-1 font-22 text-primary"></i>
							</div>
							<h5 class="mb-0 text-primary">Update Customer Details</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('customers/edit')}}/{{$data->userId}}" enctype="multipart/form-data">
							@csrf
							@if(Auth::user()->role_id  == 1)
							<div class="col-lg-4">
								<label for="dealer_id" class="form-label">Select Dealer</label>
								<div class="input-group pb-input-group">
									<span class="input-group-text bg-transparent"><i class='bx bxs-user'></i></span>
									<select class="form-select pb-select-search @error('dealer_id') is-invalid @enderror" id="dealer_id" name="dealer_id" data-placeholder="Search dealer…">
										<option value="">Select dealer</option>
										@if($dealers)
										@foreach($dealers as $dealer)
										<option value="{{$dealer->id}}" @if(old('dealer_id',$data->dealerId)== $dealer->id) selected @endif>{{$dealer->name}}</option>
										@endforeach
										@endif
									</select>
								</div>
								@error('dealer_id')<div class="text-danger small">{{ $message }}</div>@enderror
							</div>
							@else
							<input type="hidden" name="dealer_id" value="{{Auth::user()->id}}">
							@endif
							<input type="hidden" name="licenseId" value="{{$data->licenseId}}">
							<div class="col-lg-4">
								<label for="inputEmailAddress" class="form-label">Customer Name</label>
								<div class="input-group pb-input-group">
									<span class="input-group-text bg-transparent"><i class='bx bxs-user'></i></span>
									<input type="text" name="name" id="inputEmailAddress" placeholder="Customer Name" class="form-control @error('name') is-invalid @enderror border-start-0" value="{{old('name',$data->name)}}">
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
									<input type="text" name="mobile_number" id="inputEmailAddress11" placeholder="Customer Mobile Number" class="form-control @error('mobile_number') is-invalid @enderror border-start-0" value="{{old('mobile_number',$data->contact_number)}}">
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
									<input type="text" name="shop_name" id="inputEmailAddress12" placeholder="Shop Name" class="form-control @error('shop_name') is-invalid @enderror border-start-0" value="{{old('shop_name',$data->shopName)}}">
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
									<input type="text" name="shop_address" id="inputEmailAddress13" placeholder="Shop Address" class="form-control @error('shop_address') is-invalid @enderror border-start-0" value="{{old('shop_address',$data->address)}}">
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
								<div class="input-group pb-input-group">
									<span class="input-group-text bg-transparent"><i class='bx bx-key'></i></span>
									<input type="text" class="form-control border-start-0" id="inputEmailAddress1" value="{{$data->licenseKey}}" readonly placeholder="App License Key" name="license_key"/>
								</div>
							</div>
							@include('partials.license-fields', ['license' => $data])
							<div class="col-lg-4">
								<label for="inputEmailAddress18" class="form-label">Shop Image Preview</label><br>
								<img id="output" @if($data->shopImage!=null) src="{{url('storage/app')}}/{{$data->shopImage}}" @endif style="width: 150px;height: 150px;">
							</div>
							
							<div class="col-lg-12">
								<button type="submit" class="btn btn-primary px-5">Update</button>
							</div>
						</form>
					</div>
				</div>

			</div>
		</div>
		<!--end row-->

		<div class="row">
			<div class="col-xl-12">
				<div class="card border-top border-0 border-4 border-primary">
					<div class="card-body p-5">
						<div class="card-title d-flex align-items-center">
							<div style="width: 50%;"><i class="bx bx-key me-1 font-22 text-primary"></i>
							<h5 class="mb-0 text-primary" style="display: inline;">Franchise License Key</h5>
							</div>
							<div class="btn-group" style="width: 50%;justify-content: right;">
								<a href="{{url('customers/add-license')}}/{{$data->userId}}"><button type="button" class="btn btn-primary px-5"><i class="bx bx-plus mr-1"></i>Add Key</button></a>
							</div>
						</div>
						<hr>
						<div class="table-responsive">
							<table id="myTable" class="table table-striped table-bordered" style="width:100%">
								<thead>
									<tr>
										<th>App License Key</th>
										<th>User Name</th>
										<th>User Type</th>
										<th>License Type</th>
										<th>Expiry Date</th>
										<th>License Status</th>
										<th>Action</th>
									</tr>
								</thead>

							</table>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<script type="text/javascript">
$(document).ready(function(){
    myTable();
});

function getSearchFilter()
{
    $("#myTable").DataTable().clear().destroy();
    myTable();

}

function myTable()
{
    $("#myTable").dataTable({
        "processing": true,
        "serverSide": true,
        "responsive": true,
        "searching": true,
        "lengthChange": true,
         "columnDefs": [{
                "width": "25%",
                "targets": "_all" 
            }],
        ajax:"{{url('customers/all-license')}}?userId={{$data->userId}}",
            "columns":[
            
            {
                "mData": "licenseKey",
                "bSortable": false,
            },
            {
                "mData": "userName",
                "bSortable": false,
            },
            {
                "mData": "userType",
                "bSortable": false,
            },
            
            {
                "mData": "licenseType",
                "bSortable": false,
            },
            {
                "mData": "expiryDate",
                "bSortable": false,
            },
            {
                "targets":-1,
                "mData": "licenseStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    if(row.licenseStatus=='expired'){
                        return '<div class="badge rounded-pill text-danger bg-light-danger p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Expired</div>';
                    }else{
                        return '<div class="badge rounded-pill text-success bg-light-success p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Active</div>';
                    }
                },
                
            },
            {
                "targets":-1,
                "mData": "licenseStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    var actions = '<div class="table-actions-row">';
                    actions += '<a href="{{url("customers/edit-license")}}/'+row.id+'" class="btn btn-sm btn-primary btn-action-icon" title="Edit"><i class="bx bx-edit-alt"></i></a>';
                    if(row.licenseStatus=='expired'){
                        actions += '<a href="{{url("customers/delete-license")}}/'+row.id+'" class="btn btn-sm btn-outline-success btn-action-icon" title="Activate"><i class="bx bx-check"></i></a>';
                    } else {
                        actions += '<a href="{{url("customers/delete-license")}}/'+row.id+'" class="btn btn-sm btn-outline-danger btn-action-icon" title="Deactivate"><i class="bx bx-trash"></i></a>';
                    }
                    actions += '</div>';
                    return actions;
                },
                
            },
            ]
            
        });
}
</script>
@endsection