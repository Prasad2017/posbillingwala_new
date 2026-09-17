import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';

/* Official Billingwala logo from `assets/images/png`. */
class BrandLogo extends StatelessWidget {
  const BrandLogo({super.key, this.width = 230, this.useSplashArt = false});

  final double width;
  final bool useSplashArt;

  @override
  Widget build(BuildContext context) {
    final primary = useSplashArt ? AppAssets.splashBranding : AppAssets.appLogo;
    return Image.asset(
      primary,
      width: width,
      fit: BoxFit.contain,
      errorBuilder: (context, error, stackTrace) => Image.asset(
        AppAssets.appLogo,
        width: width,
        fit: BoxFit.contain,
        errorBuilder: (context, error, stackTrace) => Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'POS²',
              style: TextStyle(
                fontFamily: AppFonts.family,
                color: AppColors.primaryDark,
                fontWeight: FontWeight.w900,
                fontSize: width * .24,
                height: .9,
              ),
            ),
            Text(
              'BillingWala',
              style: TextStyle(
                fontFamily: AppFonts.family,
                color: AppColors.primaryDark,
                fontWeight: FontWeight.w900,
                fontSize: width * .17,
                height: .95,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
