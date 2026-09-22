import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

/* On-screen 58mm vs 80mm receipt — same print font stack as thermal raster */
/* so Marathi / Hindi / English user data looks like the printed bill. */
class PaperSizePreviewCard extends StatelessWidget {
  const PaperSizePreviewCard({
    super.key,
    required this.title,
    required this.text,
    required this.paperSize,
  });

  final String title;
  final String text;
  final PrinterPaperSize paperSize;

  bool get is3Inch => paperSize == PrinterPaperSize.inch3;

  /* ~7.5px per thermal character: 32 → 240, 48 → 360. */
  double get paperWidth => is3Inch ? 360 : 240;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.fromLTRB(10, 10, 10, 12),
      decoration: BoxDecoration(
        color: const Color(0xFFF3F6FB),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFE2E8F2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
          const SizedBox(height: 4),
          Text(
            is3Inch
                ? '80mm · 48 characters · multi-language data'
                : '58mm · 32 characters · multi-language data',
            style: const TextStyle(
              fontSize: 11,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 8),
          Center(
            child: Container(
              width: paperWidth,
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 10),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(4),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x14000000),
                    blurRadius: 8,
                    offset: Offset(0, 2),
                  ),
                ],
              ),
              child: SelectableText(
                text,
                style: AppFonts.printBody(
                  fontSize: is3Inch ? 11 : 12,
                  height: 1.28,
                  weight: FontWeight.w500,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
