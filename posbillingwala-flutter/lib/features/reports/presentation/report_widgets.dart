import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';

const pageBg = Color(0xFFF3F7FC);

Color get reportPageBg => pageBg;

/// Uppercase section label used on hub and detail screens.
class ReportSectionLabel extends StatelessWidget {
  const ReportSectionLabel(this.label, {super.key});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Text(
      label.toUpperCase(),
      style: TextStyle(
        fontFamily: AppFonts.family,
        fontSize: 11,
        letterSpacing: 1.0,
        color: AppColors.textSecondary.withValues(alpha: .85),
        fontWeight: FontWeight.w600,
      ),
    );
  }
}

/// Pill used under AppBar for period / store context.
class ReportPeriodPill extends StatelessWidget {
  const ReportPeriodPill({
    super.key,
    required this.label,
    this.onTap,
  });

  final String label;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(999),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(999),
            border: Border.all(color: AppColors.primary.withValues(alpha: .35)),
            boxShadow: [
              BoxShadow(
                color: AppColors.navy.withValues(alpha: .04),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.calendar_month_rounded,
                size: 18,
                color: AppColors.primary,
              ),
              const SizedBox(width: 8),
              Flexible(
                child: Text(
                  label,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    color: AppColors.primary,
                    fontWeight: FontWeight.w700,
                    fontSize: 13.5,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// White card shell matching Masters / Settings list cards.
class ReportSurfaceCard extends StatelessWidget {
  const ReportSurfaceCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
  });

  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: margin,
      padding: padding,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border.withValues(alpha: .75)),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: .05),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: child,
    );
  }
}

class ReportKpiData {
  const ReportKpiData({
    required this.label,
    required this.value,
    this.changePercent,
  });

  final String label;
  final String value;
  final double? changePercent;
}

/// 2×2 KPI grid with blue top accent (screenshot style).
class ReportKpiGrid extends StatelessWidget {
  const ReportKpiGrid({super.key, required this.items});

  final List<ReportKpiData> items;

  @override
  Widget build(BuildContext context) {
    final rows = <List<ReportKpiData>>[];
    for (var i = 0; i < items.length; i += 2) {
      rows.add(items.sublist(i, math.min(i + 2, items.length)));
    }
    return Column(
      children: [
        for (var r = 0; r < rows.length; r++) ...[
          if (r > 0) const SizedBox(height: 10),
          Row(
            children: [
              for (var c = 0; c < rows[r].length; c++) ...[
                if (c > 0) const SizedBox(width: 10),
                Expanded(child: ReportKpiCard(data: rows[r][c])),
              ],
              if (rows[r].length == 1) const Expanded(child: SizedBox()),
            ],
          ),
        ],
      ],
    );
  }
}

class ReportKpiCard extends StatelessWidget {
  const ReportKpiCard({super.key, required this.data});

  final ReportKpiData data;

