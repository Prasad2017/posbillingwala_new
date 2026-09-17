import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_checkout_controller.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';

double checkoutPayable(CartSummary summary, PaymentCheckoutState checkout) =>
    checkout.payableTotal(
      subtotal: summary.subtotal,
      taxTotal: summary.taxTotal,
    );

class BillSummaryCard extends StatelessWidget {
  const BillSummaryCard({
    super.key,
    required this.summary,
    required this.checkout,
    required this.currency,
    required this.discountController,
    required this.packingController,
    required this.payable,
    required this.expanded,
    required this.onToggleExpanded,
    required this.onDiscountChanged,
    required this.onPackingChanged,
    required this.onDiscountTypeChanged,
  });

  final CartSummary summary;
  final PaymentCheckoutState checkout;
  final NumberFormat currency;
  final TextEditingController discountController;
  final TextEditingController packingController;
  final double payable;
  final bool expanded;
  final VoidCallback onToggleExpanded;
  final ValueChanged<String> onDiscountChanged;
  final ValueChanged<String> onPackingChanged;
  final ValueChanged<String> onDiscountTypeChanged;

  bool get isPercentDiscount =>
      checkout.discountType.toLowerCase().startsWith('p');

  @override
  Widget build(BuildContext context) {
    final discAmount = checkout.discountValue(summary.subtotal);
    return Container(
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(18)),
        border: Border.all(color: AppColors.border),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: 0.08),
            blurRadius: 16,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          InkWell(
            onTap: onToggleExpanded,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(18)),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 12, 14),
              child: Row(
                children: [
                  const SummaryIcon(
                    icon: Icons.description_outlined,
                    color: AppColors.primary,
                  ),
                  const SizedBox(width: 10),
                  const Expanded(
                    child: Text(
                      'Bill Summary',
                      style: TextStyle(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w800,
                        fontSize: 16,
                      ),
                    ),
                  ),
                  if (!expanded)
                    Text(
                      currency.format(payable),
                      style: const TextStyle(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w900,
                        fontSize: 15,
                      ),
                    ),
                  const SizedBox(width: 4),
                  AnimatedRotation(
                    turns: expanded ? 0.5 : 0,
                    duration: const Duration(milliseconds: 200),
                    child: const Icon(
                      Icons.keyboard_arrow_up_rounded,
                      color: AppColors.primary,
                      size: 26,
                    ),
                  ),
                ],
              ),
            ),
          ),
          AnimatedCrossFade(
            firstChild: const SizedBox(width: double.infinity, height: 0),
            secondChild: Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SummaryIconRow(
                    icon: Icons.shopping_cart_outlined,
                    iconColor: AppColors.primary,
                    label: 'Subtotal',
                    trailing: Text(
                      currency.format(summary.subtotal),
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 14,
                        color: AppColors.navy,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SummaryIconRow(
                    icon: Icons.local_offer_outlined,
                    iconColor: AppColors.danger,
                    label: 'Discount',
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        DiscountTypeToggle(
                          isPercent: isPercentDiscount,
                          enabled: !checkout.busy,
                          onChanged: onDiscountTypeChanged,
                        ),
                        const SizedBox(width: 6),
                        SizedBox(
                          width: 84,
                          child: EditableValueBox(
                            controller: discountController,
                            hint: '0',
                            suffix: isPercentDiscount ? '%' : null,
                            enabled: !checkout.busy,
                            onChanged: onDiscountChanged,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (discAmount > 0) ...[
                    const SizedBox(height: 4),
                    Align(
                      alignment: Alignment.centerRight,
                      child: Text(
                        '− ${currency.format(discAmount)}',
                        style: const TextStyle(
                          color: AppColors.danger,
                          fontWeight: FontWeight.w700,
                          fontSize: 12,
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 12),
                  SummaryIconRow(
                    icon: Icons.inventory_2_outlined,
                    iconColor: AppColors.green,
                    label: 'Packing Charges',
                    trailing: SizedBox(
                      width: 96,
                      child: EditableValueBox(
                        controller: packingController,
                        hint: '0',
                        prefix: '₹',
                        enabled: !checkout.busy,
                        onChanged: onPackingChanged,
                      ),
                    ),
                  ),
                  if (summary.hasCgst ||
                      summary.hasSgst ||
                      (summary.hasTax &&
                          !summary.hasCgst &&
                          !summary.hasSgst)) ...[
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 12),
                      child: Divider(height: 1, color: AppColors.border),
                    ),
                    if (summary.hasCgst) ...[
                      SummaryIconRow(
                        icon: Icons.percent_rounded,
                        iconColor: const Color(0xFF7C3AED),
                        label: 'CGST',
                        labelSuffix: 'Half of Subtotal',
                        trailing: Text(
                          currency.format(summary.cgstTotal),
                          style: const TextStyle(
                            fontWeight: FontWeight.w800,
                            fontSize: 14,
                            color: AppColors.navy,
                          ),
                        ),
                      ),
                      const SizedBox(height: 12),
                    ],
                    if (summary.hasSgst)
                      SummaryIconRow(
                        icon: Icons.percent_rounded,
                        iconColor: const Color(0xFF7C3AED),
                        label: 'SGST',
                        labelSuffix: 'Half of Subtotal',
                        trailing: Text(
                          currency.format(summary.sgstTotal),
                          style: const TextStyle(
                            fontWeight: FontWeight.w800,
                            fontSize: 14,
                            color: AppColors.navy,
                          ),
                        ),
                      ),
                    if (summary.hasTax &&
                        !summary.hasCgst &&
                        !summary.hasSgst)
                      SummaryIconRow(
                        icon: Icons.percent_rounded,
                        iconColor: const Color(0xFF7C3AED),
                        label: 'GST',
                        trailing: Text(
                          currency.format(summary.taxTotal),
                          style: const TextStyle(
                            fontWeight: FontWeight.w800,
                            fontSize: 14,
                            color: AppColors.navy,
                          ),
                        ),
                      ),
                  ],
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 12),
                    child: Divider(height: 1, color: AppColors.border),
                  ),
                  SummaryIconRow(
                    icon: Icons.payments_outlined,
                    iconColor: AppColors.primary,
                    label: 'TOTAL AMOUNT',
                    labelStyle: const TextStyle(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w900,
                      fontSize: 14,
                    ),
                    trailing: Text(
                      currency.format(payable),
                      style: const TextStyle(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w900,
                        fontSize: 18,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            crossFadeState: expanded
                ? CrossFadeState.showSecond
                : CrossFadeState.showFirst,
            duration: const Duration(milliseconds: 220),
            sizeCurve: Curves.easeInOut,
          ),
        ],
      ),
    );
  }
}

class SummaryIcon extends StatelessWidget {
  const SummaryIcon({super.key, required this.icon, required this.color});

  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 34,
      height: 34,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        shape: BoxShape.circle,
      ),
      child: Icon(icon, size: 17, color: color),
    );
  }
}

