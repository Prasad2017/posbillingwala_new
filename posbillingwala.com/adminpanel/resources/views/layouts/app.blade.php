<!doctype html>
<html lang="en">

<head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!--favicon-->
    <link rel="icon" href="{{asset('assets/images/play-store-icon.png')}}" type="image/png" />
    <!--plugins-->
    <link href="{{asset('assets/plugins/simplebar/css/simplebar.css')}}" rel="stylesheet" />
    <link href="{{asset('assets/plugins/perfect-scrollbar/css/perfect-scrollbar.css')}}" rel="stylesheet" />
    <link href="{{asset('assets/plugins/metismenu/css/metisMenu.min.css')}}" rel="stylesheet" />
    <link href="{{asset('assets/plugins/datatable/css/dataTables.bootstrap5.min.css')}}" rel="stylesheet" />
    <link href="{{asset('assets/plugins/Drag-And-Drop/dist/imageuploadify.min.css')}}" rel="stylesheet" />
    <!-- loader-->
    <link href="{{asset('assets/css/pace.min.css')}}" rel="stylesheet" />
    <script src="{{asset('assets/js/pace.min.js')}}"></script>
    <!-- Bootstrap CSS -->
    <link href="{{asset('assets/css/bootstrap.min.css')}}" rel="stylesheet">
    <link href="{{asset('assets/css/bootstrap-extended.css')}}" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600;700&family=Syne:wght@600;700&display=swap" rel="stylesheet">
    <link href="{{asset('assets/css/app.css')}}" rel="stylesheet">
    <link href="{{asset('assets/css/icons.css')}}" rel="stylesheet">
    <!-- Theme Style CSS -->
    <link rel="stylesheet" href="{{asset('assets/css/dark-theme.css')}}" />
    <link rel="stylesheet" href="{{asset('assets/css/semi-dark.css')}}" />
    <link rel="stylesheet" href="{{asset('assets/css/header-colors.css')}}" />
    <link rel="stylesheet" href="{{asset('assets/css/pos-brand.css')}}" />
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.3/jquery.min.js"></script>

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
    <script src="{{asset('assets/js/bootstrap.bundle.min.js')}}"></script>
    <!--plugins-->
    <script src="{{asset('assets/js/jquery.min.js')}}"></script>
    <script src="{{asset('assets/plugins/simplebar/js/simplebar.min.js')}}"></script>
    <script src="{{asset('assets/plugins/metismenu/js/metisMenu.min.js')}}"></script>
    <script src="{{asset('assets/plugins/perfect-scrollbar/js/perfect-scrollbar.js')}}"></script>
    <script src="{{asset('assets/plugins/datatable/js/jquery.dataTables.min.js')}}"></script>
    <script src="{{asset('assets/plugins/datatable/js/dataTables.bootstrap5.min.js')}}"></script>
    <script>
        $(document).ready(function() {
            $('#example').DataTable();
          } );
    </script>
    <script>
        $(document).ready(function() {
            var table = $('#example2').DataTable( {
                lengthChange: false,
                buttons: [ 'copy', 'excel', 'pdf', 'print']
            } );
         
            table.buttons().container()
                .appendTo( '#example2_wrapper .col-md-6:eq(0)' );
        } );
    </script>
    <!--app JS-->
    <script src="{{asset('assets/js/app.js')}}"></script>
</body>

</html>