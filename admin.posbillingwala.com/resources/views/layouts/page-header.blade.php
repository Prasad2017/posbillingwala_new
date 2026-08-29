<div class="pb-page-header mb-3">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
        <div>
            <h4 class="dash-hello mb-1">{{ $title ?? '' }}</h4>
            @if(!empty($subtitle))
            <p class="text-secondary mb-0">{{ $subtitle }}</p>
            @endif
        </div>
        @if(!empty($actionUrl) && !empty($actionLabel))
        <a href="{{ $actionUrl }}" class="btn btn-primary btn-sm">
            <i class='bx {{ $actionIcon ?? 'bx-plus' }}'></i> {{ $actionLabel }}
        </a>
        @endif
    </div>
</div>
