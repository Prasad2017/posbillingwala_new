@php
  $dealer = $dealer ?? null;
  $area = old('area', $dealer->area ?? '');
  $dealerName = old('dealer_name', $dealer->dealer_name ?? '');
  $dealerType = old('dealer_type', $dealer->dealer_type ?? 'authorized_dealer');
  $contactPerson = old('contact_person', $dealer->contact_person ?? '');
  $roleTitle = old('role_title', $dealer->role_title ?? '');
  $mobile = old('mobile', $dealer->mobile ?? '');
  $whatsapp = old('whatsapp', $dealer->whatsapp ?? '');
  $sortOrder = old('sort_order', $dealer->sort_order ?? 0);
  $address = old('address', $dealer->address ?? '');
  $mapUrl = old('map_url', $dealer->map_url ?? '');
  $isPublished = old('is_published', $dealer ? $dealer->is_published : true);
@endphp
<div class="row g-3">
    <div class="col-md-4">
        <label class="form-label">Area / City *</label>
        <input type="text" class="form-control @error('area') is-invalid @enderror" name="area" value="{{ $area }}" required placeholder="Pune">
        @error('area')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>
    <div class="col-md-4">
        <label class="form-label">Dealer / Office name *</label>
        <input type="text" class="form-control @error('dealer_name') is-invalid @enderror" name="dealer_name" value="{{ $dealerName }}" required>
        @error('dealer_name')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>
    <div class="col-md-4">
        <label class="form-label">Type *</label>
        <select class="form-select" name="dealer_type">
            <option value="authorized_dealer" @if($dealerType === 'authorized_dealer') selected @endif>Authorized Dealer</option>
            <option value="head_office" @if($dealerType === 'head_office') selected @endif>Head Office</option>
        </select>
    </div>
    <div class="col-md-6">
        <label class="form-label">Contact person</label>
        <input type="text" class="form-control" name="contact_person" value="{{ $contactPerson }}">
    </div>
    <div class="col-md-6">
        <label class="form-label">Role / title</label>
        <input type="text" class="form-control" name="role_title" value="{{ $roleTitle }}" placeholder="Sales & Marketing Manager">
    </div>
    <div class="col-md-4">
        <label class="form-label">Mobile</label>
        <input type="text" class="form-control" name="mobile" value="{{ $mobile }}">
    </div>
    <div class="col-md-4">
        <label class="form-label">WhatsApp</label>
        <input type="text" class="form-control" name="whatsapp" value="{{ $whatsapp }}">
    </div>
    <div class="col-md-4">
        <label class="form-label">Sort order</label>
        <input type="number" class="form-control" name="sort_order" value="{{ $sortOrder }}" min="0">
    </div>
    <div class="col-12">
        <label class="form-label">Address</label>
        <textarea class="form-control" name="address" rows="2">{{ $address }}</textarea>
    </div>
    <div class="col-12">
        <label class="form-label">Google Maps URL</label>
        <input type="url" class="form-control" name="map_url" value="{{ $mapUrl }}" placeholder="https://maps.google.com/...">
    </div>
    <div class="col-12">
        <div class="form-check">
            <input class="form-check-input" type="checkbox" name="is_published" value="1" id="dealerPub" @if($isPublished) checked @endif>
            <label class="form-check-label" for="dealerPub">Published on website</label>
        </div>
    </div>
</div>
