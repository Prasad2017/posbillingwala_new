@extends('layouts.app')
@section('page_title', 'Import Preview')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => $typeLabel . ' Import Preview',
            'subtitle' => 'Review validation results before confirming the import.',
            'actionUrl' => route('catalog-import-export.index', ['type' => $importType]),
            'actionLabel' => 'Back to Import',
            'actionIcon' => 'bx-arrow-back',
        ])

        @php
            $summary = $preview['summary'] ?? [];
            $errors = $preview['errors'] ?? [];
        @endphp

        <div class="row g-3">
            <div class="col-xl-8">
                <div class="card pb-form-card">
                    <div class="card-body">
                        <p class="mb-2"><strong>Customer:</strong> {{ $preview['customerName'] ?? $customerId }}</p>
                        <p class="mb-4"><strong>Session:</strong> {{ $preview['importSessionId'] ?? '—' }}</p>

                        <div class="row g-2 mb-4">
                            <div class="col-md-3"><div class="border rounded p-3 text-center"><div class="fw-bold">{{ $summary['total'] ?? 0 }}</div><small>Total Rows</small></div></div>
                            <div class="col-md-3"><div class="border rounded p-3 text-center"><div class="fw-bold text-success">{{ $summary['valid'] ?? 0 }}</div><small>Valid</small></div></div>
                            <div class="col-md-3"><div class="border rounded p-3 text-center"><div class="fw-bold">{{ $summary['new'] ?? 0 }}</div><small>New</small></div></div>
                            <div class="col-md-3"><div class="border rounded p-3 text-center"><div class="fw-bold">{{ $summary['updated'] ?? 0 }}</div><small>Updates</small></div></div>
                        </div>

                        @if(!empty($errors))
                            <h6 class="text-danger">Errors ({{ count($errors) }})</h6>
                            <div class="table-responsive mb-4">
                                <table class="table table-sm">
                                    <thead>
                                        <tr>
                                            <th>Row</th>
                                            <th>Message</th>
                                            <th>Code</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        @foreach(array_slice($errors, 0, 20) as $error)
                                            <tr>
                                                <td>{{ $error['row'] ?? '—' }}</td>
                                                <td>{{ $error['message'] ?? '—' }}</td>
                                                <td>{{ $error['code'] ?? '—' }}</td>
                                            </tr>
                                        @endforeach
                                    </tbody>
                                </table>
                            </div>

                            <form method="GET" action="{{ route('catalog-import-export.error-excel') }}" class="mb-4">
                                <input type="hidden" name="user_id" value="{{ $customerId }}">
                                <input type="hidden" name="import_session_id" value="{{ $preview['importSessionId'] }}">
                                <button type="submit" class="btn btn-outline-danger btn-sm">
                                    <i class='bx bx-download'></i> Download Error Excel
                                </button>
                            </form>
                        @endif

                        @if(($summary['valid'] ?? 0) > 0)
                            <form method="POST" action="{{ route('catalog-import-export.confirm') }}">
                                @csrf
                                <input type="hidden" name="user_id" value="{{ $customerId }}">
                                <input type="hidden" name="import_type" value="{{ $importType }}">
                                <input type="hidden" name="import_session_id" value="{{ $preview['importSessionId'] }}">
                                <button type="submit" class="btn btn-primary" onclick="return confirm('Import {{ $summary['valid'] }} valid rows?');">
                                    <i class='bx bx-import'></i> Confirm Import ({{ $summary['valid'] }} rows)
                                </button>
                            </form>
                        @else
                            <div class="alert alert-warning mb-0">No valid rows to import. Fix the Excel file and validate again.</div>
                        @endif
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
