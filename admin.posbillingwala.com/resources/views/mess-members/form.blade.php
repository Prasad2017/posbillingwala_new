@extends('layouts.app')
@section('page_title', $isEdit ? 'Edit Mess Member' : 'Add Mess Member')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => $isEdit ? 'Edit Mess Member' : 'Add Mess Member',
            'subtitle' => ($customer->name ?? '') . (!empty($customer->shopName) ? ' — ' . $customer->shopName : ''),
            'actionUrl' => route('mess-members.index', ['customerId' => $customerId, 'licenceId' => $licenceId]),
            'actionLabel' => 'Back to List',
            'actionIcon' => 'bx-arrow-back',
        ])

        <div class="card border-top border-0 border-4 border-primary pb-form-card">
            <div class="card-body p-5">
                <form class="row g-3" method="POST"
                      action="{{ $isEdit
                          ? route('mess-members.update', ['customerId' => $customerId, 'memberId' => $member['memberId']])
                          : route('mess-members.store', ['customerId' => $customerId]) }}">
                    @csrf
                    @if($isEdit)
                        @method('PUT')
                    @endif

                    <div class="col-md-6">
                        <label class="form-label">Licence</label>
                        @if($isEdit)
                            <input type="hidden" name="licenceId" value="{{ $licenceId }}">
                            <input type="text" class="form-control" value="#{{ $licenceId }}" readonly>
                        @else
                            <select name="licenceId" class="form-select @error('licenceId') is-invalid @enderror" required>
                                @foreach($licences as $lic)
                                    <option value="{{ $lic->id }}" @if((int)old('licenceId', $licenceId) === (int)$lic->id) selected @endif>
                                        #{{ $lic->id }} — {{ $lic->licenseKey ?? 'Licence' }}
                                    </option>
                                @endforeach
                            </select>
                            @error('licenceId')<div class="invalid-feedback">{{ $message }}</div>@enderror
                        @endif
                    </div>

                    <div class="col-md-6">
                        <label class="form-label">Name <span class="text-danger">*</span></label>
                        <input type="text" name="memberName" class="form-control @error('memberName') is-invalid @enderror"
                               value="{{ old('memberName', $member['memberName'] ?? '') }}" required>
                        @error('memberName')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>

                    <div class="col-md-6">
                        <label class="form-label">Mobile <span class="text-danger">*</span></label>
                        <input type="text" name="memberMobileNumber" class="form-control @error('memberMobileNumber') is-invalid @enderror"
                               value="{{ old('memberMobileNumber', $member['memberMobileNumber'] ?? '') }}" required>
                        @error('memberMobileNumber')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>

                    <div class="col-md-6">
                        <label class="form-label">Alt Mobile</label>
                        <input type="text" name="memberAltenetMobileNumber" class="form-control @error('memberAltenetMobileNumber') is-invalid @enderror"
                               value="{{ old('memberAltenetMobileNumber', $member['memberAltenetMobileNumber'] ?? '') }}">
                        @error('memberAltenetMobileNumber')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>

                    <div class="col-12">
                        <label class="form-label">Address</label>
                        <input type="text" name="memberAddress" class="form-control @error('memberAddress') is-invalid @enderror"
                               value="{{ old('memberAddress', $member['memberAddress'] ?? '') }}">
                        @error('memberAddress')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>

                    @php $type = old('memberType', $member['memberType'] ?? 'student'); @endphp
                    <div class="col-12">
                        <label class="form-label d-block">Type <span class="text-danger">*</span></label>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input" type="radio" name="memberType" id="typeStudent" value="student"
                                   @if($type === 'student') checked @endif onchange="toggleMessType()">
                            <label class="form-check-label" for="typeStudent">Student</label>
                        </div>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input" type="radio" name="memberType" id="typeWorking" value="working"
                                   @if($type === 'working') checked @endif onchange="toggleMessType()">
                            <label class="form-check-label" for="typeWorking">Working</label>
                        </div>
                        @error('memberType')<div class="text-danger small">{{ $message }}</div>@enderror
                    </div>

                    <div id="studentFields" class="row g-3" style="{{ $type === 'working' ? 'display:none' : '' }}">
                        <div class="col-md-4">
                            <label class="form-label">Roll No</label>
                            <input type="text" name="rollNo" class="form-control"
                                   value="{{ old('rollNo', $member['rollNo'] ?? '') }}">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">College</label>
                            <input type="text" name="college" class="form-control"
                                   value="{{ old('college', $member['college'] ?? '') }}">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Year</label>
                            <input type="text" name="studentYear" class="form-control"
                                   value="{{ old('studentYear', $member['studentYear'] ?? '') }}">
                        </div>
                    </div>

                    <div id="companyField" class="col-md-6" style="{{ $type === 'working' ? '' : 'display:none' }}">
                        <label class="form-label">Company</label>
                        <input type="text" name="company" class="form-control"
                               value="{{ old('company', $member['company'] ?? '') }}">
                    </div>

                    <div class="col-md-6">
                        <label class="form-label">Registration No</label>
                        <input type="text" name="registrationNo" class="form-control"
                               value="{{ old('registrationNo', $member['registrationNo'] ?? '') }}"
                               placeholder="Optional — defaults from roll/mobile">
                    </div>

                    <div class="col-12 pt-2">
                        <button type="submit" class="btn btn-primary px-5">{{ $isEdit ? 'Update' : 'Save' }}</button>
                        <a href="{{ route('mess-members.index', ['customerId' => $customerId, 'licenceId' => $licenceId]) }}" class="btn btn-outline-secondary">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<script>
function toggleMessType() {
    var working = document.getElementById('typeWorking').checked;
    document.getElementById('studentFields').style.display = working ? 'none' : '';
    document.getElementById('companyField').style.display = working ? '' : 'none';
}
</script>
@endsection
