@extends('layouts.app')
@section('content')
<div class="page-wrapper">
	<div class="page-content">

		<div class="row">
			<div class="col-xl-12">

				<div class="card border-top border-0 border-4 border-primary">
					<div class="card-body p-5">
						<div class="card-title d-flex align-items-center">
							<div><i class="bx bxs-user me-1 font-22 text-primary"></i>
							</div>
							<h5 class="mb-0 text-primary">Update Dealer Details</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('dealer/edit')}}/{{$data->id}}">
							@csrf
							<div class="col-md-4">
								<label for="inputLastName1" class="form-label">Full Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-user'></i></span>
									<input type="text" class="form-control @error('name') is-invalid @enderror border-start-0" name="name" id="inputLastName1" value="{{ old('name',$data->name) }}" placeholder="Full Name" />
									@error('name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-md-4">
								<label for="inputPhoneNo" class="form-label">Phone No</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-microphone' ></i></span>
									<input type="text" class="form-control @error('contact_number') is-invalid @enderror border-start-0" id="inputPhoneNo" maxlength="10" placeholder="Phone No" value="{{ old('contact_number',$data->contact_number) }}" name="contact_number" />
									@error('contact_number')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>

							<div class="col-lg-4">
								<label for="inputEmailAddress" class="form-label">Aadhar Number</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-message' ></i></span>
									<input type="text" class="form-control @error('aadhar_number') is-invalid @enderror border-start-0" id="inputEmailAddress" placeholder="Aadhar Number" name="aadhar_number" maxlength="12" value="{{old('aadhar_number',$data->aadhar_number)}}" />
									@error('aadhar_number')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-lg-4">
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
							<div class="col-lg-4">
								<label for="inputConfirmPassword" class="form-label">Confirm Password</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-lock' ></i></span>
									<input type="text" class="form-control border-start-0" id="inputConfirmPassword" name="password_confirmation" placeholder="Confirm Password" />
								</div>
							</div>
							<div class="col-lg-12">
								<label for="inputAddress3" class="form-label">Address</label>
								<textarea class="form-control @error('address') is-invalid @enderror" id="inputAddress3" placeholder="Enter Address" name="address" rows="3">{{old('address',$data->address)}}</textarea>
								@error('address')
								<span class="invalid-feedback" role="alert">
									<strong>{{ $message }}</strong>
								</span>
								@enderror
							</div>

							<div class="col-lg-12">
								<button type="submit" class="btn btn-primary px-5">Update Registered Details</button>
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
							<div style="width: 50%;"><i class="bx bx-user me-1 font-22 text-primary"></i>
								<h5 class="mb-0 text-primary" style="display: inline;">Customers</h5>
							</div>
							<div class="btn-group" style="width: 50%;justify-content: right;">
								<a href="{{url('customers/add')}}"><button type="button" class="btn btn-primary px-5"><i class="bx bx-plus mr-1"></i>Add Customer</button></a>
							</div>
						</div>
						<hr>
						<div class="table-responsive">
							<table id="myTable" class="table table-striped table-bordered" style="width:100%">
								<thead>
									<tr>
										<th>Customer Name</th>
										<th>Mobile Number</th>
										<th>Shop Name</th>
										<th>App License Key</th>
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
        ajax:"{{url('customers/all')}}?dealer_id={{$data->id}}",
            "columns":[
            
            {
                "mData": "name",
                "bSortable": false,
            },
            {
                "mData": "contact_number",
                "bSortable": false,
            },
            {
                "mData": "shopName",
                "bSortable": false,
            },
            
            {
                "mData": "licenseKey",
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
                    if(row.licenseStatus=='expired'){
                        return '<div class="d-flex order-actions"><a href="{{url("customers/edit")}}/'+row.userId+'" class=""><i class="bx bxs-edit"></i></a><a href="{{url("customers/delete")}}/'+row.userId+'"  data-toggle="tooltip" title="Activate" class="ms-3"><i class="bx bxs-share"></i></a></div>';
                    }
                    else
                    {
                        return '<div class="d-flex order-actions"><a href="{{url("customers/edit")}}/'+row.userId+'" class=""><i class="bx bxs-edit"></i></a><a href="{{url("customers/delete")}}/'+row.userId+'" class="ms-3"  data-toggle="tooltip" title="Delete"><i class="bx bxs-trash"></i></a></div>';
                    }

                },
                
            },
            ]
            
        });
}
</script>
@endsection