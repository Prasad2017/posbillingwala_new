import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';

/* On-screen clone of WithTable `twoLinearLayout` / `threeLinearLayout`. */
class WoosimTicket extends StatelessWidget {
  const WoosimTicket({
    super.key,
    required this.ticket,
    this.widthMm = 48,
  });

  final ThermalTicket ticket;
  final double widthMm;

  @override
  Widget build(BuildContext context) {
    final is2Inch = widthMm <= 50;
    final mm = widthMm;
    const black = Color(0xFF000000);
    final shopSize = is2Inch ? 14.0 : 18.0;
    final bodySize = is2Inch ? 12.0 : 16.0;
    final rateW = is2Inch ? 48.0 : 64.0;
    final amountW = is2Inch ? 56.0 : 72.0;
    TextStyle pop({
      double? size,
      FontWeight weight = FontWeight.w500,
    }) =>
        TextStyle(
          fontFamily: AppFonts.family,
          fontSize: size ?? bodySize,
          height: 1.2,
          color: black,
          fontWeight: weight,
        );

    Widget rule() => Container(
          height: 1,
          margin: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
          color: black,
        );

    return Container(
      width: mm * 3.78,
      color: Colors.white,
      padding: const EdgeInsets.all(2),
      child: DefaultTextStyle(
        style: pop(),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            for (final line in ticket.shopLines)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                child: Text(
                  line,
                  textAlign: TextAlign.center,
                  style: pop(size: shopSize, weight: FontWeight.w700),
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
            for (final line in ticket.footerLines)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                child: Text(
                  line,
                  textAlign: TextAlign.center,
                  style: pop(size: 13, weight: FontWeight.w600),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
