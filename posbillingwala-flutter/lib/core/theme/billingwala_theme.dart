import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';

/* Aurora + glass design tokens for Billingwala. */
@immutable
class BillingwalaTheme extends ThemeExtension<BillingwalaTheme> {
  const BillingwalaTheme({
    required this.auroraColors,
    required this.auroraBlobColors,
    required this.glassFill,
    required this.glassBorder,
    required this.glassHighlight,
    required this.glassBlurSigma,
    required this.solidAction,
    required this.solidActionForeground,
    required this.highContrastText,
    required this.mutedText,
  });

  final List<Color> auroraColors;
  final List<Color> auroraBlobColors;
  final Color glassFill;
  final Color glassBorder;
  final Color glassHighlight;
  final double glassBlurSigma;
  final Color solidAction;
  final Color solidActionForeground;
  final Color highContrastText;
  final Color mutedText;

  static const light = BillingwalaTheme(
    auroraColors: AppColors.auroraGradient,
    auroraBlobColors: [
      AppColors.auroraBlobCyan,
      AppColors.auroraBlobBlue,
      AppColors.auroraBlobPeach,
      AppColors.auroraBlobLavender,
    ],
    glassFill: AppColors.glassFill,
    glassBorder: AppColors.glassBorder,
    glassHighlight: AppColors.glassHighlight,
    glassBlurSigma: 18,
    solidAction: AppColors.primary,
    solidActionForeground: Colors.white,
    highContrastText: AppColors.textPrimary,
    mutedText: AppColors.textSecondary,
  );

  static BillingwalaTheme of(BuildContext context) {
    return Theme.of(context).extension<BillingwalaTheme>() ?? light;
  }

  ImageFilter get glassFilter => ImageFilter.blur(
    sigmaX: glassBlurSigma,
    sigmaY: glassBlurSigma,
  );

  @override
  BillingwalaTheme copyWith({
    List<Color>? auroraColors,
    List<Color>? auroraBlobColors,
    Color? glassFill,
    Color? glassBorder,
    Color? glassHighlight,
    double? glassBlurSigma,
    Color? solidAction,
    Color? solidActionForeground,
    Color? highContrastText,
    Color? mutedText,
  }) {
    return BillingwalaTheme(
      auroraColors: auroraColors ?? this.auroraColors,
      auroraBlobColors: auroraBlobColors ?? this.auroraBlobColors,
      glassFill: glassFill ?? this.glassFill,
      glassBorder: glassBorder ?? this.glassBorder,
      glassHighlight: glassHighlight ?? this.glassHighlight,
      glassBlurSigma: glassBlurSigma ?? this.glassBlurSigma,
      solidAction: solidAction ?? this.solidAction,
      solidActionForeground:
          solidActionForeground ?? this.solidActionForeground,
      highContrastText: highContrastText ?? this.highContrastText,
      mutedText: mutedText ?? this.mutedText,
    );
  }

  @override
  BillingwalaTheme lerp(ThemeExtension<BillingwalaTheme>? other, double t) {
    if (other is! BillingwalaTheme) return this;
    return BillingwalaTheme(
      auroraColors: auroraColors,
      auroraBlobColors: auroraBlobColors,
      glassFill: Color.lerp(glassFill, other.glassFill, t) ?? glassFill,
      glassBorder: Color.lerp(glassBorder, other.glassBorder, t) ?? glassBorder,
      glassHighlight:
          Color.lerp(glassHighlight, other.glassHighlight, t) ?? glassHighlight,
      glassBlurSigma: lerpDouble(glassBlurSigma, other.glassBlurSigma, t) ??
          glassBlurSigma,
      solidAction: Color.lerp(solidAction, other.solidAction, t) ?? solidAction,
      solidActionForeground:
          Color.lerp(solidActionForeground, other.solidActionForeground, t) ??
          solidActionForeground,
      highContrastText:
          Color.lerp(highContrastText, other.highContrastText, t) ??
          highContrastText,
      mutedText: Color.lerp(mutedText, other.mutedText, t) ?? mutedText,
    );
  }
}
