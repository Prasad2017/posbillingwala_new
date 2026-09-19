import 'package:flutter_test/flutter_test.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_models.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/upi_qr_service.dart';

void main() {
  const upi = UpiQrService();

  group('UpiQrService', () {
    test('rejects invalid UPI id', () {
      expect(upi.isValidUpiId(''), isFalse);
      expect(upi.isValidUpiId('not-an-upi'), isFalse);
      expect(upi.isValidUpiId('bad @bank'), isFalse);
    });

    test('accepts valid UPI id', () {
      expect(upi.isValidUpiId('restaurant@upi'), isTrue);
      expect(upi.isValidUpiId('shop.name@oksbi'), isTrue);
    });

    test('rejects zero or negative amount', () {
      final zero = upi.generate(
        upiId: 'shop@upi',
        payeeName: 'Shop',
        amount: 0,
      );
      expect(zero.ok, isFalse);
      final neg = upi.generate(
        upiId: 'shop@upi',
        payeeName: 'Shop',
        amount: -10,
      );
      expect(neg.ok, isFalse);
    });

    test('builds encoded UPI URI and SVG for valid amount', () {
      final result = upi.generate(
        upiId: 'restaurant@upi',
        payeeName: 'ABC Restaurant',
        amount: 850,
        note: '1025',
      );
      expect(result.ok, isTrue);
      expect(result.uri, contains('upi://pay?'));
      expect(result.uri, contains('pa=restaurant@upi'));
      expect(result.uri, contains('am=850.00'));
      expect(result.uri, contains('cu=INR'));
      expect(result.svg, contains('<svg'));
      expect(result.svg, contains('#000000'));
    });
  });

  group('PaymentDisplayBillPayload', () {
    test('isExpired respects expiresAt', () {
      final bill = PaymentDisplayBillPayload(
        billId: '1',
        billNumber: '1025',
        amount: 100,
        currency: 'INR',
        shopName: 'Shop',
        upiId: 'a@upi',
        payeeName: 'Shop',
        qrPayload: 'upi://pay?pa=a@upi&am=100.00',
        qrSvg: '<svg></svg>',
        displayedAt: DateTime.now().subtract(const Duration(minutes: 6)),
        expiresAt: DateTime.now().subtract(const Duration(minutes: 1)),
      );
      expect(bill.isExpired, isTrue);
      expect(bill.remaining, Duration.zero);
    });

    test('wire json omits upi id', () {
      final bill = PaymentDisplayBillPayload(
        billId: '1',
        billNumber: '1025',
        amount: 100,
        currency: 'INR',
        shopName: 'Shop',
        upiId: 'secret@upi',
        payeeName: 'Shop',
        qrPayload: 'upi://pay?pa=secret@upi&am=100.00',
        qrSvg: '<svg></svg>',
        displayedAt: DateTime.now(),
        expiresAt: DateTime.now().add(const Duration(minutes: 5)),
      );
      final wire = bill.toWireJson();
      expect(wire.containsKey('upiId'), isFalse);
      expect(wire['qrPayload'], contains('upi://pay'));
      expect(wire['expiresAt'], isNotEmpty);
    });
  });
}
