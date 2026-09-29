import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_dimensions.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/app_button.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/widgets/three_dots_loader.dart';

class AppEmptyState extends StatelessWidget {
  const AppEmptyState({
    super.key,
    required this.title,
    this.message,
    this.iconAsset,
    this.actionLabel,
    this.onAction,
  });

  final String title;
  final String? message;
  final String? iconAsset;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final compact = constraints.maxHeight < 220;
        final iconSize = compact ? 44.0 : 72.0;
        final pad = compact ? 12.0 : AppDimensions.xl;
        return Center(
          child: SingleChildScrollView(
            padding: EdgeInsets.all(pad),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (iconAsset != null) ...[
                  AppSvg(
                    iconAsset!,
                    width: iconSize,
                    height: iconSize,
                    color: AppColors.primary.withValues(alpha: .55),
                  ),
                  SizedBox(height: compact ? AppDimensions.md : AppDimensions.lg),
                ],
                Text(
                  title,
                  textAlign: TextAlign.center,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                  style: AppTypography.sectionTitle(),
                ),
                if (message != null && message != title) ...[
                  const SizedBox(height: AppDimensions.sm),
                  Text(
                    message!,
                    textAlign: TextAlign.center,
                    maxLines: compact ? 2 : 4,
                    overflow: TextOverflow.ellipsis,
                    style: AppTypography.body(),
                  ),
                ],
                if (actionLabel != null && onAction != null) ...[
                  SizedBox(height: compact ? AppDimensions.md : AppDimensions.lg),
                  AppButton(label: actionLabel!, onPressed: onAction),
                ],
              ],
            ),
          ),
        );
      },
    );
  }
}

class AppErrorState extends StatelessWidget {
  const AppErrorState({
    super.key,
    this.title = 'Something went wrong',
    this.message = 'Please try again.',
    this.onRetry,
  });

  final String title;
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppDimensions.xl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AppSvg(
              AppAssets.svgWarning,
              width: 56,
              height: 56,
              color: AppColors.danger,
            ),
            const SizedBox(height: AppDimensions.lg),
            Text(
              title,
              textAlign: TextAlign.center,
              style: AppTypography.sectionTitle(),
            ),
            const SizedBox(height: AppDimensions.sm),
            Text(
              message,
              textAlign: TextAlign.center,
              style: AppTypography.body(),
            ),
            if (onRetry != null) ...[
              const SizedBox(height: AppDimensions.lg),
              AppButton(
                label: 'Retry',
                onPressed: onRetry,
                icon: Icons.refresh_rounded,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class AppLoadingState extends StatelessWidget {
  const AppLoadingState({super.key, this.message});

  final String? message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const ThreeDotsLoader(color: AppColors.primary),
          if (message != null) ...[
            const SizedBox(height: AppDimensions.md),
            Text(message!, style: AppTypography.bodySmall()),
          ],
        ],
      ),
    );
  }
}

class AppStatusBadge extends StatelessWidget {
  const AppStatusBadge({
    super.key,
    required this.label,
    this.color = AppColors.primary,
    this.filled = false,
  });

  final String label;
  final Color color;
  final bool filled;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: filled ? color : color.withValues(alpha: .12),
        borderRadius: BorderRadius.circular(AppDimensions.radiusPill),
        border: filled ? null : Border.all(color: color.withValues(alpha: .25)),
      ),
      child: Text(
        label,
        style: AppTypography.statusLabel(color: filled ? Colors.white : color),
      ),
    );
  }
}
