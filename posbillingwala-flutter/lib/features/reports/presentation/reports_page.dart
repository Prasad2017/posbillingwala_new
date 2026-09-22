import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/payment_display/presentation/payment_display_actions.dart';
import 'package:pos_billingwala_v2/features/reports/domain/report_export.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_period_controls.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class ReportsPage extends ConsumerWidget {
  const ReportsPage({super.key});

  Future<void> showPeriodMenu(BuildContext context, WidgetRef ref) async {
    final selected = await showMenu<String>(
      context: context,
      position: const RelativeRect.fromLTRB(1000, 80, 16, 0),
      color: AppColors.primary,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      items: [
        for (final entry in const [
          ('all', 'All Records'),
          ('day', 'Day wise'),
          ('month', 'Month Wise'),
          ('year', 'Year Wise'),
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
    if (selected == null || !context.mounted) return;
    if (selected == 'all') {
      ref.read(reportPeriodProvider.notifier).useAll();
      return;
    }
    await applyReportPeriodFilterChoice(context, ref, selected);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final period = ref.watch(reportPeriodProvider);
    final paymentFilter = ref.watch(reportPaymentFilterProvider);
    final invoicesAsync = ref.watch(periodInvoicesProvider);
    final filtered = ref.watch(filteredPeriodInvoicesProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final pad = AppBreakpoints.pagePaddingFor(context.widthClass);

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
        child: Column(
        children: [
          Padding(
            padding: EdgeInsets.fromLTRB(pad, context.isShortHeight ? 6 : 12, pad, 0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Align(
                  alignment: Alignment.centerLeft,
                  child: ReportPeriodPill(
                    label: reportPeriodDisplayLabel(period),
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
                          child: ReportFilterChip(
                            label: entry.$2,
                            selected: paymentFilter == entry.$1,
                            showCheckmark: true,
                            onSelected: (_) => ref
                                .read(reportPaymentFilterProvider.notifier)
                                .select(entry.$1),
                          ),
                        ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: invoicesAsync.when(
              data: (_) {
                if (filtered.isEmpty) {
                  return Center(
                    child: Text(AppStrings.of(ref).noBillsPeriod),
                  );
                }
                return Padding(
                  padding: EdgeInsets.fromLTRB(pad, 4, pad, 28),
                  child: Material(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(14),
                    clipBehavior: Clip.antiAlias,
                    child: ListView.separated(
                      itemCount: filtered.length,
                      separatorBuilder: (_, _) => Divider(
                        height: 1,
                        indent: 12,
                        endIndent: 12,
                        color: AppColors.border.withValues(alpha: .7),
                      ),
                      itemBuilder: (context, i) {
                        final invoice = filtered[i];
                        return ReportInvoiceRow(
                          index: i + 1,
                          invoice: invoice,
                          currency: currency,
                          denseDate: true,
                          onTap: () => context.push(
                            '/reports/invoice/${invoice.invoiceId}',
                          ),
                          onShowQr:
                              AppPlatform.isWeb ||
                                  invoice.invoiceOrderStatus ==
                                      'cancelled' ||
                                  invoice.invoiceOrderStatus == 'refunded'
                              ? null
                              : () => requestShowInvoicePaymentQr(
                                  context,
                                  ref,
                                  invoice,
                                ),
                        );
                      },
                    ),
                  ),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(child: Text('$e')),
            ),
          ),
        ],
      ),
      ),
    );
  }
}
