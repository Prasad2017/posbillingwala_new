<!doctype html>
<html lang="en">

<head>
	<!-- Required meta tags -->
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<!--favicon-->
	<link rel="icon" href="{{asset('assets/images/favicon-32x32.png')}}" type="image/png" />
	<!--plugins-->
	<link href="{{asset('assets/plugins/simplebar/css/simplebar.css')}}" rel="stylesheet" />
	<link href="{{asset('assets/plugins/perfect-scrollbar/css/perfect-scrollbar.css')}}" rel="stylesheet" />
	<link href="{{asset('assets/plugins/metismenu/css/metisMenu.min.css')}}" rel="stylesheet" />
	<link href="{{asset('assets/plugins/datatable/css/dataTables.bootstrap5.min.css')}}" rel="stylesheet" />
	<link href="{{asset('assets/plugins/Drag-And-Drop/dist/imageuploadify.min.css')}}" rel="stylesheet" />
	<!-- loader-->
	<link href="{{asset('assets/css/pace.min.css')}}" rel="stylesheet" />
	<script src="{{asset('assets/js/pace.min.js')}}"></script>
	<!-- Bootstrap CSS -->
	<link href="{{asset('assets/css/bootstrap.min.css')}}" rel="stylesheet">
	<link href="{{asset('assets/css/bootstrap-extended.css')}}" rel="stylesheet">
	<link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500&amp;display=swap" rel="stylesheet">
	<link href="{{asset('assets/css/app.css')}}" rel="stylesheet">
	<link href="{{asset('assets/css/icons.css')}}" rel="stylesheet">
	<!-- Theme Style CSS -->
	<link rel="stylesheet" href="{{asset('assets/css/dark-theme.css')}}" />
	<link rel="stylesheet" href="{{asset('assets/css/semi-dark.css')}}" />
	<link rel="stylesheet" href="{{asset('assets/css/header-colors.css')}}" />
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.3/jquery.min.js"></script>

	<title>Dashboard - Admin Panel</title>
</head>

<body>
	<!--wrapper-->
	<div class="wrapper">
		<div class="page-wrapper" style="margin: 0 !important;padding: 0 !important">
			<div class="page-content">
				<!--end breadcrumb-->
				<div class="card">
					<div class="card-body">
						<div id="invoice">

							<div class="invoice overflow-auto">
								<div style="min-width: 600px">
									<header>
										<div class="row">
											<div class="col">
												<a href="javascript:;">
													<img src="assets/images/logo-icon.png" width="80" alt="" />
												</a>
											</div>
											<div class="col company-details">
												<h2 class="name">
													<a target="_blank" href="javascript:;">
														{{$data->companyName??''}}

													</a>
												</h2>
												<div>{{$data->companyAddress??''}}</div>
												<div>{{$data->companyMobile??''}}</div>
											</div>
										</div>
									</header>
									<main>
										<div class="row contacts">
											<div class="col invoice-to">
												<div class="text-gray-light">INVOICE TO:</div>
												<h2 class="to">{{$data->customerName??''}}</h2>
												<div class="address">{{$data->customerAddress??''}}</div>
												<div class="email"><a href="mailto:{{$data->customerEmail??''}}">{{$data->customerEmail??''}}</a>
												</div>
											</div>
											<div class="col invoice-details">
												<h3 class="invoice-id">INVOICE {{$data->invoiceNumber??''}}</h3>
												<div class="date">Date of Invoice: {{$data->invoiceDate??''}}</div>
											</div>
										</div>
										<table>
											<thead>
												<tr>
													<th>#</th>
													<th class="text-left">Item Name</th>
													<th class="text-right">Rate</th>
													<th class="text-right">Qty</th>
													<th class="text-right">CGST</th>
													<th class="text-right">SGST</th>
													<th class="text-right">Amount</th>
												</tr>
											</thead>
											<tbody>
												@foreach($data->products as $key=>$list)
												<tr>
													<td class="no">{{++$key}}</td>
													<td class="text-left">
														<h3>{{$list->productName??''}}</h3>
													</td>
													<td class="unit">&#8377; {{number_format($list->productPrice)??0}}</td>
													<td class="qty">{{$list->productQuantity??0}}</td>
													<td class="unit">{{$list->productCGST??0}}</td>
													<td class="qty"> {{$list->productSGST??0}}</td>
													<td class="total">&#8377; {{number_format($list->productPrice)??0}}</td>

												</tr>
												@endforeach
												<tr>
													<th class="text-left" colspan="2">Total</th>
													<th class="text-right unit">&#8377; {{number_format($data->subTotal)??0}}</th>
													<th class="text-right qty">{{$data->products->sum('productQuantity')??0}}</th>
													<th class="text-right unit">{{$data->products->sum('productCGST')??0}}</th>
													<th class="text-right qty">{{$data->products->sum('productSGST')??0}}</th>
													<th class="text-right total">&#8377; {{number_format($data->products->sum('productPrice'))??0}}</th>

												</tr>
											</tbody>
											<tfoot>
												<tr>
													<td colspan="3"></td>
													<td colspan="3">Total Amount Before Tax</td>
													<td>&#8377; {{number_format($data->subTotal)??0}}</td>
												</tr>
												<tr>
													<td colspan="3"></td>
													<td colspan="3">Total Tax Amount:GST</td>
													<td>&#8377; {{number_format($data->totalGSTAmount)??0}}</td>
												</tr>
												<tr>
													<td colspan="3"></td>
													<td colspan="3">Total Discount</td>
													<td>&#8377; {{number_format($data->discount)??''}}</td>
												</tr>
												<tr>
													<td colspan="3"></td>
													<td colspan="3">Total Amount After Tax</td>
													<td>&#8377; {{number_format($data->totalAmount)??''}}</td>
												</tr>
											</tfoot>
										</table>
										<div class="thanks">Thank you!</div>
								<!-- <div class="notices">
									<div>NOTICE:</div>
									<div class="notice">A finance charge of 1.5% will be made on unpaid balances after 30 days.</div>
								</div> -->
							</main>
							<footer>Invoice was created on a computer and is valid without the signature and seal.</footer>
						</div>
						<!--DO NOT DELETE THIS div. IT is responsible for showing footer always at the bottom-->
						<div></div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
</div>
    <!--end wrapper-->
   
    <!-- Bootstrap JS -->
    <script src="{{asset('assets/js/bootstrap.bundle.min.js')}}"></script>
    <!--plugins-->
    <script src="{{asset('assets/js/jquery.min.js')}}"></script>
    <script src="{{asset('assets/plugins/simplebar/js/simplebar.min.js')}}"></script>
    <script src="{{asset('assets/plugins/metismenu/js/metisMenu.min.js')}}"></script>
    <script src="{{asset('assets/plugins/perfect-scrollbar/js/perfect-scrollbar.js')}}"></script>
    <script src="{{asset('assets/plugins/datatable/js/jquery.dataTables.min.js')}}"></script>
    <script src="{{asset('assets/plugins/datatable/js/dataTables.bootstrap5.min.js')}}"></script>
    <script src="{{asset('assets/js/app.js')}}"></script>
</body>

</html>
