<!doctype html>
<html lang="en">

<head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <!--favicon-->
    <link rel="icon" href="{{ $adminFaviconUrl ?? admin_asset('images/app_logo.png') }}" type="image/png" />
    <!--plugins-->
    <link href="{{admin_asset('plugins/simplebar/css/simplebar.css')}}" rel="stylesheet" />
    <link href="{{admin_asset('plugins/perfect-scrollbar/css/perfect-scrollbar.css')}}" rel="stylesheet" />
    <link href="{{admin_asset('plugins/metismenu/css/metisMenu.min.css')}}" rel="stylesheet" />
    <link href="{{admin_asset('plugins/datatable/css/dataTables.bootstrap5.min.css')}}" rel="stylesheet" />
    <link href="{{admin_asset('plugins/Drag-And-Drop/dist/imageuploadify.min.css')}}" rel="stylesheet" />
    <link href="{{admin_asset('plugins/select2/css/select2.min.css')}}" rel="stylesheet" />
    <link href="{{admin_asset('plugins/select2/css/select2-bootstrap4.css')}}" rel="stylesheet" />
    <!-- loader-->
    <link href="{{admin_asset('css/pace.min.css')}}" rel="stylesheet" />
    <script src="{{admin_asset('js/pace.min.js')}}"></script>
    <!-- Bootstrap CSS -->
    <link href="{{admin_asset('css/bootstrap.min.css')}}" rel="stylesheet">
    <link href="{{admin_asset('css/bootstrap-extended.css')}}" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600;700&family=Syne:wght@600;700&display=swap" rel="stylesheet">
    <link href="{{admin_asset('css/app.css')}}" rel="stylesheet">
    <link href="{{admin_asset('css/icons.css')}}" rel="stylesheet">
    <!-- Theme Style CSS -->
    <link rel="stylesheet" href="{{admin_asset('css/dark-theme.css')}}" />
    <link rel="stylesheet" href="{{admin_asset('css/semi-dark.css')}}" />
    <link rel="stylesheet" href="{{admin_asset('css/header-colors.css')}}" />
    <link rel="stylesheet" href="{{ admin_asset('css/pos-brand.css') }}" />
    <link rel="stylesheet" href="{{ admin_asset('css/pos-responsive.css') }}" />
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.3/jquery.min.js"></script>
    <script src="{{admin_asset('plugins/chartjs/js/Chart.min.js')}}"></script>
    <script>
    window.PB = {
        colors: ['#2563eb', '#16a34a', '#ea580c', '#7c3aed', '#0b1f33', '#149687', '#dc2626'],
        chartDonut: {
            customers: ['#16a34a', '#ea580c', '#dc2626'],
            licenses: ['#16a34a', '#2563eb', '#ea580c', '#dc2626'],
            devices: ['#16a34a', '#ea580c', '#dc2626'],
            branches: ['#16a34a', '#ea580c', '#7c3aed'],
            online: ['#16a34a', '#94a3b8'],
            today: ['#2563eb', '#16a34a']
        },
        donut: function (id, labels, values, colors) {
            var el = document.getElementById(id);
            if (!el || typeof Chart === 'undefined') return;
            colors = colors || this.colors;
            var data = (values || []).map(function (v) { return Number(v) || 0; });
            var has = data.some(function (v) { return v > 0; });
            new Chart(el.getContext('2d'), {
                type: 'doughnut',
                data: {
                    labels: has ? labels : ['No data'],
                    datasets: [{
                        data: has ? data : [1],
                        backgroundColor: has ? colors : ['#e5e7eb'],
                        borderWidth: 3,
                        borderColor: '#ffffff'
                    }]
                },
                options: {
                    cutoutPercentage: 62,
                    maintainAspectRatio: false,
                    legend: { position: 'bottom', labels: { boxWidth: 12, padding: 10, fontSize: 11 } },
                    tooltips: { enabled: has }
                }
            });
        },
        line: function (id, labels, values, color, fillColor) {
            var el = document.getElementById(id);
            if (!el || typeof Chart === 'undefined') return;
            color = color || '#2563eb';
            fillColor = fillColor || 'rgba(37, 99, 235, 0.14)';
            new Chart(el.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        data: (values || []).map(function (v) { return Number(v) || 0; }),
                        borderColor: color,
                        backgroundColor: fillColor,
                        fill: true,
                        lineTension: 0.35,
                        borderWidth: 2,
                        pointRadius: 3,
                        pointBackgroundColor: color
                    }]
                },
                options: {
                    maintainAspectRatio: false,
                    legend: { display: false },
                    scales: {
                        xAxes: [{ gridLines: { display: false } }],
                        yAxes: [{ ticks: { beginAtZero: true }, gridLines: { color: 'rgba(11,31,51,0.06)' } }]
                    }
                }
            });
        },
        bar: function (id, labels, values, color) {
            var el = document.getElementById(id);
            if (!el || typeof Chart === 'undefined') return;
            color = color || '#2563eb';
            new Chart(el.getContext('2d'), {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        data: (values || []).map(function (v) { return Number(v) || 0; }),
                        backgroundColor: color
                    }]
                },
                options: {
                    maintainAspectRatio: false,
                    legend: { display: false },
                    scales: {
                        xAxes: [{ gridLines: { display: false } }],
                        yAxes: [{ ticks: { beginAtZero: true }, gridLines: { color: 'rgba(11,31,51,0.06)' } }]
                    }
                }
            });
        }
    };
    PB.emptyStateHtml = function (opts) {
        opts = opts || {};
        var title = opts.title || 'No data found';
        var subtitle = opts.subtitle || 'There is nothing to show here at the moment.';
        var compact = opts.compact ? ' pb-empty-state--compact' : '';
        var img = opts.image || @json(admin_asset('images/empty-state.svg'));
        var action = '';
        if (opts.actionUrl && opts.actionLabel) {
            action = '<a href="' + opts.actionUrl + '" class="btn btn-primary btn-sm pb-empty-action">' + opts.actionLabel + '</a>';
        }
        return '<div class="pb-empty-state' + compact + '">' +
            '<img src="' + img + '" alt="" class="pb-empty-image" loading="lazy">' +
            '<h6 class="pb-empty-title">' + title + '</h6>' +
            '<p class="pb-empty-subtitle">' + subtitle + '</p>' + action + '</div>';
    };
    PB.renderTableEmpty = function (api, opts) {
        if (!api.rows({ filter: 'applied' }).count()) {
            var cols = api.columns(':visible').count() || api.columns().count();
            $(api.table().body()).html(
                '<tr class="pb-empty-row"><td colspan="' + cols + '">' +
                PB.emptyStateHtml(opts || {}) + '</td></tr>'
            );
        }
    };
    PB.initForms = function (root) {
        var $root = root ? $(root) : $(document);
        $root.find('input.form-control:not([type=checkbox]):not([type=radio]):not([type=file]):not([readonly]), textarea.form-control')
            .addClass('pb-field');
        $root.find('select.pb-select-search').each(function () {
            var $el = $(this);
            if ($el.data('select2') || !$.fn.select2) {
                return;
            }
            var $group = $el.closest('.input-group');
            var placeholder = $el.attr('data-placeholder')
                || $el.find('option[value=""]').first().text()
                || 'Select…';
            $el.select2({
                theme: 'bootstrap4',
                width: '100%',
                placeholder: placeholder,
                allowClear: $el.find('option[value=""]').length > 0,
                dropdownParent: $group.length ? $group.parent() : $('body')
            });
        });
        $root.find('.pb-filter-auto').each(function () {
            var $el = $(this);
            if ($el.data('pbAutoSubmit')) {
                return;
            }
            $el.data('pbAutoSubmit', true);
            var submitForm = function () {
                var form = $el.closest('form')[0];
                if (form) {
                    form.submit();
                }
            };
            if ($el.is('select') && $el.hasClass('pb-select-search')) {
                $el.on('select2:select select2:clear', submitForm);
            } else {
                $el.on('change', submitForm);
            }
        });
    };
    </script>

    <title>POS Billingwala — Admin</title>
