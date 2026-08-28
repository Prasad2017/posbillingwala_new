<?php

namespace App\Services;

use Carbon\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

class AdminMetrics
{
    public static function today(): Carbon
    {
        return Carbon::now('Asia/Kolkata')->startOfDay();
    }

    public static function rupee($amount): string
    {
        return '₹ ' . number_format((float) $amount, 2);
    }

    public static function pctChange($current, $previous): string
    {
        $c = (float) $current;
        $p = (float) $previous;
        if ($p == 0.0) {
            return $c > 0 ? '100.0' : '0.0';
        }
        return (string) round((($c - $p) / $p) * 100.0, 1);
    }

    public static function trendLabel($pct, $short = false): string
    {
        $v = (float) $pct;
        $arrow = $v >= 0 ? '↑' : '↓';
        $text = $arrow . ' ' . number_format(abs($v), 1) . '%';
        return $short ? $text : $text . ' vs last month';
    }

    public static function signedPct($current, $previous): string
    {
        $c = (float) $current;
        $p = (float) $previous;
        if ($p == 0.0) {
            return $c > 0 ? '+100%' : '0%';
        }
        $v = round((($c - $p) / $p) * 100, 1);
        return ($v >= 0 ? '+' : '') . $v . '%';
    }

    private static function expiredStatuses(): array
    {
        return ['expire', 'expired', 'suspended', 'revoked'];
    }

    private static function validExpirySql($alias = 'l'): string
    {
        return "({$alias}.expiryDate IS NOT NULL AND {$alias}.expiryDate <> '' AND {$alias}.expiryDate <> '0000-00-00')";
    }

    private static function licenseActiveSql($alias = 'l'): string
    {
        return "LOWER(IFNULL({$alias}.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
            AND ({$alias}.expiryDate IS NULL OR {$alias}.expiryDate = '' OR {$alias}.expiryDate = '0000-00-00' OR {$alias}.expiryDate >= ?)";
    }

