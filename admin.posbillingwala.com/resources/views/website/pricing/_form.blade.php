@php
  $plan = $plan ?? null;
@endphp
<div class="row g-3">
    <div class="col-md-4">
        <label class="form-label">Plan type *</label>
        <select class="form-select" name="plan_type">
            <option value="subscription" @if(old('plan_type', $plan->plan_type ?? '') === 'subscription') selected @endif>Subscription (new)</option>
            <option value="renewal" @if(old('plan_type', $plan->plan_type ?? '') === 'renewal') selected @endif>Renewal</option>
        </select>
    </div>
    <div class="col-md-4">
        <label class="form-label">Validity *</label>
        <input type="text" class="form-control" name="validity_label" value="{{ old('validity_label', $plan->validity_label ?? '') }}" required placeholder="6 Months">
    </div>
    <div class="col-md-4">
        <label class="form-label">Price (₹) *</label>
        <input type="number" step="0.01" min="0" class="form-control" name="price" value="{{ old('price', $plan->price ?? 0) }}" required>
    </div>
    <div class="col-md-6">
        <label class="form-label">GST note *</label>
        <input type="text" class="form-control" name="gst_note" value="{{ old('gst_note', $plan->gst_note ?? 'GST included') }}" required>
    </div>
    <div class="col-md-3">
        <label class="form-label">Sort order</label>
        <input type="number" class="form-control" name="sort_order" value="{{ old('sort_order', $plan->sort_order ?? 0) }}" min="0">
    </div>
    <div class="col-md-3 d-flex align-items-end">
        <div class="form-check">
            <input class="form-check-input" type="checkbox" name="is_published" value="1" id="planPub" @if(old('is_published', $plan ? $plan->is_published : true)) checked @endif>
            <label class="form-check-label" for="planPub">Published</label>
        </div>
    </div>
    <div class="col-12">
        <label class="form-label">Description</label>
        <input type="text" class="form-control" name="description" value="{{ old('description', $plan->description ?? '') }}">
    </div>
</div>
