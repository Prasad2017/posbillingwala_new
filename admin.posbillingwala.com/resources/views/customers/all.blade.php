@extends('layouts.app')
@section('page_title', 'Customers')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'All Customers',
            'subtitle' => 'Manage shop owners, licenses, and catalog access.',
            'actionUrl' => url('customers/add'),
            'actionLabel' => 'Add Customer',
            'actionIcon' => 'bx-user-plus',
        ])
        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table id="myTable" class="table pb-transactions-table" style="width:100%">
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
        pbEmpty: {
            title: 'No customers found',
            subtitle: 'Add your first customer to start managing licenses and POS access.',
            actionUrl: '{{ url("customers/add") }}',
            actionLabel: 'Add Customer'
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
        ajax:"{{url('customers/all')}}?dealer_id=@if(Auth::user()->role_id !=1 ){{Auth::user()->id??null}}@endif",
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
                        return '<div class="badge rounded-pill text-danger bg-light-danger p-2 text-uppercase px-3"><i class="bx bx-x-circle me-1"></i>Expired</div>';
                    }else{
                        return '<div class="badge rounded-pill text-success bg-light-success p-2 text-uppercase px-3"><i class="bx bx-check-circle me-1"></i>Active</div>';
                    }
                },
                
            },
            {
                "targets":-1,
                "mData": "licenseStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    var uid = row.userId || row.id;
                    var actions = '<div class="table-actions-row">';
                    actions += '<a href="{{url("customers/edit")}}/'+uid+'" class="btn btn-sm btn-primary btn-action-icon" title="Edit"><i class="bx bx-edit-alt"></i></a>';
                    actions += '<a href="{{url("customers/add-license")}}/'+uid+'" class="btn btn-sm btn-outline-primary btn-action-icon" title="New License"><i class="bx bx-key"></i></a>';
                    actions += '<a href="{{url("categories/all")}}?customer_id='+uid+'" class="btn btn-sm btn-outline-primary btn-action-icon" title="Categories"><i class="bx bx-category-alt"></i></a>';
                    actions += '<a href="{{url("products/all")}}?customer_id='+uid+'" class="btn btn-sm btn-outline-primary btn-action-icon" title="Products"><i class="bx bx-box"></i></a>';
                    actions += '<a href="{{url("products/add")}}?user_id='+uid+'" class="btn btn-sm btn-outline-primary btn-action-icon" title="Add Product"><i class="bx bx-plus"></i></a>';
                    actions += '</div>';
                    return actions;
                },
                
            },
            ]
            
        });
}
</script>
@endsection