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
