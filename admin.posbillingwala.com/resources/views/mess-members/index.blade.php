@extends('layouts.app')
@section('page_title', 'Mess Members')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Mess Members',
            'subtitle' => ($customer->name ?? '') . (!empty($customer->shopName) ? ' — ' . $customer->shopName : ''),
            'actionUrl' => url('customers/edit/' . $customerId),
            'actionLabel' => 'Back to Customer',
            'actionIcon' => 'bx-arrow-back',
        ])

        <div class="card pb-form-card mb-3">
            <div class="card-body">
                <form method="GET" action="{{ route('mess-members.index', ['customerId' => $customerId]) }}" class="row g-3 align-items-end">
                    <div class="col-md-6">
                        <label class="form-label">Licence</label>
                        <select name="licenceId" class="form-select" onchange="this.form.submit()">
                            @forelse($licences as $lic)
                                <option value="{{ $lic->id }}" @if((int)$licenceId === (int)$lic->id) selected @endif>
                                    #{{ $lic->id }} — {{ $lic->licenseKey ?? 'Licence' }}
                                </option>
                            @empty
                                <option value="">No licences</option>
                            @endforelse
                        </select>
                    </div>
                    <div class="col-md-6 d-flex flex-wrap gap-2">
                        @if($licenceId > 0)
                            <a href="{{ route('mess-members.create', ['customerId' => $customerId, 'licenceId' => $licenceId]) }}" class="btn btn-primary">
                                <i class='bx bx-plus'></i> Add Member
                            </a>
                            <a href="{{ route('mess-members.excel', ['customerId' => $customerId, 'licenceId' => $licenceId]) }}" class="btn btn-outline-primary">
                                <i class='bx bx-table'></i> Excel
                            </a>
                        @endif
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
                                <th>Name</th>
                                <th>Mobile</th>
                                <th>Type</th>
                                <th>Registration</th>
                                <th>Status</th>
                                <th class="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            @forelse($members as $m)
                                <tr>
                                    <td>{{ $m['memberName'] ?? '' }}</td>
                                    <td>{{ $m['memberMobileNumber'] ?? '' }}</td>
                                    <td>{{ ucfirst($m['memberType'] ?? 'student') }}</td>
                                    <td>{{ $m['registrationNo'] ?? '' }}</td>
                                    <td>{{ $m['memberStatus'] ?? '' }}</td>
                                    <td class="text-end">
                                        <a href="{{ route('mess-members.edit', ['customerId' => $customerId, 'memberId' => $m['memberId']]) }}" class="btn btn-sm btn-primary" title="Edit">
                                            <i class='bx bx-edit-alt'></i>
                                        </a>
                                        <a href="{{ route('mess-members.payments', ['customerId' => $customerId, 'memberId' => $m['memberId']]) }}" class="btn btn-sm btn-outline-success" title="Payments">
                                            <i class='bx bx-rupee'></i>
                                        </a>
                                    </td>
                                </tr>
                            @empty
                                <tr>
                                    <td colspan="6" class="text-center text-secondary py-4">No mess members for this licence.</td>
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
