import 'package:flutter/material.dart';

/* Billingwala brand palette — Aurora + Glass design system. */
class AppColors {
  AppColors._();

  static const Color primary = Color(0xFF0756C9);
  static const Color primaryDark = Color(0xFF062B73);
  static const Color primaryBright = Color(0xFF168BFF);

  /* Soft wash for chips / pills / selected rows (derived from primary). */
  static const Color primarySoft = Color(0xFFE7F0FC);
  static const Color primaryLight = primarySoft;

  static const Color cyan = Color(0xFF00CFFF);
  static const Color cyanSoft = Color(0xFFD6F7FF);

  static const Color orange = Color(0xFFFF7800);
  static const Color orangeDark = Color(0xFFE56A00);
  static const Color orangeLight = Color(0xFFFF9D1A);
  static const Color yellow = Color(0xFFFFC400);

  static const Color red = Color(0xFFDC2626);
  static const Color green = Color(0xFF16A34A);
  static const Color teal = Color(0xFF00CFFF);
  static const Color purple = Color(0xFF168BFF);

  /* High-contrast text on aurora / glass. */
  static const Color navy = Color(0xFF0B1F4A);
  static const Color card = Color(0xF5FFFFFF);
  /* Soft aurora-tinted page fill when a solid surface is required. */
  static const Color surface = Color(0xFFEAF4FF);

  static const Color success = green;
  static const Color warning = Color(0xFFF59E0B);
  static const Color danger = red;
  static const Color error = red;

  static const Color secondary = orange;
  static const Color secondaryLight = orangeLight;
  static const Color background = surface;
  static const Color textPrimary = navy;
  static const Color textSecondary = Color(0xFF3D4F6F);
  static const Color border = Color(0xFFC5D6EF);

  /* Aurora wash (page backdrop). */
  static const Color auroraTop = Color(0xFFEAF4FF);
  static const Color auroraMid = Color(0xFFD9ECFF);
  static const Color auroraBottom = Color(0xFFF3F0FF);
  static const Color auroraWarm = Color(0xFFFFF1E6);
  static const Color auroraBlobCyan = Color(0xFF7FE8FF);
  static const Color auroraBlobBlue = Color(0xFF6BA8FF);
  static const Color auroraBlobPeach = Color(0xFFFFB38A);
  static const Color auroraBlobLavender = Color(0xFFB8A6FF);

  static const Color glassFill = Color(0xB8FFFFFF);
  static const Color glassBorder = Color(0x99FFFFFF);
  static const Color glassHighlight = Color(0xCCFFFFFF);
  static const Color glassSolid = Color(0xF5FFFFFF);

  static const List<Color> auroraGradient = [
    auroraTop,
    auroraMid,
    auroraWarm,
    auroraBottom,
  ];

  static const List<Color> brandGradient = [
    primaryBright,
    primary,
    primaryDark,
  ];
  static const List<Color> warmGradient = [orangeLight, orange];
  static const List<Color> analyticsGradient = [primaryBright, primaryDark];
}
