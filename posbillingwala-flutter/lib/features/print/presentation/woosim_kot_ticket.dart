import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/print/domain/kot_slip_layout.dart';

/* On-screen KOT — same paper width / Poppins family as [WoosimTicket] invoice. */
class WoosimKotTicket extends StatelessWidget {
  const WoosimKotTicket({
    super.key,
    required this.layout,
    this.widthMm = 48,
  });

  final KotSlipLayout layout;
  final double widthMm;

  @override
  Widget build(BuildContext context) {
    final is2Inch = widthMm <= 55;
    final mm = widthMm;
    const black = Color(0xFF000000);
    final titleSize = is2Inch ? 16.0 : 20.0;
    final bodySize = is2Inch ? 13.0 : 16.0;
    final qtyW = is2Inch ? (widthMm <= 49 ? 42.0 : 46.0) : 56.0;

    TextStyle pop({double? size, FontWeight weight = FontWeight.w500}) =>
        AppFonts.printBody(
          fontSize: size ?? bodySize,
          weight: weight,
          height: 1.25,
        );

    Widget rule() => Container(
      height: 1,
      margin: const EdgeInsets.symmetric(horizontal: 5, vertical: 3),
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
            Padding(
              padding: const EdgeInsets.fromLTRB(5, 6, 5, 4),
              child: Text(
                layout.title,
                textAlign: TextAlign.center,
                style: pop(size: titleSize, weight: FontWeight.w700),
              ),
            ),
            rule(),
            for (final line in layout.metaLines)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                child: Text(line, textAlign: TextAlign.start),
              ),
            rule(),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      layout.colItem,
                      style: pop(weight: FontWeight.w700),
                    ),
                  ),
                  SizedBox(
                    width: qtyW,
                    child: Text(
                      layout.colQty,
                      textAlign: TextAlign.end,
                      style: pop(weight: FontWeight.w700),
                    ),
                  ),
                ],
              ),
            ),
            rule(),
            for (final item in layout.items)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 3),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Text(
                        item.name,
                        style: pop(weight: FontWeight.w500),
                      ),
                    ),
                    SizedBox(
                      width: qtyW,
                      child: Text(
                        item.qty,
                        textAlign: TextAlign.end,
                        style: pop(weight: FontWeight.w700),
                      ),
                    ),
                  ],
                ),
              ),
            rule(),
          ],
        ),
      ),
    );
  }
}
