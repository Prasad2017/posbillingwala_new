import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';

/// Shared Master Data list/form chrome matching the reference screens.
abstract final class MasterUi {
  static const bg = Color(0xFFF3F7FC);
  static const cardRadius = 14.0;
  static const fieldRadius = 10.0;
}

class MasterSectionLabel extends StatelessWidget {
  const MasterSectionLabel(this.label, {super.key, this.trailing});

  final String label;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Text(
            label.toUpperCase(),
            style: TextStyle(
              fontFamily: AppFonts.family,
              fontSize: 11,
              letterSpacing: 0.9,
              color: AppColors.textSecondary.withValues(alpha: .9),
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        ?trailing,
      ],
    );
  }
}

class MasterCard extends StatelessWidget {
  const MasterCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(14),
  });

  final Widget child;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(MasterUi.cardRadius),
        border: Border.all(color: AppColors.border.withValues(alpha: .8)),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: .04),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(padding: padding, child: child),
    );
  }
}

class MasterOutlinedField extends StatelessWidget {
  const MasterOutlinedField({
    super.key,
    required this.controller,
    required this.hint,
    this.keyboardType,
    this.enabled = true,
    this.onChanged,
    this.textCapitalization = TextCapitalization.none,
  });

  final TextEditingController controller;
  final String hint;
  final TextInputType? keyboardType;
  final bool enabled;
  final ValueChanged<String>? onChanged;
  final TextCapitalization textCapitalization;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      enabled: enabled,
      keyboardType: keyboardType,
      textCapitalization: textCapitalization,
      onChanged: onChanged,
      style: const TextStyle(
        fontFamily: AppFonts.family,
        fontSize: 14.5,
        color: AppColors.navy,
        fontWeight: FontWeight.w500,
      ),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(
          fontFamily: AppFonts.family,
          color: AppColors.navy.withValues(alpha: .38),
          fontWeight: FontWeight.w400,
          fontSize: 14,
        ),
        filled: true,
        fillColor: Colors.white,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
          borderSide: BorderSide(color: AppColors.border.withValues(alpha: .9)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
          borderSide: BorderSide(color: AppColors.border.withValues(alpha: .9)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.4),
        ),
        disabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
          borderSide: BorderSide(color: AppColors.border.withValues(alpha: .5)),
        ),
      ),
    );
  }
}

class MasterDropdown<T> extends StatelessWidget {
  const MasterDropdown({
    super.key,
    required this.value,
    required this.items,
    required this.itemLabel,
    required this.onChanged,
    this.hint,
  });

  final T? value;
  final List<T> items;
  final String Function(T) itemLabel;
  final ValueChanged<T?> onChanged;
  final String? hint;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
        border: Border.all(color: AppColors.border.withValues(alpha: .9)),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<T>(
          value: value,
          isExpanded: true,
          hint: hint == null
              ? null
              : Text(
                  hint!,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    color: AppColors.navy.withValues(alpha: .38),
                  ),
                ),
          icon: const Icon(
            Icons.keyboard_arrow_down_rounded,
            color: AppColors.primary,
          ),
          style: const TextStyle(
            fontFamily: AppFonts.family,
            fontSize: 14.5,
            color: AppColors.navy,
            fontWeight: FontWeight.w500,
          ),
          items: [
            for (final item in items)
              DropdownMenuItem(value: item, child: Text(itemLabel(item))),
          ],
          onChanged: onChanged,
        ),
      ),
    );
  }
}

class MasterPrimaryButton extends StatelessWidget {
  const MasterPrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.isLoading = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool isLoading;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 48,
      child: FilledButton(
        onPressed: isLoading ? null : onPressed,
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.primary,
          disabledBackgroundColor: AppColors.primary.withValues(alpha: .45),
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
        child: isLoading
            ? const SizedBox(
                width: 22,
                height: 22,
                child: CircularProgressIndicator(
                  strokeWidth: 2.2,
                  color: Colors.white,
                ),
              )
            : Text(
                label,
                style: const TextStyle(
                  fontFamily: AppFonts.family,
                  fontWeight: FontWeight.w700,
                  fontSize: 15,
                  color: Colors.white,
                ),
              ),
      ),
    );
  }
}

