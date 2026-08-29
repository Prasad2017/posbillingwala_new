@extends('layouts.app')
@section('page_title', 'Dealer Reports')
@section('content')
@php
  $dealerLabels = collect($data['dealers'])->pluck('dealerName')->values();
  $dealerValues = collect($data['dealers'])->map(function ($d) {
      return $d['totalSales'] > 0 ? $d['totalSales'] : $d['totalCustomer'];
  })->values();
@endphp
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.page-header', [
            'title' => 'Dealer Reports',
            'subtitle' => 'This month sales · ' . \App\Services\AdminMetrics::rupee($data['totalSales']),
            'actionUrl' => url('reports'),
            'actionLabel' => 'Reports Hub',
            'actionIcon' => 'bx-grid-alt',
        ])
        <div class="row g-3">
            <div class="col-lg-5">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Sales mix</h6>
                    <div class="donut-wrap">
                        <canvas id="mix"></canvas>
                        <div class="donut-center">
                            <small>Total</small>
                            <strong>{{ \App\Services\AdminMetrics::rupee($data['totalSales']) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-7">
                <div class="card h-100"><div class="card-body table-responsive">
                    <h6 class="section-title">Dealer performance</h6>
                    <table class="table pb-transactions-table">
                        <thead><tr><th>Dealer</th><th>Customers</th><th>Active licenses</th><th>Sales</th></tr></thead>
                        <tbody>
                        @forelse($data['dealers'] as $d)
                            <tr>
                                <td><a href="{{ url('dealer/edit/'.$d['dealerId']) }}">{{ $d['dealerName'] }}</a></td>
                                <td>{{ $d['totalCustomer'] }}</td>
                                <td>{{ $d['activeLicenses'] }}</td>
                                <td>{{ \App\Services\AdminMetrics::rupee($d['totalSales']) }}</td>
                            </tr>
                        @empty
                            <tr><td colspan="4">
                                @include('layouts.empty-state', [
                                    'compact' => true,
                                    'title' => 'No dealer data',
                                    'subtitle' => 'Dealer performance will show once dealers onboard customers.',
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
PB.donut('mix', @json($dealerLabels), @json($dealerValues));
</script>
@endpush
