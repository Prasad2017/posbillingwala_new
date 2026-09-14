import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';

class AppTheme {
  AppTheme._();

  static ThemeData light() {
    final scheme = ColorScheme.fromSeed(
      seedColor: AppColors.primary,
      primary: AppColors.primary,
      secondary: AppColors.orange,
      error: AppColors.red,
      surface: Colors.white,
      brightness: Brightness.light,
    );

    final base = ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      fontFamily: AppFonts.family,
      scaffoldBackgroundColor: AppColors.surface,
      splashFactory: InkSparkle.splashFactory,
      dividerColor: const Color(0xFFE9EEF7),
    );

    final textTheme = base.textTheme.apply(
      fontFamily: AppFonts.family,
      bodyColor: AppColors.navy,
      displayColor: AppColors.navy,
    );

    return base.copyWith(
      textTheme: textTheme,
      primaryTextTheme: textTheme,
      appBarTheme: AppBarTheme(
        centerTitle: false,
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
        titleTextStyle: const TextStyle(
          fontFamily: AppFonts.family,
          color: Colors.white,
          fontSize: 21,
          fontWeight: FontWeight.w700,
        ),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        color: Colors.white,
        surfaceTintColor: Colors.transparent,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(22),
          side: BorderSide(color: AppColors.primary.withValues(alpha: .07)),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white,
          minimumSize: const Size(0, 52),
          padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 16),
          textStyle: const TextStyle(
            fontFamily: AppFonts.family,
            fontWeight: FontWeight.w700,
          ),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.primary,
          minimumSize: const Size(0, 50),
          side: const BorderSide(color: Color(0xFFBFD5F5)),
          textStyle: const TextStyle(
            fontFamily: AppFonts.family,
            fontWeight: FontWeight.w700,
          ),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        ),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 6,
        extendedPadding: const EdgeInsets.symmetric(horizontal: 20),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
        labelStyle: const TextStyle(
          fontFamily: AppFonts.family,
          fontWeight: FontWeight.w700,
        ),
        hintStyle: const TextStyle(
          fontFamily: AppFonts.family,
          color: Color(0xFF9AA7B9),
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: Color(0xFFDCE6F5)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: Color(0xFFDCE6F5)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.6),
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: AppColors.primaryLight,
        selectedColor: AppColors.primary,
        secondarySelectedColor: AppColors.primary,
        labelStyle: const TextStyle(
          fontFamily: AppFonts.family,
          fontWeight: FontWeight.w700,
        ),
        side: BorderSide.none,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: Colors.white,
        elevation: 7,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        titleTextStyle: const TextStyle(
          fontFamily: AppFonts.family,
          fontWeight: FontWeight.w700,
          fontSize: 17,
          color: AppColors.navy,
        ),
        contentTextStyle: const TextStyle(
          fontFamily: AppFonts.family,
          fontWeight: FontWeight.w400,
          fontSize: 14,
          color: AppColors.navy,
        ),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
      ),
    );
  }
}
