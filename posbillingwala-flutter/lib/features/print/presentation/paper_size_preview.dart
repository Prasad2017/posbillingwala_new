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
  });

  final String title;
  final String text;
  final PrinterPaperSize paperSize;

  @override
  Widget build(BuildContext context) {
    final profile = paperSize.profile;
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
              width: profile.onScreenPreviewWidth,
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
                  fontSize: profile.isNarrowLayout ? 12 : 11,
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
