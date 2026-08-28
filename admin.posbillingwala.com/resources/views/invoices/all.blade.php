@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <!--breadcrumb-->
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Invoices</div>
            <div class="ps-3">
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-0 p-0">
                        <li class="breadcrumb-item"><a href="javascript:;"><i class="bx bx-home-alt"></i></a>
                        </li>
                        <li class="breadcrumb-item active" aria-current="page">Invoices</li>
                    </ol>
                </nav>
            </div>
            <div class="ms-auto">
                <!-- <div class="btn-group">
                    <a href="{{url('customers/add')}}"><button type="button" class="btn btn-primary px-5"><i class="bx bx-plus mr-1"></i>Add Customer</button></a>
                </div> -->
            </div>
        </div>
        <!--end breadcrumb-->
        <h6 class="mb-0 text-uppercase">All Record</h6>
        <hr/>
        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table id="myTable" class="table table-striped table-bordered" style="width:100%">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Invoice Number</th>
                                <th>Subtotal</th>
                                <th>GST</th>
                                <th>Discount</th>
                                <th>Total Amount</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        
                    </table>
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
        ajax:"{{url('invoices/all')}}",
            "columns":[
            
            {
                "mData": "invoiceDate",
                "bSortable": false,
            },
            {
                "mData": "invoiceNumber",
                "bSortable": false,
            },
            {
                "mData": "subTotal",
                "bSortable": false,
            },
            {
                "mData": "totalGSTAmount",
                "bSortable": false,
            },
            {
                "mData": "discount",
                "bSortable": false,
            },
            {
                "mData": "totalAmount",
                "bSortable": false,
            },
            {
                "targets":-1,
                "mData": "invoiceOrderStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    return '<div class="table-actions-row"><a href="{{url("invoices/edit")}}/'+row.invoiceId+'" class="btn btn-sm btn-primary btn-action-icon" title="View / Edit"><i class="bx bx-edit-alt"></i></a></div>';
                },
                
            },
            ]
            
        });
}
</script>
@endsection