    private static function licenseExpiredSql($alias = 'l'): string
    {
        return "(LOWER(IFNULL({$alias}.licenseStatus,'')) IN ('expire','expired')
            OR (" . self::validExpirySql($alias) . " AND {$alias}.expiryDate < ?))";
    }

    private static function countOne(string $sql, array $params = []): int
    {
        try {
            $row = DB::selectOne($sql, $params);

            return (int) ($row->c ?? 0);
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics query failed: ' . $e->getMessage());

            return 0;
        }
    }

    private static function deviceCount(): int
    {
        if (!Schema::hasColumn('licenses', 'android_device_id')) {
            return 0;
        }

        try {
            return (int) DB::table('licenses')
                ->whereNotNull('android_device_id')
                ->whereRaw("TRIM(android_device_id) <> ''")
                ->count();
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics device count failed: ' . $e->getMessage());

            return 0;
        }
    }

    public static function onlineMinutes(): int
    {
        return 15;
    }

    public static function pickLastSeen($lastLoginAt, $tokenLastUsedAt): ?string
    {
        $best = null;
        foreach ([$lastLoginAt, $tokenLastUsedAt] as $ts) {
            if (!$ts || $ts === '0000-00-00 00:00:00') {
                continue;
            }
            if ($best === null || strtotime((string) $ts) > strtotime((string) $best)) {
                $best = (string) $ts;
            }
        }

        return $best;
    }

    public static function connectionStatus($lastSeenAt): string
    {
        if (!$lastSeenAt || $lastSeenAt === '0000-00-00 00:00:00') {
            return 'OFFLINE';
        }
        $ts = strtotime((string) $lastSeenAt);

        return ($ts !== false && $ts >= (time() - (self::onlineMinutes() * 60))) ? 'ONLINE' : 'OFFLINE';
    }

    public static function formatLastSeen($lastSeenAt): string
    {
        if (!$lastSeenAt || $lastSeenAt === '0000-00-00 00:00:00') {
            return 'Never';
        }
        $ts = strtotime((string) $lastSeenAt);
        if ($ts === false) {
            return 'Never';
        }
        $diff = time() - $ts;
        if ($diff < 60) {
            return 'Just now';
        }
        if ($diff < 3600) {
            return (int) floor($diff / 60) . ' min ago';
        }
        if ($diff < 86400) {
            return (int) floor($diff / 3600) . ' hr ago';
        }
        if ($diff < 604800) {
            return (int) floor($diff / 86400) . ' day(s) ago';
        }

        return date('d M Y, h:i A', $ts);
    }

    private static function tokenLastUsedSubquery(string $alias = 'l'): string
    {
        return "(SELECT MAX(t.last_used_at) FROM api_tokens t
                 WHERE t.actor_type='pos_licence' AND t.actor_id={$alias}.id
                   AND (t.device_id={$alias}.android_device_id OR t.device_id IS NULL OR TRIM(t.device_id)=''))";
    }

    private static function canTrackPresence(): bool
    {
        return Schema::hasTable('api_tokens')
            && Schema::hasColumn('api_tokens', 'last_used_at')
            && Schema::hasColumn('api_tokens', 'actor_type')
            && Schema::hasColumn('api_tokens', 'actor_id');
    }

    private static function countOnlineDevices(): int
    {
        if (!Schema::hasColumn('licenses', 'android_device_id')) {
            return 0;
        }

        try {
            if (!self::canTrackPresence()) {
                if (!Schema::hasColumn('licenses', 'lastLoginAt')) {
                    return 0;
                }
                $mins = self::onlineMinutes();

                return self::countOne(
                    "SELECT COUNT(*) AS c FROM licenses l
                     WHERE l.android_device_id IS NOT NULL AND TRIM(l.android_device_id)<>''
                       AND l.lastLoginAt IS NOT NULL
                       AND l.lastLoginAt >= DATE_SUB(NOW(), INTERVAL {$mins} MINUTE)"
                );
            }

            $mins = self::onlineMinutes();
            $loginExpr = Schema::hasColumn('licenses', 'lastLoginAt') ? 'l.lastLoginAt' : 'NULL';

            return self::countOne(
                "SELECT COUNT(*) AS c FROM licenses l
                 WHERE l.android_device_id IS NOT NULL AND TRIM(l.android_device_id)<>''
                   AND GREATEST(
                     COALESCE({$loginExpr}, '1970-01-01 00:00:00'),
                     COALESCE(" . self::tokenLastUsedSubquery('l') . ", '1970-01-01 00:00:00')
                   ) >= DATE_SUB(NOW(), INTERVAL {$mins} MINUTE)"
            );
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics countOnlineDevices failed: ' . $e->getMessage());

            return 0;
        }
    }

    public static function enrichDeviceRow($row): object
    {
        $lastLogin = Schema::hasColumn('licenses', 'lastLoginAt') ? ($row->lastLoginAt ?? null) : null;
        $tokenLast = $row->tokenLastUsedAt ?? null;
        $lastSeen = self::pickLastSeen($lastLogin, $tokenLast);
        $row->lastSeenAt = $lastSeen;
        $row->lastSeenLabel = self::formatLastSeen($lastSeen);
        $row->connectionStatus = self::connectionStatus($lastSeen);
        $row->lastLoginAt = ($lastLogin && $lastLogin !== '0000-00-00 00:00:00') ? $lastLogin : null;
        $row->licenseDisplayStatus = self::licenseDisplayStatus($row);

        return $row;
    }

    private static function trialSql($alias = 'l'): string
    {
        return "({$alias}.licenseType IN ('Demo','Trial') OR {$alias}.licenseValidity='7')";
    }

    public static function dashboard(): array
    {
        $today = self::today()->toDateString();
        $yesterday = self::today()->copy()->subDay()->toDateString();
        $tomorrow = self::today()->copy()->addDay()->toDateString();
        $in30 = self::today()->copy()->addDays(30)->toDateString();
        $in7 = self::today()->copy()->addDays(7)->toDateString();
        $monthStart = self::today()->copy()->startOfMonth()->toDateString();
        $lastMonthStart = self::today()->copy()->subMonth()->startOfMonth()->toDateString();
        $lastMonthEnd = self::today()->copy()->subMonth()->endOfMonth()->toDateString();
        $sparkStart = self::today()->copy()->subDays(6)->toDateString();

        $totalCustomer = (int) DB::table('users')->where('role_id', 3)->count();
        $totalDealer = (int) DB::table('users')->where('role_id', 2)->count();

        $activeCustomer = self::countOne(
            "SELECT COUNT(DISTINCT u.id) AS c FROM users u
             INNER JOIN licenses l ON l.userId = u.id
             WHERE u.role_id='3' AND u.is_active='1'
               AND " . self::licenseActiveSql('l'),
            [$today]
        );

        $trialCustomer = self::countOne(
            "SELECT COUNT(DISTINCT u.id) AS c FROM users u
             INNER JOIN licenses l ON l.userId = u.id
             WHERE u.role_id='3'
               AND " . self::trialSql('l') . "
               AND " . self::licenseActiveSql('l'),
            [$today]
        );

        $expiredCustomer = self::countOne(
            "SELECT COUNT(DISTINCT u.id) AS c FROM users u
             WHERE u.role_id='3'
               AND NOT EXISTS (
                 SELECT 1 FROM licenses l
                 WHERE l.userId = u.id
                   AND " . self::licenseActiveSql('l') . "
               )",
            [$today]
        );

        $activeLicenses = self::countOne(
            "SELECT COUNT(*) AS c FROM licenses WHERE " . self::licenseActiveSql(),
            [$today]
        );

        $expiringLicenses = self::countOne(
            "SELECT COUNT(*) AS c FROM licenses
             WHERE LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
               AND expiryDate >= ? AND expiryDate <= ?",
            [$today, $in30]
        );

        $expiredLicenses = self::countOne(
            "SELECT COUNT(*) AS c FROM licenses
             WHERE LOWER(IFNULL(licenseStatus,'')) IN ('expire','expired')
                OR expiryDate < ?",
            [$today]
        );

        $totalBranches = (int) DB::table('licenses')->count();
        $totalDevices = self::deviceCount();

        $trialLicenses = self::countOne(
            "SELECT COUNT(*) AS c FROM licenses
             WHERE " . self::trialSql() . " AND " . self::licenseActiveSql(),
            [$today]
        );

        $expiringLicenses7Days = self::countOne(
            "SELECT COUNT(*) AS c FROM licenses
             WHERE LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
               AND expiryDate >= ? AND expiryDate <= ?",
            [$today, $in7]
        );

        $trialLicensesExpiringTomorrow = self::countOne(
            "SELECT COUNT(*) AS c FROM licenses
             WHERE " . self::trialSql() . "
               AND LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
               AND expiryDate = ?",
            [$tomorrow]
        );

        $customersAddedThisMonth = (int) DB::table('users')->where('role_id', 3)->where('created_at', '>=', $monthStart)->count();
        $customersAddedLastMonth = (int) DB::table('users')->where('role_id', 3)
            ->where('created_at', '>=', $lastMonthStart)->where('created_at', '<', $monthStart)->count();
        $customersBeforeThisMonth = (int) DB::table('users')->where('role_id', 3)
            ->where(function ($q) use ($monthStart) {
                $q->whereNull('created_at')->orWhere('created_at', '<', $monthStart);
            })->count();

        $branchesThisMonth = (int) DB::table('licenses')->where('created_at', '>=', $monthStart)->count();
        $branchesLastMonth = (int) DB::table('licenses')
            ->where('created_at', '>=', $lastMonthStart)->where('created_at', '<', $monthStart)->count();

        $monthSales = self::salesSum($monthStart, $today);
        $lastMonthSales = self::salesSum($lastMonthStart, $lastMonthEnd);
        $allTimeSales = self::salesSum(null, null);
        $todayRow = self::salesRow($today, $today);
        $yesterdaySales = self::salesSum($yesterday, $yesterday);

        $sparkMap = [];
        foreach (self::salesByDay($sparkStart, $today) as $row) {
            $sparkMap[$row->d] = (float) $row->total;
        }
        $salesSparkline = [];
        for ($i = 6; $i >= 0; $i--) {
            $d = Carbon::now('Asia/Kolkata')->subDays($i)->toDateString();
            $salesSparkline[] = round($sparkMap[$d] ?? 0, 2);
        }

        $netSalesTrend = self::pctChange($monthSales, $lastMonthSales);
        $todaySalesTrend = self::pctChange($todayRow['total'], $yesterdaySales);
        $customersAddedTrend = self::pctChange($customersAddedThisMonth, $customersAddedLastMonth);
        $totalCustomerTrend = self::pctChange($customersAddedThisMonth, $customersBeforeThisMonth);
        $activeBranchesTrend = self::pctChange($branchesThisMonth, $branchesLastMonth);

        return [
            'totalCustomer' => $totalCustomer,
            'totalDealer' => $totalDealer,
            'activeCustomer' => $activeCustomer,
            'trialCustomer' => $trialCustomer,
            'expiredCustomer' => $expiredCustomer,
            'activeLicenses' => $activeLicenses,
            'expiringLicenses' => $expiringLicenses,
            'expiredLicenses' => $expiredLicenses,
            'totalBranches' => $totalBranches,
            'totalDevices' => $totalDevices,
            'trialLicenses' => $trialLicenses,
            'expiringLicenses7Days' => $expiringLicenses7Days,
            'trialLicensesExpiringTomorrow' => $trialLicensesExpiringTomorrow,
            'customersAddedThisMonth' => $customersAddedThisMonth,
            'netSales' => $monthSales,
            'monthSales' => $monthSales,
            'lastMonthSales' => $lastMonthSales,
            'allTimeSales' => $allTimeSales,
            'todaySales' => $todayRow['total'],
            'billCount' => $todayRow['bills'],
            'salesSparkline' => $salesSparkline,
            'netSalesTrend' => $netSalesTrend,
            'todaySalesTrend' => $todaySalesTrend,
            'customersAddedTrend' => $customersAddedTrend,
            'activeBranchesTrend' => $activeBranchesTrend,
            'totalCustomerTrend' => $totalCustomerTrend,
            'netSalesTrendLabel' => self::trendLabel($netSalesTrend, true),
            'todaySalesTrendLabel' => self::trendLabel($todaySalesTrend, true),
            'customersAddedTrendLabel' => self::trendLabel($customersAddedTrend, true),
            'activeBranchesTrendLabel' => self::trendLabel($activeBranchesTrend, true),
            'totalCustomerTrendLabel' => self::trendLabel($totalCustomerTrend),
            'activeCustomerTrendLabel' => self::trendLabel($totalCustomerTrend),
            'trialCustomerTrendLabel' => self::trendLabel($customersAddedTrend),
            'expiredCustomerTrendLabel' => self::trendLabel($expiredCustomer),
            'activeLicensesTrendLabel' => self::trendLabel($activeBranchesTrend),
            'expiringLicensesTrendLabel' => self::trendLabel($expiringLicenses),
            'trialLicensesTrendLabel' => self::trendLabel($customersAddedTrend),
            'expiredLicensesTrendLabel' => self::trendLabel($expiredLicenses),
            'notificationCount' => $expiringLicenses7Days + $expiredLicenses + $trialLicensesExpiringTomorrow,
            'greeting' => self::greeting(),
            'todayLabel' => Carbon::now('Asia/Kolkata')->format('d M Y'),
        ];
    }

    public static function greeting(): string
    {
        $hour = (int) Carbon::now('Asia/Kolkata')->format('G');
        if ($hour < 12) {
            return 'Good Morning';
        }
        if ($hour < 17) {
            return 'Good Afternoon';
        }
        return 'Good Evening';
    }

    public static function salesSum(?string $from, ?string $to): float
    {
        try {
            $sql = "SELECT COALESCE(SUM(i.totalAmount),0) AS total
                    FROM invoice i
                    INNER JOIN licenses l ON l.id = i.licenseId
                    INNER JOIN users u ON u.id = l.userId AND u.role_id='3'";
            $params = [];
            if ($from && $to) {
                $sql .= " WHERE DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?";
                $params = [$from, $to];
            } elseif ($from) {
                $sql .= " WHERE DATE(i.invoiceDate) >= ?";
                $params = [$from];
            }

            return round((float) (DB::selectOne($sql, $params)->total ?? 0), 2);
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics salesSum failed: ' . $e->getMessage());

            return 0.0;
        }
    }

    public static function salesRow(string $from, string $to): array
    {
        try {
            $row = DB::selectOne(
                "SELECT COALESCE(SUM(i.totalAmount),0) AS total, COUNT(*) AS bills
                 FROM invoice i
                 INNER JOIN licenses l ON l.id = i.licenseId
                 INNER JOIN users u ON u.id = l.userId AND u.role_id='3'
                 WHERE DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?",
                [$from, $to]
            );

            return [
                'total' => round((float) ($row->total ?? 0), 2),
                'bills' => (int) ($row->bills ?? 0),
            ];
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics salesRow failed: ' . $e->getMessage());

            return ['total' => 0.0, 'bills' => 0];
        }
    }

    public static function salesByDay(string $from, string $to): array
    {
        return DB::select(
            "SELECT DATE(i.invoiceDate) AS d, COALESCE(SUM(i.totalAmount),0) AS total
             FROM invoice i
             INNER JOIN licenses l ON l.id = i.licenseId
             INNER JOIN users u ON u.id = l.userId AND u.role_id='3'
             WHERE DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?
             GROUP BY DATE(i.invoiceDate)
             ORDER BY d ASC",
            [$from, $to]
        );
    }

    public static function dealerSales(int $limit = 10, string $period = 'month'): array
    {
        $today = self::today()->toDateString();
        $monthStart = self::today()->copy()->startOfMonth()->toDateString();
        $limit = max(1, min(50, $limit));

        if ($period === 'all') {
            $rows = DB::select(
                "SELECT d.id AS dealerId, d.name AS dealerName,
                        COUNT(DISTINCT c.id) AS totalCustomer,
                        COUNT(DISTINCT CASE WHEN " . self::licenseActiveSql('l') . " THEN l.id END) AS activeLicenses,
                        COALESCE(SUM(i.totalAmount), 0) AS totalSales
                 FROM users d
                 LEFT JOIN users c ON c.dealerId = d.id AND c.role_id = '3'
                 LEFT JOIN licenses l ON l.userId = c.id
                 LEFT JOIN invoice i ON i.licenseId = l.id
                 WHERE d.role_id = '2'
                 GROUP BY d.id, d.name
                 ORDER BY totalSales DESC, totalCustomer DESC
                 LIMIT {$limit}",
                [$today]
            );
        } else {
            $rows = DB::select(
                "SELECT d.id AS dealerId, d.name AS dealerName,
                        COUNT(DISTINCT c.id) AS totalCustomer,
                        COUNT(DISTINCT CASE WHEN " . self::licenseActiveSql('l') . " THEN l.id END) AS activeLicenses,
                        COALESCE(SUM(CASE WHEN i.invoiceDate IS NOT NULL AND DATE(i.invoiceDate) >= ? THEN i.totalAmount ELSE 0 END), 0) AS totalSales
                 FROM users d
                 LEFT JOIN users c ON c.dealerId = d.id AND c.role_id = '3'
                 LEFT JOIN licenses l ON l.userId = c.id
                 LEFT JOIN invoice i ON i.licenseId = l.id
                 WHERE d.role_id = '2'
                 GROUP BY d.id, d.name
                 ORDER BY totalSales DESC, totalCustomer DESC
                 LIMIT {$limit}",
                [$today, $monthStart]
            );
        }

        $list = [];
        $grand = 0.0;
        foreach ($rows as $row) {
            $sales = round((float) $row->totalSales, 2);
            $grand += $sales;
            $list[] = [
                'dealerId' => $row->dealerId,
                'dealerName' => $row->dealerName ?: 'Dealer',
                'totalCustomer' => (int) $row->totalCustomer,
                'activeLicenses' => (int) $row->activeLicenses,
                'totalSales' => $sales,
            ];
        }
        return ['dealers' => $list, 'totalSales' => round($grand, 2)];
    }

    public static function recentCustomers(int $limit = 5): array
    {
        $today = self::today()->toDateString();
        return DB::select(
            "SELECT u.id, u.name, u.shopName, u.address, u.contact_number, u.created_at,
                    l.licenseStatus, l.licenseType, l.expiryDate, l.licenseKey
             FROM users u
             LEFT JOIN licenses l ON l.userId = u.id AND l.userType = 'owner'
             WHERE u.role_id = 3
             ORDER BY u.id DESC
             LIMIT " . (int) $limit
        );
    }

    public static function licenseDisplayStatus($license): string
    {
        if (!$license) {
            return 'Pending';
        }
        $status = strtolower((string) ($license->licenseStatus ?? ''));
        $expiry = (string) ($license->expiryDate ?? '');
        $today = self::today()->toDateString();
        if (in_array($status, self::expiredStatuses(), true) || ($expiry !== '' && $expiry < $today)) {
            return 'Expired';
        }
        $type = (string) ($license->licenseType ?? '');
        if (in_array($type, ['Demo', 'Trial'], true) || (string) ($license->licenseValidity ?? '') === '7') {
            return 'Trial';
        }
        return 'Active';
    }

    public static function salesDashboard(): array
    {
        $today = self::today()->toDateString();
        $yesterday = self::today()->copy()->subDay()->toDateString();
        $weekStart = self::today()->copy()->subDays(6)->toDateString();
        $cur = self::salesRow($today, $today);
        $prev = self::salesRow($yesterday, $yesterday);
        $avg = $cur['bills'] > 0 ? round($cur['total'] / $cur['bills'], 2) : 0;
        $pAvg = $prev['bills'] > 0 ? $prev['total'] / $prev['bills'] : 0;

        $trend = [];
        foreach (self::salesByDay($weekStart, $today) as $row) {
            $trend[] = ['date' => $row->d, 'total' => round((float) $row->total, 2)];
        }

        return [
            'periodLabel' => 'Today, ' . Carbon::now('Asia/Kolkata')->format('d M Y'),
            'totalSales' => $cur['total'],
            'netSales' => $cur['total'],
            'totalInvoices' => $cur['bills'],
            'avgBill' => $avg,
            'totalSalesTrend' => self::signedPct($cur['total'], $prev['total']),
            'netSalesTrend' => self::signedPct($cur['total'], $prev['total']),
            'invoicesTrend' => self::signedPct($cur['bills'], $prev['bills']),
            'avgBillTrend' => self::signedPct($avg, $pAvg),
            'salesTrend' => $trend,
            'recentInvoices' => self::recentInvoices(8),
        ];
    }

    public static function salesOverview(?string $month = null): array
    {
        if (!$month || !preg_match('/^\d{4}-\d{2}$/', $month)) {
            $month = Carbon::now('Asia/Kolkata')->format('Y-m');
        }
        $monthStart = $month . '-01';
        $monthEnd = Carbon::parse($monthStart, 'Asia/Kolkata')->endOfMonth()->toDateString();
        $prevStart = Carbon::parse($monthStart, 'Asia/Kolkata')->subMonth()->startOfMonth()->toDateString();
        $prevEnd = Carbon::parse($monthStart, 'Asia/Kolkata')->subMonth()->endOfMonth()->toDateString();

        $cur = self::salesRow($monthStart, $monthEnd);
        $prev = self::salesRow($prevStart, $prevEnd);
        $avg = $cur['bills'] > 0 ? round($cur['total'] / $cur['bills'], 2) : 0;
        $pAvg = $prev['bills'] > 0 ? $prev['total'] / $prev['bills'] : 0;

        $trend = [];
        foreach (self::salesByDay($monthStart, $monthEnd) as $row) {
            $trend[] = ['date' => $row->d, 'total' => round((float) $row->total, 2)];
        }

        $top = DB::select(
            "SELECT u.id AS customerId, u.name AS customerName, u.shopName,
                    COALESCE(SUM(i.totalAmount),0) AS totalSales
             FROM invoice i
             INNER JOIN licenses l ON l.id = i.licenseId
             INNER JOIN users u ON u.id = l.userId AND u.role_id='3'
             WHERE DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?
             GROUP BY u.id, u.name, u.shopName
             ORDER BY totalSales DESC
             LIMIT 8",
            [$monthStart, $monthEnd]
        );

        return [
            'month' => $month,
            'periodLabel' => 'This Month (' . Carbon::parse($monthStart)->format('d') . ' - ' . Carbon::parse($monthEnd)->format('d M Y') . ')',
            'totalSales' => $cur['total'],
            'netSales' => $cur['total'],
            'totalInvoices' => $cur['bills'],
            'avgBill' => $avg,
            'totalSalesTrend' => self::signedPct($cur['total'], $prev['total']) . ' vs last month',
            'netSalesTrend' => self::signedPct($cur['total'], $prev['total']) . ' vs last month',
            'invoicesTrend' => self::signedPct($cur['bills'], $prev['bills']) . ' vs last month',
            'avgBillTrend' => self::signedPct($avg, $pAvg) . ' vs last month',
            'salesTrend' => $trend,
            'topCustomers' => $top,
        ];
    }

    public static function recentInvoices(int $limit = 50, string $q = ''): array
    {
        $limit = max(1, min(200, $limit));
        $sql = "SELECT i.invoiceId, i.invoiceNumber, i.invoiceDate, i.totalAmount, i.paymentMode,
                       u.id AS customerId, u.name AS customerName, u.shopName
                FROM invoice i
                INNER JOIN licenses l ON l.id=i.licenseId
                INNER JOIN users u ON u.id=l.userId AND u.role_id='3'";
        $params = [];
        if ($q !== '') {
            $sql .= " WHERE i.invoiceNumber LIKE ? OR u.name LIKE ? OR u.shopName LIKE ?";
            $like = '%' . $q . '%';
            $params = [$like, $like, $like];
        }
        $sql .= " ORDER BY i.invoiceId DESC LIMIT {$limit}";
        return DB::select($sql, $params);
    }

    public static function customerReport(): array
    {
        $empty = [
            'totalCustomer' => 0,
            'activeCustomer' => 0,
            'trialCustomer' => 0,
            'expiredCustomer' => 0,
            'activePercent' => 0,
            'trialPercent' => 0,
            'expiredPercent' => 0,
            'growthBars' => [],
        ];

        try {
            $today = self::today()->toDateString();
            $total = (int) DB::table('users')->where('role_id', 3)->count();
            $active = self::countOne(
                "SELECT COUNT(DISTINCT u.id) AS c FROM users u
                 INNER JOIN licenses l ON l.userId=u.id
                 WHERE u.role_id='3'
                   AND " . self::licenseActiveSql('l') . "
                   AND NOT " . self::trialSql('l'),
                [$today]
            );
            $trial = self::countOne(
                "SELECT COUNT(DISTINCT u.id) AS c FROM users u
                 INNER JOIN licenses l ON l.userId=u.id
                 WHERE u.role_id='3'
                   AND " . self::trialSql('l') . "
                   AND " . self::licenseActiveSql('l'),
                [$today]
            );
            $expired = max(0, $total - $active - $trial);
            $pct = function ($n) use ($total) {
                return $total > 0 ? round(($n / $total) * 100, 1) : 0;
            };
            $growth = [];
            for ($i = 6; $i >= 0; $i--) {
                $d = Carbon::now('Asia/Kolkata')->subDays($i)->toDateString();
                $c = Schema::hasColumn('users', 'created_at')
                    ? (int) DB::table('users')->where('role_id', 3)->whereDate('created_at', $d)->count()
                    : 0;
                $growth[] = ['label' => Carbon::parse($d)->format('d M'), 'count' => $c];
            }

            return [
                'totalCustomer' => $total,
                'activeCustomer' => $active,
                'trialCustomer' => $trial,
                'expiredCustomer' => $expired,
                'activePercent' => $pct($active),
                'trialPercent' => $pct($trial),
                'expiredPercent' => $pct($expired),
                'growthBars' => $growth,
            ];
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics customerReport failed: ' . $e->getMessage());

            return $empty;
        }
    }

    public static function licenseReport(): array
    {
        $empty = [
            'activeLicenses' => 0,
            'trialLicenses' => 0,
            'expiringLicenses' => 0,
            'expiredLicenses' => 0,
            'totalLicenses' => 0,
            'activePercent' => 0,
            'trialPercent' => 0,
            'expiringPercent' => 0,
            'expiredPercent' => 0,
            'expiryWindows' => [],
        ];

        try {
            $today = self::today()->toDateString();
            $in30 = self::today()->copy()->addDays(30)->toDateString();
            $active = self::countOne(
                "SELECT COUNT(*) AS c FROM licenses l
                 WHERE " . self::licenseActiveSql('l') . " AND NOT " . self::trialSql('l'),
                [$today]
            );
            $trial = self::countOne(
                "SELECT COUNT(*) AS c FROM licenses l
                 WHERE " . self::trialSql('l') . " AND " . self::licenseActiveSql('l'),
                [$today]
            );
            $expiring = self::countOne(
                "SELECT COUNT(*) AS c FROM licenses l
                 WHERE LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
                   AND " . self::validExpirySql('l') . "
                   AND l.expiryDate>=? AND l.expiryDate<=?",
                [$today, $in30]
            );
            $expired = self::countOne(
                "SELECT COUNT(*) AS c FROM licenses l WHERE " . self::licenseExpiredSql('l'),
                [$today]
            );
            $total = max(1, $active + $trial + $expiring + $expired);
            $windows = [];
            foreach ([[0, 7, 'Next 7 days'], [8, 15, '8 - 15 days'], [16, 30, '16 - 30 days']] as $b) {
                $from = self::today()->copy()->addDays($b[0])->toDateString();
                $to = self::today()->copy()->addDays($b[1])->toDateString();
                $c = self::countOne(
                    "SELECT COUNT(*) AS c FROM licenses
                     WHERE expiryDate IS NOT NULL AND expiryDate <> '' AND expiryDate <> '0000-00-00'
                       AND expiryDate>=? AND expiryDate<=?
                       AND LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')",
                    [$from, $to]
                );
                $windows[] = [
                    'label' => $b[2] . ' (' . Carbon::parse($from)->format('d M') . ' - ' . Carbon::parse($to)->format('d M Y') . ')',
                    'count' => $c,
                ];
            }

            return [
                'activeLicenses' => $active,
                'trialLicenses' => $trial,
                'expiringLicenses' => $expiring,
                'expiredLicenses' => $expired,
                'totalLicenses' => $active + $trial + $expiring + $expired,
                'activePercent' => round(($active / $total) * 100, 1),
                'trialPercent' => round(($trial / $total) * 100, 1),
                'expiringPercent' => round(($expiring / $total) * 100, 1),
                'expiredPercent' => round(($expired / $total) * 100, 1),
                'expiryWindows' => $windows,
            ];
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics licenseReport failed: ' . $e->getMessage());

            return $empty;
        }
    }

    public static function branchReport(): array
    {
        $empty = [
            'totalBranches' => 0,
            'activeBranches' => 0,
            'inactiveBranches' => 0,
            'newBranches' => 0,
            'activePercent' => 0,
            'inactivePercent' => 0,
            'newPercent' => 0,
            'topCustomers' => [],
        ];

        try {
            $today = self::today()->toDateString();
            $monthStart = self::today()->copy()->startOfMonth()->toDateString();
            $total = (int) DB::table('licenses')->count();
            $active = self::countOne(
                "SELECT COUNT(*) AS c FROM licenses WHERE " . self::licenseActiveSql(),
                [$today]
            );
            $inactive = max(0, $total - $active);
            $newThisMonth = 0;
            if (Schema::hasColumn('licenses', 'created_at')) {
                $newThisMonth = (int) DB::table('licenses')->where('created_at', '>=', $monthStart)->count();
            }
            $t = max(1, $total);
            $top = DB::select(
                "SELECT u.id AS customerId, u.name AS customerName, u.shopName, COUNT(l.id) AS branchCount
                 FROM users u
                 INNER JOIN licenses l ON l.userId=u.id
                 WHERE u.role_id='3'
                 GROUP BY u.id, u.name, u.shopName
                 ORDER BY branchCount DESC
                 LIMIT 8"
            );

            return [
                'totalBranches' => $total,
                'activeBranches' => $active,
                'inactiveBranches' => $inactive,
                'newBranches' => $newThisMonth,
                'activePercent' => round(($active / $t) * 100, 1),
                'inactivePercent' => round(($inactive / $t) * 100, 1),
                'newPercent' => round(($newThisMonth / $t) * 100, 1),
                'topCustomers' => $top,
            ];
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics branchReport failed: ' . $e->getMessage());

            return $empty;
        }
    }

    public static function deviceReport(): array
    {
        if (!Schema::hasColumn('licenses', 'android_device_id')) {
            return [
                'totalDevices' => 0,
                'activeDevices' => 0,
                'inactiveDevices' => 0,
                'notUsedDevices' => 0,
                'onlineDevices' => 0,
                'offlineDevices' => 0,
                'activePercent' => 0,
                'inactivePercent' => 0,
                'notUsedPercent' => 0,
                'onlinePercent' => 0,
                'topCustomers' => [],
            ];
        }

        try {
            $today = self::today()->toDateString();
            $total = (int) DB::table('licenses')->whereNotNull('android_device_id')->whereRaw("TRIM(android_device_id)<>''")->count();
            $active = self::countOne(
                "SELECT COUNT(*) AS c FROM licenses
                 WHERE android_device_id IS NOT NULL AND TRIM(android_device_id)<>''
                   AND " . self::licenseActiveSql(),
                [$today]
            );
            $inactive = max(0, $total - $active);
            $notUsed = self::countOne(
                "SELECT COUNT(*) AS c FROM licenses
                 WHERE android_device_id IS NOT NULL AND TRIM(android_device_id)<>''
                   AND (LOWER(IFNULL(licenseStatus,'')) IN ('expire','expired')
                        OR (expiryDate IS NOT NULL AND expiryDate <> '' AND expiryDate <> '0000-00-00'
                            AND expiryDate < DATE_SUB(?, INTERVAL 30 DAY)))",
                [$today]
            );
            $online = self::countOnlineDevices();
            $t = max(1, $total);
            $top = DB::select(
                "SELECT u.id AS customerId, u.name AS customerName, u.shopName, COUNT(l.id) AS deviceCount
                 FROM users u
                 INNER JOIN licenses l ON l.userId=u.id
                 WHERE u.role_id='3' AND l.android_device_id IS NOT NULL AND TRIM(l.android_device_id)<>''
                 GROUP BY u.id, u.name, u.shopName
                 ORDER BY deviceCount DESC
                 LIMIT 8"
            );

            return [
                'totalDevices' => $total,
                'activeDevices' => $active,
                'inactiveDevices' => $inactive,
                'notUsedDevices' => $notUsed,
                'onlineDevices' => $online,
                'offlineDevices' => max(0, $total - $online),
                'activePercent' => round(($active / $t) * 100, 1),
                'inactivePercent' => round(($inactive / $t) * 100, 1),
                'notUsedPercent' => round(($notUsed / $t) * 100, 1),
                'onlinePercent' => round(($online / $t) * 100, 1),
                'topCustomers' => $top,
            ];
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics deviceReport failed: ' . $e->getMessage());

            return [
                'totalDevices' => 0,
                'activeDevices' => 0,
                'inactiveDevices' => 0,
                'notUsedDevices' => 0,
                'onlineDevices' => 0,
                'offlineDevices' => 0,
                'activePercent' => 0,
                'inactivePercent' => 0,
                'notUsedPercent' => 0,
                'onlinePercent' => 0,
                'topCustomers' => [],
            ];
        }
    }

    public static function devices(?int $customerId = null): array
    {
        try {
            $loginCol = Schema::hasColumn('licenses', 'lastLoginAt') ? 'l.lastLoginAt,' : 'NULL AS lastLoginAt,';
            $tokenSub = self::canTrackPresence()
                ? self::tokenLastUsedSubquery('l') . ' AS tokenLastUsedAt'
                : 'NULL AS tokenLastUsedAt';
            $sql = "SELECT l.*, u.shopName, u.name AS ownerName, u.contact_number,
                           {$loginCol}
                           {$tokenSub}
                    FROM licenses l
                    INNER JOIN users u ON u.id = l.userId
                    WHERE u.role_id='3'
                      AND l.android_device_id IS NOT NULL AND TRIM(l.android_device_id) <> ''";
            $params = [];
            if ($customerId) {
                $sql .= ' AND u.id=?';
                $params[] = $customerId;
            }
            $sql .= ' ORDER BY l.id DESC LIMIT 200';
            $rows = DB::select($sql, $params);

            return array_map(function ($row) {
                return self::enrichDeviceRow($row);
            }, $rows);
        } catch (\Throwable $e) {
            \Log::warning('AdminMetrics devices failed: ' . $e->getMessage());

            return self::devicesFallback($customerId);
        }
    }

    private static function devicesFallback(?int $customerId = null): array
    {
        try {
            $sql = "SELECT l.*, u.shopName, u.name AS ownerName, u.contact_number
                    FROM licenses l
                    INNER JOIN users u ON u.id = l.userId
                    WHERE u.role_id='3'
                      AND l.android_device_id IS NOT NULL AND TRIM(l.android_device_id) <> ''";
            $params = [];
            if ($customerId) {
                $sql .= ' AND u.id=?';
                $params[] = $customerId;
            }
            $sql .= ' ORDER BY l.id DESC LIMIT 200';
            $rows = DB::select($sql, $params);

            return array_map(function ($row) {
                $row->lastSeenAt = null;
                $row->lastSeenLabel = 'Never';
                $row->connectionStatus = 'OFFLINE';
                $row->lastLoginAt = null;
                $row->licenseDisplayStatus = self::licenseDisplayStatus($row);

                return $row;
            }, $rows);
        } catch (\Throwable $e) {
            \Log::error('AdminMetrics devicesFallback failed: ' . $e->getMessage());

            return [];
        }
    }

    public static function crashAnalytics(): array
    {
        if (!Schema::hasTable('admin_crash_logs')) {
            return ['totalCrashes' => 0, 'byApp' => [], 'overTime' => [], 'topErrors' => []];
        }
        $total = (int) DB::table('admin_crash_logs')->count();
        $byApp = [];
        foreach (DB::select("SELECT app_name, COUNT(*) AS c FROM admin_crash_logs GROUP BY app_name") as $r) {
            $byApp[] = [
                'label' => $r->app_name,
                'count' => (int) $r->c,
                'percent' => $total > 0 ? round(($r->c / $total) * 100, 1) : 0,
            ];
        }
        $overTime = [];
        for ($i = 6; $i >= 0; $i--) {
            $d = Carbon::now('Asia/Kolkata')->subDays($i)->toDateString();
            $c = (int) DB::table('admin_crash_logs')->whereRaw('DATE(created_at)=?', [$d])->count();
            $overTime[] = ['date' => $d, 'total' => $c];
        }
        $topErrors = [];
        foreach (DB::select("SELECT error_title, SUM(occurrences) AS c FROM admin_crash_logs GROUP BY error_title ORDER BY c DESC LIMIT 5") as $r) {
            $topErrors[] = [
                'label' => $r->error_title,
                'count' => (int) $r->c,
                'percent' => $total > 0 ? round(($r->c / max(1, $total)) * 100, 1) : 0,
            ];
        }
        return [
            'totalCrashes' => $total,
            'periodLabel' => Carbon::now('Asia/Kolkata')->subDays(6)->format('d M Y') . ' - ' . Carbon::now('Asia/Kolkata')->format('d M Y'),
            'byApp' => $byApp,
            'overTime' => $overTime,
            'topErrors' => $topErrors,
        ];
    }

    public static function hasErrorLogs(): bool
    {
        return Schema::hasTable('error_logs');
    }
}
