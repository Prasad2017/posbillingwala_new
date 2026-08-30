@php $product = $product ?? null; @endphp
<div class="row g-3">
    <div class="col-md-8"><label class="form-label">Product name *</label><input type="text" class="form-control" name="name" value="{{ old('name', $product->name ?? '') }}" required></div>
    <div class="col-md-4"><label class="form-label">Icon (emoji)</label><input type="text" class="form-control" name="icon" value="{{ old('icon', $product->icon ?? '') }}" maxlength="16"></div>
    <div class="col-md-4"><label class="form-label">Category *</label>
        <select class="form-select" name="category">
            @foreach(['software','hardware','consumables','accessories'] as $cat)
            <option value="{{ $cat }}" @if(old('category', $product->category ?? '') === $cat) selected @endif>{{ ucfirst($cat) }}</option>
            @endforeach
        </select>
    </div>
    <div class="col-md-4"><label class="form-label">Sort order</label><input type="number" class="form-control" name="sort_order" value="{{ old('sort_order', $product->sort_order ?? 0) }}" min="0"></div>
    <div class="col-md-4 d-flex align-items-end"><div class="form-check"><input class="form-check-input" type="checkbox" name="is_published" value="1" id="prodPub" @if(old('is_published', $product ? $product->is_published : true)) checked @endif><label class="form-check-label" for="prodPub">Published</label></div></div>
    <div class="col-12"><label class="form-label">Description</label><textarea class="form-control" name="description" rows="3">{{ old('description', $product->description ?? '') }}</textarea></div>
</div>
