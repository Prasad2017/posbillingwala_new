@extends('layouts.app')
@section('page_title', 'Dealers')
@section('content')
@php
    $metrics = $metrics ?? [
        'totalDealer' => 0,
        'activeDealer' => 0,
        'inactiveDealer' => 0,
        'totalCustomers' => 0,
        'activePercent' => 0,
        'inactivePercent' => 0,
        'growthBars' => [],
    ];
@endphp
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

        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6">
                <div class="kpi-card kpi-blue">
                    <span class="kpi-icon blue"><i class='bx bx-store-alt'></i></span>
                    <span class="kpi-label">Total</span>
                    <span class="kpi-value">{{ number_format($metrics['totalDealer']) }}</span>
                </div>
            </div>
            <div class="col-md-3 col-6">
                <div class="kpi-card kpi-green">
                    <span class="kpi-icon green"><i class='bx bx-check-circle'></i></span>
                    <span class="kpi-label">Active</span>
                    <span class="kpi-value">{{ number_format($metrics['activeDealer']) }}</span>
                    <span class="kpi-trend up">{{ $metrics['activePercent'] }}%</span>
                </div>
            </div>
            <div class="col-md-3 col-6">
                <div class="kpi-card kpi-red">
                    <span class="kpi-icon red"><i class='bx bx-block'></i></span>
                    <span class="kpi-label">Inactive</span>
                    <span class="kpi-value">{{ number_format($metrics['inactiveDealer']) }}</span>
                    <span class="kpi-trend down">{{ $metrics['inactivePercent'] }}%</span>
                </div>
            </div>
            <div class="col-md-3 col-6">
                <div class="kpi-card kpi-orange">
                    <span class="kpi-icon orange"><i class='bx bx-group'></i></span>
                    <span class="kpi-label">Customers</span>
                    <span class="kpi-value">{{ number_format($metrics['totalCustomers']) }}</span>
                </div>
            </div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-lg-5">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Status mix</h6>
                    <div class="donut-wrap">
                        <canvas id="dealerMix"></canvas>
                        <div class="donut-center">
                            <small>Total</small>
                            <strong>{{ number_format($metrics['totalDealer']) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-7">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">New dealers (7 days)</h6>
                    <div class="bar-wrap"><canvas id="dealerGrowth"></canvas></div>
                </div></div>
            </div>
        </div>

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
@push('scripts')
<script>
(function () {
    PB.donut('dealerMix', ['Active', 'Inactive'], [
        {{ (int) $metrics['activeDealer'] }},
        {{ (int) $metrics['inactiveDealer'] }}
    ], ['#16a34a', '#6b7280']);
    var g = @json($metrics['growthBars'] ?? []);
    PB.bar('dealerGrowth', g.map(function (x) { return x.label; }), g.map(function (x) { return x.count; }), '#f59e0b');
})();
</script>
@endpush
