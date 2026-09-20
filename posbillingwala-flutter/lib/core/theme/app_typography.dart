import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';

/* Central typography — readable sizes, soft weights for daily POS use. */
abstract final class AppTypography {
  static const appTypographyFamily = AppFonts.family;

  static TextStyle screenTitle({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 24,
    fontWeight: FontWeight.w700,
    height: 1.3,
    letterSpacing: -0.2,
    color: color ?? AppColors.navy,
  );

  static TextStyle sectionTitle({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 18,
    fontWeight: FontWeight.w600,
    height: 1.35,
    color: color ?? AppColors.navy,
  );

  static TextStyle cardTitle({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.35,
    color: color ?? AppColors.navy,
  );

  static TextStyle body({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 15,
    fontWeight: FontWeight.w500,
    height: 1.45,
    color: color ?? AppColors.textPrimary,
  );

  static TextStyle bodySmall({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 13,
    fontWeight: FontWeight.w500,
    height: 1.4,
    color: color ?? AppColors.textSecondary,
  );

  static TextStyle button({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.25,
    letterSpacing: 0.1,
    color: color ?? Colors.white,
  );

  static TextStyle amount({Color? color, double size = 22}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: size,
    fontWeight: FontWeight.w700,
    height: 1.2,
    letterSpacing: -0.3,
    color: color ?? AppColors.navy,
  );

  static TextStyle invoiceTotal({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 26,
    fontWeight: FontWeight.w700,
    height: 1.2,
    letterSpacing: -0.4,
    color: color ?? AppColors.primaryDark,
  );

  static TextStyle statusLabel({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 12,
    fontWeight: FontWeight.w600,
    height: 1.25,
    letterSpacing: 0.15,
    color: color ?? AppColors.navy,
  );

  static TextStyle tableHeader({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 13,
    fontWeight: FontWeight.w700,
    height: 1.3,
    color: color ?? AppColors.textPrimary,
  );

  static TextStyle caption({Color? color}) => TextStyle(
    fontFamily: appTypographyFamily,
    fontFamilyFallback: AppFonts.indicFallbacks,
    fontSize: 12,
    fontWeight: FontWeight.w500,
    height: 1.35,
    color: color ?? AppColors.textSecondary,
  );
}
