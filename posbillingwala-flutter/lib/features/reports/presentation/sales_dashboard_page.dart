import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_period_controls.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/// High-level sales dashboard / overview matching the refreshed reports UI.
class SalesDashboardPage extends ConsumerWidget {
  const SalesDashboardPage({super.key});

  double _pctChange(double current, double previous) {
    if (previous <= 0) return current > 0 ? 100 : 0;
    return ((current - previous) / previous) * 100;
  }

  Future<void> _pickPeriod(BuildContext context, WidgetRef ref) async {
    final period = ref.read(reportPeriodProvider);
    final selected = await showModalBottomSheet<ReportPeriodKind>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 8),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.border,
                borderRadius: BorderRadius.circular(999),
              ),
            ),
            for (final entry in const [
              (ReportPeriodKind.today, 'Today', Icons.today_rounded),
              (ReportPeriodKind.month, 'This Month', Icons.calendar_view_month),
              (ReportPeriodKind.day, 'Pick a Day', Icons.event_rounded),
              (ReportPeriodKind.year, 'This Year', Icons.calendar_today_rounded),
            ])
              ListTile(
                leading: Icon(entry.$3, color: AppColors.primary),
                title: Text(entry.$2),
                onTap: () => Navigator.pop(ctx, entry.$1),
              ),
          ],
        ),
      ),
    );
    if (selected == null || !context.mounted) return;
    if (selected == ReportPeriodKind.day) {
      final now = DateTime.now();
      final picked = await showDatePicker(
        context: context,
        initialDate: period.day ?? now,
        firstDate: DateTime(now.year - 2),
        lastDate: now,
      );
      if (picked != null) {
        ref.read(reportPeriodProvider.notifier).useDay(picked);
      }
      return;
    }
    if (selected == ReportPeriodKind.month) {
      await pickReportMonth(context, ref);
      return;
    }
    onReportPeriodSelected(ref, selected, period);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final period = ref.watch(reportPeriodProvider);
    final summary = ref.watch(periodSalesSummaryProvider);
    final invoicesAsync = ref.watch(filteredPeriodInvoicesProvider);
    final trendAsync = ref.watch(_last7DaysProvider);
    final shop =
        ref.watch(authControllerProvider).session?.shopName?.trim() ?? '';
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final dayLabel = DateFormat('dd');

    final periodLabel =
        '${reportPeriodDisplayLabel(period)} · ${shop.isEmpty ? 'Main Store' : shop}';

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).salesDashboard),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
          padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            12,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            32,
          ),
          children: [
          Align(
            alignment: Alignment.centerLeft,
            child: ReportPeriodPill(
              label: periodLabel,
              onTap: () => _pickPeriod(context, ref),
            ),
          ),
          const SizedBox(height: 14),
          trendAsync.when(
            data: (points) {
              final prev = points.length >= 2
                  ? points[points.length - 2].total
                  : 0.0;
              final salesPct = _pctChange(summary.totalSales, prev);
              final billsPrev = points.length >= 2 ? 1.0 : 0.0;
              final billsPct = _pctChange(
                summary.billCount.toDouble(),
                billsPrev,
              );
              return ReportKpiGrid(
                items: [
                  ReportKpiData(
                    label: 'Total Sales',
                    value: currency.format(summary.totalSales),
                    changePercent: salesPct,
                  ),
                  ReportKpiData(
                    label: 'Net Sales',
                    value: currency.format(
                      summary.totalSales - summary.discountTotal,
                    ),
                    changePercent: salesPct,
                  ),
                  ReportKpiData(
                    label: 'Total Bills',
                    value: '${summary.billCount}',
                    changePercent: billsPct,
                  ),
                  ReportKpiData(
                    label: 'Avg. Bill Value',
                    value: currency.format(summary.avgBill),
                    changePercent: salesPct,
                  ),
                ],
              );
            },
            loading: () => ReportKpiGrid(
              items: [
                ReportKpiData(
                  label: 'Total Sales',
                  value: currency.format(summary.totalSales),
                ),
                ReportKpiData(
                  label: 'Net Sales',
                  value: currency.format(
                    summary.totalSales - summary.discountTotal,
                  ),
                ),
                ReportKpiData(
                  label: 'Total Bills',
                  value: '${summary.billCount}',
                ),
                ReportKpiData(
                  label: 'Avg. Bill Value',
                  value: currency.format(summary.avgBill),
                ),
              ],
            ),
            error: (_, _) => ReportKpiGrid(
              items: [
                ReportKpiData(
                  label: 'Total Sales',
                  value: currency.format(summary.totalSales),
                ),
                ReportKpiData(
                  label: 'Net Sales',
                  value: currency.format(
                    summary.totalSales - summary.discountTotal,
                  ),
                ),
                ReportKpiData(
                  label: 'Total Bills',
                  value: '${summary.billCount}',
                ),
                ReportKpiData(
                  label: 'Avg. Bill Value',
                  value: currency.format(summary.avgBill),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          ReportSurfaceCard(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Sales Trend',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w800,
                    fontSize: 16,
                    color: AppColors.navy,
                  ),
                ),
                const SizedBox(height: 12),
                trendAsync.when(
                  data: (points) => ReportLineTrend(
                    values: points.map((e) => e.total).toList(),
                    labels: points.map((e) => dayLabel.format(e.date)).toList(),
                  ),
                  loading: () => const SizedBox(
                    height: 180,
                    child: Center(child: CircularProgressIndicator()),
                  ),
                  error: (e, _) => Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text('$e'),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          ReportSurfaceCard(
            padding: const EdgeInsets.fromLTRB(4, 12, 4, 4),
            child: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 0, 4, 4),
                  child: Row(
                    children: [
                      const Expanded(
                        child: Text(
                          'Recent Bills',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontWeight: FontWeight.w800,
                            fontSize: 16,
                            color: AppColors.navy,
                          ),
                        ),
                      ),
                      TextButton(
                        onPressed: () {
                          ref
                              .read(reportInvoiceTypeFilterProvider.notifier)
                              .select(ReportInvoiceTypeFilter.all);
                          ref
                              .read(reportPaymentFilterProvider.notifier)
                              .select(ReportPaymentFilter.all);
                          context.push('/reports/invoices');
                        },
                        child: Text(AppStrings.of(ref).viewAll),
                      ),
                    ],
                  ),
                ),
                Builder(
                  builder: (context) {
                    final invoices = invoicesAsync;
                    final recent = invoices.take(8).toList();
                    if (recent.isEmpty) {
                      return Padding(
                        padding: const EdgeInsets.all(24),
                        child: Text(AppStrings.of(ref).noBillsPeriod),
                      );
                    }
                    return Column(
                      children: [
                        for (var i = 0; i < recent.length; i++) ...[
                          if (i > 0)
                            Divider(
                              height: 1,
                              color: AppColors.border.withValues(alpha: .7),
                            ),
                          ReportInvoiceRow(
                            index: i + 1,
                            invoice: recent[i],
                            currency: currency,
                            showPaymentTag: true,
                            denseDate: true,
                            onTap: () => context.push(
                              '/reports/invoice/${recent[i].invoiceId}',
                            ),
                          ),
                        ],
                      ],
                    );
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          ReportDonutBreakdown(
            title: 'Top by Payment Mode',
            slices: paymentSlices(summary, invoicesAsync),
          ),
        ],
        ),
      ),
    );
  }
}

final _last7DaysProvider = FutureProvider<List<DailySalesPoint>>((ref) {
  // Rebuild when invoices change so chart updates after sync / new bills.
  ref.watch(todayInvoicesProvider);
  ref.watch(monthInvoicesProvider);
  return ref.watch(appDatabaseProvider).getDailySalesLastDays(7);
});