class MasterIndexBadge extends StatelessWidget {
  const MasterIndexBadge(this.index, {super.key});

  final int index;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 34,
      height: 34,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: AppColors.primaryLight,
        borderRadius: BorderRadius.circular(9),
      ),
      child: Text(
        '$index',
        style: const TextStyle(
          fontFamily: AppFonts.family,
          color: AppColors.primary,
          fontWeight: FontWeight.w700,
          fontSize: 13,
        ),
      ),
    );
  }
}

class MasterIconAction extends StatelessWidget {
  const MasterIconAction({
    super.key,
    required this.icon,
    required this.color,
    required this.onTap,
    this.filled = false,
  });

  final IconData icon;
  final Color color;
  final VoidCallback? onTap;
  final bool filled;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: filled ? color.withValues(alpha: .12) : Colors.transparent,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: SizedBox(
          width: 38,
          height: 38,
          child: Icon(icon, color: color, size: 20),
        ),
      ),
    );
  }
}

class MasterListRow extends StatelessWidget {
  const MasterListRow({
    super.key,
    required this.index,
    required this.title,
    this.subtitle,
    required this.onEdit,
    required this.onDelete,
    this.showDivider = true,
  });

  final int index;
  final String title;
  final String? subtitle;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          child: Row(
            children: [
              MasterIndexBadge(index),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        color: AppColors.navy,
                        fontWeight: FontWeight.w600,
                        fontSize: 14.5,
                      ),
                    ),
                    if (subtitle != null && subtitle!.isNotEmpty) ...[
                      const SizedBox(height: 2),
                      Text(
                        subtitle!,
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          color: AppColors.navy.withValues(alpha: .45),
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              MasterIconAction(
                icon: Icons.edit_rounded,
                color: AppColors.primary,
                onTap: onEdit,
              ),
              MasterIconAction(
                icon: Icons.delete_outline_rounded,
                color: AppColors.red,
                onTap: onDelete,
              ),
            ],
          ),
        ),
        if (showDivider)
          Divider(
            height: 1,
            thickness: 1,
            indent: 58,
            endIndent: 12,
            color: AppColors.border.withValues(alpha: .7),
          ),
      ],
    );
  }
}

class MasterLinkButton extends StatelessWidget {
  const MasterLinkButton({
    super.key,
    required this.label,
    required this.onTap,
  });

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onTap,
      style: TextButton.styleFrom(
        foregroundColor: AppColors.primary,
        padding: EdgeInsets.zero,
        minimumSize: Size.zero,
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
      child: Text(
        label,
        style: const TextStyle(
          fontFamily: AppFonts.family,
          fontWeight: FontWeight.w600,
          fontSize: 12.5,
        ),
      ),
    );
  }
}

class MasterPillTabs extends StatelessWidget {
  const MasterPillTabs({
    super.key,
    required this.labels,
    required this.index,
    required this.onChanged,
  });

  final List<String> labels;
  final int index;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (var i = 0; i < labels.length; i++) ...[
          if (i > 0) const SizedBox(width: 8),
          Expanded(
            child: Material(
              color: i == index ? AppColors.primary : Colors.white,
              borderRadius: BorderRadius.circular(22),
              child: InkWell(
                onTap: () => onChanged(i),
                borderRadius: BorderRadius.circular(22),
                child: Container(
                  height: 40,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(22),
                    border: Border.all(
                      color: i == index
                          ? AppColors.primary
                          : AppColors.border.withValues(alpha: .9),
                    ),
                  ),
                  child: Text(
                    labels[i],
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w700,
                      fontSize: 13.5,
                      color: i == index ? Colors.white : AppColors.navy,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ],
    );
  }
}

class MasterEmptyState extends StatelessWidget {
  const MasterEmptyState({
    super.key,
    required this.title,
    this.subtitle,
  });

  final String title;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 28, horizontal: 16),
      child: Column(
        children: [
          Text(
            title,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontWeight: FontWeight.w700,
              fontSize: 15,
              color: AppColors.navy,
            ),
          ),
          if (subtitle != null) ...[
            const SizedBox(height: 6),
            Text(
              subtitle!,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 12.5,
                color: AppColors.navy.withValues(alpha: .45),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
