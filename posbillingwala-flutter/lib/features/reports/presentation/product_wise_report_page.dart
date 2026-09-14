import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/reports/domain/report_export.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_period_controls.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

class ProductWiseReportPage extends ConsumerStatefulWidget {
  const ProductWiseReportPage({super.key, this.initialType = 'all'});

  final String initialType;

  @override
  ConsumerState<ProductWiseReportPage> createState() =>
      _ProductWiseReportPageState();
}

class _ProductWiseReportPageState extends ConsumerState<ProductWiseReportPage> {
  late String _typeFilter; // all | product | combo
  bool _leastSold = false;
  AsyncValue<List<ProductSalesRow>> _rows = const AsyncLoading();

  @override
  void initState() {
    super.initState();
    _typeFilter =
        widget.initialType == 'combo' || widget.initialType == 'product'
            ? widget.initialType
            : 'all';
    Future.microtask(_load);
  }

  Future<void> _load() async {
    final period = ref.read(reportPeriodProvider);
    final (start, end) = period.range;
    setState(() => _rows = const AsyncLoading());
    final result = await AsyncValue.guard(() {
      return ref.read(appDatabaseProvider).getProductWiseSales(
            start: start,
            end: end,
            invoiceItemType: _typeFilter == 'all' ? null : _typeFilter,
            leastSold: _leastSold,
          );
    });
    if (!mounted) return;
    setState(() => _rows = result);
  }

  @override
  Widget build(BuildContext context) {
    final period = ref.watch(reportPeriodProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final isCombo = widget.initialType == 'combo' || _typeFilter == 'combo';

    ref.listen(reportPeriodProvider, (_, _) => _load());

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(
          isCombo && _typeFilter == 'combo'
              ? 'Combo Wise Report'
              : 'Product Wise Report',
        ),
        actions: [
          IconButton(
            tooltip: 'Export CSV',
            onPressed: () {
              final rows = _rows.asData?.value;
              if (rows == null || rows.isEmpty) return;
              shareProductSalesCsv(
                rows: rows,
                title: 'Product-wise — ${period.label}',
              );
            },
            icon: const Icon(Icons.ios_share_rounded),
          ),
          IconButton(
            tooltip: 'Filter period',
            onPressed: () => showReportPeriodFilterMenu(context, ref),
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
              onTap: () => showReportPeriodFilterMenu(context, ref),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                for (final entry in const [
                  (false, 'Top sellers'),
                  (true, 'Least sold'),
                ])
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Text(entry.$2),
                      selected: _leastSold == entry.$1,
                      onSelected: (_) {
                        setState(() => _leastSold = entry.$1);
                        _load();
                      },
                    ),
                  ),
                for (final entry in const [
                  ('all', 'All'),
                  ('product', 'Products'),
                  ('combo', 'Combos'),
                ])
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Text(entry.$2),
                      selected: _typeFilter == entry.$1,
                      onSelected: (_) {
                        setState(() => _typeFilter = entry.$1);
                        _load();
                      },
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          _rows.when(
            data: (rows) {
              if (rows.isEmpty) {
                return ReportSurfaceCard(
                  padding: const EdgeInsets.all(28),
                  child: Center(child: Text(AppStrings.of(ref).noBillsPeriod)),
                );
              }
              final totalQty = rows.fold<int>(0, (s, r) => s + r.totalQuantity);
              final totalAmt =
                  rows.fold<double>(0, (s, r) => s + r.totalAmount);
              final topRows = rows.take(6).toList();
              final topSlices = [
                for (var i = 0; i < topRows.length; i++)
                  ReportSlice(
                    label: topRows[i].productName,
                    value: topRows[i].totalAmount,
                    color: [
                      AppColors.primary,
                      AppColors.green,
                      AppColors.orange,
                      AppColors.purple,
                      AppColors.teal,
                      const Color(0xFFE6A100),
                    ][i % 6],
                  ),
              ];

              return Column(
                children: [
                  ReportKpiGrid(
                    items: [
                      ReportKpiData(
                        label: 'Items',
                        value: '${rows.length}',
                      ),
                      ReportKpiData(
                        label: 'TOTAL AMOUNT',
                        value: currency.format(totalAmt),
                      ),
                      ReportKpiData(
                        label: 'Total Qty',
                        value: '$totalQty',
                      ),
                      ReportKpiData(
                        label: 'Avg / Item',
                        value: currency.format(
                          rows.isEmpty ? 0 : totalAmt / rows.length,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),
                  ReportDonutBreakdown(
                    title: _leastSold ? 'Least Sold Mix' : 'Top Sellers Mix',
                    slices: topSlices,
                    centerValue: currency.format(totalAmt),
                  ),
                  const SizedBox(height: 14),
                  ReportSurfaceCard(
                    padding: const EdgeInsets.fromLTRB(4, 12, 4, 4),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Padding(
                          padding: const EdgeInsets.fromLTRB(12, 0, 12, 8),
                          child: Text(
                            _leastSold ? 'Least Sold Items' : 'Product Sales',
                            style: const TextStyle(
                              fontFamily: AppFonts.family,
                              fontWeight: FontWeight.w800,
                              fontSize: 16,
                              color: AppColors.navy,
                            ),
                          ),
                        ),
                        for (var i = 0; i < rows.length; i++) ...[
                          if (i > 0)
                            Divider(
                              height: 1,
                              color: AppColors.border.withValues(alpha: .7),
                            ),
                          _ProductRow(
                            index: i + 1,
                            row: rows[i],
                            currency: currency,
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              );
            },
            loading: () => const Padding(
              padding: EdgeInsets.all(40),
              child: Center(child: CircularProgressIndicator()),
            ),
            error: (e, _) => ReportSurfaceCard(
              padding: EdgeInsets.all(
            AppBreakpoints.pagePaddingFor(context.widthClass),
          ),
              child: Text('$e'),
            ),
          ),
        ],
      ),
      ),
    );
  }
}

class _ProductRow extends StatelessWidget {
  const _ProductRow({
    required this.index,
    required this.row,
    required this.currency,
  });

  final int index;
  final ProductSalesRow row;
  final NumberFormat currency;

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
              color: AppColors.primary.withValues(alpha: .12),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Text(
              '$index',
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w800,
                color: AppColors.primary,
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
                  row.productName,
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
                  'Qty ${row.totalQuantity}'
                  '${row.itemType == 'combo' ? ' · Combo' : ''}',
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
            currency.format(row.totalAmount),
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
