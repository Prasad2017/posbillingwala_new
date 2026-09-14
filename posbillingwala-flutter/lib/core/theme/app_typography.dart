import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';

/// Central typography for POS Billingwala.
abstract final class AppTypography {
  static const _family = AppFonts.family;

  static TextStyle screenTitle({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 22,
        fontWeight: FontWeight.w800,
        height: 1.25,
        color: color ?? AppColors.navy,
      );

  static TextStyle sectionTitle({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 17,
        fontWeight: FontWeight.w800,
        height: 1.3,
        color: color ?? AppColors.navy,
      );

  static TextStyle cardTitle({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 15,
        fontWeight: FontWeight.w700,
        height: 1.3,
        color: color ?? AppColors.navy,
      );

  static TextStyle body({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 14,
        fontWeight: FontWeight.w500,
        height: 1.4,
        color: color ?? AppColors.navy.withValues(alpha: .78),
      );

  static TextStyle bodySmall({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 12,
        fontWeight: FontWeight.w500,
        height: 1.35,
        color: color ?? AppColors.navy.withValues(alpha: .62),
      );

  static TextStyle button({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 15,
        fontWeight: FontWeight.w700,
        height: 1.2,
        color: color ?? Colors.white,
      );

  static TextStyle amount({Color? color, double size = 20}) => TextStyle(
        fontFamily: _family,
        fontSize: size,
        fontWeight: FontWeight.w800,
        height: 1.2,
        color: color ?? AppColors.navy,
      );

  static TextStyle invoiceTotal({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 24,
        fontWeight: FontWeight.w900,
        height: 1.15,
        color: color ?? AppColors.primaryDark,
      );

  static TextStyle statusLabel({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 11,
        fontWeight: FontWeight.w700,
        height: 1.2,
        letterSpacing: 0.2,
        color: color ?? AppColors.navy,
      );

  static TextStyle tableHeader({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 12,
        fontWeight: FontWeight.w700,
        height: 1.25,
        color: color ?? AppColors.navy.withValues(alpha: .7),
      );

  static TextStyle caption({Color? color}) => TextStyle(
        fontFamily: _family,
        fontSize: 11,
        fontWeight: FontWeight.w500,
        height: 1.3,
        color: color ?? AppColors.navy.withValues(alpha: .5),
      );
}
