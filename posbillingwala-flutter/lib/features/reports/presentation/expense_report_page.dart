import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/reports/domain/report_export.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_period_controls.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/// Expense-wise report with period filter and Excel share.
class ExpenseReportPage extends ConsumerWidget {
  const ExpenseReportPage({super.key});

  List<ShopExpense> inPeriod(
    List<ShopExpense> rows,
    ReportPeriod period,
  ) {
    final range = period.range;
    return rows
        .where(
          (e) =>
              !e.expensesDate.isBefore(range.$1) &&
              e.expensesDate.isBefore(range.$2),
        )
        .toList();
  }

  List<ReportSlice> categorySlices(List<ShopExpense> rows) {
    final map = <String, double>{};
    for (final row in rows) {
      final key = row.expensesName.trim().isEmpty ? 'Other' : row.expensesName.trim();
      map[key] = (map[key] ?? 0) + row.expensesAmount;
    }
    final colors = [
      AppColors.orange,
      AppColors.primary,
      AppColors.purple,
      AppColors.green,
      AppColors.red,
      AppColors.teal,
    ];
    final entries = map.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));
    final top = entries.take(8).toList();
    return [
      for (var i = 0; i < top.length; i++)
        ReportSlice(
          label: top[i].key,
          value: top[i].value,
          color: colors[i % colors.length],
        ),
    ];
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final period = ref.watch(reportPeriodProvider);
    final expensesAsync = ref.watch(expensesProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final dateFmt = DateFormat('yyyy-MM-dd');

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).expenseWiseReport),
        actions: [
          IconButton(
            tooltip: 'Export',
            onPressed: expensesAsync.maybeWhen(
              data: (all) {
                final filtered = inPeriod(all, period);
                if (filtered.isEmpty) return null;
                return () => shareExpensesCsv(
                      expenses: filtered,
                      title: 'Expense Report — ${period.label}',
                    );
              },
              orElse: () => null,
            ),
            icon: const Icon(Icons.ios_share_rounded),
          ),
          IconButton(
            tooltip: 'Filter period',
            onPressed: () => showReportPeriodFilterMenu(context, ref),
            icon: const Icon(Icons.filter_list_rounded),
          ),
        ],
      ),
      body: expensesAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (all) {
          final filtered = inPeriod(all, period);
          final total =
              filtered.fold<double>(0, (s, e) => s + e.expensesAmount);
          final avg = filtered.isEmpty ? 0.0 : total / filtered.length;
          final slices = categorySlices(filtered);

          return ResponsiveScrollShell(
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
                  onTap: () => showReportPeriodFilterMenu(context, ref),
                ),
              ),
              const SizedBox(height: 14),
              if (filtered.isEmpty)
                const ReportSurfaceCard(
                  padding: EdgeInsets.all(28),
                  child: Column(
                    children: [
                      Icon(
                        Icons.account_balance_wallet_outlined,
                        size: 48,
                        color: AppColors.orange,
                      ),
                      SizedBox(height: 12),
                      Text(
                        'No expenses for this period',
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          fontWeight: FontWeight.w700,
                          color: AppColors.navy,
                        ),
                      ),
                      SizedBox(height: 6),
                      Text(
                        'Add expenses from Expense Management.',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                )
              else ...[
                ReportKpiGrid(
                  items: [
                    ReportKpiData(
                      label: 'Expenses',
                      value: '${filtered.length}',
                    ),
                    ReportKpiData(
                      label: 'TOTAL AMOUNT',
                      value: currency.format(total),
                    ),
                    ReportKpiData(
                      label: 'Avg. Expense',
                      value: currency.format(avg),
                    ),
                    ReportKpiData(
                      label: 'Categories',
                      value: '${slices.length}',
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                ReportDonutBreakdown(
                  title: 'Expense Breakdown',
                  slices: slices,
                  centerValue: currency.format(total),
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
                          'Expense List',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontWeight: FontWeight.w800,
                            fontSize: 16,
                            color: AppColors.navy,
                          ),
                        ),
                      ),
                      for (var i = 0; i < filtered.length; i++) ...[
                        if (i > 0)
                          Divider(
                            height: 1,
                            color: AppColors.border.withValues(alpha: .7),
                          ),
                        ExpenseRow(
                          index: i + 1,
                          row: filtered[i],
                          currency: currency,
                          dateFmt: dateFmt,
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                ReportSurfaceCard(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 14,
                  ),
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
              ],
            ],
          ),
      );
        },
      ),
    );
  }
}

class ExpenseRow extends StatelessWidget {
  const ExpenseRow({super.key, 
    required this.index,
    required this.row,
    required this.currency,
    required this.dateFmt,
  });

  final int index;
  final ShopExpense row;
  final NumberFormat currency;
  final DateFormat dateFmt;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        children: [
          Container(
            width: 34,
            height: 34,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.orange.withValues(alpha: .14),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Text(
              '$index',
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w800,
                color: AppColors.orange,
                fontSize: 13,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  row.expensesName.isEmpty ? 'Expense' : row.expensesName,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w700,
                    fontSize: 14.5,
                    color: AppColors.navy,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  dateFmt.format(row.expensesDate),
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12,
                    color: AppColors.navy.withValues(alpha: .48),
                  ),
                ),
              ],
            ),
          ),
          Text(
            currency.format(row.expensesAmount),
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontWeight: FontWeight.w800,
              fontSize: 14.5,
              color: AppColors.primary,
            ),
          ),
        ],
      ),
    );
  }
}
