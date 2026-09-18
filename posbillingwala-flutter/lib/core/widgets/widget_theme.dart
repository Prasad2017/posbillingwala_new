import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';

/* Theme tokens used by reusable widgets under `core/widgets`. */
extension AppWidgetTheme on BuildContext {
  Color get textPrimary =>
      Theme.of(this).colorScheme.onSurface.withValues(alpha: 0.92);

  Color get textSecondary =>
      Theme.of(this).colorScheme.onSurfaceVariant.withValues(alpha: 0.85);

  Color get cardColor => Theme.of(this).cardTheme.color ?? AppColors.card;

  Color get borderColor => AppColors.border;

  Color get subtleBackground => AppColors.primarySoft;

  Color get primary => AppColors.primary;
}
