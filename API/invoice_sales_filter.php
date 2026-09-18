<?php
/**
 * Exclude refunded bills from sales totals.
 */
if (!function_exists('invoice_not_refunded_sql')) {
    function invoice_not_refunded_sql($alias = 'i')
    {
        if ($alias === null || $alias === '') {
            return "IFNULL(`invoiceOrderStatus`,'completed') <> 'refunded'";
        }
        return "IFNULL(" . $alias . ".invoiceOrderStatus,'completed') <> 'refunded'";
    }
}

if (!function_exists('invoice_and_not_refunded')) {
    function invoice_and_not_refunded($alias = 'i')
    {
        return ' AND ' . invoice_not_refunded_sql($alias);
    }
}

/**
 * Staff row filter — ONLY when client asks with staffScope=1.
 * Sync / full licence downloads omit staffScope so all licence data is returned.
 * Reports / home UI send staffScope=1 when a staff user is logged in.
 *
 * @return string SQL fragment starting with AND, or empty string
 */
if (!function_exists('invoice_and_staff_scope')) {
    function invoice_and_staff_scope($con, $alias = '')
    {
        $flag = '';
        if (isset($_REQUEST['staffScope'])) {
            $flag = strtolower(trim((string) $_REQUEST['staffScope']));
        }
        if ($flag !== '1' && $flag !== 'true' && $flag !== 'yes') {
            return '';
        }
        if (!function_exists('pos_posted_staff_id')) {
            require_once __DIR__ . '/pos_schema.php';
        }
        $staffId = (int) pos_posted_staff_id();
        if ($staffId <= 0) {
            return '';
        }
        $col = ($alias === null || $alias === '')
            ? '`createdByStaffId`'
            : $alias . '.`createdByStaffId`';
        return ' AND ' . $col . '=' . $staffId;
    }
}
