{{-- Rich text editor (Quill). User formats visually; HTML is saved in the form field. --}}
@php
    $name = $name ?? 'body_html';
    $value = $value ?? '';
    $label = $label ?? 'Content';
    $height = $height ?? '420px';
    $editorId = $editorId ?? ('rte_' . preg_replace('/[^a-zA-Z0-9_]/', '_', $name));
@endphp

<div class="mb-3 pb-rich-text-wrap">
    <label class="form-label">{{ $label }}</label>
    <textarea name="{{ $name }}" id="{{ $editorId }}_field" class="d-none" aria-hidden="true"></textarea>
    <div id="{{ $editorId }}" class="pb-rich-text-editor" data-initial="1" style="min-height: {{ $height }}; background:#fff;"></div>
    <script>
        document.getElementById(@json($editorId . '_field')).value = @json($value);
    </script>
    <div class="form-text">Use the toolbar for bold, italic, colour, headings, lists and links. Just type and click Save — no HTML tags.</div>
    @error($name)<div class="text-danger small mt-1">{{ $message }}</div>@enderror
</div>

@once
@push('styles')
<link href="https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.snow.css" rel="stylesheet">
<style>
    .pb-rich-text-wrap .ql-toolbar.ql-snow {
        border-radius: 0.5rem 0.5rem 0 0;
        background: #f8fafc;
    }
    .pb-rich-text-wrap .ql-container.ql-snow {
        border-radius: 0 0 0.5rem 0.5rem;
        font-size: 1rem;
        line-height: 1.6;
    }
    .pb-rich-text-wrap .ql-editor {
        min-height: 360px;
    }
    .pb-rich-text-wrap .ql-editor h2 { font-size: 1.35rem; margin-top: 1rem; }
    .pb-rich-text-wrap .ql-editor h3 { font-size: 1.15rem; margin-top: 0.85rem; }
</style>
@endpush

@push('scripts')
<script src="https://cdn.jsdelivr.net/npm/quill@2.0.3/dist/quill.js"></script>
<script>
(function () {
    window.PB = window.PB || {};
    window.PB.initRichTextEditors = function () {
        document.querySelectorAll('.pb-rich-text-editor').forEach(function (el) {
            if (el.dataset.quillReady === '1') return;
            var id = el.id;
            var field = document.getElementById(id + '_field');
            if (!field || typeof Quill === 'undefined') return;

            var quill = new Quill('#' + id, {
                theme: 'snow',
                placeholder: 'Type your content here…',
                modules: {
                    toolbar: [
                        [{ header: [1, 2, 3, false] }],
                        ['bold', 'italic', 'underline', 'strike'],
                        [{ color: [] }, { background: [] }],
                        [{ list: 'ordered' }, { list: 'bullet' }],
                        [{ align: [] }],
                        ['link'],
                        ['clean']
                    ]
                }
            });

            var initial = field.value || '';
            if (initial.trim() !== '') {
                quill.root.innerHTML = initial;
            }

            var sync = function () {
                var html = quill.root.innerHTML;
                if (html === '<p><br></p>' || html === '<p></p>') {
                    html = '';
                }
                field.value = html;
            };

            quill.on('text-change', sync);
            sync();

            var form = el.closest('form');
            if (form) {
                form.addEventListener('submit', function (e) {
                    sync();
                    if (!quill.getText().trim()) {
                        e.preventDefault();
                        alert('Please enter page content before saving.');
                        quill.focus();
                    }
                });
            }

            el.dataset.quillReady = '1';
        });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', window.PB.initRichTextEditors);
    } else {
        window.PB.initRichTextEditors();
    }
})();
</script>
@endpush
@endonce
