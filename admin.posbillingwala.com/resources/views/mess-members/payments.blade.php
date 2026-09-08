@extends('layouts.app')
@section('page_title', 'Mess Payments')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Mess Payments',
            'subtitle' => ($member['memberName'] ?? '') . ' — ' . ($member['memberMobileNumber'] ?? ''),
            'actionUrl' => route('mess-members.index', ['customerId' => $customerId, 'licenceId' => $licenceId]),
            'actionLabel' => 'Back to Members',
            'actionIcon' => 'bx-arrow-back',
        ])

        <div class="card border-top border-0 border-4 border-primary pb-form-card mb-3">
            <div class="card-body p-5">
                <h6 class="mb-3">Add Payment</h6>
                <form class="row g-3" method="POST"
                      action="{{ route('mess-members.store-payment', ['customerId' => $customerId, 'memberId' => $member['memberId']]) }}">
                    @csrf
                    <div class="col-md-3">
                        <label class="form-label">Mess Amount <span class="text-danger">*</span></label>
                        <input type="text" name="paymentMessAmount" class="form-control @error('paymentMessAmount') is-invalid @enderror"
                               value="{{ old('paymentMessAmount') }}" required>
                        @error('paymentMessAmount')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>
                    <div class="col-md-3">
                        <label class="form-label">Paid Amount <span class="text-danger">*</span></label>
                        <input type="text" name="paymentPaidAmount" class="form-control @error('paymentPaidAmount') is-invalid @enderror"
                               value="{{ old('paymentPaidAmount') }}" required>
                        @error('paymentPaidAmount')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>
                    <div class="col-md-3">
                        <label class="form-label">Mess Days</label>
                        <select name="messTotalDays" class="form-select">
                            <option value="">Select</option>
                            <option value="One Time" @if(old('messTotalDays') === 'One Time') selected @endif>One Time</option>
                            <option value="Two Time" @if(old('messTotalDays') === 'Two Time') selected @endif>Two Time</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label">Payment Date</label>
                        <input type="date" name="paymentDate" class="form-control"
                               value="{{ old('paymentDate', date('Y-m-d')) }}">
                    </div>
                    <div class="col-12">
                        <button type="submit" class="btn btn-primary px-4">Save Payment</button>
                    </div>
                </form>
            </div>
        </div>

        <div class="card pb-form-card">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Mess Amount</th>
                                <th>Paid</th>
                                <th>Days</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            @forelse($payments as $p)
                                <tr>
                                    <td>{{ $p['paymentDate'] ?? '' }}</td>
                                    <td>{{ $p['paymentMessAmount'] ?? '' }}</td>
                                    <td>{{ $p['paymentPaidAmount'] ?? '' }}</td>
                                    <td>{{ $p['messTotalDays'] ?? '' }}</td>
                                    <td>{{ $p['paymentStatus'] ?? '' }}</td>
                                </tr>
                            @empty
                                <tr>
                                    <td colspan="5" class="text-center text-secondary py-4">No payments yet.</td>
                                </tr>
                            @endforelse
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
