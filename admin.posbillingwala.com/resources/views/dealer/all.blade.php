@extends('layouts.app')
@section('page_title', 'Dealers')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'All Dealers',
            'subtitle' => 'Manage dealer accounts and their customer networks.',
            'actionUrl' => url('dealer/add'),
            'actionLabel' => 'Add Dealer',
            'actionIcon' => 'bx-store',
        ])
        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table id="myTable" class="table pb-transactions-table" style="width:100%">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Contact Number</th>
                                <th>Aadhar Number</th>
                                <th>Total Customers</th>
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
            title: 'No dealers found',
            subtitle: 'Add dealers to manage customers and licenses in your network.',
            actionUrl: '{{ url("dealer/add") }}',
            actionLabel: 'Add Dealer'
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
        ajax:"{{url('dealer/all')}}",
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
                "mData": "aadhar_number",
                "bSortable": false,
            },
            {
                "mData": "customers_count",
                "bSortable": false,
            },
            {
                "targets":-1,
                "mData": "is_active",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    if(row.is_active==0){
                        return '<div class="badge rounded-pill text-danger bg-light-danger p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Inactive</div>';
                    }else{
                        return '<div class="badge rounded-pill text-success bg-light-success p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Active</div>';
                    }
                },
                
            },
            {
                "targets":-1,
                "mData": "is_active",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    var actions = '<div class="table-actions-row">';
                    actions += '<a href="{{url("dealer/edit")}}/'+row.id+'" class="btn btn-sm btn-primary btn-action-icon" title="Edit"><i class="bx bx-edit-alt"></i></a>';
                    if(row.is_active==0){
                        actions += '<a href="{{url("dealer/delete")}}/'+row.id+'" class="btn btn-sm btn-outline-success btn-action-icon" title="Activate"><i class="bx bx-check"></i></a>';
                    } else {
                        actions += '<a href="{{url("dealer/delete")}}/'+row.id+'" class="btn btn-sm btn-outline-danger btn-action-icon" title="Deactivate"><i class="bx bx-trash"></i></a>';
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