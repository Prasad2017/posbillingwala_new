@php $product = $product ?? null; @endphp
<div class="row g-3">
    <div class="col-md-8"><label class="form-label">Product name *</label><input type="text" class="form-control" name="name" value="{{ old('name', $product->name ?? '') }}" required placeholder="e.g. 80mm Bluetooth Thermal Printer"></div>
    <div class="col-md-4"><label class="form-label">Icon (emoji)</label><input type="text" class="form-control" name="icon" value="{{ old('icon', $product->icon ?? '') }}" maxlength="16" placeholder="🖨️"></div>
    <div class="col-md-4"><label class="form-label">Category *</label>
        <select class="form-select" name="category" id="productCategory">
            @foreach(['software'=>'Software','hardware'=>'Hardware','consumables'=>'Consumables','accessories'=>'Accessories'] as $cat => $label)
            <option value="{{ $cat }}" @if(old('category', $product->category ?? '') === $cat) selected @endif>{{ $label }}</option>
            @endforeach
        </select>
    </div>
    <div class="col-md-4"><label class="form-label">Sort order</label><input type="number" class="form-control" name="sort_order" value="{{ old('sort_order', $product->sort_order ?? 0) }}" min="0"></div>
    <div class="col-md-4 d-flex align-items-end"><div class="form-check"><input class="form-check-input" type="checkbox" name="is_published" value="1" id="prodPub" @if(old('is_published', $product ? $product->is_published : true)) checked @endif><label class="form-check-label" for="prodPub">Published</label></div></div>
    <div class="col-12">
        <label class="form-label">Description</label>
        <textarea class="form-control" name="description" id="productDescription" rows="4" placeholder="Short product description shown on the website card (1–2 sentences).">{{ old('description', $product->description ?? '') }}</textarea>
        <div class="form-text" id="productDescHint">Shown on the website under the product name. Keep it short and clear.</div>
    </div>
</div>
<script>
(function () {
    var hints = {
        software: 'Example: Complete Android POS app with GST billing, inventory, offline mode and Bluetooth printing.',
        hardware: 'Example: 80mm Bluetooth thermal printer for restaurant counters. Fast receipt printing with easy pairing.',
        consumables: 'Example: Premium 80mm thermal billing rolls. Smooth printing and long shelf life.',
        accessories: 'Example: Adjustable tablet stand for Android billing. Keeps screen secure at the counter.'
    };
    var cat = document.getElementById('productCategory');
    var hint = document.getElementById('productDescHint');
    if (!cat || !hint) return;
    function update() { hint.textContent = hints[cat.value] || hints.software; }
    cat.addEventListener('change', update);
    update();
})();
</script>
