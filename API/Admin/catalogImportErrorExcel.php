<?php
include_once('config.php');
require_once __DIR__ . '/../catalog/bootstrap.php';
require_once __DIR__ . '/../catalog/catalog_handlers.php';

$actor = catalog_require_actor($con, 'admin');

catalog_handle_error_excel($con, 'admin', (int) $actor['actor_id']);
mysqli_close($con);
