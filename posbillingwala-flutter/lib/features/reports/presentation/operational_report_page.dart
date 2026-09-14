import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/reports/domain/report_export.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_period_controls.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/* Reusable operational report: period filter, KPIs, donut, invoice list. */
class OperationalReportPage extends ConsumerStatefulWidget {
  const OperationalReportPage({
    super.key,
    required this.titleKey,
    this.typeFilter,
    this.paymentFilter,
    this.paymentBreakdown = false,
  });

  final String titleKey;
  final ReportInvoiceTypeFilter? typeFilter;
  final ReportPaymentFilter? paymentFilter;
  final bool paymentBreakdown;

  @override
  ConsumerState<OperationalReportPage> createState() =>
      OperationalReportPageState();
}

class OperationalReportPageState extends ConsumerState<OperationalReportPage> {
  late final bool showTypeChips;

  @override
  void initState() {
    super.initState();
    showTypeChips = widget.typeFilter == null ||
        widget.typeFilter == ReportInvoiceTypeFilter.all;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(reportInvoiceTypeFilterProvider.notifier).select(
            widget.typeFilter ?? ReportInvoiceTypeFilter.all,
          );
      ref.read(reportPaymentFilterProvider.notifier).select(
            widget.paymentFilter ?? ReportPaymentFilter.all,
          );
    });
  }

  Future<void> onFilterPressed() async {
    final period = ref.read(reportPeriodProvider);
    final selected = await showMenu<String>(
      context: context,
      position: const RelativeRect.fromLTRB(1000, 80, 16, 0),
      color: AppColors.primary,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      items: [
        const PopupMenuItem(
          value: 'day',
          child: Row(
            children: [
              Icon(Icons.calendar_month_rounded, color: Colors.white, size: 18),
              SizedBox(width: 10),
              Text(
                'Day wise',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
        const PopupMenuItem(
          value: 'month',
          child: Row(
            children: [
              Icon(Icons.calendar_month_rounded, color: Colors.white, size: 18),
              SizedBox(width: 10),
              Text(
                'Month Wise',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
        const PopupMenuItem(
          value: 'year',
          child: Row(
            children: [
              Icon(Icons.calendar_month_rounded, color: Colors.white, size: 18),
              SizedBox(width: 10),
              Text(
                'Year Wise',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
        if (widget.typeFilter == ReportInvoiceTypeFilter.table)
          const PopupMenuItem(
            value: 'table-list',
            child: Row(
              children: [
                Icon(Icons.table_rows_rounded, color: Colors.white, size: 18),
                SizedBox(width: 10),
                Text(
                  'Table List',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
      ],
    );
    if (!mounted || selected == null) return;
    if (selected == 'table-list') {
      context.push('/reports/table-list');
      return;
    }
    if (selected == 'month') {
      await pickReportMonth(context, ref);
      return;
    }
    onReportPeriodSelected(
      ref,
      selected == 'year' ? ReportPeriodKind.year : ReportPeriodKind.today,
      period,
    );
  }

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings.of(ref);
    final title = strings.ui(widget.titleKey);
    final period = ref.watch(reportPeriodProvider);
    final paymentFilter = ref.watch(reportPaymentFilterProvider);
    final typeFilter = ref.watch(reportInvoiceTypeFilterProvider);
    final invoicesAsync = ref.watch(periodInvoicesProvider);
    final filtered = ref.watch(filteredPeriodInvoicesProvider);
    final summary = ref.watch(periodSalesSummaryProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');

    final slices = widget.paymentBreakdown
        ? paymentSlices(summary, filtered)
        : billingTypeSlices(filtered);
    final categories = slices.where((s) => s.value > 0).length;

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(title),
        actions: [
          IconButton(
            tooltip: 'Export CSV',
            onPressed: filtered.isEmpty
                ? null
                : () => shareInvoicesCsv(
                      invoices: filtered,
                      title: '$title — ${period.label}',
                    ),
            icon: const Icon(Icons.ios_share_rounded),
          ),
          IconButton(
            tooltip: 'Filter',
            onPressed: onFilterPressed,
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
            28),
        children: [
          Align(
            alignment: Alignment.centerLeft,
            child: ReportPeriodPill(
              label: reportPeriodDisplayLabel(period) == 'Today'
                  ? 'All Records'
                  : reportPeriodDisplayLabel(period),
              onTap: onFilterPressed,
            ),
          ),
          if (showTypeChips) ...[
            const SizedBox(height: 12),
            SizedBox(
              height: 40,
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: [
                  for (final entry in [
                    (ReportInvoiceTypeFilter.all, strings.ui('ui_all')),
                    (ReportInvoiceTypeFilter.pos, strings.fastBilling),
                    (ReportInvoiceTypeFilter.table, strings.dineIn),
                    (ReportInvoiceTypeFilter.takeaway, strings.takeAway),
                    (ReportInvoiceTypeFilter.mess, strings.mess),
                    (ReportInvoiceTypeFilter.discountOnly, strings.ui('ui_discount')),
                    (ReportInvoiceTypeFilter.refundOnly, strings.refundWiseReport),
                  ])
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: FilterChip(
                        label: Text(entry.$2),
                        selected: typeFilter == entry.$1,
                        onSelected: (_) => ref
                            .read(reportInvoiceTypeFilterProvider.notifier)
                            .select(entry.$1),
                      ),
                    ),
                ],
              ),
            ),
          ],
          if (!widget.paymentBreakdown) ...[
            const SizedBox(height: 10),
            SizedBox(
              height: 40,
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: [
                  for (final entry in [
                    (ReportPaymentFilter.all, strings.ui('ui_all')),
                    (ReportPaymentFilter.cash, strings.cash),
                    (ReportPaymentFilter.upi, strings.upi),
                    (ReportPaymentFilter.cashPlusUpi, strings.ui('ui_cash_plus_upi')),
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
          ],
          const SizedBox(height: 12),
          if (widget.paymentBreakdown)
            ReportKpiGrid(
              items: [
                ReportKpiData(
                  label: 'Subtotal',
                  value: currency.format(summary.subTotal),
                ),
                ReportKpiData(
                  label: 'CGST + SGST',
                  value: currency.format(summary.gstTotal),
                ),
                ReportKpiData(
                  label: 'Discount',
                  value: currency.format(summary.discountTotal),
                ),
                ReportKpiData(
                  label: 'TOTAL AMOUNT',
                  value: currency.format(summary.totalSales),
                ),
              ],
            )
          else
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
                ReportKpiData(
                  label: 'Categories',
                  value: '$categories',
                ),
              ],
            ),
          const SizedBox(height: 14),
          ReportDonutBreakdown(
            title: widget.paymentBreakdown
                ? 'Payment Wise Details'
                : widget.typeFilter == ReportInvoiceTypeFilter.table
                    ? 'Table Number'
                    : 'Billing Wise Details',
            slices: widget.typeFilter == ReportInvoiceTypeFilter.table
                ? tableSlices(filtered)
                : slices,
            centerValue: currency.format(summary.totalSales),
          ),
          const SizedBox(height: 14),
          ReportSurfaceCard(
            padding: const EdgeInsets.fromLTRB(4, 12, 4, 4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 0, 12, 8),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          widget.typeFilter == ReportInvoiceTypeFilter.table
                              ? 'Table Summary'
                              : 'Invoice Sale',
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            fontWeight: FontWeight.w800,
                            fontSize: 16,
                            color: AppColors.navy,
                          ),
                        ),
                      ),
                      if (widget.typeFilter == ReportInvoiceTypeFilter.table)
                        TextButton(
                          onPressed: () =>
                              context.push('/reports/table-list'),
                          child: Text(AppStrings.of(ref).tableList),
                        ),
                    ],
                  ),
                ),
                if (widget.typeFilter == ReportInvoiceTypeFilter.table)
                  TableSummaryList(
                    invoices: filtered,
                    currency: currency,
                  )
                else
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
                      final rows = filtered.take(50).toList();
                      return Column(
                        children: [
                          for (var i = 0; i < rows.length; i++) ...[
                            if (i > 0)
                              Divider(
                                height: 1,
                                color:
                                    AppColors.border.withValues(alpha: .7),
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
                      padding: EdgeInsets.all(
            AppBreakpoints.pagePaddingFor(context.widthClass),
          ),
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

  List<ReportSlice> tableSlices(List<Invoice> filtered) {
    final map = <String, double>{};
    for (final inv in filtered) {
      final key =
          (inv.noOfTable.trim().isEmpty) ? '—' : inv.noOfTable.trim();
      map[key] = (map[key] ?? 0) + inv.totalAmount;
    }
    final colors = [
      AppColors.primary,
      AppColors.green,
      AppColors.orange,
      AppColors.purple,
      AppColors.red,
      AppColors.teal,
      const Color(0xFFE6A100),
      const Color(0xFFE91E63),
    ];
    final entries = map.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));
    return [
      for (var i = 0; i < entries.length; i++)
        ReportSlice(
          label: entries[i].key,
          value: entries[i].value,
          color: colors[i % colors.length],
        ),
    ];
  }
}

class TableSummaryList extends ConsumerWidget {
  const TableSummaryList({super.key, 
    required this.invoices,
    required this.currency,
  });

  final List<Invoice> invoices;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final map = <String, double>{};
    for (final inv in invoices) {
      final key =
          (inv.noOfTable.trim().isEmpty) ? '—' : inv.noOfTable.trim();
      map[key] = (map[key] ?? 0) + inv.totalAmount;
    }
    final entries = map.entries.toList()
      ..sort((a, b) => a.key.compareTo(b.key));

    if (entries.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(24),
        child: Center(child: Text(AppStrings.of(ref).noTableBills)),
      );
    }

    return Column(
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          color: AppColors.border.withValues(alpha: .35),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  'Sr No.  Table Number',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: AppColors.navy.withValues(alpha: .55),
                  ),
                ),
              ),
              Text(
                'AMOUNT',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: AppColors.navy.withValues(alpha: .55),
                ),
              ),
              const SizedBox(width: 20),
            ],
          ),
        ),
        for (var i = 0; i < entries.length; i++) ...[
          if (i > 0)
            Divider(
              height: 1,
              color: AppColors.border.withValues(alpha: .7),
            ),
          Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: () => context.push(
                '/reports/table-list?table=${Uri.encodeComponent(entries[i].key)}',
              ),
              child: Padding(
                padding:
                    const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                child: Row(
                  children: [
                    SizedBox(
                      width: 28,
                      child: Text(
                        '${i + 1}',
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          color: AppColors.navy.withValues(alpha: .45),
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    Expanded(
                      child: Text(
                        entries[i].key,
                        style: const TextStyle(
                          fontFamily: AppFonts.family,
                          fontWeight: FontWeight.w800,
                          color: AppColors.navy,
                          fontSize: 15,
                        ),
                      ),
                    ),
                    Text(
                      currency.format(entries[i].value),
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        fontWeight: FontWeight.w800,
                        color: AppColors.primary,
                        fontSize: 14.5,
                      ),
                    ),
                    const SizedBox(width: 4),
                    Icon(
                      Icons.chevron_right_rounded,
                      color: AppColors.navy.withValues(alpha: .28),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ],
    );
  }
}
