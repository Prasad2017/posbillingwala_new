<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$docType = strtoupper(pos_api_post('documentType', 'KOT'));
$perm = $docType === 'BILL' ? 'bill.print' : 'kot.print';
$actor = pos_require_permission($con, $licenceId, $perm);

$job = pos_print_job_create($con, $licenceId, array(
    'printerId' => (int) pos_api_post('printerId'),
    'staffId' => isset($actor['id']) ? $actor['id'] : 0,
    'deviceId' => pos_api_post('android_device_id', pos_api_post('deviceId')),
    'documentType' => $docType,
    'documentId' => pos_api_post('documentId'),
    'payload' => pos_api_post('payload'),
    'idempotencyKey' => pos_api_post('idempotencyKey'),
    'priority' => (int) pos_api_post('priority', '0'),
));
if ($job === null) {
    pos_api_json(array('status' => '0', 'message' => 'Printer not found or disabled'));
}
pos_api_json(array('status' => '1', 'printJob' => $job));
