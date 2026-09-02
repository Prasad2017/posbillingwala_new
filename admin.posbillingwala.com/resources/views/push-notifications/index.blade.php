@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="row">
            <div class="col-xl-8">
                <div class="card border-top border-0 border-4 border-primary">
                    <div class="card-body p-5">
                        <div class="card-title d-flex align-items-center">
                            <div><i class="bx bx-bell me-1 font-22 text-primary"></i></div>
                            <h5 class="mb-0 text-primary">Send Push Notification</h5>
                        </div>
                        <p class="text-secondary mb-4">Send offers, promotions, or announcements to POS app users who have registered for push notifications.</p>
                        <hr>

                        @if(session('success'))
                            <div class="alert alert-success">{{ session('success') }}</div>
                        @endif
                        @if(session('error'))
                            <div class="alert alert-danger">{{ session('error') }}</div>
                        @endif

                        <form method="POST" action="{{ url('push-notifications/send') }}" class="row g-3">
                            @csrf
                            <div class="col-12">
                                <label class="form-label">Title</label>
                                <input type="text" name="title" class="form-control @error('title') is-invalid @enderror"
                                       value="{{ old('title') }}" maxlength="120" placeholder="e.g. Special offer this week" required>
                                @error('title')<div class="text-danger small">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-12">
                                <label class="form-label">Message</label>
                                <textarea name="message" rows="4" class="form-control @error('message') is-invalid @enderror"
                                          maxlength="500" placeholder="Write your promotional message…" required>{{ old('message') }}</textarea>
                                @error('message')<div class="text-danger small">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Audience</label>
                                <select name="target" id="push-target" class="form-select @error('target') is-invalid @enderror" required>
                                    <option value="active" @selected(old('target', 'active') === 'active')>Active licences (with FCM token)</option>
                                    <option value="all" @selected(old('target') === 'all')>All licences (with FCM token)</option>
                                    <option value="license_ids" @selected(old('target') === 'license_ids')>Specific licence IDs</option>
                                </select>
                                @error('target')<div class="text-danger small">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-md-6" id="license-ids-wrap" style="display:none;">
                                <label class="form-label">Licence IDs (comma separated)</label>
                                <input type="text" name="license_ids" class="form-control @error('license_ids') is-invalid @enderror"
                                       value="{{ old('license_ids') }}" placeholder="12,45,78">
                                @error('license_ids')<div class="text-danger small">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Link URL (optional)</label>
                                <input type="url" name="url" class="form-control @error('url') is-invalid @enderror"
                                       value="{{ old('url') }}" placeholder="https://posbillingwala.com/offer">
                                @error('url')<div class="text-danger small">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Image URL (optional)</label>
                                <input type="url" name="image_url" class="form-control @error('image_url') is-invalid @enderror"
                                       value="{{ old('image_url') }}" placeholder="https://…">
                                @error('image_url')<div class="text-danger small">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-12">
                                <button type="submit" class="btn btn-primary px-4"><i class="bx bx-send me-1"></i> Send notification</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
            <div class="col-xl-4">
                <div class="card">
                    <div class="card-body">
                        <h6 class="mb-3">Licence expiry reminders</h6>
                        <p class="text-secondary small mb-2">Automatic push notifications are sent once per day starting 3 days before licence expiry.</p>
                        <p class="text-secondary small mb-0">Cron: <code>API/cron/notifyExpiringLicenses.php</code> (daily, with <code>X-Cron-Secret</code>).</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script>
(function () {
    var sel = document.getElementById('push-target');
    var wrap = document.getElementById('license-ids-wrap');
    function toggle() {
        wrap.style.display = sel.value === 'license_ids' ? '' : 'none';
    }
    sel.addEventListener('change', toggle);
    toggle();
})();
</script>
@endsection
