@extends('layouts.app')
@section('page_title', 'Mess Members Excel')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Mess Members — Excel',
            'subtitle' => ($customer->name ?? '') . (!empty($customer->shopName) ? ' — ' . $customer->shopName : ''),
            'actionUrl' => route('mess-members.index', ['customerId' => $customerId, 'licenceId' => $licenceId]),
            'actionLabel' => 'Back to Members',
            'actionIcon' => 'bx-arrow-back',
        ])

        <div class="card pb-form-card">
            <div class="card-body">
                <div class="mb-4">
                    <label class="form-label">Licence</label>
                    <form method="GET" action="{{ route('mess-members.excel', ['customerId' => $customerId]) }}">
                        <select name="licenceId" class="form-select" style="max-width: 420px" onchange="this.form.submit()">
                            @forelse($licences as $lic)
                                <option value="{{ $lic->id }}" @if((int)$licenceId === (int)$lic->id) selected @endif>
                                    #{{ $lic->id }} — {{ $lic->licenseKey ?? 'Licence' }}
                                </option>
                            @empty
                                <option value="">No licences</option>
                            @endforelse
                        </select>
                    </form>
                </div>

                @if($licenceId > 0)
                    <h6 class="mb-3">Template</h6>
                    <p class="text-secondary mb-3">Download a blank Excel template with Members and Payments sheets.</p>
                    <a href="{{ route('mess-members.template', ['customerId' => $customerId]) }}" class="btn btn-outline-primary mb-4">
                        <i class='bx bx-download'></i> Download Template
                    </a>

                    <hr class="my-4">

                    <h6 class="mb-3">Export</h6>
                    <p class="text-secondary mb-3">Export all members and payments for the selected licence.</p>
                    <form method="POST" action="{{ route('mess-members.export', ['customerId' => $customerId]) }}" class="mb-4">
                        @csrf
                        <input type="hidden" name="licenceId" value="{{ $licenceId }}">
                        <button type="submit" class="btn btn-success">
                            <i class='bx bx-export'></i> Export Excel
                        </button>
                    </form>

                    <hr class="my-4">

                    <h6 class="mb-3">Import</h6>
                    <p class="text-secondary mb-3">Upload an <strong>.xlsx</strong> file (Members + optional Payments sheets). Max 10 MB.</p>
                    <form method="POST" action="{{ route('mess-members.import', ['customerId' => $customerId]) }}" enctype="multipart/form-data" class="row g-3">
                        @csrf
                        <input type="hidden" name="licenceId" value="{{ $licenceId }}">
                        <div class="col-md-8">
                            <input type="file" name="import_file" class="form-control @error('import_file') is-invalid @enderror" accept=".xlsx" required>
                            @error('import_file')<div class="invalid-feedback">{{ $message }}</div>@enderror
                        </div>
                        <div class="col-md-4">
                            <button type="submit" class="btn btn-primary w-100">
                                <i class='bx bx-import'></i> Import
                            </button>
                        </div>
                    </form>
                @else
                    <p class="text-secondary mb-0">Select a licence to use Excel tools.</p>
                @endif
            </div>
        </div>
    </div>
</div>
@endsection
