@php
    $title = $title ?? 'No data found';
    $subtitle = $subtitle ?? 'There is nothing to show here at the moment.';
    $compact = $compact ?? false;
    $actionUrl = $actionUrl ?? null;
    $actionLabel = $actionLabel ?? null;
    $icon = $icon ?? admin_asset('images/empty-state.svg');
@endphp
<div class="pb-empty-state{{ $compact ? ' pb-empty-state--compact' : '' }}">
    <img src="{{ $icon }}" alt="" class="pb-empty-image" loading="lazy">
    <h6 class="pb-empty-title">{{ $title }}</h6>
    <p class="pb-empty-subtitle">{{ $subtitle }}</p>
    @if(!empty($actionUrl) && !empty($actionLabel))
        <a href="{{ $actionUrl }}" class="btn btn-primary btn-sm pb-empty-action">
            {{ $actionLabel }}
        </a>
    @endif
</div>
