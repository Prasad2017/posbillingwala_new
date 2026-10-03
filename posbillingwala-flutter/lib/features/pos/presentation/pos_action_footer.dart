import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_scale.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';

/* POS footer: cart summary bar + Save / Share / Print (payment mode on action). */
class PosActionFooter extends StatelessWidget {
  const PosActionFooter({
    super.key,
    required this.summary,
    required this.currency,
    required this.onCartTap,
    required this.onSave,
    required this.onShare,
    required this.onPrint,
    this.onKot,
    this.unprintedCount = 0,
    this.kotEnabled = false,
    this.compact = false,
    this.showCartBar = true,
    this.showActions = true,
    this.displayTotal,
  });

  final CartSummary summary;
  final NumberFormat currency;
  final VoidCallback onCartTap;
  final VoidCallback onSave;
  final VoidCallback onShare;
  final VoidCallback onPrint;
  final VoidCallback? onKot;
  final double unprintedCount;
  final bool kotEnabled;
  final bool compact;
  final bool showCartBar;
  final bool showActions;
  /* When set (e.g. cart payable with discount/packing), overrides summary.grandTotal. */
  final double? displayTotal;

  @override
  Widget build(BuildContext context) {
    final qtyLabel = ProductUnits.formatQty(summary.totalQuantity);
    final itemsLabel =
        '$qtyLabel ${summary.totalQuantity == 1 ? 'Item' : 'Items'}';
    final total = displayTotal ?? summary.grandTotal;

    return Material(
      color: Colors.white,
      elevation: 12,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: EdgeInsets.fromLTRB(12, compact ? 8 : 10, 12, compact ? 8 : 10),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (showCartBar) ...[
                Material(
                  color: AppColors.primary,
                  elevation: 2,
                  shadowColor: AppColors.primary.withValues(alpha: 0.45),
                  borderRadius: BorderRadius.circular(12),
                  child: InkWell(
                    onTap: onCartTap,
                    borderRadius: BorderRadius.circular(12),
                    splashColor: Colors.white24,
                    highlightColor: Colors.white10,
                    child: Padding(
                      padding: EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: compact ? 10 : 12,
                      ),
                      child: Row(
                        children: [
                          Stack(
                            clipBehavior: Clip.none,
                            children: [
                              const Icon(
                                Icons.shopping_cart_outlined,
                                color: Colors.white,
                                size: 22,
                              ),
                              if (summary.totalQuantity > 0)
                                Positioned(
                                  right: -8,
                                  top: -8,
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 5,
                                      vertical: 1,
                                    ),
                                    decoration: BoxDecoration(
                                      color: AppColors.danger,
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Text(
                                      qtyLabel,
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontSize: 10,
                                        fontWeight: FontWeight.w800,
                                      ),
                                    ),
                                  ),
                                ),
                            ],
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  itemsLabel,
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w700,
                                    fontSize: 14,
                                  ),
                                ),
                                Text(
                                  currency.format(total),
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w900,
                                    fontSize: 16,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 7,
                            ),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.18),
                              borderRadius: BorderRadius.circular(20),
                              border: Border.all(
                                color: Colors.white.withValues(alpha: 0.35),
                              ),
                            ),
                            child: const Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  'View Bill',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 12,
                                  ),
                                ),
                                SizedBox(width: 2),
                                Icon(
                                  Icons.chevron_right_rounded,
                                  color: Colors.white,
                                  size: 20,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                if (showActions) SizedBox(height: compact ? 8 : 10),
              ],
              if (showActions)
                Row(
                  children: [
                    if (kotEnabled) ...[
                      _ActionIconButton(
                        icon: Icons.receipt_long_outlined,
                        label: unprintedCount > 0
                            ? 'KOT (${ProductUnits.formatQty(unprintedCount)})'
                            : 'KOT',
                        color: AppColors.orange,
                        onTap: unprintedCount <= 0 ? null : onKot,
                        compact: compact,
                      ),
                      const SizedBox(width: 8),
                    ],
                    _ActionIconButton(
                      icon: Icons.save_outlined,
                      label: 'Save',
                      color: AppColors.danger,
                      onTap: onSave,
                      compact: compact,
                    ),
                    const SizedBox(width: 8),
                    _ActionIconButton(
                      icon: Icons.share_outlined,
                      label: 'Share',
                      color: AppColors.green,
                      onTap: onShare,
                      compact: compact,
                    ),
                    const SizedBox(width: 8),
                    _ActionIconButton(
                      icon: Icons.print_outlined,
                      label: 'Print',
                      color: AppColors.primary,
                      onTap: onPrint,
                      compact: compact,
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ActionIconButton extends StatelessWidget {
  const _ActionIconButton({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
    this.compact = false,
  });

  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback? onTap;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final enabled = onTap != null;
    final height = AppScale.buttonHeight(context);
    return Expanded(
      child: Material(
        color: enabled
            ? color.withValues(alpha: 0.1)
            : AppColors.border.withValues(alpha: 0.35),
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: SizedBox(
            height: compact ? height * 0.92 : height,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  icon,
                  size: context.sp(compact ? 20 : 22),
                  color: enabled ? color : AppColors.textSecondary,
                ),
                const SizedBox(height: 2),
                Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: context.sp(12),
                    fontWeight: FontWeight.w800,
                    color: enabled ? color : AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
