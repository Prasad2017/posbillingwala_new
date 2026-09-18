import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';

class AppTheme {
  AppTheme._();

  /* Status bar matches AppBar (primary) with light icons. */
  static const lightSystemUi = SystemUiOverlayStyle(
    statusBarColor: AppColors.primary,
    statusBarIconBrightness: Brightness.light,
    statusBarBrightness: Brightness.dark,
    systemNavigationBarColor: Colors.white,
    systemNavigationBarIconBrightness: Brightness.dark,
    systemNavigationBarDividerColor: Colors.transparent,
  );

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
      dividerColor: AppColors.border,
    );

    /* Friendlier Material text scale — larger body, softer titles. */
    final textTheme = base.textTheme
        .copyWith(
          displayLarge: AppTypography.screenTitle(),
          displayMedium: AppTypography.screenTitle(),
          displaySmall: AppTypography.sectionTitle(),
          headlineLarge: AppTypography.screenTitle(),
          headlineMedium: AppTypography.sectionTitle(),
          headlineSmall: AppTypography.sectionTitle(),
          titleLarge: AppTypography.sectionTitle(),
          titleMedium: AppTypography.cardTitle(),
          titleSmall: AppTypography.cardTitle().copyWith(fontSize: 14),
          bodyLarge: AppTypography.body().copyWith(fontSize: 16),
          bodyMedium: AppTypography.body(),
          bodySmall: AppTypography.bodySmall(),
          labelLarge: AppTypography.button(color: AppColors.navy),
          labelMedium: AppTypography.statusLabel(),
          labelSmall: AppTypography.caption(),
        )
        .apply(
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
        systemOverlayStyle: lightSystemUi,
        titleTextStyle: AppTypography.sectionTitle(
          color: Colors.white,
        ).copyWith(fontSize: 20, fontWeight: FontWeight.w600),
        toolbarTextStyle: AppTypography.body(color: Colors.white),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        color: AppColors.card,
        surfaceTintColor: Colors.transparent,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(22),
          side: BorderSide(color: AppColors.border),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white,
          minimumSize: const Size(0, 52),
          padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 16),
          textStyle: AppTypography.button(),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.primary,
          minimumSize: const Size(0, 50),
          side: const BorderSide(color: Color(0xFFBFD5F5)),
          textStyle: AppTypography.button(color: AppColors.primary),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.primary,
          textStyle: AppTypography.body().copyWith(
            fontWeight: FontWeight.w600,
            color: AppColors.primary,
          ),
        ),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 6,
        extendedPadding: const EdgeInsets.symmetric(horizontal: 20),
        extendedTextStyle: AppTypography.button(),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 18,
          vertical: 16,
        ),
        labelStyle: AppTypography.body().copyWith(
          fontSize: 14,
          fontWeight: FontWeight.w500,
          color: AppColors.navy.withValues(alpha: .7),
        ),
        floatingLabelStyle: AppTypography.bodySmall().copyWith(
          fontSize: 14,
          fontWeight: FontWeight.w600,
          color: AppColors.primary,
        ),
        hintStyle: AppTypography.body().copyWith(
          color: const Color(0xFF9AA7B9),
          fontWeight: FontWeight.w400,
        ),
        helperStyle: AppTypography.caption(),
        errorStyle: AppTypography.caption(color: AppColors.red),
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
      listTileTheme: ListTileThemeData(
        titleTextStyle: AppTypography.cardTitle(),
        subtitleTextStyle: AppTypography.bodySmall(),
        leadingAndTrailingTextStyle: AppTypography.bodySmall(),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: AppColors.primaryLight,
        selectedColor: AppColors.primary,
        secondarySelectedColor: AppColors.primary,
        labelStyle: AppTypography.bodySmall().copyWith(
          fontWeight: FontWeight.w600,
        ),
        side: BorderSide.none,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        contentTextStyle: AppTypography.body(color: Colors.white),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: Colors.white,
        elevation: 7,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        titleTextStyle: AppTypography.sectionTitle(),
        contentTextStyle: AppTypography.body(),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
      ),
      tabBarTheme: TabBarThemeData(
        labelStyle: AppTypography.cardTitle(color: AppColors.primary),
        unselectedLabelStyle: AppTypography.body(),
        labelColor: AppColors.primary,
        unselectedLabelColor: AppColors.navy.withValues(alpha: .55),
      ),
    );
  }
}
