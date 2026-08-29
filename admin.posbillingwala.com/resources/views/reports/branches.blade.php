@extends('layouts.app')
@section('page_title', 'Branch Reports')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.page-header', [
            'title' => 'Branch Reports',
            'subtitle' => 'Branch distribution and status across all licenses.',
            'actionUrl' => url('reports'),
            'actionLabel' => 'Reports Hub',
            'actionIcon' => 'bx-grid-alt',
        ])
        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6"><div class="kpi-card kpi-blue"><span class="kpi-icon blue"><i class='bx bx-buildings'></i></span><span class="kpi-label">Total branches</span><span class="kpi-value">{{ number_format($data['totalBranches']) }}</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-green"><span class="kpi-icon green"><i class='bx bx-check-circle'></i></span><span class="kpi-label">Active</span><span class="kpi-value">{{ number_format($data['activeBranches']) }}</span><span class="kpi-trend up">{{ $data['activePercent'] }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-orange"><span class="kpi-icon orange"><i class='bx bx-pause-circle'></i></span><span class="kpi-label">Inactive</span><span class="kpi-value">{{ number_format($data['inactiveBranches']) }}</span><span class="kpi-trend down">{{ $data['inactivePercent'] }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-purple"><span class="kpi-icon purple"><i class='bx bx-plus-circle'></i></span><span class="kpi-label">New this month</span><span class="kpi-value">{{ number_format($data['newBranches']) }}</span><span class="kpi-trend up">{{ $data['newPercent'] }}%</span></div></div>
        </div>
        <div class="row g-3">
            <div class="col-lg-5">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Branch status</h6>
                    <div class="donut-wrap">
                        <canvas id="branchDonut"></canvas>
                        <div class="donut-center">
                            <small>Total</small>
                            <strong>{{ number_format($data['totalBranches']) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-7">
                <div class="card h-100"><div class="card-body table-responsive">
                    <h6 class="section-title">Top customers by branches</h6>
                    <table class="table">
                        <thead><tr><th>Customer</th><th>Shop</th><th>Branches</th></tr></thead>
                        <tbody>
                        @forelse($data['topCustomers'] as $c)
                            <tr>
                                <td><a href="{{ url('customers/edit/'.$c->customerId) }}">{{ $c->customerName }}</a></td>
                                <td>{{ $c->shopName }}</td>
                                <td>{{ $c->branchCount }}</td>
                            </tr>
                        @empty
                            <tr><td colspan="3">
                                @include('layouts.empty-state', [
                                    'compact' => true,
                                    'title' => 'No branches found',
                                    'subtitle' => 'Branch data will appear when customers add shop locations.',
                                ])
                            </td></tr>
                        @endforelse
                        </tbody>
                    </table>
                </div></div>
            </div>
        </div>
    </div>
</div>
@endsection
@push('scripts')
<script>
(function () {
    PB.donut('branchDonut', ['Active', 'Inactive', 'New this month'], [
        {{ (int) $data['activeBranches'] }},
        {{ (int) $data['inactiveBranches'] }},
        {{ (int) $data['newBranches'] }}
    ], ['#16a34a', '#ea580c', '#7c3aed']);
})();
</script>
@endpush
