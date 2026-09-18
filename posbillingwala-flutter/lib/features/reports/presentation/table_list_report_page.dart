import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/reports/domain/report_export.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_period_controls.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Invoice Table List Report — filter table bills by selected table + period. */
class TableListReportPage extends ConsumerStatefulWidget {
  const TableListReportPage({super.key, this.initialTableNumber});

  final String? initialTableNumber;

  @override
  ConsumerState<TableListReportPage> createState() =>
      TableListReportPageState();
}

class TableListReportPageState extends ConsumerState<TableListReportPage> {
  String? tableListReportPageTableNumber;

  @override
  void initState() {
    super.initState();
    tableListReportPageTableNumber = widget.initialTableNumber;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(reportInvoiceTypeFilterProvider.notifier)
          .select(ReportInvoiceTypeFilter.table);
      ref
          .read(reportPaymentFilterProvider.notifier)
          .select(ReportPaymentFilter.all);
    });
  }

  Future<void> onFilterPressed() async {
    final period = ref.read(reportPeriodProvider);
    final selected = await showMenu<String>(
      context: context,
      position: const RelativeRect.fromLTRB(1000, 80, 16, 0),
      color: AppColors.primary,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      items: const [
        PopupMenuItem(
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
        PopupMenuItem(
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
        PopupMenuItem(
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
      ],
    );
    if (!mounted || selected == null) return;
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
    final period = ref.watch(reportPeriodProvider);
    final tablesAsync = ref.watch(posTablesProvider);
    final invoicesAsync = ref.watch(periodInvoicesProvider);
    final filtered = ref.watch(filteredPeriodInvoicesProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');

    final tables = tablesAsync.maybeWhen(
      data: (v) => v,
      orElse: () => const <PosTable>[],
    );

    PosTable? selected;
    for (final t in tables) {
      if (t.tableNumber == tableListReportPageTableNumber) {
        selected = t;
        break;
      }
    }

    final tableInvoices = filtered.where((inv) {
      if (tableListReportPageTableNumber == null ||
          tableListReportPageTableNumber!.trim().isEmpty) {
        return true;
      }
      return inv.noOfTable.trim() == tableListReportPageTableNumber!.trim();
    }).toList();

    final total = tableInvoices.fold<double>(0, (s, e) => s + e.totalAmount);
    final title =
        tableListReportPageTableNumber == null ||
            tableListReportPageTableNumber!.isEmpty
        ? 'Invoice List'
        : 'Report of $tableListReportPageTableNumber';

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(title),
        actions: [
          IconButton(
            tooltip: 'Export CSV',
            onPressed: tableInvoices.isEmpty
                ? null
                : () => shareInvoicesCsv(
                    invoices: tableInvoices,
                    title:
                        'Table ${tableListReportPageTableNumber ?? 'all'} — ${period.label}',
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
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: Align(
              alignment: Alignment.centerLeft,
              child: ReportPeriodPill(
                label: reportPeriodDisplayLabel(period),
                onTap: onFilterPressed,
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: AppDropdownFormField<PosTable>(
              required: true,
              label: 'Select Table',
              items: tables,
              itemLabel: (t) => t.displayName.trim().isNotEmpty
                  ? '${t.tableNumber} · ${t.displayName}'
                  : t.tableNumber,
              value: selected,
              enableSearch: true,
              onChanged: (t) => setState(
                () => tableListReportPageTableNumber = t?.tableNumber,
              ),
            ),
          ),
          Expanded(
            child: invoicesAsync.when(
              data: (_) {
                if (tableInvoices.isEmpty) {
                  return Center(child: Text(AppStrings.of(ref).noTableBills));
                }
                return ResponsiveScrollShell(
                  dashboard: true,
                  child: ListView(
                    padding: EdgeInsets.fromLTRB(
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      12,
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      24,
                    ),
                    children: [
                      ReportSurfaceCard(
                        padding: const EdgeInsets.fromLTRB(4, 12, 4, 4),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Padding(
                              padding: EdgeInsets.fromLTRB(12, 0, 12, 8),
                              child: Text(
                                'Invoice List',
                                style: TextStyle(
                                  fontFamily: AppFonts.family,
                                  fontWeight: FontWeight.w800,
                                  fontSize: 16,
                                  color: AppColors.navy,
                                ),
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 14,
                                vertical: 10,
                              ),
                              color: AppColors.border.withValues(alpha: .35),
                              child: Row(
                                children: [
                                  Expanded(
                                    child: Text(
                                      'Sr No.   Invoice Date   Invoice Number',
                                      style: TextStyle(
                                        fontFamily: AppFonts.family,
                                        fontSize: 11,
                                        fontWeight: FontWeight.w600,
                                        color: AppColors.navy.withValues(
                                          alpha: .55,
                                        ),
                                      ),
                                    ),
                                  ),
                                  Text(
                                    'AMOUNT',
                                    style: TextStyle(
                                      fontFamily: AppFonts.family,
                                      fontSize: 11,
                                      fontWeight: FontWeight.w600,
                                      color: AppColors.navy.withValues(
                                        alpha: .55,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            for (var i = 0; i < tableInvoices.length; i++) ...[
                              if (i > 0)
                                Divider(
                                  height: 1,
                                  color: AppColors.border.withValues(alpha: .7),
                                ),
                              ReportInvoiceRow(
                                index: i + 1,
                                invoice: tableInvoices[i],
                                currency: currency,
                                denseDate: true,
                                onTap: () => context.push(
                                  '/reports/invoice/${tableInvoices[i].invoiceId}',
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(child: Text('$e')),
            ),
          ),
          Container(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border(
                top: BorderSide(color: AppColors.border.withValues(alpha: .8)),
              ),
            ),
            child: SafeArea(
              top: false,
              child: Row(
                children: [
                  const Text(
                    'TOTAL AMOUNT',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w800,
                      color: AppColors.navy,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    currency.format(total),
                    style: const TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w900,
                      color: AppColors.primary,
                      fontSize: 16,
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
