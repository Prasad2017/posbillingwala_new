import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_checkout_controller.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/bill_summary_card.dart';

class PaymentTenderPanel extends ConsumerStatefulWidget {
  const PaymentTenderPanel({
    super.key,
    required this.summary,
    required this.checkout,
    required this.payable,
    required this.currency,
    required this.discountController,
    required this.packingController,
    required this.receivedController,
    required this.cashController,
    required this.upiController,
    required this.onDiscountChanged,
    required this.onDiscountTypeChanged,
    required this.onPackingChanged,
    required this.onSyncControllers,
    this.sideBySide = false,
  });

  final CartSummary summary;
  final PaymentCheckoutState checkout;
  final double payable;
  final NumberFormat currency;
  final TextEditingController discountController;
  final TextEditingController packingController;
  final TextEditingController receivedController;
  final TextEditingController cashController;
  final TextEditingController upiController;
  final ValueChanged<String> onDiscountChanged;
  final ValueChanged<String> onDiscountTypeChanged;
  final ValueChanged<String> onPackingChanged;
  final VoidCallback onSyncControllers;
  final bool sideBySide;

  @override
  ConsumerState<PaymentTenderPanel> createState() => _PaymentTenderPanelState();
}

class _PaymentTenderPanelState extends ConsumerState<PaymentTenderPanel> {
  bool billExpanded = true;

  @override
  Widget build(BuildContext context) {
    /* Payment mode is chosen via sheet on Save / Share / Print — not inline. */
    return BillSummaryCard(
      summary: widget.summary,
      checkout: widget.checkout,
      currency: widget.currency,
      discountController: widget.discountController,
      packingController: widget.packingController,
      payable: widget.payable,
      expanded: billExpanded,
      onToggleExpanded: () => setState(() => billExpanded = !billExpanded),
      onDiscountChanged: widget.onDiscountChanged,
      onDiscountTypeChanged: widget.onDiscountTypeChanged,
      onPackingChanged: widget.onPackingChanged,
    );
  }
}

class PaymentBalanceBar extends StatelessWidget {
  const PaymentBalanceBar({
    super.key,
    required this.checkout,
    required this.payable,
    required this.currency,
  });

  final PaymentCheckoutState checkout;
  final double payable;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context) {
    final change = checkout.balanceToReturn(payable);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: const Color(0xFFE8F8EE),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.green.withValues(alpha: 0.35)),
      ),
      child: Row(
        children: [
          const Icon(Icons.payments_outlined, color: AppColors.green),
          const SizedBox(width: 10),
          const Expanded(
            child: Text(
              'Balance to Return',
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: AppColors.navy,
              ),
            ),
          ),
          Text(
            currency.format(change),
            style: const TextStyle(
              fontWeight: FontWeight.w900,
              fontSize: 18,
              color: AppColors.green,
            ),
          ),
        ],
      ),
    );
  }
}

class PaymentDoneBar extends StatelessWidget {
  const PaymentDoneBar({
    super.key,
    required this.busy,
    required this.enabled,
    required this.onCancel,
    required this.onDonePrint,
    this.onSave,
    this.onShare,
  });

  final bool busy;
  final bool enabled;
  final VoidCallback onCancel;
  final VoidCallback onDonePrint;
  final VoidCallback? onSave;
  final VoidCallback? onShare;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (onSave != null || onShare != null) ...[
            Row(
              children: [
                if (onSave != null)
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: busy || !enabled ? null : onSave,
                      icon: const Icon(Icons.save_outlined, size: 18),
                      label: const Text('Save'),
                    ),
                  ),
                if (onSave != null && onShare != null) const SizedBox(width: 8),
                if (onShare != null)
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: busy || !enabled ? null : onShare,
                      icon: const Icon(Icons.share_outlined, size: 18),
                      label: const Text('Share'),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 8),
          ],
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: busy ? null : onCancel,
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(50),
                    foregroundColor: AppColors.textSecondary,
                  ),
                  child: const Text(
                    'Cancel',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                flex: 2,
                child: FilledButton.icon(
                  onPressed: busy || !enabled ? null : onDonePrint,
                  style: FilledButton.styleFrom(
                    minimumSize: const Size.fromHeight(50),
                    backgroundColor: AppColors.primary,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  icon: busy
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.print_rounded),
                  label: Text(
                    busy ? 'Processing…' : 'Done & Print',
                    style: const TextStyle(fontWeight: FontWeight.w800),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