  @override
  Widget build(BuildContext context) {
    final change = data.changePercent;
    final changeColor = change == null
        ? AppColors.textSecondary
        : change >= 0
            ? AppColors.green
            : AppColors.red;
    final changeText = change == null
        ? null
        : '${change >= 0 ? '' : ''}${change.toStringAsFixed(0)}%';

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.border.withValues(alpha: .8)),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: .04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(height: 3, color: AppColors.primary),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  data.label,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: AppColors.navy.withValues(alpha: .55),
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  data.value,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: AppColors.navy,
                    height: 1.15,
                  ),
                ),
                if (changeText != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    changeText,
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: changeColor,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class ReportSlice {
  const ReportSlice({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final double value;
  final Color color;
}

/// Donut + legend block used on invoice / sale / table reports.
class ReportDonutBreakdown extends StatelessWidget {
  const ReportDonutBreakdown({
    super.key,
    required this.title,
    required this.slices,
    this.centerLabel,
    this.centerValue,
  });

  final String title;
  final List<ReportSlice> slices;
  final String? centerLabel;
  final String? centerValue;

  @override
  Widget build(BuildContext context) {
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final total = slices.fold<double>(0, (s, e) => s + e.value);
    final visible = slices.where((s) => s.value > 0).toList();
    final paintSlices = visible.isEmpty ? slices : visible;

    return ReportSurfaceCard(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontWeight: FontWeight.w800,
              fontSize: 16,
              color: AppColors.navy,
            ),
          ),
          const SizedBox(height: 16),
          Center(
            child: SizedBox(
              width: 180,
              height: 180,
              child: CustomPaint(
                painter: ReportDonutPainter(slices: paintSlices),
                child: Center(
                  child: Padding(
                    padding: const EdgeInsets.all(28),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (centerLabel != null)
                          Text(
                            centerLabel!,
                            textAlign: TextAlign.center,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontFamily: AppFonts.family,
                              fontSize: 11,
                              fontWeight: FontWeight.w600,
                              color: AppColors.navy.withValues(alpha: .55),
                            ),
                          ),
                        Text(
                          centerValue ?? currency.format(total),
                          textAlign: TextAlign.center,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            fontSize: 14,
                            fontWeight: FontWeight.w800,
                            color: AppColors.navy,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          for (final slice in paintSlices)
            LegendRow(
              slice: slice,
              total: total,
              currency: currency,
            ),
        ],
      ),
    );
  }
}

class LegendRow extends StatelessWidget {
  const LegendRow({super.key, 
    required this.slice,
    required this.total,
    required this.currency,
  });

  final ReportSlice slice;
  final double total;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context) {
    final pct = total <= 0 ? 0.0 : (slice.value / total) * 100;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Container(
            width: 10,
            height: 10,
            decoration: BoxDecoration(
              color: slice.color,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  slice.label,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w700,
                    fontSize: 14,
                    color: AppColors.navy,
                  ),
                ),
                Text(
                  currency.format(slice.value),
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12,
                    color: AppColors.navy.withValues(alpha: .5),
                  ),
                ),
              ],
            ),
          ),
          Text(
            '${pct.toStringAsFixed(0)}%',
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontWeight: FontWeight.w800,
              fontSize: 14,
              color: AppColors.navy,
            ),
          ),
        ],
      ),
    );
  }
}

class ReportDonutPainter extends CustomPainter {
  ReportDonutPainter({required this.slices});

  final List<ReportSlice> slices;

  @override
  void paint(Canvas canvas, Size size) {
    final total = slices.fold<double>(0, (s, e) => s + e.value);
    final rect = Offset.zero & size;
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 22
      ..strokeCap = StrokeCap.butt;

    if (total <= 0) {
      paint.color = const Color(0xFFE3EAF5);
      canvas.drawArc(rect.deflate(14), 0, math.pi * 2, false, paint);
      return;
    }

    var start = -math.pi / 2;
    for (final slice in slices) {
      final sweep = (slice.value / total) * math.pi * 2;
      paint.color = slice.color;
      canvas.drawArc(rect.deflate(14), start, sweep, false, paint);
      start += sweep;
    }
  }

  @override
  bool shouldRepaint(covariant ReportDonutPainter oldDelegate) =>
      oldDelegate.slices != slices;
}

/// Simple line trend for Sales Dashboard / Overview.
class ReportLineTrend extends StatelessWidget {
  const ReportLineTrend({
    super.key,
    required this.values,
    required this.labels,
  });

