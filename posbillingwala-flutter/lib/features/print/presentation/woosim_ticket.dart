import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';
import 'package:qr_flutter/qr_flutter.dart';

/* On-screen thermal bill — 48mm ≈ 2″ (58mm), 72mm ≈ 3″ (80mm). */
class WoosimTicket extends StatelessWidget {
  const WoosimTicket({
    super.key,
    required this.ticket,
    this.widthMm = 48,
    this.logoPath,
    this.showLogo = false,
  });

  final ThermalTicket ticket;
  final double widthMm;
  final String? logoPath;
  final bool showLogo;

  @override
  Widget build(BuildContext context) {
    final is2Inch = widthMm <= 50;
    final mm = widthMm;
    const black = Color(0xFF000000);
    final shopSize = is2Inch ? 14.0 : 18.0;
    final bodySize = is2Inch ? 11.0 : 14.0;
    final qtyW = is2Inch ? 28.0 : 36.0;
    final rateW = is2Inch ? 42.0 : 56.0;
    final amountW = is2Inch ? 48.0 : 64.0;
    final logoSize = is2Inch ? 56.0 : 80.0;
    final qrSize = is2Inch ? 96.0 : 130.0;
    TextStyle pop({double? size, FontWeight weight = FontWeight.w500}) =>
        AppFonts.printBody(
          fontSize: size ?? bodySize,
          weight: weight,
          height: 1.2,
        );

    Widget rule() => Container(
      height: 1,
      margin: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
      color: black,
    );

    Widget headerRow(List<(String, double, TextAlign, FontWeight)> cols) {
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: 5),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final c in cols)
              c.$2 < 0
                  ? Expanded(
                      child: Text(
                        c.$1,
                        textAlign: c.$3,
                        style: pop(weight: c.$4),
                      ),
                    )
                  : SizedBox(
                      width: c.$2,
                      child: Text(
                        c.$1,
                        textAlign: c.$3,
                        style: pop(weight: c.$4),
                      ),
                    ),
          ],
        ),
      );
    }

    final path = logoPath?.trim() ?? '';
    final canShowLogo =
        showLogo && path.isNotEmpty && !kIsWeb && File(path).existsSync();
    final qr = ticket.qrPayload?.trim() ?? '';
    final usePairs = !is2Inch && ticket.metaPairs.isNotEmpty;

    return Container(
      width: mm * 3.78,
      color: Colors.white,
      padding: const EdgeInsets.all(2),
      child: DefaultTextStyle(
        style: pop(),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (canShowLogo)
              Padding(
                padding: const EdgeInsets.only(top: 6, bottom: 4),
                child: Center(
                  child: Image.file(
                    File(path),
                    width: logoSize,
                    height: logoSize,
                    fit: BoxFit.contain,
                    errorBuilder: (_, _, _) => const SizedBox.shrink(),
                  ),
                ),
              ),
            for (var i = 0; i < ticket.shopLines.length; i++)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                child: Text(
                  ticket.shopLines[i],
                  textAlign: TextAlign.center,
                  style: pop(
                    size: i == 0 ? shopSize : bodySize,
                    weight: i == 0 ? FontWeight.w700 : FontWeight.w500,
                  ),
                ),
              ),
            if (usePairs)
              for (final pair in ticket.metaPairs)
                if (pair.$1.isNotEmpty || pair.$2.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 5,
                      vertical: 1,
                    ),
                    child: Row(
                      children: [
                        Expanded(child: Text(pair.$1, style: pop())),
                        Expanded(
                          child: Text(
                            pair.$2,
                            textAlign: TextAlign.end,
                            style: pop(),
                          ),
                        ),
                      ],
                    ),
                  )
            else
              for (final line in ticket.metaLines)
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 5,
                    vertical: 1,
                  ),
                  child: Text(line, textAlign: TextAlign.start),
                ),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Text(
                ticket.copyBanner,
                textAlign: TextAlign.center,
                style: pop(size: bodySize, weight: FontWeight.w600),
              ),
            ),
            rule(),
            headerRow([
              (ticket.colItem, -1, TextAlign.left, FontWeight.w700),
              (ticket.colQty, qtyW, TextAlign.center, FontWeight.w700),
              (ticket.colRate, rateW, TextAlign.center, FontWeight.w700),
              (ticket.colAmount, amountW, TextAlign.end, FontWeight.w700),
            ]),
            rule(),
            for (final item in ticket.items)
              Padding(
                padding: const EdgeInsets.fromLTRB(5, 3, 5, 3),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Text(item.name, style: pop(weight: FontWeight.w500)),
                    ),
                    SizedBox(
                      width: qtyW,
                      child: Text(item.qty, textAlign: TextAlign.center),
                    ),
                    SizedBox(
                      width: rateW,
                      child: Text(item.rate, textAlign: TextAlign.center),
                    ),
                    SizedBox(
                      width: amountW,
                      child: Text(item.amount, textAlign: TextAlign.end),
                    ),
                  ],
                ),
              ),
            rule(),
            if (ticket.totalItemsLine.trim().isNotEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                child: Text(
                  ticket.totalItemsLine,
                  style: pop(weight: FontWeight.w600),
                ),
              ),
            for (final pair in ticket.pairs)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(pair.$1, style: pop(weight: FontWeight.w600)),
                    ),
                    Text(pair.$2, style: pop(weight: FontWeight.w600)),
                  ],
                ),
              ),
            if (ticket.grandTotalLabel.trim().isNotEmpty)
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
                color: const Color(0xFFE3F2FD),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        ticket.grandTotalLabel,
                        style: pop(
                          size: bodySize + 2,
                          weight: FontWeight.w800,
                        ),
                      ),
                    ),
                    Text(
                      ticket.grandTotalValue,
                      style: pop(
                        size: bodySize + 3,
                        weight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
            rule(),
            if (ticket.upiLine.trim().isNotEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                child: Text(
                  ticket.upiLine,
                  textAlign: TextAlign.center,
                  style: pop(weight: FontWeight.w600),
                ),
              ),
            if (ticket.terms.trim().isNotEmpty)
              Padding(
                padding: const EdgeInsets.all(5),
                child: Text(
                  ticket.terms.trim(),
                  textAlign: TextAlign.center,
                  style: pop(size: bodySize * 0.9),
                ),
              ),
            if (qr.isNotEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Center(
                  child: QrImageView(
                    data: qr,
                    size: qrSize,
                    padding: EdgeInsets.zero,
                    backgroundColor: Colors.white,
                    eyeStyle: const QrEyeStyle(
                      eyeShape: QrEyeShape.square,
                      color: Colors.black,
                    ),
                    dataModuleStyle: const QrDataModuleStyle(
                      dataModuleShape: QrDataModuleShape.square,
                      color: Colors.black,
                    ),
                  ),
                ),
              ),
            for (final line in ticket.footerLines)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                child: Text(
                  line,
                  textAlign: TextAlign.center,
                  style: pop(size: bodySize, weight: FontWeight.w500),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
