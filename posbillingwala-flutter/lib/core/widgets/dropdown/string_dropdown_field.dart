import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/widgets/dropdown/dropdown_theme.dart';
import 'package:pos_billingwala_v2/core/widgets/dropdown/text_dropdown.dart';

class StringDropdownField extends StatelessWidget {
  const StringDropdownField({
    super.key,
    required this.label,
    this.hint,
    required this.value,
    required this.options,
    required this.onChanged,
    this.enableSearch = false,
    this.showLabel,
    this.required = false,
    this.validator,
  });

  final String label;
  final String? hint;
  final String? value;
  final List<String> options;
  final ValueChanged<String?> onChanged;
  final bool enableSearch;
  final bool? showLabel;
  final bool required;
  final String? Function(String?)? validator;

  @override
  Widget build(BuildContext context) {
    final visibleLabel = showLabel ?? label.trim().isNotEmpty;
    return NullableTextDropdownFormField(
      label: label,
      hint: hint,
      value: value,
      options: options,
      decoration: appDropdownDecoration(
        context,
        label: label,
        hint: hint,
        showLabel: visibleLabel,
      ),
      enableSearch: enableSearch || options.length > 6,
      showLabel: visibleLabel,
      validator: validator,
      onChanged: onChanged,
    );
  }
}
