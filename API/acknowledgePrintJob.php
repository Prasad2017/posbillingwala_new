<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$job = pos_print_job_ack(
    $con,
    $licenceId,
    (int) pos_api_post('id'),
    pos_api_post('ackStatus', pos_api_post('status')),
    pos_api_post('errorMessage')
);
if ($job === null) {
    pos_api_json(array('status' => '0', 'message' => 'Print job not found'));
}
pos_api_json(array('status' => '1', 'printJob' => $job));
