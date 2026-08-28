@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <a href="{{ url('sales/invoices') }}" class="mb-3 d-inline-block">&larr; Back to invoices</a>
        <div class="card mb-3">
            <div class="card-body">
                <h5 class="dash-hello">Invoice {{ $invoice->invoiceNumber }}</h5>
                <p class="text-secondary mb-1">{{ $invoice->shopName ?: $invoice->ownerName }} · {{ $invoice->invoiceDate }}</p>
                <p class="mb-0">Payment: {{ $invoice->paymentMode ?: 'Paid' }}</p>
            </div>
        </div>
        <div class="row g-3 mb-3">
            <div class="col-md-3"><div class="kpi-card kpi-blue"><span class="kpi-label">Subtotal</span><span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($invoice->subTotal ?? $invoice->totalAmount) }}</span></div></div>
            <div class="col-md-3"><div class="kpi-card kpi-orange"><span class="kpi-label">Discount</span><span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($invoice->discount ?? 0) }}</span></div></div>
            <div class="col-md-3"><div class="kpi-card kpi-green"><span class="kpi-label">GST</span><span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($invoice->totalGSTAmount ?? 0) }}</span></div></div>
            <div class="col-md-3"><div class="kpi-card kpi-navy"><span class="kpi-label">Total</span><span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($invoice->totalAmount) }}</span></div></div>
        </div>
        <div class="card">
            <div class="card-body table-responsive">
                <table class="table">
                    <thead><tr><th>Item</th><th>Portion</th><th>Qty</th><th>Price</th></tr></thead>
                    <tbody>
                    @forelse($items as $item)
                        <tr>
                            <td>{{ $item->productName }}</td>
                            <td>{{ $item->portionName ?: '—' }}</td>
                            <td>{{ $item->productQuantity }}</td>
                            <td>{{ \App\Services\AdminMetrics::rupee($item->productPrice) }}</td>
                        </tr>
                    @empty
                        <tr><td colspan="4">
                            @include('layouts.empty-state', [
                                'compact' => true,
                                'title' => 'No line items',
                                'subtitle' => 'Product details for this invoice are not stored on the server.',
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
