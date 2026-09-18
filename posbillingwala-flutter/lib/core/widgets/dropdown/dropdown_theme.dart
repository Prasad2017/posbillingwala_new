import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/widgets/app_text_field.dart';
import 'package:pos_billingwala_v2/core/widgets/widget_theme.dart';

InputDecoration appDropdownDecoration(
  BuildContext context, {
  String? label,
  String? hint,
  bool required = false,
  bool? showLabel,
}) {
  final bodyStyle = Theme.of(context).textTheme.bodyMedium;
  final visible = showLabel ?? (label?.trim().isNotEmpty ?? false);

  return InputDecoration(
    labelText: visible ? label : null,
    floatingLabelBehavior:
        visible ? FloatingLabelBehavior.auto : FloatingLabelBehavior.never,
    labelStyle: const TextStyle(
      fontSize: AppTextField.labelFontSize,
      fontWeight: FontWeight.w500,
    ),
    floatingLabelStyle: const TextStyle(
      fontSize: AppTextField.labelFontSize,
      fontWeight: FontWeight.w600,
    ),
    hintText: hint ??
        (label != null && label.trim().isNotEmpty ? 'Select $label' : null),
    hintStyle: bodyStyle?.copyWith(color: context.textSecondary),
    filled: true,
    fillColor: context.subtleBackground,
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    suffixIcon: Icon(
      Icons.keyboard_arrow_down_rounded,
      color: context.textSecondary,
    ),
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: BorderSide(color: context.borderColor, width: 1.2),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: BorderSide(color: context.borderColor, width: 1.2),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppColors.primary, width: 1.6),
    ),
    errorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppColors.danger, width: 1.4),
    ),
    focusedErrorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppColors.danger, width: 1.6),
    ),
  );
}
