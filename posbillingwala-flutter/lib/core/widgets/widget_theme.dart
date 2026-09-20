import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/billingwala_theme.dart';

/* Theme tokens used by reusable widgets under `core/widgets`. */
extension AppWidgetTheme on BuildContext {
  BillingwalaTheme get billingwala => BillingwalaTheme.of(this);

  Color get textPrimary => AppColors.textPrimary;

  Color get textSecondary => AppColors.textSecondary;

  Color get cardColor =>
      Theme.of(this).cardTheme.color ?? AppColors.glassFill;

  Color get borderColor => AppColors.glassBorder;

  Color get subtleBackground => AppColors.primarySoft;

  Color get primary => AppColors.primary;
}
