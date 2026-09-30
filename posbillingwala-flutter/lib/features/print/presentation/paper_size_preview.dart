import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

/* On-screen receipt preview sized from [PrinterPaperProfile]. */
class PaperSizePreviewCard extends StatelessWidget {
  const PaperSizePreviewCard({
    super.key,
    required this.title,
    required this.text,
    required this.paperSize,
    this.largeType = false,
  });

  final String title;
  final String text;
  final PrinterPaperSize paperSize;
  /* KOT / kitchen tickets — larger type + monospace for column alignment. */
  final bool largeType;

  @override
  Widget build(BuildContext context) {
    final profile = paperSize.profile;
    final fontSize = largeType
        ? (profile.isNarrowLayout ? 15.0 : 16.0)
        : (profile.isNarrowLayout ? 12.0 : 11.0);
    final previewWidth = largeType
        ? profile.charsPerLine * 9.2
        : profile.onScreenPreviewWidth;

    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.fromLTRB(10, 10, 12, 12),
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
            profile.detailLabel,
            style: const TextStyle(
              fontSize: 11,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 8),
          Center(
            child: Container(
              width: previewWidth,
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
                  fontSize: fontSize,
                  height: largeType ? 1.35 : 1.28,
                  weight: FontWeight.w600,
                ).copyWith(
                  /* Monospace keeps qty flush-right with space padding. */
                  fontFamily: largeType ? 'Courier New' : null,
                  fontFamilyFallback: largeType
                      ? const [
                          'Courier New',
                          'Consolas',
                          'monospace',
                          ...AppFonts.indicFallbacks,
                        ]
                      : AppFonts.printFallbacks,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
