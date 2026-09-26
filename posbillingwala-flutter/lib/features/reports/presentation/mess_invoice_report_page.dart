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
import 'package:pos_billingwala_v2/features/reports/domain/report_export.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Android InvoiceMessReport — mess coupons grouped by Lunch / Dinner.
 * Date on line 1, 12-hour time (no seconds) on line 2. */
class MessInvoiceReportPage extends ConsumerWidget {
  const MessInvoiceReportPage({super.key});

  static final dateOnlyFmt = DateFormat('yyyy-MM-dd');
  static final timeFmt = DateFormat('hh:mm a');

  static String normalizeMeal(String? raw) => (raw ?? '').trim().toLowerCase();

  static bool isLunch(MessInvoice row) =>
      normalizeMeal(row.messType) == 'lunch';

  static bool isDinner(MessInvoice row) =>
      normalizeMeal(row.messType) == 'dinner';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).invoiceMessReport),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
        actions: [
          IconButton(
            tooltip: 'Export Excel',
            onPressed: () async {
              final rows =
                  await ref.read(appDatabaseProvider).watchMessInvoices().first;
              if (rows.isEmpty) return;
              await shareMessInvoicesExcel(
                invoices: rows,
                title: AppStrings.of(ref).invoiceMessReport,
              );
            },
            icon: const Icon(Icons.ios_share_rounded),
          ),
        ],
      ),
      body: StreamBuilder<List<MessInvoice>>(
        stream: ref.read(appDatabaseProvider).watchMessInvoices(),
        builder: (context, snap) {
          if (snap.connectionState == ConnectionState.waiting &&
              !snap.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final all = snap.data ?? const <MessInvoice>[];
          if (all.isEmpty) {
            return Center(
              child: Text(
                AppStrings.of(ref).tr('empty_sub_mess_invoices'),
                textAlign: TextAlign.center,
              ),
            );
          }

          final lunch = all.where(isLunch).toList();
          final dinner = all.where(isDinner).toList();
          final other = all
              .where((r) => !isLunch(r) && !isDinner(r))
              .toList();

          return ResponsiveScrollShell(
            dashboard: true,
            child: ListView(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                12,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                28,
              ),
              children: [
                ReportKpiGrid(
                  items: [
                    ReportKpiData(
                      label: AppStrings.of(ref).tr('ui_total_bills'),
                      value: '${all.length}',
                    ),
                    ReportKpiData(
                      label: AppStrings.of(ref).tr('ui_lunch'),
                      value: '${lunch.length}',
                    ),
                    ReportKpiData(
                      label: AppStrings.of(ref).tr('ui_dinner'),
                      value: '${dinner.length}',
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                ReportSurfaceCard(
                  padding: EdgeInsets.zero,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const _TableHeader(),
                      if (lunch.isNotEmpty)
                        _MealSection(title: 'Lunch', count: lunch.length, rows: lunch),
                      if (dinner.isNotEmpty)
                        _MealSection(title: 'Dinner', count: dinner.length, rows: dinner),
                      if (other.isNotEmpty)
                        _MealSection(title: 'Other', count: other.length, rows: other),
                    ],
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _TableHeader extends StatelessWidget {
  const _TableHeader();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: AppColors.primary.withValues(alpha: .08),
        border: Border(
          bottom: BorderSide(color: AppColors.border.withValues(alpha: .7)),
        ),
      ),
      child: Row(
        children: [
          const SizedBox(
            width: 36,
            child: Text(
              'Sr',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                fontSize: 11,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          const Expanded(
            flex: 5,
            child: Text(
              'Invoice Date',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                fontSize: 11,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          const SizedBox(width: 8),
          const Expanded(
            flex: 4,
            child: Text(
              'Member Name',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                fontSize: 11,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          const SizedBox(
            width: 64,
            child: Text(
              'Type',
              textAlign: TextAlign.end,
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                fontSize: 11,
                color: AppColors.textSecondary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MealSection extends StatelessWidget {
  const _MealSection({
    required this.title,
    required this.count,
    required this.rows,
  });

  final String title;
  final int count;
  final List<MessInvoice> rows;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          color: AppColors.primaryLight,
          child: Row(
            children: [
              Expanded(
                child: Text(
                  title.toUpperCase(),
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w800,
                    fontSize: 13,
                    letterSpacing: 0.6,
                    color: AppColors.primary,
                  ),
                ),
              ),
              Text(
                '$count coupons',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontWeight: FontWeight.w600,
                  fontSize: 12,
                  color: AppColors.navy.withValues(alpha: .55),
                ),
              ),
            ],
          ),
        ),
        for (var i = 0; i < rows.length; i++) ...[
          if (i > 0)
            Divider(
              height: 1,
              indent: 14,
              endIndent: 14,
              color: AppColors.border.withValues(alpha: .7),
            ),
          _MessInvoiceRow(index: i + 1, invoice: rows[i]),
        ],
      ],
    );
  }
}

class _MessInvoiceRow extends StatelessWidget {
  const _MessInvoiceRow({required this.index, required this.invoice});

  final int index;
  final MessInvoice invoice;

  @override
  Widget build(BuildContext context) {
    final date = MessInvoiceReportPage.dateOnlyFmt.format(invoice.messInvoiceDate);
    final time = MessInvoiceReportPage.timeFmt.format(invoice.messInvoiceDate);

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 36,
            child: Text(
              '$index',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                fontSize: 12,
                color: AppColors.navy.withValues(alpha: .4),
              ),
            ),
          ),
          Expanded(
            flex: 5,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  date,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w600,
                    fontSize: 12.5,
                    color: AppColors.navy,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  time,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 11.5,
                    color: AppColors.navy.withValues(alpha: .48),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            flex: 4,
            child: Text(
              invoice.memberName,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                fontSize: 13.5,
                color: AppColors.navy,
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          SizedBox(
            width: 64,
            child: Text(
              invoice.messType,
              textAlign: TextAlign.end,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                fontSize: 12,
                color: AppColors.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
