import 'package:flutter/material.dart';

/* Billingwala brand palette. */
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

  static const Color navy = Color(0xFF102A5C);
  static const Color card = Color(0xFFFFFFFF);
  static const Color surface = Color(0xFFF7F9FC);

  static const Color success = green;
  static const Color warning = Color(0xFFF59E0B);
  static const Color danger = red;
  static const Color error = red;

  static const Color secondary = orange;
  static const Color secondaryLight = orangeLight;
  static const Color background = surface;
  static const Color textPrimary = navy;
  static const Color textSecondary = Color(0xFF64748B);
  static const Color border = Color(0xFFE2E8F0);

  static const List<Color> brandGradient = [primaryBright, primary, primaryDark];
  static const List<Color> warmGradient = [orangeLight, orange];
  static const List<Color> analyticsGradient = [primaryBright, primaryDark];
}
