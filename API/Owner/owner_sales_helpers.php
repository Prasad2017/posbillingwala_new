<?php
/**
 * Owner org sales queries — all branches or one branch (licenses.userId = owner).
 */
require_once __DIR__ . '/../branch_scope.php';
require_once __DIR__ . '/../invoice_sales_filter.php';

if (!function_exists('owner_sales_invoice_join')) {
    function owner_sales_invoice_join()
    {
        return 'INNER JOIN `licenses` l ON (i.`branch_id` = l.`id` OR (i.`branch_id` IS NULL AND i.`licenseId` = l.`id`))';
    }
}

if (!function_exists('owner_sales_scope_bind')) {
    /**
     * @return array{where:string, types:string, params:array}
     */
    function owner_sales_scope_bind($con, $ownerUserId, $branchId)
    {
        $where = 'l.`userId` = ?';
        $types = 's';
        $params = array((string) $ownerUserId);

        $branchId = trim((string) $branchId);
        if ($branchId !== '' && strtolower($branchId) !== 'all' && $branchId !== '0') {
            if (!branch_owner_owns_branch($con, $ownerUserId, $branchId)) {
                return null;
            }
            $where .= ' AND l.`id` = ?';
            $types .= 'i';
            $params[] = (int) $branchId;
        }

        return array('where' => $where, 'types' => $types, 'params' => $params);
    }
}

if (!function_exists('owner_sales_period_label')) {
    function owner_sales_period_label($branchId, $branchLabel, $baseLabel)
    {
        $branchId = trim((string) $branchId);
        if ($branchId === '' || strtolower($branchId) === 'all' || $branchId === '0') {
            return $baseLabel . ' · All Branches';
        }
        if ($branchLabel !== null && trim($branchLabel) !== '') {
            return $baseLabel . ' · ' . trim($branchLabel);
        }
        return $baseLabel . ' · Branch #' . $branchId;
    }
}

if (!function_exists('owner_sales_trend_pct')) {
    function owner_sales_trend_pct($current, $previous)
    {
        $c = (float) $current;
        $p = (float) $previous;
        if ($p == 0.0) {
            return $c > 0 ? '+100%' : '0%';
        }
        $v = round((($c - $p) / $p) * 100.0, 0);
        return ($v >= 0 ? '+' : '') . $v . '%';
    }
}
