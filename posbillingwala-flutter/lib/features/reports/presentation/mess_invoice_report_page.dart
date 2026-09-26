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

/* Paper coupon vs QR token are separate report sources (Android MessReportItem). */
enum MessReportSource { coupon, qr }

class MessReportRow {
  const MessReportRow({
    required this.source,
    required this.memberName,
    required this.messType,
    required this.dateTime,
    required this.detail,
  });

  final MessReportSource source;
  final String memberName;
  final String messType;
  final DateTime dateTime;
  final String detail;

  bool get isQr => source == MessReportSource.qr;

  String get displayType {
    final meal = messType.trim();
    if (isQr) return meal.isEmpty ? 'QR Token' : 'QR · $meal';
    return meal.isEmpty ? 'Coupon' : 'Coupon · $meal';
  }
}

/* Android InvoiceMessReport — Coupons / QR Tokens, then Lunch / Dinner. */
class MessInvoiceReportPage extends ConsumerWidget {
  const MessInvoiceReportPage({super.key});

  static final dateOnlyFmt = DateFormat('yyyy-MM-dd');
  static final timeFmt = DateFormat('hh:mm a');

  static String normalizeMeal(String? raw) => (raw ?? '').trim().toLowerCase();

  static String matchKey(String name, String meal, DateTime date) {
    final day = DateFormat('yyyy-MM-dd').format(date);
    return '${name.trim().toLowerCase()}|${meal.trim().toLowerCase()}|$day';
  }

  static List<MessReportRow> buildRows({
    required List<MessInvoice> invoices,
    required List<MessToken> tokens,
  }) {
    final qrKeys = <String>{};
    final out = <MessReportRow>[];
    for (final t in tokens) {
      final name = t.memberName ?? '';
      qrKeys.add(matchKey(name, t.messType, t.tokenDate));
      final code = t.tokenCode;
      out.add(
        MessReportRow(
          source: MessReportSource.qr,
          memberName: name,
          messType: t.messType,
          dateTime: t.tokenDate,
          detail: code.length > 8
              ? code.substring(0, 8).toUpperCase()
              : code.toUpperCase(),
        ),
      );
    }
    for (final inv in invoices) {
      if (qrKeys.contains(
        matchKey(inv.memberName, inv.messType, inv.messInvoiceDate),
      )) {
        continue; // QR twin invoice
      }
      out.add(
        MessReportRow(
          source: MessReportSource.coupon,
          memberName: inv.memberName,
          messType: inv.messType,
          dateTime: inv.messInvoiceDate,
          detail: 'Coupon',
        ),
      );
    }
    return out;
  }

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
              final db = ref.read(appDatabaseProvider);
              final invoices = await db.watchMessInvoices().first;
              final tokens = await db.getAllMessTokens();
              final rows = buildRows(invoices: invoices, tokens: tokens);
              if (rows.isEmpty) return;
              await shareMessReportExcel(
                rows: rows
                    .map(
                      (r) => (
                        isQr: r.isQr,
                        dateTime: r.dateTime,
                        memberName: r.memberName,
                        messType: r.messType,
                        detail: r.detail,
                      ),
                    )
                    .toList(),
                title: AppStrings.of(ref).invoiceMessReport,
              );
            },
            icon: const Icon(Icons.ios_share_rounded),
          ),
        ],
      ),
      body: StreamBuilder<List<MessInvoice>>(
        stream: ref.read(appDatabaseProvider).watchMessInvoices(),
        builder: (context, invSnap) {
          return StreamBuilder<List<MessToken>>(
            stream: ref.read(appDatabaseProvider).watchAllMessTokens(),
            builder: (context, tokSnap) {
              if (invSnap.connectionState == ConnectionState.waiting &&
                  !invSnap.hasData) {
                return const Center(child: CircularProgressIndicator());
              }
              final invoices = invSnap.data ?? const <MessInvoice>[];
              final tokens = tokSnap.data ?? const <MessToken>[];
              final all = buildRows(invoices: invoices, tokens: tokens);
              if (all.isEmpty) {
                return Center(
                  child: Text(
                    AppStrings.of(ref).tr('empty_sub_mess_invoices'),
                    textAlign: TextAlign.center,
                  ),
                );
              }

              final coupons =
                  all.where((r) => r.source == MessReportSource.coupon).toList();
              final qr =
                  all.where((r) => r.source == MessReportSource.qr).toList();

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
                          label: 'Coupons',
                          value: '${coupons.length}',
                        ),
                        ReportKpiData(
                          label: 'QR Tokens',
                          value: '${qr.length}',
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
                          if (coupons.isNotEmpty)
                            _SourceSection(
                              title: 'Paper Coupons',
                              rows: coupons,
                            ),
                          if (qr.isNotEmpty)
                            _SourceSection(
                              title: 'QR Tokens',
                              rows: qr,
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              );
            },
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
      child: const Row(
        children: [
          SizedBox(
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
          Expanded(
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
          SizedBox(width: 8),
          Expanded(
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
          SizedBox(
            width: 88,
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

class _SourceSection extends StatelessWidget {
  const _SourceSection({required this.title, required this.rows});

  final String title;
  final List<MessReportRow> rows;

  @override
  Widget build(BuildContext context) {
    final lunch = rows
        .where((r) => MessInvoiceReportPage.normalizeMeal(r.messType) == 'lunch')
        .toList();
    final dinner = rows
        .where(
          (r) => MessInvoiceReportPage.normalizeMeal(r.messType) == 'dinner',
        )
        .toList();
    final other = rows
        .where((r) {
          final m = MessInvoiceReportPage.normalizeMeal(r.messType);
          return m != 'lunch' && m != 'dinner';
        })
        .toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          color: AppColors.primary.withValues(alpha: .12),
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
                '${rows.length} items',
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
        if (lunch.isNotEmpty) _MealSection(title: 'Lunch', rows: lunch),
        if (dinner.isNotEmpty) _MealSection(title: 'Dinner', rows: dinner),
        if (other.isNotEmpty) _MealSection(title: 'Other', rows: other),
      ],
    );
  }
}

class _MealSection extends StatelessWidget {
  const _MealSection({required this.title, required this.rows});

  final String title;
  final List<MessReportRow> rows;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          color: AppColors.primaryLight,
          child: Row(
            children: [
              Expanded(
                child: Text(
                  title.toUpperCase(),
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                    letterSpacing: 0.5,
                    color: AppColors.primary,
                  ),
                ),
              ),
              Text(
                '${rows.length}',
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
          _MessReportItemRow(index: i + 1, row: rows[i]),
        ],
      ],
    );
  }
}

class _MessReportItemRow extends StatelessWidget {
  const _MessReportItemRow({required this.index, required this.row});

  final int index;
  final MessReportRow row;

  @override
  Widget build(BuildContext context) {
    final date = MessInvoiceReportPage.dateOnlyFmt.format(row.dateTime);
    final time = MessInvoiceReportPage.timeFmt.format(row.dateTime);

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
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  row.memberName,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w700,
                    fontSize: 13.5,
                    color: AppColors.navy,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                if (row.detail.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    row.detail,
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontSize: 11,
                      color: AppColors.navy.withValues(alpha: .45),
                    ),
                  ),
                ],
              ],
            ),
          ),
          SizedBox(
            width: 88,
            child: Text(
              row.displayType,
              textAlign: TextAlign.end,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                fontSize: 11,
                color: AppColors.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
