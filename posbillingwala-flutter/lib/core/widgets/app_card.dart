import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/billingwala_theme.dart';

/* Frosted glass card — default surface for content panels. */
class AppCard extends StatelessWidget {
  const AppCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.margin,
    this.color,
    this.onTap,
    this.accentColor,
    this.radius = 22,
    this.enableBlur = true,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry? margin;
  final Color? color;
  final VoidCallback? onTap;
  final Color? accentColor;
  final double radius;
  final bool enableBlur;

  @override
  Widget build(BuildContext context) {
    final theme = BillingwalaTheme.of(context);
    final radiusGeom = BorderRadius.circular(radius);
    final fill = color ?? theme.glassFill;
    final borderColor = accentColor != null
        ? accentColor!.withValues(alpha: 0.35)
        : theme.glassBorder;

    final content = Padding(padding: padding, child: child);

    final panel = DecoratedBox(
      decoration: BoxDecoration(
        color: fill,
        borderRadius: radiusGeom,
        border: Border.all(color: borderColor, width: 1.2),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: 0.06),
            blurRadius: 24,
            offset: const Offset(0, 10),
          ),
        ],
        gradient: color == null
            ? LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  theme.glassHighlight,
                  theme.glassFill,
                ],
              )
            : null,
      ),
      child: Material(
        type: MaterialType.transparency,
        child: onTap == null
            ? content
            : InkWell(
                onTap: onTap,
                borderRadius: radiusGeom,
                child: content,
              ),
      ),
    );

    Widget card = ClipRRect(
      borderRadius: radiusGeom,
      child: enableBlur
          ? BackdropFilter(
              filter: theme.glassFilter,
              child: panel,
            )
          : panel,
    );

    if (margin != null) {
      card = Padding(padding: margin!, child: card);
    }

    return card;
  }
}
