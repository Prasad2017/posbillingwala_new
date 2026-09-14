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
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

final _overviewWindowProvider = StreamProvider<List<Invoice>>((ref) {
  final now = DateTime.now();
  final start = DateTime(now.year, now.month - 1, 1);
  final end = DateTime(now.year, now.month + 1, 1);
  return ref.watch(appDatabaseProvider).watchInvoicesInRange(start, end);
});

/// WithTable `SalesOverview` — monthly snapshot KPIs (not dashboard charts).
class SalesOverviewPage extends ConsumerWidget {
  const SalesOverviewPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final now = DateTime.now();
    final start = DateTime(now.year, now.month, 1);
    final end = DateTime(now.year, now.month + 1, 1);
    final prevStart = DateTime(now.year, now.month - 1, 1);
    final rows = ref.watch(_overviewWindowProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <Invoice>[],
        );
    final summary = SalesSummary.fromInvoices(
      rows
          .where((i) => !i.invoiceDate.isBefore(start) && i.invoiceDate.isBefore(end))
          .toList(),
    );
    final prevSummary = SalesSummary.fromInvoices(
      rows
          .where(
            (i) =>
                !i.invoiceDate.isBefore(prevStart) &&
                i.invoiceDate.isBefore(start),
          )
          .toList(),
    );
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final shop =
        ref.watch(authControllerProvider).session?.shopName?.trim() ?? '';

    double pct(double cur, double prev) {
      if (prev <= 0) return cur > 0 ? 100 : 0;
      return ((cur - prev) / prev) * 100;
    }

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).salesOverview),
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
            Text(
              '${DateFormat('MMMM yyyy').format(now)} · ${shop.isEmpty ? 'Main Store' : shop}',
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: 14),
            ReportKpiGrid(
              items: [
                ReportKpiData(
                  label: 'Total Sales',
                  value: currency.format(summary.totalSales),
                  changePercent: pct(summary.totalSales, prevSummary.totalSales),
                ),
                ReportKpiData(
                  label: 'Net Sales',
                  value: currency.format(
                    summary.totalSales - summary.discountTotal,
                  ),
                  changePercent: pct(
                    summary.totalSales - summary.discountTotal,
                    prevSummary.totalSales - prevSummary.discountTotal,
                  ),
                ),
                ReportKpiData(
                  label: 'Total Invoices',
                  value: '${summary.billCount}',
                  changePercent: pct(
                    summary.billCount.toDouble(),
                    prevSummary.billCount.toDouble(),
                  ),
                ),
                ReportKpiData(
                  label: 'Avg. Bill',
                  value: currency.format(summary.avgBill),
                  changePercent: pct(summary.avgBill, prevSummary.avgBill),
                ),
              ],
            ),
            const SizedBox(height: 16),
            ReportSurfaceCard(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    'This month by mode',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w800,
                      fontSize: 16,
                      color: AppColors.navy,
                    ),
                  ),
                  const SizedBox(height: 12),
                  _kv('Fast billing bills', '${summary.posCount}'),
                  _kv('Dine-in bills', '${summary.tableCount}'),
                  _kv('Takeaway bills', '${summary.takeawayCount}'),
                  const Divider(height: 24),
                  _kv('Cash', currency.format(summary.cashTotal)),
                  _kv('UPI', currency.format(summary.upiTotal)),
                ],
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: () => context.push('/reports/sales-list'),
              child: Text(AppStrings.of(ref).openSalesList),
            ),
          ],
        ),
      ),
    );
  }

  Widget _kv(String k, String v) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Expanded(
            child: Text(
              k,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          Text(v, style: const TextStyle(fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }
}
