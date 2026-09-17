import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/brand_logo.dart';

/* Desktop auth chrome: navy brand panel + centered form. Mobile callers skip this. */
class WebAuthShell extends StatelessWidget {
  const WebAuthShell({
    super.key,
    required this.child,
    this.maxContentWidth = 440,
  });

  final Widget child;
  final double maxContentWidth;

  static bool get enabled => AppPlatform.useDesktopShell;

  @override
  Widget build(BuildContext context) {
    final wide = context.widthClass.index >= AppWidthClass.expanded.index;
    return Scaffold(
      backgroundColor: const Color(0xFFF4F7FB),
      body: Row(
        children: [
          if (wide) const Expanded(flex: 5, child: WebAuthBrandPanel()),
          Expanded(
            flex: 6,
            child: SafeArea(
              child: Center(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 28,
                    vertical: 32,
                  ),
                  child: ConstrainedBox(
                    constraints: BoxConstraints(maxWidth: maxContentWidth),
                    child: child,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class WebAuthBrandPanel extends StatelessWidget {
  const WebAuthBrandPanel({super.key});

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            AppColors.primaryDark,
            AppColors.primary,
            AppColors.primaryBright,
          ],
        ),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(40, 40, 40, 32),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: Image.asset(
                  AppAssets.appLogo,
                  width: 56,
                  height: 56,
                  fit: BoxFit.cover,
                  errorBuilder: (_, _, _) => const BrandLogo(width: 56),
                ),
              ),
              const Spacer(),
              const Text(
                AppConstants.appName,
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  color: Colors.white,
                  fontWeight: FontWeight.w800,
                  fontSize: 34,
                  height: 1.15,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Web POS · Online only',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  color: Colors.white.withValues(alpha: .88),
                  fontWeight: FontWeight.w600,
                  fontSize: 16,
                ),
              ),
              const SizedBox(height: 18),
              Text(
                'Bill, manage catalog, and view reports from the browser. Keep the Android or iOS app for offline billing.',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  color: Colors.white.withValues(alpha: .72),
                  fontWeight: FontWeight.w400,
                  fontSize: 14.5,
                  height: 1.45,
                ),
              ),
              const Spacer(),
              Text(
                AppConstants.websiteDisplay,
                style: AppTypography.bodySmall(
                  color: Colors.white.withValues(alpha: .7),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
