import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';
import 'package:qr_flutter/qr_flutter.dart';

/* On-screen clone of WithTable `twoLinearLayout` / `threeLinearLayout`. */
/* widthMm 48 ≈ 2-Inch (58mm), 72 ≈ 3-Inch (80mm). */
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
    final bodySize = is2Inch ? 12.0 : 16.0;
    final rateW = is2Inch ? 48.0 : 64.0;
    final amountW = is2Inch ? 56.0 : 72.0;
    final logoSize = is2Inch ? 64.0 : 88.0;
    final qrSize = is2Inch ? 110.0 : 150.0;
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

    final path = logoPath?.trim() ?? '';
    final canShowLogo =
        showLogo && path.isNotEmpty && !kIsWeb && File(path).existsSync();
    final qr = ticket.qrPayload?.trim() ?? '';

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
                    /* Shop name larger; address / phone / GST = Bill No size. */
                    size: i == 0 ? shopSize : bodySize,
                    weight: i == 0 ? FontWeight.w700 : FontWeight.w500,
                  ),
                ),
              ),
            for (final line in ticket.metaLines)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                child: Text(line, textAlign: TextAlign.start),
              ),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Text(
                ticket.copyBanner,
                textAlign: TextAlign.center,
                style: pop(size: 14, weight: FontWeight.w500),
              ),
            ),
            rule(),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 5),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      ticket.colItem,
                      style: pop(weight: FontWeight.w700),
                    ),
                  ),
                  SizedBox(
                    width: rateW,
                    child: Text(
                      ticket.colRate,
                      textAlign: TextAlign.center,
                      style: pop(weight: FontWeight.w700),
                    ),
                  ),
                  SizedBox(
                    width: amountW,
                    child: Text(
                      ticket.colAmount,
                      textAlign: TextAlign.end,
                      style: pop(weight: FontWeight.w700),
                    ),
                  ),
                ],
              ),
            ),
            rule(),
            for (final item in ticket.items) ...[
              Padding(
                padding: const EdgeInsets.fromLTRB(5, 4, 5, 0),
                child: Text(item.name, style: pop(weight: FontWeight.w500)),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(5, 0, 5, 4),
                child: Row(
                  children: [
                    Expanded(child: Text('X${item.qty}')),
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
            ],
            rule(),
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
            rule(),
            if (ticket.terms.trim().isNotEmpty)
              Padding(
                padding: const EdgeInsets.all(5),
                child: Text(
                  ticket.terms.trim(),
                  textAlign: TextAlign.center,
                  style: pop(size: 13),
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
                  /* Powered by / website — same size as Bill No. */
                  style: pop(size: bodySize, weight: FontWeight.w500),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
