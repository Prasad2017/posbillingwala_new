import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';
import 'package:qr_flutter/qr_flutter.dart';

/* A4 tax-invoice preview/share — same [ThermalTicket] data as thermal bills. */
class A4InvoiceTicket extends StatelessWidget {
  const A4InvoiceTicket({
    super.key,
    required this.ticket,
    this.logoPath,
    this.showLogo = false,
  });

  final ThermalTicket ticket;
  final String? logoPath;
  final bool showLogo;

  static const pageW = 595.0; /* ~A4 @ 72dpi */

  @override
  Widget build(BuildContext context) {
    const ink = Color(0xFF1A1A1A);
    const line = Color(0xFFBDBDBD);
    const accent = Color(0xFFE3F2FD);
    TextStyle t({
      double size = 11,
      FontWeight weight = FontWeight.w500,
      Color? color,
    }) =>
        AppFonts.printBody(
          fontSize: size,
          weight: weight,
          height: 1.25,
          color: color ?? ink,
        );

    final path = logoPath?.trim() ?? '';
    final canShowLogo =
        showLogo && path.isNotEmpty && !kIsWeb && File(path).existsSync();
    final qr = ticket.qrPayload?.trim() ?? '';
    final shopTitle =
        ticket.shopLines.isNotEmpty ? ticket.shopLines.first : 'Billingwala';
    final shopRest =
        ticket.shopLines.length > 1 ? ticket.shopLines.sublist(1) : <String>[];

    Widget cell(String text, {FontWeight w = FontWeight.w500, TextAlign a = TextAlign.left}) =>
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 5),
          child: Text(text, textAlign: a, style: t(size: 10, weight: w)),
        );

    return Container(
      width: pageW,
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(28, 24, 28, 24),
      child: DefaultTextStyle(
        style: t(),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (canShowLogo)
                  Padding(
                    padding: const EdgeInsets.only(right: 12),
                    child: Image.file(
                      File(path),
                      width: 56,
                      height: 56,
                      fit: BoxFit.contain,
                      errorBuilder: (_, _, _) => const SizedBox.shrink(),
                    ),
                  ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        shopTitle,
                        style: t(size: 16, weight: FontWeight.w800),
                      ),
                      for (final line in shopRest)
                        Text(line, style: t(size: 10)),
                    ],
                  ),
                ),
                Container(
                  width: 200,
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    border: Border.all(color: line),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        ticket.taxInvoiceTitle,
                        style: t(size: 12, weight: FontWeight.w800),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        ticket.copyBanner,
                        style: t(size: 9, weight: FontWeight.w600),
                      ),
                      const SizedBox(height: 6),
                      for (final line in ticket.metaLines.take(6))
                        Padding(
                          padding: const EdgeInsets.only(bottom: 2),
                          child: Text(line, style: t(size: 9)),
                        ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                border: Border.all(color: line),
                borderRadius: BorderRadius.circular(4),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Customer',
                    style: t(size: 11, weight: FontWeight.w700),
                  ),
                  const SizedBox(height: 4),
                  Text('Name: ${ticket.customerName}', style: t(size: 10)),
                  if (ticket.customerMobile.isNotEmpty)
                    Text(
                      'Mobile: ${ticket.customerMobile}',
                      style: t(size: 10),
                    ),
                  if (ticket.customerAddress.isNotEmpty)
                    Text(
                      'Address: ${ticket.customerAddress}',
                      style: t(size: 10),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            Table(
              border: TableBorder.all(color: line, width: 0.8),
              columnWidths: const {
                0: FixedColumnWidth(36),
                1: FlexColumnWidth(3.2),
                2: FixedColumnWidth(48),
                3: FixedColumnWidth(64),
                4: FixedColumnWidth(72),
              },
              children: [
                TableRow(
                  decoration: const BoxDecoration(color: Color(0xFFF5F5F5)),
                  children: [
                    cell('Sr', w: FontWeight.w700, a: TextAlign.center),
                    cell(ticket.colItem, w: FontWeight.w700),
                    cell(ticket.colQty, w: FontWeight.w700, a: TextAlign.center),
                    cell(ticket.colRate, w: FontWeight.w700, a: TextAlign.right),
                    cell(
                      ticket.colAmount,
                      w: FontWeight.w700,
                      a: TextAlign.right,
                    ),
                  ],
                ),
                for (var i = 0; i < ticket.items.length; i++)
                  TableRow(
                    children: [
                      cell('${i + 1}', a: TextAlign.center),
                      cell(ticket.items[i].name),
                      cell(ticket.items[i].qty, a: TextAlign.center),
                      cell(ticket.items[i].rate, a: TextAlign.right),
                      cell(ticket.items[i].amount, a: TextAlign.right),
                    ],
                  ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (ticket.amountInWords.isNotEmpty) ...[
                        Text(
                          'Amount in Words',
                          style: t(size: 10, weight: FontWeight.w700),
                        ),
                        Text(ticket.amountInWords, style: t(size: 10)),
                        const SizedBox(height: 10),
                      ],
                      if (ticket.upiLine.isNotEmpty)
                        Text(ticket.upiLine, style: t(size: 10, weight: FontWeight.w600)),
                      if (ticket.terms.trim().isNotEmpty) ...[
                        const SizedBox(height: 10),
                        Text(
                          'Terms & Conditions',
                          style: t(size: 10, weight: FontWeight.w700),
                        ),
                        Text(ticket.terms.trim(), style: t(size: 9)),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                SizedBox(
                  width: 220,
                  child: Column(
                    children: [
                      for (final pair in ticket.pairs)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 2),
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(pair.$1, style: t(size: 10)),
                              ),
                              Text(pair.$2, style: t(size: 10, weight: FontWeight.w600)),
                            ],
                          ),
                        ),
                      if (ticket.grandTotalLabel.isNotEmpty)
                        Container(
                          margin: const EdgeInsets.only(top: 6),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 8,
                          ),
                          color: accent,
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  ticket.grandTotalLabel,
                                  style: t(size: 12, weight: FontWeight.w800),
                                ),
                              ),
                              Text(
                                ticket.grandTotalValue,
                                style: t(size: 13, weight: FontWeight.w800),
                              ),
                            ],
                          ),
                        ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      for (final line in ticket.footerLines)
                        Text(line, style: t(size: 11, weight: FontWeight.w600)),
                    ],
                  ),
                ),
                if (qr.isNotEmpty)
                  Column(
                    children: [
                      QrImageView(
                        data: qr,
                        size: 88,
                        padding: EdgeInsets.zero,
                        backgroundColor: Colors.white,
                      ),
                      Text('Scan for digital copy', style: t(size: 8)),
                    ],
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
