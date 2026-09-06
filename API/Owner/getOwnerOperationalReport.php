<?php
/**
 * Owner operational reports — one endpoint for all POS-style report types.
 * GET: userId, branchId (optional), reportType, date (optional YYYY-MM-DD or YYYY-MM)
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/owner_sales_helpers.php';

header('Content-Type: application/json; charset=utf-8');
date_default_timezone_set('Asia/Kolkata');

$response = array(
    'status' => 'false',
    'periodLabel' => '',
    'totalBills' => '0',
    'totalAmount' => '0',
    'avgBill' => '0',
    'extraKpiLabel' => '',
    'extraKpiValue' => '0',
    'breakdown' => array(),
    'items' => array(),
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

owner_require_auth($con);
mysqli_query($con, 'set names utf8');

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
$userId = auth_user_id_from_request($con, $userId, 'owner');
if ($userId === null || $userId === '') {
    $response['message'] = 'Invalid or expired auth token';
    echo json_encode($response);
    exit;
}

$branchId = isset($_GET['branchId']) ? trim($_GET['branchId']) : '';
$scope = owner_sales_scope_bind($con, $userId, $branchId);
if ($scope === null) {
    $response['message'] = 'Invalid branch';
    echo json_encode($response);
    exit;
}

$reportType = isset($_GET['reportType']) ? strtolower(trim($_GET['reportType'])) : 'invoice';
$dateRaw = isset($_GET['date']) ? trim($_GET['date']) : '';

$branchLabel = '';
if ($branchId !== '' && strtolower($branchId) !== 'all' && $branchId !== '0') {
    require_once __DIR__ . '/../licence_expiry.php';
    $lic = db_stmt_fetch_one($con, 'SELECT * FROM `licenses` WHERE `id`=? LIMIT 1', 'i', (int) $branchId);
    if ($lic !== null) {
        $bf = licence_branch_fields($lic);
        $branchLabel = $bf['branchLabel'];
    }
}

$dateFilter = '';
$dateTypes = '';
$dateParams = array();
$baseLabel = 'All time';
if ($dateRaw !== '') {
    if (preg_match('/^\d{4}-\d{2}-\d{2}$/', $dateRaw)) {
        $dateFilter = ' AND DATE(i.invoiceDate) = ?';
        $dateTypes = 's';
        $dateParams[] = $dateRaw;
        $baseLabel = $dateRaw;
    } elseif (preg_match('/^\d{4}-\d{2}$/', $dateRaw)) {
        $monthStart = $dateRaw . '-01';
        $monthEnd = date('Y-m-t', strtotime($monthStart));
        $dateFilter = ' AND DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?';
        $dateTypes = 'ss';
        $dateParams[] = $monthStart;
        $dateParams[] = $monthEnd;
        $baseLabel = $dateRaw;
    }
}

$response['periodLabel'] = owner_sales_period_label($branchId, $branchLabel, $baseLabel);
$join = owner_sales_invoice_join();

function owner_report_push_breakdown(&$out, $label, $amount, $count)
{
    $out[] = array(
        'label' => (string) $label,
        'amount' => number_format((float) $amount, 2, '.', ''),
        'count' => (string) ((int) $count),
    );
}

function owner_report_push_item(&$out, $title, $subtitle, $amount)
{
    $out[] = array(
        'title' => (string) $title,
        'subtitle' => (string) $subtitle,
        'amount' => number_format((float) $amount, 2, '.', ''),
    );
}

function owner_report_set_totals(&$response, $bills, $amount)
{
    $bills = (int) $bills;
    $amount = (float) $amount;
    $response['totalBills'] = (string) $bills;
    $response['totalAmount'] = number_format($amount, 2, '.', '');
    $response['avgBill'] = $bills > 0
        ? number_format($amount / $bills, 2, '.', '')
        : '0.00';
}

$types = $scope['types'] . $dateTypes;
$params = array_merge($scope['params'], $dateParams);

try {
    switch ($reportType) {
        case 'sale': {
            $sql = "SELECT LOWER(IFNULL(i.invoiceType,'')) AS channel,
                           COUNT(*) AS cnt,
                           COALESCE(SUM(i.totalAmount),0) AS amt
                    FROM `invoice` i $join
                    WHERE {$scope['where']} $dateFilter" . invoice_and_not_refunded() . "
                    GROUP BY LOWER(IFNULL(i.invoiceType,''))
                    ORDER BY amt DESC";
            $rows = db_stmt_fetch_all($con, $sql, $types, ...$params);
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            $amount = 0.0;
            foreach ($rows as $row) {
                $ch = $row['channel'];
                if ($ch === 'table_wise') {
                    $label = 'Dine-in';
                } elseif ($ch === 'take_away') {
                    $label = 'Take Away';
                } elseif ($ch === 'fast_billing' || $ch === 'fast') {
                    $label = 'Fast Billing';
                } elseif ($ch === 'mess') {
                    $label = 'Mess';
                } else {
                    $label = $ch !== '' ? $ch : 'Other';
                }
                owner_report_push_breakdown($response['breakdown'], $label, $row['amt'], $row['cnt']);
                owner_report_push_item($response['items'], $label, $row['cnt'] . ' bills', $row['amt']);
                $bills += (int) $row['cnt'];
                $amount += (float) $row['amt'];
            }
            owner_report_set_totals($response, $bills, $amount);
            $response['extraKpiLabel'] = 'Channels';
            $response['extraKpiValue'] = (string) count($response['breakdown']);
            break;
        }

        case 'table': {
            $sql = "SELECT IFNULL(i.noOfTable,'') AS tbl,
                           COUNT(*) AS cnt,
                           COALESCE(SUM(i.totalAmount),0) AS amt
                    FROM `invoice` i $join
                    WHERE {$scope['where']} $dateFilter
                      AND LOWER(IFNULL(i.invoiceType,'')) = 'table_wise'" . invoice_and_not_refunded() . "
                    GROUP BY IFNULL(i.noOfTable,'')
                    ORDER BY amt DESC
                    LIMIT 100";
            $rows = db_stmt_fetch_all($con, $sql, $types, ...$params);
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            $amount = 0.0;
            foreach ($rows as $row) {
                $label = $row['tbl'] !== '' ? ('Table ' . $row['tbl']) : 'Table';
                owner_report_push_breakdown($response['breakdown'], $label, $row['amt'], $row['cnt']);
                owner_report_push_item($response['items'], $label, $row['cnt'] . ' bills', $row['amt']);
                $bills += (int) $row['cnt'];
                $amount += (float) $row['amt'];
            }
            owner_report_set_totals($response, $bills, $amount);
            $response['extraKpiLabel'] = 'Tables';
            $response['extraKpiValue'] = (string) count($response['breakdown']);
            break;
        }

        case 'takeaway': {
            $sql = "SELECT IFNULL(i.noOfTable,'') AS parcel,
                           COUNT(*) AS cnt,
                           COALESCE(SUM(i.totalAmount),0) AS amt
                    FROM `invoice` i $join
                    WHERE {$scope['where']} $dateFilter
                      AND LOWER(IFNULL(i.invoiceType,'')) = 'take_away'" . invoice_and_not_refunded() . "
                    GROUP BY IFNULL(i.noOfTable,'')
                    ORDER BY amt DESC
                    LIMIT 100";
            $rows = db_stmt_fetch_all($con, $sql, $types, ...$params);
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            $amount = 0.0;
            foreach ($rows as $row) {
                $label = $row['parcel'] !== '' ? ('Parcel ' . $row['parcel']) : 'Parcel';
                owner_report_push_breakdown($response['breakdown'], $label, $row['amt'], $row['cnt']);
                owner_report_push_item($response['items'], $label, $row['cnt'] . ' bills', $row['amt']);
                $bills += (int) $row['cnt'];
                $amount += (float) $row['amt'];
            }
            owner_report_set_totals($response, $bills, $amount);
            $response['extraKpiLabel'] = 'Parcels';
            $response['extraKpiValue'] = (string) count($response['breakdown']);
            break;
        }

        case 'payment': {
            $sql = "SELECT IFNULL(NULLIF(TRIM(i.paymentMode),''),'Other') AS mode,
                           COUNT(*) AS cnt,
                           COALESCE(SUM(i.totalAmount),0) AS amt
                    FROM `invoice` i $join
                    WHERE {$scope['where']} $dateFilter" . invoice_and_not_refunded() . "
                    GROUP BY IFNULL(NULLIF(TRIM(i.paymentMode),''),'Other')
                    ORDER BY amt DESC";
            $rows = db_stmt_fetch_all($con, $sql, $types, ...$params);
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            $amount = 0.0;
            foreach ($rows as $row) {
                owner_report_push_breakdown($response['breakdown'], $row['mode'], $row['amt'], $row['cnt']);
                owner_report_push_item($response['items'], $row['mode'], $row['cnt'] . ' bills', $row['amt']);
                $bills += (int) $row['cnt'];
                $amount += (float) $row['amt'];
            }
            owner_report_set_totals($response, $bills, $amount);
            $response['extraKpiLabel'] = 'Modes';
            $response['extraKpiValue'] = (string) count($response['breakdown']);
            break;
        }

        case 'discount': {
            $sql = "SELECT i.invoiceNumber, i.customerName, i.discount, i.totalAmount, i.invoiceDate, i.paymentMode
                    FROM `invoice` i $join
                    WHERE {$scope['where']} $dateFilter
                      AND CAST(IFNULL(i.discount,0) AS DECIMAL(16,2)) > 0" . invoice_and_not_refunded() . "
                    ORDER BY i.invoiceDate DESC
                    LIMIT 200";
            $rows = db_stmt_fetch_all($con, $sql, $types, ...$params);
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            $amount = 0.0;
            $discountSum = 0.0;
            foreach ($rows as $row) {
                $title = $row['invoiceNumber'] !== '' ? $row['invoiceNumber'] : 'Invoice';
                $sub = trim($row['customerName'] . ' · Disc ' . number_format((float) $row['discount'], 2, '.', ''));
                owner_report_push_item($response['items'], $title, $sub, $row['totalAmount']);
                $bills++;
                $amount += (float) $row['totalAmount'];
                $discountSum += (float) $row['discount'];
            }
            if ($discountSum > 0) {
                owner_report_push_breakdown($response['breakdown'], 'Total discount', $discountSum, $bills);
            }
            owner_report_set_totals($response, $bills, $amount);
            $response['extraKpiLabel'] = 'Discount';
            $response['extraKpiValue'] = number_format($discountSum, 2, '.', '');
            break;
        }

        case 'refund': {
            $sql = "SELECT i.invoiceNumber, i.customerName, i.totalAmount, i.invoiceDate, i.paymentMode
                    FROM `invoice` i $join
                    WHERE {$scope['where']} $dateFilter
                      AND LOWER(IFNULL(i.invoiceOrderStatus,'')) = 'refunded'
                    ORDER BY i.invoiceDate DESC
                    LIMIT 200";
            $rows = db_stmt_fetch_all($con, $sql, $types, ...$params);
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            $amount = 0.0;
            foreach ($rows as $row) {
                $title = $row['invoiceNumber'] !== '' ? $row['invoiceNumber'] : 'Refund';
                $sub = trim($row['customerName'] . ' · ' . $row['paymentMode']);
                owner_report_push_item($response['items'], $title, $sub, $row['totalAmount']);
                $bills++;
                $amount += (float) $row['totalAmount'];
            }
            if ($amount > 0) {
                owner_report_push_breakdown($response['breakdown'], 'Refunded', $amount, $bills);
            }
            owner_report_set_totals($response, $bills, $amount);
            $response['extraKpiLabel'] = 'Refunds';
            $response['extraKpiValue'] = (string) $bills;
            break;
        }

        case 'product':
        case 'combo': {
            $itemType = ($reportType === 'combo') ? 'COMBO' : 'PRODUCT';
            $sql = "SELECT IFNULL(NULLIF(TRIM(p.productName),''),'Item') AS pname,
                           COALESCE(SUM(p.productQuantity),0) AS qty,
                           COALESCE(SUM(p.productPrice * p.productQuantity),0) AS amt
                    FROM `invoice_final_product` p
                    INNER JOIN `invoice` i ON i.invoiceNumber = p.invoiceNumber
                    $join
                    WHERE {$scope['where']} $dateFilter" . invoice_and_not_refunded() . "
                      AND UPPER(IFNULL(p.invoiceItemType,'PRODUCT')) = ?
                    GROUP BY IFNULL(NULLIF(TRIM(p.productName),''),'Item')
                    ORDER BY amt DESC
                    LIMIT 100";
            $pTypes = $types . 's';
            $pParams = array_merge($params, array($itemType));
            $rows = db_stmt_fetch_all($con, $sql, $pTypes, ...$pParams);
            if (!is_array($rows) || count($rows) === 0) {
                // Fallback when invoiceItemType column is missing on older DBs
                $sqlFb = "SELECT IFNULL(NULLIF(TRIM(p.productName),''),'Item') AS pname,
                                 COALESCE(SUM(p.productQuantity),0) AS qty,
                                 COALESCE(SUM(p.productPrice * p.productQuantity),0) AS amt
                          FROM `invoice_final_product` p
                          INNER JOIN `invoice` i ON i.invoiceNumber = p.invoiceNumber
                          $join
                          WHERE {$scope['where']} $dateFilter" . invoice_and_not_refunded() . "
                          GROUP BY IFNULL(NULLIF(TRIM(p.productName),''),'Item')
                          ORDER BY amt DESC
                          LIMIT 100";
                if ($reportType === 'product') {
                    $rows = db_stmt_fetch_all($con, $sqlFb, $types, ...$params);
                } else {
                    $rows = array();
                }
            }
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            $amount = 0.0;
            foreach ($rows as $row) {
                $qty = (float) $row['qty'];
                owner_report_push_breakdown($response['breakdown'], $row['pname'], $row['amt'], (int) round($qty));
                owner_report_push_item($response['items'], $row['pname'], number_format($qty, 2, '.', '') . ' qty', $row['amt']);
                $bills += (int) round($qty);
                $amount += (float) $row['amt'];
            }
            owner_report_set_totals($response, $bills, $amount);
            $response['extraKpiLabel'] = ($reportType === 'combo') ? 'Combos' : 'Products';
            $response['extraKpiValue'] = (string) count($response['breakdown']);
            break;
        }

        case 'expense': {
            $expJoin = 'INNER JOIN `licenses` l ON (e.`branch_id` = l.`id` OR (e.`branch_id` IS NULL AND e.`userId` = l.`id`))';
            $expDateFilter = '';
            $expTypes = $scope['types'];
            $expParams = $scope['params'];
            if ($dateRaw !== '') {
                if (preg_match('/^\d{4}-\d{2}-\d{2}$/', $dateRaw)) {
                    $expDateFilter = ' AND DATE(e.expensesDate) = ?';
                    $expTypes .= 's';
                    $expParams[] = $dateRaw;
                } elseif (preg_match('/^\d{4}-\d{2}$/', $dateRaw)) {
                    $monthStart = $dateRaw . '-01';
                    $monthEnd = date('Y-m-t', strtotime($monthStart));
                    $expDateFilter = ' AND DATE(e.expensesDate) >= ? AND DATE(e.expensesDate) <= ?';
                    $expTypes .= 'ss';
                    $expParams[] = $monthStart;
                    $expParams[] = $monthEnd;
                }
            }
            $sql = "SELECT IFNULL(NULLIF(TRIM(e.expensesName),''),'Expense') AS ename,
                           COUNT(*) AS cnt,
                           COALESCE(SUM(CAST(e.expensesAmount AS DECIMAL(16,2))),0) AS amt
                    FROM `expenses` e
                    $expJoin
                    WHERE {$scope['where']} $expDateFilter
                      AND IFNULL(e.expensesStatus,'1') <> '0'
                    GROUP BY IFNULL(NULLIF(TRIM(e.expensesName),''),'Expense')
                    ORDER BY amt DESC
                    LIMIT 100";
            $rows = db_stmt_fetch_all($con, $sql, $expTypes, ...$expParams);
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            $amount = 0.0;
            foreach ($rows as $row) {
                owner_report_push_breakdown($response['breakdown'], $row['ename'], $row['amt'], $row['cnt']);
                owner_report_push_item($response['items'], $row['ename'], $row['cnt'] . ' entries', $row['amt']);
                $bills += (int) $row['cnt'];
                $amount += (float) $row['amt'];
            }
            owner_report_set_totals($response, $bills, $amount);
            $response['extraKpiLabel'] = 'Categories';
            $response['extraKpiValue'] = (string) count($response['breakdown']);
            break;
        }

        case 'mess_member': {
            $mJoin = 'INNER JOIN `licenses` l ON (m.`branch_id` = l.`id` OR (m.`branch_id` IS NULL AND m.`userId` = l.`id`))';
            $sql = "SELECT IFNULL(NULLIF(TRIM(m.member_name),''),'Member') AS mname,
                           IFNULL(m.member_mobile_number,'') AS mobile,
                           COUNT(*) AS cnt
                    FROM `mess_member` m
                    $mJoin
                    WHERE {$scope['where']}
                      AND IFNULL(m.member_status,'1') <> '0'
                    GROUP BY m.id
                    ORDER BY mname ASC
                    LIMIT 200";
            $rows = db_stmt_fetch_all($con, $sql, $scope['types'], ...$scope['params']);
            if (!is_array($rows)) {
                $rows = array();
            }
            foreach ($rows as $row) {
                owner_report_push_item($response['items'], $row['mname'], $row['mobile'], 0);
            }
            owner_report_set_totals($response, count($rows), 0);
            $response['extraKpiLabel'] = 'Members';
            $response['extraKpiValue'] = (string) count($rows);
            if (count($rows) > 0) {
                owner_report_push_breakdown($response['breakdown'], 'Members', 0, count($rows));
            }
            break;
        }

        case 'mess': {
            $mJoin = 'INNER JOIN `licenses` l ON (m.`branch_id` = l.`id` OR (m.`branch_id` IS NULL AND m.`userId` = l.`id`))';
            $messDateFilter = '';
            $messTypes = $scope['types'];
            $messParams = $scope['params'];
            if ($dateRaw !== '') {
                if (preg_match('/^\d{4}-\d{2}-\d{2}$/', $dateRaw)) {
                    $messDateFilter = ' AND DATE(m.messInvoiceDate) = ?';
                    $messTypes .= 's';
                    $messParams[] = $dateRaw;
                } elseif (preg_match('/^\d{4}-\d{2}$/', $dateRaw)) {
                    $monthStart = $dateRaw . '-01';
                    $monthEnd = date('Y-m-t', strtotime($monthStart));
                    $messDateFilter = ' AND DATE(m.messInvoiceDate) >= ? AND DATE(m.messInvoiceDate) <= ?';
                    $messTypes .= 'ss';
                    $messParams[] = $monthStart;
                    $messParams[] = $monthEnd;
                }
            }
            $sql = "SELECT IFNULL(NULLIF(TRIM(m.messType),''),'Mess') AS mtype,
                           COUNT(*) AS cnt
                    FROM `mess_invoice` m
                    $mJoin
                    WHERE {$scope['where']} $messDateFilter
                      AND IFNULL(m.messInvoiceStatus,'1') <> '0'
                    GROUP BY IFNULL(NULLIF(TRIM(m.messType),''),'Mess')
                    ORDER BY cnt DESC";
            $rows = db_stmt_fetch_all($con, $sql, $messTypes, ...$messParams);
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            foreach ($rows as $row) {
                owner_report_push_breakdown($response['breakdown'], $row['mtype'], 0, $row['cnt']);
                owner_report_push_item($response['items'], $row['mtype'], $row['cnt'] . ' invoices', 0);
                $bills += (int) $row['cnt'];
            }
            owner_report_set_totals($response, $bills, 0);
            $response['extraKpiLabel'] = 'Types';
            $response['extraKpiValue'] = (string) count($response['breakdown']);
            break;
        }

        case 'invoice':
        default: {
            $sql = "SELECT DATE(i.invoiceDate) AS d,
                           COUNT(*) AS cnt,
                           COALESCE(SUM(i.totalAmount),0) AS amt
                    FROM `invoice` i $join
                    WHERE {$scope['where']} $dateFilter" . invoice_and_not_refunded() . "
                    GROUP BY DATE(i.invoiceDate)
                    ORDER BY d DESC
                    LIMIT 60";
            $rows = db_stmt_fetch_all($con, $sql, $types, ...$params);
            if (!is_array($rows)) {
                $rows = array();
            }
            $bills = 0;
            $amount = 0.0;
            foreach ($rows as $row) {
                $label = $row['d'];
                owner_report_push_breakdown($response['breakdown'], $label, $row['amt'], $row['cnt']);
                owner_report_push_item($response['items'], $label, $row['cnt'] . ' bills', $row['amt']);
                $bills += (int) $row['cnt'];
                $amount += (float) $row['amt'];
            }
            owner_report_set_totals($response, $bills, $amount);
            $response['extraKpiLabel'] = 'Days';
            $response['extraKpiValue'] = (string) count($response['breakdown']);
            break;
        }
    }

    $response['status'] = 'true';
} catch (Exception $e) {
    $response['status'] = 'false';
    $response['message'] = 'Report query failed';
}

echo json_encode($response);
mysqli_close($con);
