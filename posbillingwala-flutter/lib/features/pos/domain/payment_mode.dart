/* Payment modes aligned with Android `PaymentSettlementHelper` + enterprise. */
enum PaymentMode {
  cash('Cash'),
  upi('UPI'),
  cashPlusUpi('Cash+UPI'),
  credit('Credit'),
  wallet('Wallet');

  const PaymentMode(this.label);

  final String label;

  static PaymentMode fromLabel(String? value) {
    final v = (value ?? '').trim().toLowerCase();
    if (v.contains('credit')) return PaymentMode.credit;
    if (v.contains('wallet')) return PaymentMode.wallet;
    if ((v.contains('cash') && v.contains('upi')) ||
        v.contains('mixed') ||
        v.contains('split') ||
        v.contains('+')) {
      return PaymentMode.cashPlusUpi;
    }
    if (v.contains('upi') || v == 'online') return PaymentMode.upi;
    return PaymentMode.cash;
  }
}

class PaymentTender {
  const PaymentTender({
    required this.mode,
    required this.cashAmount,
    required this.upiAmount,
  });

  final PaymentMode mode;
  final double cashAmount;
  final double upiAmount;

  factory PaymentTender.resolve({
    required PaymentMode mode,
    required double totalAmount,
    double? cashAmount,
    double? upiAmount,
  }) {
    final total = double.parse(totalAmount.toStringAsFixed(2));

    switch (mode) {
      case PaymentMode.cash:
        return PaymentTender(mode: mode, cashAmount: total, upiAmount: 0);
      case PaymentMode.upi:
        return PaymentTender(mode: mode, cashAmount: 0, upiAmount: total);
      case PaymentMode.credit:
      case PaymentMode.wallet:
        return PaymentTender(mode: mode, cashAmount: 0, upiAmount: 0);
      case PaymentMode.cashPlusUpi:
        final cash = double.parse((cashAmount ?? 0).toStringAsFixed(2));
        final upi = double.parse((upiAmount ?? 0).toStringAsFixed(2));
        return PaymentTender(mode: mode, cashAmount: cash, upiAmount: upi);
    }
  }

  bool isValidFor(double totalAmount) {
    if (mode == PaymentMode.credit || mode == PaymentMode.wallet) return true;
    /* Cash / UPI always settle the full bill — no amount entry to verify. */
    if (mode != PaymentMode.cashPlusUpi) return true;
    final total = double.parse(totalAmount.toStringAsFixed(2));
    final sum = double.parse((cashAmount + upiAmount).toStringAsFixed(2));
    return (sum - total).abs() <= 0.05 && cashAmount >= 0 && upiAmount >= 0;
  }
}

class SavedInvoiceResult {
  const SavedInvoiceResult({
    required this.invoiceId,
    required this.invoiceNumber,
    required this.totalAmount,
    required this.paymentMode,
  });

  final int invoiceId;
  final String invoiceNumber;
  final double totalAmount;
  final String paymentMode;
}
