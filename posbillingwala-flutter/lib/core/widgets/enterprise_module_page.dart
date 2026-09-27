import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

/* Enterprise module shell — production chrome for modules rolling out by vertical. */
class EnterpriseModulePage extends StatelessWidget {
  const EnterpriseModulePage({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    this.bullets = const [],
    this.primaryActionLabel,
    this.onPrimaryAction,
    this.color = AppColors.primary,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final List<String> bullets;
  final String? primaryActionLabel;
  final VoidCallback? onPrimaryAction;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: Text(title),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () {
            if (context.canPop()) {
              context.pop();
            } else {
              context.go('/');
            }
          },
        ),
      ),
      body: ResponsivePageBody(
        dashboard: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: EdgeInsets.all(
                context.isCompactWidth ? 18 : 24,
              ),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    color.withValues(alpha: .92),
                    color.withValues(alpha: .72),
                    AppColors.primaryDark.withValues(alpha: .85),
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(22),
                boxShadow: [
                  BoxShadow(
                    color: color.withValues(alpha: .25),
                    blurRadius: 18,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: Row(
                children: [
                  Container(
                    width: 56,
                    height: 56,
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: .18),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Icon(icon, color: Colors.white, size: 28),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            color: Colors.white,
                            fontWeight: FontWeight.w800,
                            fontSize: 20,
                          ),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          subtitle,
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            color: Colors.white.withValues(alpha: .88),
                            fontWeight: FontWeight.w500,
                            fontSize: 13.5,
                            height: 1.35,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),
            Container(
              padding: const EdgeInsets.fromLTRB(18, 16, 18, 18),
              decoration: BoxDecoration(
                color: AppColors.glassFill,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: AppColors.glassBorder, width: 1.2),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Module capabilities',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      color: AppColors.navy,
                      fontWeight: FontWeight.w800,
                      fontSize: 15,
                    ),
                  ),
                  const SizedBox(height: 12),
                  for (final bullet in bullets) ...[
                    Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Icon(
                            Icons.check_circle_rounded,
                            size: 18,
                            color: color,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              bullet,
                              style: TextStyle(
                                fontFamily: AppFonts.family,
                                color: AppColors.navy.withValues(alpha: .78),
                                fontWeight: FontWeight.w500,
                                fontSize: 13.5,
                                height: 1.35,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                  if (bullets.isEmpty)
                    Text(
                      'This module is enabled for your business type and will sync with cloud masters.',
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        color: AppColors.navy.withValues(alpha: .65),
                        fontSize: 13.5,
                      ),
                    ),
                ],
              ),
            ),
            if (primaryActionLabel != null && onPrimaryAction != null) ...[
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: onPrimaryAction,
                icon: const Icon(Icons.arrow_forward_rounded),
                label: Text(primaryActionLabel!),
                style: FilledButton.styleFrom(
                  backgroundColor: color,
                  minimumSize: const Size.fromHeight(52),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
