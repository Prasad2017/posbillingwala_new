import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/widgets/app_text_field.dart';

/* Shared outline style — matches [ThemeData.inputDecorationTheme] / text fields. */
abstract final class AppFieldBorders {
  static const radius = 16.0;
  static const borderWidth = 1.0;
  static const focusedWidth = 1.8;
  static const errorWidth = 1.4;

  static OutlineInputBorder outline({
    Color color = AppColors.border,
    double width = borderWidth,
  }) {
    return OutlineInputBorder(
      borderRadius: BorderRadius.circular(radius),
      borderSide: BorderSide(color: color, width: width),
    );
  }

  static OutlineInputBorder get enabled => outline();

  static OutlineInputBorder get focused =>
      outline(color: AppColors.primary, width: focusedWidth);

  static OutlineInputBorder get error =>
      outline(color: AppColors.danger, width: errorWidth);

  static OutlineInputBorder get focusedError =>
      outline(color: AppColors.danger, width: focusedWidth);

  static OutlineInputBorder get disabled =>
      outline(color: AppColors.border.withValues(alpha: 0.5));
}

InputDecoration appDropdownDecoration(
  BuildContext context, {
  String? label,
  String? hint,
  bool required = false,
  bool? showLabel,
  bool hasError = false,
  bool isDense = false,
}) {
  final bodyStyle = Theme.of(context).textTheme.bodyMedium;
  final visible = showLabel ?? (label?.trim().isNotEmpty ?? false);
  final base = OutlineInputBorder(
    borderRadius: BorderRadius.circular(AppFieldBorders.radius),
    borderSide: BorderSide(
      color: hasError ? AppColors.danger : AppColors.border,
      width: hasError ? AppFieldBorders.errorWidth : AppFieldBorders.borderWidth,
    ),
  );

  return InputDecoration(
    labelText: visible ? label : null,
    floatingLabelBehavior:
        visible ? FloatingLabelBehavior.auto : FloatingLabelBehavior.never,
    labelStyle: const TextStyle(
      fontSize: AppTextField.labelFontSize,
      fontWeight: FontWeight.w500,
      color: AppColors.textSecondary,
    ),
    floatingLabelStyle: TextStyle(
      fontSize: AppTextField.labelFontSize,
      fontWeight: FontWeight.w600,
      color: hasError ? AppColors.danger : AppColors.primary,
    ),
    hintText: hint ??
        (label != null && label.trim().isNotEmpty ? 'Select $label' : null),
    hintStyle: bodyStyle?.copyWith(
      color: AppColors.textSecondary.withValues(alpha: 0.75),
      fontWeight: FontWeight.w500,
    ),
    filled: true,
    fillColor: AppColors.glassSolid,
    isDense: isDense,
    contentPadding: EdgeInsets.symmetric(
      horizontal: isDense ? 14 : 18,
      vertical: isDense ? 12 : 16,
    ),
    border: base,
    enabledBorder: base,
    focusedBorder: AppFieldBorders.focused,
    errorBorder: AppFieldBorders.error,
    focusedErrorBorder: AppFieldBorders.focusedError,
    disabledBorder: AppFieldBorders.disabled,
  );
}