</head>

<body>
    <!--wrapper-->
    <div class="wrapper">
        <!--sidebar wrapper -->
        @include('layouts.sidebar')
        <!--end sidebar wrapper -->
        <!--start header -->
        @include('layouts.header')
        <!--end header -->
        <!--start page wrapper -->
        @yield('content')
        <!--end page wrapper -->
        <!--start overlay-->
        <div class="overlay toggle-icon"></div>
        <!--end overlay-->
        <!--Start Back To Top Button--> <a href="javaScript:;" class="back-to-top"><i class='bx bxs-up-arrow-alt'></i></a>
        <!--End Back To Top Button-->
    </div>
    <!--end wrapper-->
   
    <!-- Bootstrap JS -->
    <script src="{{admin_asset('js/bootstrap.bundle.min.js')}}"></script>
    <!--plugins-->
    <script src="{{admin_asset('js/jquery.min.js')}}"></script>
    <script src="{{admin_asset('plugins/simplebar/js/simplebar.min.js')}}"></script>
    <script src="{{admin_asset('plugins/metismenu/js/metisMenu.min.js')}}"></script>
    <script src="{{admin_asset('plugins/perfect-scrollbar/js/perfect-scrollbar.js')}}"></script>
    <script src="{{admin_asset('plugins/datatable/js/jquery.dataTables.min.js')}}"></script>
    <script src="{{admin_asset('plugins/datatable/js/dataTables.bootstrap5.min.js')}}"></script>
    <script>
        $(function () {
            if (!$.fn.dataTable) {
                return;
            }
            var emptyImg = @json(admin_asset('images/empty-state.svg'));
            $.extend(true, $.fn.dataTable.defaults, {
                language: {
                    emptyTable: '',
                    zeroRecords: ''
                },
                drawCallback: function (settings) {
                    var api = new $.fn.dataTable.Api(settings);
                    var custom = settings.oInit.pbEmpty || {};
                    if (!api.rows({ filter: 'applied' }).count()) {
                        var isSearch = (api.search() || '').length > 0;
                        PB.renderTableEmpty(api, {
                            title: isSearch ? (custom.searchTitle || 'No matching records') : (custom.title || 'No records found'),
                            subtitle: isSearch ? (custom.searchSubtitle || 'Try a different search term or clear the filter.') : (custom.subtitle || 'Nothing has been added yet.'),
                            image: emptyImg,
                            compact: true,
                            actionUrl: isSearch ? '' : (custom.actionUrl || ''),
                            actionLabel: isSearch ? '' : (custom.actionLabel || '')
                        });
                    }
                }
            });
        });
    </script>
    <script>
        $(document).ready(function() {
            if (!$('#example2').length || !$.fn.DataTable) {
                return;
            }
            var table = $('#example2').DataTable( {
                lengthChange: false,
                buttons: [ 'copy', 'excel', 'pdf', 'print']
            } );
            if (table.buttons) {
                table.buttons().container()
                    .appendTo( '#example2_wrapper .col-md-6:eq(0)' );
            }
        } );
    </script>
    <!--app JS-->
    <script src="{{admin_asset('plugins/select2/js/select2.min.js')}}"></script>
    <script src="{{ admin_asset('js/app.js') }}"></script>
    <script>
        $(function () {
            PB.initForms();
        });
    </script>
    @stack('scripts')
</body>

</html>