class SummaryIconRow extends StatelessWidget {
  const SummaryIconRow({
    super.key,
    required this.icon,
    required this.iconColor,
    required this.label,
    required this.trailing,
    this.labelSuffix,
    this.labelStyle,
  });

  final IconData icon;
  final Color iconColor;
  final String label;
  final String? labelSuffix;
  final TextStyle? labelStyle;
  final Widget trailing;

  @override
  Widget build(BuildContext context) {
    final style = labelStyle ??
        const TextStyle(
          fontWeight: FontWeight.w700,
          fontSize: 13.5,
          color: AppColors.navy,
        );
    return Row(
      children: [
        SummaryIcon(icon: icon, color: iconColor),
        const SizedBox(width: 8),
        Expanded(
          child: labelSuffix == null
              ? Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: style,
                )
              : Text.rich(
                  TextSpan(
                    children: [
                      TextSpan(text: label, style: style),
                      TextSpan(
                        text: ' ($labelSuffix)',
                        style: const TextStyle(
                          fontWeight: FontWeight.w500,
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
        ),
        const SizedBox(width: 6),
        trailing,
      ],
    );
  }
}

class DiscountTypeToggle extends StatelessWidget {
  const DiscountTypeToggle({
    super.key,
    required this.isPercent,
    required this.onChanged,
    this.enabled = true,
  });

  final bool isPercent;
  final ValueChanged<String> onChanged;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 32,
      decoration: BoxDecoration(
        color: AppColors.primarySoft,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _chip(label: '₹', selected: !isPercent, type: 'Amount'),
          _chip(label: '%', selected: isPercent, type: 'Percent'),
        ],
      ),
    );
  }

  Widget _chip({
    required String label,
    required bool selected,
    required String type,
  }) {
    return InkWell(
      onTap: enabled ? () => onChanged(type) : null,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        width: 28,
        height: 32,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selected ? AppColors.primary : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontWeight: FontWeight.w800,
            fontSize: 12,
            color: selected ? Colors.white : AppColors.textSecondary,
          ),
        ),
      ),
    );
  }
}

class EditableValueBox extends StatelessWidget {
  const EditableValueBox({
    super.key,
    required this.controller,
    required this.onChanged,
    this.hint,
    this.prefix,
    this.suffix,
    this.enabled = true,
  });

  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final String? hint;
  final String? prefix;
  final String? suffix;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(10),
      borderSide: const BorderSide(color: AppColors.border),
    );
    return SizedBox(
      height: 36,
      child: TextField(
        controller: controller,
        enabled: enabled,
        textAlign: TextAlign.right,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        inputFormatters: [
          FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
        ],
        decoration: InputDecoration(
          isDense: true,
          filled: true,
          fillColor: const Color(0xFFF1F5F9),
          hintText: hint,
          hintStyle: TextStyle(
            fontWeight: FontWeight.w600,
            fontSize: 13,
            color: AppColors.textSecondary.withValues(alpha: 0.65),
          ),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 10,
            vertical: 8,
          ),
          border: border,
          enabledBorder: border,
          disabledBorder: border,
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(10),
            borderSide: const BorderSide(color: AppColors.primary, width: 1.4),
          ),
          prefixText: prefix == null ? null : '$prefix ',
          suffixText: suffix,
          prefixStyle: const TextStyle(
            fontWeight: FontWeight.w700,
            color: AppColors.textSecondary,
          ),
          suffixStyle: const TextStyle(
            fontWeight: FontWeight.w700,
            color: AppColors.textSecondary,
          ),
        ),
        style: const TextStyle(
          fontWeight: FontWeight.w700,
          fontSize: 14,
          color: AppColors.navy,
        ),
        onChanged: onChanged,
      ),
    );
  }
}
