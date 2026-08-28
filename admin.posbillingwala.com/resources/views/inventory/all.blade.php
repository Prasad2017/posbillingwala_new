@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <!--breadcrumb-->
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Inventory</div>
            <div class="ps-3">
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-0 p-0">
                        <li class="breadcrumb-item"><a href="javascript:;"><i class="bx bx-home-alt"></i></a>
                        </li>
                        <li class="breadcrumb-item active" aria-current="page">Inventory</li>
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
                                <th>Product Name</th>
                                <th>Product Inventory Quantity</th>
                                <th>Inventory Date</th>
                                <!--<th>Network Status</th>-->
                                <th>Status</th>
                                <!-- <th>Action</th> -->
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
        ajax:"{{url('inventory/all')}}",
            "columns":[
            
            {
                "mData": "productName",
                "bSortable": false,
            },
            {
                "mData": "productInventoryQuantity",
                "bSortable": false,
            },
            {
                "mData": "inventoryDate",
                "bSortable": false,
            },
            
            // {
            //     "mData": "inventoryNetworkStatus",
            //     "bSortable": false,
            // },
            {
                "targets":-1,
                "mData": "inventoryStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    if(row.licenseStatus=='inactive'){
                        return '<div class="badge rounded-pill text-danger bg-light-danger p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Inactive</div>';
                    }else{
                        return '<div class="badge rounded-pill text-success bg-light-success p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Active</div>';
                    }
                },
                
            },
            // {
            //     "targets":-1,
            //     "mData": "licenseStatus",
            //     "bSortable": false,
            //     "ilter":false,
            //     "mRender": function(data, type, row){
            //         return '<div class="d-flex order-actions"><a href="{{url("customers/edit")}}/'+row.userId+'" class=""><i class="bx bxs-edit"></i></a>';

            //     },
                
            // },
            ]
            
        });
}
</script>
@endsection