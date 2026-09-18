import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/reports/domain/report_export.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_period_controls.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class ReportsPage extends ConsumerWidget {
  const ReportsPage({super.key});

  Future<void> showPeriodMenu(BuildContext context, WidgetRef ref) async {
    final period = ref.read(reportPeriodProvider);
    final selected = await showMenu<ReportPeriodKind>(
      context: context,
      position: const RelativeRect.fromLTRB(1000, 80, 16, 0),
      color: AppColors.primary,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      items: [
        for (final entry in const [
          (ReportPeriodKind.today, 'Day wise'),
          (ReportPeriodKind.month, 'Month Wise'),
          (ReportPeriodKind.year, 'Year Wise'),
        ])
          PopupMenuItem(
            value: entry.$1,
            child: Row(
              children: [
                const Icon(
                  Icons.calendar_month_rounded,
                  color: Colors.white,
                  size: 18,
                ),
                const SizedBox(width: 10),
                Text(
                  entry.$2,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
      ],
    );
    if (selected == null) return;
    if (selected == ReportPeriodKind.month) {
      if (!context.mounted) return;
      await pickReportMonth(context, ref);
      return;
    }
    onReportPeriodSelected(ref, selected, period);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final period = ref.watch(reportPeriodProvider);
    final paymentFilter = ref.watch(reportPaymentFilterProvider);
    final invoicesAsync = ref.watch(periodInvoicesProvider);
    final filtered = ref.watch(filteredPeriodInvoicesProvider);
    final summary = ref.watch(periodSalesSummaryProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final slices = billingTypeSlices(filtered);
    final categories = slices.where((s) => s.value > 0).length;

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).invoiceReports),
        actions: [
          IconButton(
            tooltip: 'Export CSV',
            onPressed: filtered.isEmpty
                ? null
                : () => shareInvoicesCsv(
                    invoices: filtered,
                    title: 'Invoices — ${period.label}',
                  ),
            icon: const Icon(Icons.ios_share_rounded),
          ),
          IconButton(
            tooltip: 'Filter period',
            onPressed: () => showPeriodMenu(context, ref),
            icon: const Icon(Icons.filter_list_rounded),
          ),
        ],
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
          padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            12,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            28,
          ),
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: ReportPeriodPill(
                label:
                    period.kind == ReportPeriodKind.today &&
                        period.label == 'Today'
                    ? 'All Records'
                    : reportPeriodDisplayLabel(period),
                onTap: () => showPeriodMenu(context, ref),
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              height: 40,
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: [
                  for (final entry in const [
                    (ReportPaymentFilter.all, 'All'),
                    (ReportPaymentFilter.cash, 'Cash'),
                    (ReportPaymentFilter.upi, 'UPI'),
                    (ReportPaymentFilter.cashPlusUpi, 'Cash+UPI'),
                  ])
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: FilterChip(
                        label: Text(entry.$2),
                        selected: paymentFilter == entry.$1,
                        onSelected: (_) => ref
                            .read(reportPaymentFilterProvider.notifier)
                            .select(entry.$1),
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            ReportKpiGrid(
              items: [
                ReportKpiData(
                  label: 'Total Bills',
                  value: '${summary.billCount}',
                ),
                ReportKpiData(
                  label: 'TOTAL AMOUNT',
                  value: currency.format(summary.totalSales),
                ),
                ReportKpiData(
                  label: 'Avg. Bill Value',
                  value: currency.format(summary.avgBill),
                ),
                ReportKpiData(label: 'Categories', value: '$categories'),
              ],
            ),
            const SizedBox(height: 14),
            ReportDonutBreakdown(
              title: 'Billing Wise Details',
              slices: slices,
              centerValue: currency.format(summary.totalSales),
            ),
            const SizedBox(height: 14),
            ReportSurfaceCard(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Amount Breakdown',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w800,
                      fontSize: 16,
                      color: AppColors.navy,
                    ),
                  ),
                  const SizedBox(height: 12),
                  AmountBars(slices: slices.where((s) => s.value > 0).toList()),
                ],
              ),
            ),
            const SizedBox(height: 14),
            ReportSurfaceCard(
              padding: const EdgeInsets.fromLTRB(4, 12, 4, 4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Padding(
                    padding: EdgeInsets.fromLTRB(12, 0, 12, 8),
                    child: Text(
                      'Invoice Sale',
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontWeight: FontWeight.w800,
                        fontSize: 16,
                        color: AppColors.navy,
                      ),
                    ),
                  ),
                  invoicesAsync.when(
                    data: (_) {
                      if (filtered.isEmpty) {
                        return Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(
                            child: Text(AppStrings.of(ref).noBillsPeriod),
                          ),
                        );
                      }
                      final rows = filtered.take(40).toList();
                      return Column(
                        children: [
                          for (var i = 0; i < rows.length; i++) ...[
                            if (i > 0)
                              Divider(
                                height: 1,
                                color: AppColors.border.withValues(alpha: .7),
                              ),
                            ReportInvoiceRow(
                              index: i + 1,
                              invoice: rows[i],
                              currency: currency,
                              denseDate: true,
                              onTap: () => context.push(
                                '/reports/invoice/${rows[i].invoiceId}',
                              ),
                            ),
                          ],
                        ],
                      );
                    },
                    loading: () => const Padding(
                      padding: EdgeInsets.all(24),
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
          ],
        ),
      ),
    );
  }
}

class AmountBars extends ConsumerWidget {
  const AmountBars({super.key, required this.slices});

  final List<ReportSlice> slices;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (slices.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Text(AppStrings.of(ref).noData),
      );
    }
    final maxV = slices.fold<double>(0, (m, s) => s.value > m ? s.value : m);
    return SizedBox(
      height: 160,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          for (final slice in slices)
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    Expanded(
                      child: Align(
                        alignment: Alignment.bottomCenter,
                        child: Container(
                          width: double.infinity,
                          height: maxV <= 0
                              ? 4
                              : (8 + (slice.value / maxV) * 110).clamp(8, 118),
                          decoration: BoxDecoration(
                            color: AppColors.primary,
                            borderRadius: BorderRadius.circular(8),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      slice.label.split(' ').first,
                      textAlign: TextAlign.center,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 10,
                        fontWeight: FontWeight.w600,
                        color: AppColors.navy.withValues(alpha: .55),
                      ),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}
