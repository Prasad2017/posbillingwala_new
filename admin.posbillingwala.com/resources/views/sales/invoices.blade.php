@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div>
                <h5 class="dash-hello mb-0">Recent Invoices</h5>
                <p class="text-secondary mb-0">{{ count($invoices) }} bills · {{ \App\Services\AdminMetrics::rupee($total) }}</p>
            </div>
            <form class="d-flex gap-2" method="get">
                <input class="form-control" name="q" value="{{ $q }}" placeholder="Search invoice, shop, customer">
                <button class="btn btn-primary">Search</button>
            </form>
        </div>
        <div class="card">
            <div class="card-body table-responsive">
                <table class="table">
                    <thead><tr><th>Invoice</th><th>Customer</th><th>Shop</th><th>Date</th><th>Amount</th><th></th></tr></thead>
                    <tbody>
                    @forelse($invoices as $inv)
                        <tr>
                            <td>{{ $inv->invoiceNumber }}</td>
                            <td>{{ $inv->customerName }}</td>
                            <td>{{ $inv->shopName }}</td>
                            <td>{{ $inv->invoiceDate }}</td>
                            <td>{{ \App\Services\AdminMetrics::rupee($inv->totalAmount) }}</td>
                            <td><a class="btn btn-sm btn-primary" href="{{ url('sales/invoices/'.$inv->invoiceId) }}">Details</a></td>
                        </tr>
                    @empty
                        <tr><td colspan="6">
                            @include('layouts.empty-state', [
                                'compact' => true,
                                'title' => 'No invoices found',
                                'subtitle' => 'Recent invoices from all customers will be listed here.',
                            ])
                        </td></tr>
                    @endforelse
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
@endsection
