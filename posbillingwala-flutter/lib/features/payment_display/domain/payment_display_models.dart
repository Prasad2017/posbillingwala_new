import 'dart:convert';

/* Models for offline payment QR browser display. */

enum PaymentDisplayConnectionStatus {
  stopped,
  starting,
  waitingForPair,
  connected,
  error,
}

class PaymentDisplayBillPayload {
  const PaymentDisplayBillPayload({
    required this.billId,
    required this.billNumber,
    required this.amount,
    required this.currency,
    required this.shopName,
    required this.upiId,
    required this.payeeName,
    required this.qrPayload,
    required this.qrSvg,
    required this.displayedAt,
    required this.expiresAt,
  });

  final String billId;
  final String billNumber;
  final double amount;
  final String currency;
  final String shopName;
  final String upiId;
  final String payeeName;
  final String qrPayload;
  final String qrSvg;
  final DateTime displayedAt;
  final DateTime expiresAt;

  bool get isExpired => DateTime.now().isAfter(expiresAt);

  Duration get remaining {
    final left = expiresAt.difference(DateTime.now());
    return left.isNegative ? Duration.zero : left;
  }

  Map<String, dynamic> toJson() => {
    'billId': billId,
    'billNumber': billNumber,
    'amount': amount,
    'currency': currency,
    'shopName': shopName,
    'upiId': maskUpi(upiId),
    'payeeName': payeeName,
    'qrPayload': qrPayload,
    'qrSvg': qrSvg,
    'displayedAt': displayedAt.toIso8601String(),
    'expiresAt': expiresAt.toIso8601String(),
  };

  /* Wire payload for browser (no UPI ID needed on screen). */
  Map<String, dynamic> toWireJson() => {
    'billId': billId,
    'billNumber': billNumber,
    'amount': amount,
    'currency': currency,
    'shopName': shopName,
    'payeeName': payeeName,
    'qrPayload': qrPayload,
    'qrSvg': qrSvg,
    'displayedAt': displayedAt.toIso8601String(),
    'expiresAt': expiresAt.toIso8601String(),
  };

  factory PaymentDisplayBillPayload.fromJson(Map<String, dynamic> json) {
    return PaymentDisplayBillPayload(
      billId: '${json['billId'] ?? ''}',
      billNumber: '${json['billNumber'] ?? ''}',
      amount: (json['amount'] as num?)?.toDouble() ?? 0,
      currency: '${json['currency'] ?? 'INR'}',
      shopName: '${json['shopName'] ?? ''}',
      upiId: '${json['upiId'] ?? ''}',
      payeeName: '${json['payeeName'] ?? ''}',
      qrPayload: '${json['qrPayload'] ?? ''}',
      qrSvg: '${json['qrSvg'] ?? ''}',
      displayedAt:
          DateTime.tryParse('${json['displayedAt'] ?? ''}') ?? DateTime.now(),
      expiresAt:
          DateTime.tryParse('${json['expiresAt'] ?? ''}') ?? DateTime.now(),
    );
  }

  static String maskUpi(String upi) {
    final v = upi.trim();
    final at = v.indexOf('@');
    if (at <= 1) return '***';
    return '${v[0]}***${v.substring(at)}';
  }
}

class PaymentDisplayMessage {
  const PaymentDisplayMessage({required this.type, this.payload});

  final String type;
  final Map<String, dynamic>? payload;

  Map<String, dynamic> toJson() => {
    'type': type,
    if (payload != null) 'payload': payload,
  };

  String encode() => jsonEncode(toJson());
}

enum ShowPaymentDisplayResultCode {
  success,
  notSupported,
  displayNotConnected,
  upiNotConfigured,
  invalidAmount,
  invoiceInvalid,
  qrGenerationFailed,
  serverError,
}

class ShowPaymentDisplayResult {
  const ShowPaymentDisplayResult({
    required this.code,
    this.message,
    this.payload,
  });

  final ShowPaymentDisplayResultCode code;
  final String? message;
  final PaymentDisplayBillPayload? payload;

  bool get isSuccess => code == ShowPaymentDisplayResultCode.success;
}
