@extends('layouts.app')
@section('content')
<div class="page-wrapper">
	<div class="page-content">

		<div class="row">
			<div class="col-xl-12">

				<div class="card border-top border-0 border-4 border-primary">
					<div class="card-body p-5">
						<div class="card-title d-flex align-items-center">
							<div><i class="bx bx-key me-1 font-22 text-primary"></i>
							</div>
							<h5 class="mb-0 text-primary">Add Franchise</h5>
						</div>
						<hr>
						<form class="row g-3" method="POST" action="{{url('customers/add-license')}}/{{$data->id}}" enctype="multipart/form-data">
							@csrf
							
							<div class="col-lg-4">
								<label for="inputEmailAddress" class="form-label">Name</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bxs-user' ></i></span>
									<input type="text" name="name" id="inputEmailAddress" placeholder="Name" class="form-control @error('name') is-invalid @enderror border-start-0" value="{{old('name')}}">
									@error('name')
									<span class="invalid-feedback" role="alert">
										<strong>{{ $message }}</strong>
									</span>
									@enderror
								</div>
							</div>
							<div class="col-lg-4">
								<label for="inputEmailAddress" class="form-label">Branch Name</label>
								<div class="input-group">
									<span class="input-group-text bg-transparent"><i class='bx bx-store'></i></span>
									<input type="text" name="branch_name" placeholder="Franchise branch name" class="form-control border-start-0" value="{{ old('branch_name') }}">
								</div>
							</div>
							<input type="hidden" name="user_type" value="franchise">
							<div class="col-lg-4">
								<label for="inputEmailAddress1" class="form-label">App License Key</label>
								<div class="input-group"> <span class="input-group-text bg-transparent"><i class='bx bx-key' ></i></span>
									<input type="text" class="form-control border-start-0" id="inputEmailAddress1" readonly placeholder="App License Key" name="license_key"/>
								</div>
							</div>
							<div class="col-lg-4">
								<label class="form-label">PB-PIN (auto on register)</label>
								<div class="input-group">
									<span class="input-group-text bg-transparent"><i class='bx bx-lock-alt'></i></span>
									<input type="text" class="form-control border-start-0" value="9082" readonly>
								</div>
							</div>
							@include('partials.license-fields')
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

	$(document).ready(function(){
		generateString(10);
	});

	const characters ='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

	function generateString(length) {
		let result = ' ';
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