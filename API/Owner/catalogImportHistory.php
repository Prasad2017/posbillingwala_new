<?php
include_once('config.php');
require_once __DIR__ . '/../catalog/bootstrap.php';
require_once __DIR__ . '/../catalog/catalog_handlers.php';

$actor = catalog_require_actor($con, 'owner');

catalog_handle_history($con, 'owner', (int) $actor['actor_id']);
mysqli_close($con);
