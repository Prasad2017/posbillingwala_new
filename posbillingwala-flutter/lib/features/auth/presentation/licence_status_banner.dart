import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/licence_display.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_widgets.dart';

/* Shows current licence plan; testing/trial users get an Upgrade action. */
class LicenceStatusBanner extends ConsumerWidget {
  const LicenceStatusBanner({super.key, this.compact = false});

  final bool compact;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(authControllerProvider).session;
    if (session == null || session.userId.isEmpty) {
      return const SizedBox.shrink();
    }

    final testing = LicenceDisplay.isTestingLicence(session);
    final plan = LicenceDisplay.planLabel(session);
    final detail = LicenceDisplay.subtitle(session);
    final accent = testing ? const Color(0xFFB45309) : AppColors.primary;
    final bg = testing ? const Color(0xFFFFF7ED) : const Color(0xFFEEF4FF);
    final border = testing
        ? const Color(0xFFFDBA74)
        : AppColors.primary.withValues(alpha: 0.22);

    return Material(
      color: bg,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: testing ? () => callSupport(context) : null,
        child: Container(
          width: double.infinity,
          padding: EdgeInsets.symmetric(
            horizontal: compact ? 12 : 14,
            vertical: compact ? 10 : 12,
          ),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: border),
          ),
          child: Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: accent.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(
                  testing
                      ? Icons.science_outlined
                      : Icons.verified_outlined,
                  color: accent,
                  size: 20,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      plan,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontWeight: FontWeight.w800,
                        fontSize: 13.5,
                        color: AppColors.navy,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      detail,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 11.5,
                        fontWeight: FontWeight.w500,
                        color: AppColors.navy.withValues(alpha: 0.62),
                      ),
                    ),
                  ],
                ),
              ),
              if (testing) ...[
                const SizedBox(width: 8),
                Material(
                  color: accent,
                  borderRadius: BorderRadius.circular(10),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(10),
                    onTap: () => callSupport(context),
                    child: const Padding(
                      padding: EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                      child: Text(
                        'Upgrade',
                        style: TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w800,
                          fontSize: 12.5,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
