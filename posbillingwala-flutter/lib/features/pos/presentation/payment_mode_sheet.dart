import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class PaymentModeChip extends StatelessWidget {
  const PaymentModeChip({
    super.key,
    required this.mode,
    required this.selected,
    this.onTap,
  });

  final PaymentMode mode;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final label = switch (mode) {
      PaymentMode.cash => 'CASH',
      PaymentMode.upi => 'UPI',
      PaymentMode.cashPlusUpi => 'SPLIT',
    };

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(10),
            border: Border.all(
              color: selected ? AppColors.primary : const Color(0xFFC9D4E5),
              width: selected ? 1.8 : 1,
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                selected ? Icons.radio_button_checked : Icons.radio_button_off,
                size: 16,
                color: selected ? AppColors.navy : AppColors.textSecondary,
              ),
              const SizedBox(width: 4),
              Flexible(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 11,
                    color: selected ? AppColors.navy : AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class PaymentModeSheet extends ConsumerStatefulWidget {
  const PaymentModeSheet({
    super.key,
    required this.totalAmount,
    required this.currency,
    required this.initialMode,
    required this.initialCash,
    required this.initialUpi,
    required this.onContinue,
  });

  final double totalAmount;
  final NumberFormat currency;
  final PaymentMode initialMode;
  final double initialCash;
  final double initialUpi;
  final void Function(PaymentMode mode, double cash, double upi) onContinue;

  @override
  ConsumerState<PaymentModeSheet> createState() => PaymentModeSheetState();
}

class PaymentModeSheetState extends ConsumerState<PaymentModeSheet> {
  late PaymentMode paymentPageMode;
  late final TextEditingController cashController;
  late final TextEditingController upiController;

  @override
  void initState() {
    super.initState();
    paymentPageMode = widget.initialMode;
    cashController = TextEditingController(
      text: amountInputText(widget.initialCash),
    );
    upiController = TextEditingController(
      text: amountInputText(widget.initialUpi),
    );
  }

  @override
  void dispose() {
    cashController.dispose();
    upiController.dispose();
    super.dispose();
  }

  double get paymentPageCash => double.tryParse(cashController.text) ?? 0;

  double get paymentPageUpi => double.tryParse(upiController.text) ?? 0;

  double get settlement =>
      double.parse((paymentPageCash + paymentPageUpi).toStringAsFixed(2));

  bool get settlementOk {
    if (paymentPageMode != PaymentMode.cashPlusUpi) return true;
    return (settlement - widget.totalAmount).abs() <= 0.05;
  }

  void paymentPageSelectMode(PaymentMode mode) {
    setState(() {
      paymentPageMode = mode;
      cashController.clear();
      upiController.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings.of(ref);
    final bottom = MediaQuery.viewInsetsOf(context).bottom;
    return Padding(
      padding: EdgeInsets.only(bottom: bottom),
      child: Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(18)),
        ),
        padding: const EdgeInsets.fromLTRB(18, 18, 18, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'Payment Mode',
              style: TextStyle(
                color: AppColors.danger,
                fontWeight: FontWeight.w900,
                fontSize: 18,
              ),
            ),
            const SizedBox(height: 10),
            Text(
              'Total Amount: ${widget.currency.format(widget.totalAmount)}',
              style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                for (final mode in PaymentMode.values) ...[
                  Expanded(
                    child: PaymentModeChip(
                      mode: mode,
                      selected: paymentPageMode == mode,
                      onTap: () => paymentPageSelectMode(mode),
                    ),
                  ),
                  if (mode != PaymentMode.values.last) const SizedBox(width: 8),
                ],
              ],
            ),
            if (paymentPageMode == PaymentMode.cashPlusUpi) ...[
              const SizedBox(height: 18),
              Row(
                children: [
                  Expanded(
                    child: AppTextField(
                      required: true,
                      controller: cashController,
                      label: strings.cashAmount,
                      hint: strings.cashAmount,
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                      ],
                      onChanged: (_) => setState(() {}),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: AppTextField(
                      required: true,
                      controller: upiController,
                      label: strings.upiAmount,
                      hint: strings.upiAmount,
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                      ],
                      onChanged: (_) => setState(() {}),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  '${strings.totalSettlement}: ${widget.currency.format(settlement)}',
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
              if (!settlementOk) ...[
                const SizedBox(height: 6),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    strings.settlementNotMatched,
                    style: const TextStyle(
                      color: AppColors.danger,
                      fontWeight: FontWeight.w600,
                      fontSize: 13,
                    ),
                  ),
                ),
              ],
            ],
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(
                  child: AppButton(
                    label: 'Dismiss',
                    variant: AppButtonVariant.danger,
                    onPressed: () => Navigator.pop(context, false),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: AppButton(
                    label: 'Continue',
                    onPressed: !settlementOk
                        ? null
                        : () => widget.onContinue(
                            paymentPageMode,
                            paymentPageCash,
                            paymentPageUpi,
                          ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
