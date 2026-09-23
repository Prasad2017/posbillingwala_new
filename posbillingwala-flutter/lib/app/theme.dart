import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/theme/billingwala_theme.dart';

class AppTheme {
  AppTheme._();

  /* Status bar matches AppBar (primary). Nav bar is solid white so screens
     sit cleanly above the system navigation area. */
  static const lightSystemUi = SystemUiOverlayStyle(
    statusBarColor: AppColors.primary,
    statusBarIconBrightness: Brightness.light,
    statusBarBrightness: Brightness.dark,
    systemNavigationBarColor: Colors.white,
    systemNavigationBarIconBrightness: Brightness.dark,
    systemNavigationBarDividerColor: Colors.transparent,
    systemNavigationBarContrastEnforced: true,
  );

  static ThemeData light() {
    final scheme = ColorScheme.fromSeed(
      seedColor: AppColors.primary,
      primary: AppColors.primary,
      secondary: AppColors.orange,
      error: AppColors.red,
      surface: AppColors.glassSolid,
      onSurface: AppColors.textPrimary,
      onSurfaceVariant: AppColors.textSecondary,
      brightness: Brightness.light,
    );

    final base = ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      fontFamily: AppFonts.family,
      scaffoldBackgroundColor: Colors.transparent,
      splashFactory: InkSparkle.splashFactory,
      dividerColor: AppColors.border,
      extensions: const [BillingwalaTheme.light],
    );

    /* Friendlier Material text scale — larger body, high-contrast titles. */
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
          bodyColor: AppColors.textPrimary,
          displayColor: AppColors.textPrimary,
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
        ).copyWith(fontSize: 20, fontWeight: FontWeight.w700),
        toolbarTextStyle: AppTypography.body(color: Colors.white),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        color: AppColors.glassFill,
        surfaceTintColor: Colors.transparent,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(22),
          side: const BorderSide(color: AppColors.glassBorder, width: 1.2),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white,
          elevation: 2,
          shadowColor: AppColors.primaryDark.withValues(alpha: 0.35),
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
          backgroundColor: AppColors.glassSolid,
          minimumSize: const Size(0, 50),
          side: const BorderSide(color: AppColors.primary, width: 1.6),
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
            fontWeight: FontWeight.w700,
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
        fillColor: AppColors.glassSolid,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 18,
          vertical: 16,
        ),
        labelStyle: AppTypography.body().copyWith(
          fontSize: 14,
          fontWeight: FontWeight.w600,
          color: AppColors.textSecondary,
        ),
        floatingLabelStyle: AppTypography.bodySmall().copyWith(
          fontSize: 14,
          fontWeight: FontWeight.w700,
          color: AppColors.primary,
        ),
        hintStyle: AppTypography.body().copyWith(
          color: AppColors.textSecondary.withValues(alpha: 0.75),
          fontWeight: FontWeight.w500,
        ),
        helperStyle: AppTypography.caption(),
        errorStyle: AppTypography.caption(color: AppColors.red),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.8),
        ),
      ),
      listTileTheme: ListTileThemeData(
        titleTextStyle: AppTypography.cardTitle(),
        subtitleTextStyle: AppTypography.bodySmall(),
        leadingAndTrailingTextStyle: AppTypography.bodySmall(),
        iconColor: AppColors.textPrimary,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: AppColors.primaryLight,
        selectedColor: AppColors.primary,
        secondarySelectedColor: AppColors.primary,
        checkmarkColor: Colors.white,
        labelStyle: AppTypography.bodySmall(
          color: AppColors.navy,
        ).copyWith(fontWeight: FontWeight.w700),
        secondaryLabelStyle: AppTypography.bodySmall(
          color: Colors.white,
        ).copyWith(fontWeight: FontWeight.w700),
        side: BorderSide.none,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: AppColors.navy,
        contentTextStyle: AppTypography.body(color: Colors.white),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: AppColors.glassSolid,
        elevation: 10,
        shadowColor: AppColors.navy.withValues(alpha: 0.18),
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        titleTextStyle: AppTypography.sectionTitle(),
        contentTextStyle: AppTypography.body(),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: AppColors.glassSolid,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
      ),
      tabBarTheme: TabBarThemeData(
        labelStyle: AppTypography.cardTitle(color: AppColors.primary),
        unselectedLabelStyle: AppTypography.body(
          color: AppColors.textSecondary,
        ),
        labelColor: AppColors.primary,
        unselectedLabelColor: AppColors.textSecondary,
      ),
    );
  }
}
