@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <!--breadcrumb-->
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Products</div>
            <div class="ps-3">
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-0 p-0">
                        <li class="breadcrumb-item"><a href="javascript:;"><i class="bx bx-home-alt"></i></a>
                        </li>
                        <li class="breadcrumb-item active" aria-current="page">Products</li>
                    </ol>
                </nav>
            </div>
            <div class="ms-auto">
                <div class="btn-group">
                    <a href="{{url('products/add')}}"><button type="button" class="btn btn-primary px-5"><i class="bx bx-plus mr-1"></i>Add Product</button></a>
                </div>
            </div>
        </div>
        <!--end breadcrumb-->
        <h6 class="mb-0 text-uppercase">All Products</h6>
        <hr/>
        @if(isset($customers) && count($customers))
        <div class="row mb-3">
            <div class="col-md-4">
                <select id="customerFilter" class="form-select" onchange="getSearchFilter()">
                    <option value="">All Customers</option>
                    @foreach($customers as $customer)
                    <option value="{{ $customer->id }}">{{ $customer->name }}</option>
                    @endforeach
                </select>
            </div>
        </div>
        @endif
        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table id="myTable" class="table table-striped table-bordered" style="width:100%">
                        <thead>
                            <tr>
                                <th>Category Name</th>
                                @if(Auth::user()->role_id == 2)
                                <th>Customer Name</th>
                                @endif
                                <th>Product Name</th>
                                <th>Product Price</th>
                                <th>Product Unit</th>
                                <th>Status</th>
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
    var params = new URLSearchParams(window.location.search);
    if(params.get('customer_id')) {
        $("#customerFilter").val(params.get('customer_id'));
    }
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
        pbEmpty: {
            title: 'No products found',
            subtitle: 'Add products to build your customer catalog.',
            actionUrl: '{{ url("products/add") }}',
            actionLabel: 'Add Product'
        },
        "processing": true,
        "serverSide": true,
        "responsive": true,
        "searching": true,
        "lengthChange": true,
         "columnDefs": [{
                "width": "25%",
                "targets": "_all" 
            }],
        ajax:"{{url('products/all')}}?customer_id="+($("#customerFilter").val() || ''),
            "columns":[
            
            {
                "mData": "categoryName",
                "bSortable": false,
            },
            @if(Auth::user()->role_id == 2)
            {
                "mData": "userName",
                "bSortable": false,
            },
            @endif
            {
                "mData": "productName",
                "bSortable": false,
            },
            {
                "mData": "productPrice",
                "bSortable": false,
            },
            {
                "mData": "productUnit",
                "bSortable": false,
            },
            
            {
                "targets":-1,
                "mData": "productStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    if(row.productStatus=='inactive'){
                        return '<div class="badge rounded-pill text-danger bg-light-danger p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Inactive</div>';
                    }else{
                        return '<div class="badge rounded-pill text-success bg-light-success p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Active</div>';
                    }
                },
                
            },
            {
                "targets":-1,
                "mData": "productStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    var actions = '<div class="table-actions-row">';
                    actions += '<a href="{{url("products/edit")}}/'+row.productId+'" class="btn btn-sm btn-primary btn-action-icon" title="Edit"><i class="bx bx-edit-alt"></i></a>';
                    actions += '<a href="{{url("portions/all")}}/'+row.userId+'/'+row.productId+'" class="btn btn-sm btn-outline-primary btn-action-icon" title="Portions"><i class="bx bx-food-menu"></i></a>';
                    actions += '<a href="{{url("products/delete")}}/'+row.productId+'" class="btn btn-sm btn-outline-danger btn-action-icon" title="Toggle"><i class="bx bx-trash"></i></a>';
                    actions += '</div>';
                    return actions;
                },
                
            },
            ]
            
        });
}
</script>
@endsection