  final List<double> values;
  final List<String> labels;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 180,
      child: Column(
        children: [
          Expanded(
            child: CustomPaint(
              painter: LineTrendPainter(values: values),
              child: const SizedBox.expand(),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              for (final label in labels)
                Expanded(
                  child: Text(
                    label,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontSize: 11,
                      color: AppColors.navy.withValues(alpha: .45),
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class LineTrendPainter extends CustomPainter {
  LineTrendPainter({required this.values});

  final List<double> values;

  @override
  void paint(Canvas canvas, Size size) {
    final grid = Paint()
      ..color = AppColors.border.withValues(alpha: .7)
      ..strokeWidth = 1;
    for (var i = 0; i < 4; i++) {
      final y = size.height * (i / 3);
      canvas.drawLine(Offset(0, y), Offset(size.width, y), grid);
    }

    if (values.isEmpty) return;
    final maxV = values.fold<double>(0, (m, v) => v > m ? v : m);
    final safeMax = maxV <= 0 ? 1.0 : maxV;

    final path = Path();
    final points = <Offset>[];
    for (var i = 0; i < values.length; i++) {
      final x = values.length == 1
          ? size.width / 2
          : size.width * (i / (values.length - 1));
      final y = size.height - (values[i] / safeMax) * (size.height * 0.85);
      points.add(Offset(x, y));
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }

    final line = Paint()
      ..color = AppColors.primary
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.5
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    canvas.drawPath(path, line);

    final fill = Paint()
      ..shader = LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [
          AppColors.primary.withValues(alpha: .18),
          AppColors.primary.withValues(alpha: 0),
        ],
      ).createShader(Offset.zero & size);
    final fillPath = Path.from(path)
      ..lineTo(points.last.dx, size.height)
      ..lineTo(points.first.dx, size.height)
      ..close();
    canvas.drawPath(fillPath, fill);

    final dot = Paint()..color = AppColors.primary;
    final ring = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;
    for (final p in points) {
      canvas.drawCircle(p, 4.5, dot);
      canvas.drawCircle(p, 4.5, ring);
    }
  }

  @override
  bool shouldRepaint(covariant LineTrendPainter oldDelegate) =>
      oldDelegate.values != values;
}

/// Indexed invoice row matching Sales List / Invoice Sale screenshots.
class ReportInvoiceRow extends StatelessWidget {
  const ReportInvoiceRow({
    super.key,
    required this.index,
    required this.invoice,
    required this.currency,
    required this.onTap,
    this.showPaymentTag = false,
    this.denseDate = false,
  });

  final int index;
  final Invoice invoice;
  final NumberFormat currency;
  final VoidCallback onTap;
  final bool showPaymentTag;
  final bool denseDate;

  @override
  Widget build(BuildContext context) {
    final dateFmt = DateFormat(denseDate ? 'yyyy-MM-dd' : 'dd MMM yyyy');
    final customer = invoice.customerName?.trim();
    final subtitle = [
      if (customer != null && customer.isNotEmpty) customer,
      dateFmt.format(invoice.invoiceDate),
    ].join(' · ');

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: Padding(
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
                      invoice.invoiceNumber,
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
                      subtitle,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 12,
                        color: AppColors.navy.withValues(alpha: .48),
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    currency.format(invoice.totalAmount),
                    style: const TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w800,
                      fontSize: 14.5,
                      color: AppColors.primary,
                    ),
                  ),
                  if (showPaymentTag) ...[
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: AppColors.green.withValues(alpha: .12),
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Text(
                        invoice.paymentMode,
                        style: const TextStyle(
                          fontFamily: AppFonts.family,
                          fontSize: 10.5,
                          fontWeight: FontWeight.w700,
                          color: AppColors.green,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

List<ReportSlice> billingTypeSlices(List<Invoice> invoices) {
  double pos = 0, table = 0, takeaway = 0, mess = 0, other = 0;
  for (final inv in invoices) {
    final type = inv.invoiceType.toLowerCase();
    if (type.contains('mess')) {
      mess += inv.totalAmount;
    } else if (type.contains('table')) {
      table += inv.totalAmount;
    } else if (type.contains('take')) {
      takeaway += inv.totalAmount;
    } else if (type.contains('pos') || type.contains('fast') || type.isEmpty) {
      pos += inv.totalAmount;
    } else {
      other += inv.totalAmount;
    }
  }
  return [
    ReportSlice(label: 'Fast Billing', value: pos, color: AppColors.primary),
    ReportSlice(label: 'Dine In', value: table, color: AppColors.green),
    ReportSlice(label: 'Take Away', value: takeaway, color: AppColors.orange),
    if (mess > 0)
      ReportSlice(label: 'Mess', value: mess, color: AppColors.yellow),
    if (other > 0)
      ReportSlice(label: 'Other', value: other, color: AppColors.purple),
  ];
}

List<ReportSlice> paymentSlices(SalesSummary summary, List<Invoice> invoices) {
  final cashUpi = invoices
      .where((e) => e.paymentMode == 'Cash+UPI')
      .fold<double>(0, (s, e) => s + e.totalAmount);
  return [
    ReportSlice(label: 'Cash', value: summary.cashTotal, color: AppColors.primary),
    ReportSlice(label: 'UPI', value: summary.upiTotal, color: AppColors.green),
    if (cashUpi > 0)
      ReportSlice(label: 'Cash+UPI', value: cashUpi, color: AppColors.purple),
  ];
}

String reportPeriodDisplayLabel(ReportPeriod period) {
  switch (period.kind) {
    case ReportPeriodKind.today:
      return 'Today';
    case ReportPeriodKind.month:
      return period.label == 'This month' ? 'This Month' : period.label;
    case ReportPeriodKind.day:
      return period.label;
    case ReportPeriodKind.year:
      return period.label;
  }
}
