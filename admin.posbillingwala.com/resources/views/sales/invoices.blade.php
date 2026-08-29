@extends('layouts.app')
@section('page_title', 'Transactions')
@section('content')
@php
  $filters = $filters ?? ['dealer_id' => 0, 'customer_id' => 0, 'payment' => ''];
  $selectedDate = $selectedDate ?? '';
  $payments = $paymentSummary['items'] ?? [];
@endphp
<div class="page-wrapper">
    <div class="page-content">
        <div class="pb-page-header mb-3">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
                <div>
                    <h4 class="dash-hello mb-1">Transactions</h4>
                    <p class="text-secondary mb-0">{{ count($invoices) }} bills · {{ \App\Services\AdminMetrics::rupee($total) }}</p>
                </div>
            </div>
        </div>

        <div class="card mb-3">
            <div class="card-body">
                <form method="GET" action="{{ url('sales/invoices') }}" class="row g-3 align-items-end">
                    <div class="col-md-3">
                        <label class="form-label">Search</label>
                        <input class="form-control pb-field" name="q" value="{{ $q }}" placeholder="Invoice, shop, license, dealer">
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Date</label>
                        <input type="date" class="form-control pb-field pb-filter-auto" name="date" value="{{ $selectedDate }}" max="{{ date('Y-m-d') }}">
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Dealer</label>
                        <select name="dealer_id" class="form-select pb-select-search" data-placeholder="All dealers">
                            <option value="">All dealers</option>
                            @foreach($dealers as $dealer)
                            <option value="{{ $dealer->id }}" @if((int)($filters['dealer_id'] ?? 0) === (int)$dealer->id) selected @endif>{{ $dealer->name }}</option>
                            @endforeach
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label">Customer</label>
                        <select name="customer_id" class="form-select pb-select-search" data-placeholder="All customers">
                            <option value="">All customers</option>
                            @foreach($customers as $customer)
                            <option value="{{ $customer->id }}" @if((int)($filters['customer_id'] ?? 0) === (int)$customer->id) selected @endif>{{ $customer->name }} — {{ $customer->shopName }}</option>
                            @endforeach
                        </select>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">Payment</label>
                        <select name="payment" class="form-select">
                            <option value="" @if(empty($filters['payment'])) selected @endif>All</option>
                            <option value="cash" @if(($filters['payment'] ?? '') === 'cash') selected @endif>Cash</option>
                            <option value="online" @if(($filters['payment'] ?? '') === 'online') selected @endif>Online</option>
                        </select>
                    </div>
                    <div class="col-12 d-flex gap-2">
                        <button type="submit" class="btn btn-primary btn-sm">Apply Filters</button>
                        <a href="{{ url('sales/invoices') }}" class="btn btn-outline-secondary btn-sm">Reset</a>
                    </div>
                </form>
            </div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-lg-8">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table pb-transactions-table">
                                <thead>
                                    <tr>
                                        <th>Invoice</th>
                                        <th>Dealer</th>
                                        <th>Customer</th>
                                        <th>Shop</th>
                                        <th>License</th>
                                        <th>Branch</th>
                                        <th>Date</th>
                                        <th>Amount</th>
                                        <th>Payment</th>
                                        <th></th>
                                    </tr>
                                </thead>
                                <tbody>
                                @forelse($invoices as $inv)
                                    <tr>
                                        <td>{{ $inv->invoiceNumber }}</td>
                                        <td>{{ $inv->dealerName ?: '—' }}</td>
                                        <td>{{ $inv->customerName }}</td>
                                        <td>{{ $inv->shopName }}</td>
                                        <td><small>{{ $inv->licenseKey ?: '—' }}</small></td>
                                        <td>{{ $inv->branchName ?: '—' }}</td>
                                        <td>{{ $inv->invoiceDate }}</td>
                                        <td>{{ \App\Services\AdminMetrics::rupee($inv->totalAmount) }}</td>
                                        <td>{{ \App\Services\AdminMetrics::normalizePaymentLabel($inv->paymentMode ?? '') }}</td>
                                        <td><a class="btn btn-sm btn-outline-primary" href="{{ url('sales/invoices/'.$inv->invoiceId) }}">Details</a></td>
                                    </tr>
                                @empty
                                    <tr><td colspan="10">
                                        @include('layouts.empty-state', [
                                            'compact' => true,
                                            'title' => 'No invoices found',
                                            'subtitle' => 'Try changing date, dealer, customer, or payment filter.',
                                        ])
                                    </td></tr>
                                @endforelse
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card h-100">
                    <div class="card-body">
                        <h6 class="section-title">Payment Summary</h6>
                        <div class="pb-payment-list">
                            @forelse($payments as $pay)
                            <div class="pb-payment-row">
                                <span class="pb-payment-icon"><i class='bx {{ \App\Services\AdminMetrics::paymentIcon($pay['mode']) }}'></i></span>
                                <div class="pb-payment-meta flex-grow-1">
                                    <strong>{{ $pay['mode'] }}</strong>
                                    <small class="d-block text-secondary">{{ $pay['percent'] }}% · {{ $pay['bills'] }} bills</small>
                                </div>
                                <span class="pb-payment-amount">{{ \App\Services\AdminMetrics::rupee($pay['total']) }}</span>
                            </div>
                            @empty
                            <p class="text-secondary mb-0">No payment data for selected filters.</p>
                            @endforelse
                        </div>
                        @if(($paymentSummary['grandTotal'] ?? 0) > 0)
                        <div class="pb-payment-total">
                            <span>Total</span>
                            <strong>{{ \App\Services\AdminMetrics::rupee($paymentSummary['grandTotal']) }}</strong>
                        </div>
                        @endif
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
