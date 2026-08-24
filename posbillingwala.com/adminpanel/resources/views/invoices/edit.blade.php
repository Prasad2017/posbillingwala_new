@extends('layouts.app')
@section('content')
<!--start page wrapper -->
<div class="page-wrapper">
	<div class="page-content">
		<!--breadcrumb-->
		<div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
			<div class="breadcrumb-title pe-3">Invoice</div>
			<div class="ps-3">
				<nav aria-label="breadcrumb">
					<ol class="breadcrumb mb-0 p-0">
						<li class="breadcrumb-item"><a href="javascript:;"><i class="bx bx-home-alt"></i></a>
						</li>
						<li class="breadcrumb-item active" aria-current="page">Invoice</li>
					</ol>
				</nav>
			</div>

		</div>
		<!--end breadcrumb-->
		<div class="card">
			<div class="card-body">
				<div id="invoice">
					<div class="toolbar hidden-print">
						<div class="text-end">
							<!-- <button type="button" class="btn btn-dark"><i class="fa fa-print"></i> Print</button> -->
							<!--<a href="{{url('invoices/download')}}/{{$data->invoiceId}}"><button type="button" class="btn btn-danger"><i class="fa fa-file-pdf-o"></i> Export as PDF</button></a>-->
						</div>
						<hr/>
					</div>
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
										@include('invoices.partials.store_header')
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