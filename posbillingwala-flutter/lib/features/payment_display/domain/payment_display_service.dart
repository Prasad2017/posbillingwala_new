import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/display_connection_manager.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_models.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/upi_qr_service.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';

/* Orchestrates Show QR for a specific invoice (auto or manual). */
class PaymentDisplayService {
  PaymentDisplayService(this.ref);

  final Ref ref;
  final _upi = const UpiQrService();

  Future<ShowPaymentDisplayResult> showInvoiceOnPaymentDisplay(
    Invoice invoice, {
    bool requireConnectedDisplay = true,
  }) async {
    return await showBill(
      billId: '${invoice.invoiceId}',
      billNumber: invoice.invoiceNumber,
      amount: _payableAmount(invoice),
      invoiceOrderStatus: invoice.invoiceOrderStatus,
      requireConnectedDisplay: requireConnectedDisplay,
    );
  }

  Future<ShowPaymentDisplayResult> showSavedInvoiceResult(
    SavedInvoiceResult result,
  ) async {
    try {
      final db = ref.read(appDatabaseProvider);
      final invoice = await db.getInvoiceById(result.invoiceId);
      if (invoice != null) {
        return await showInvoiceOnPaymentDisplay(invoice);
      }
    } catch (e, st) {
      AppLogger.error('PAYMENT_DISPLAY_ERROR load_invoice', e, st);
    }
    return await showBill(
      billId: '${result.invoiceId}',
      billNumber: result.invoiceNumber,
      amount: result.totalAmount,
      invoiceOrderStatus: 'completed',
    );
  }

  Future<ShowPaymentDisplayResult> showBill({
    required String billId,
    required String billNumber,
    required double amount,
    String invoiceOrderStatus = 'completed',
    bool requireConnectedDisplay = true,
  }) async {
    try {
      final status = invoiceOrderStatus.trim().toLowerCase();
      if (status == 'cancelled' || status == 'refunded') {
        return const ShowPaymentDisplayResult(
          code: ShowPaymentDisplayResultCode.invoiceInvalid,
          message: 'Cannot display QR for voided or refunded bills.',
        );
      }

      final payable = double.parse(amount.toStringAsFixed(2));
      if (payable <= 0 || payable.isNaN || payable.isInfinite) {
        return const ShowPaymentDisplayResult(
          code: ShowPaymentDisplayResultCode.invalidAmount,
          message: 'Invalid payment amount.',
        );
      }

      final shop = ref.read(shopReceiptProfileProvider);
      if (!shop.hasUpiId || !_upi.isValidUpiId(shop.upiId)) {
        return const ShowPaymentDisplayResult(
          code: ShowPaymentDisplayResultCode.upiNotConfigured,
          message: 'UPI payment is not configured.',
        );
      }

      final manager = ref.read(displayConnectionManagerProvider.notifier);
      final ui = ref.read(displayConnectionManagerProvider);
      if (requireConnectedDisplay &&
          (!ui.serverRunning || ui.connectedClients < 1)) {
        return const ShowPaymentDisplayResult(
          code: ShowPaymentDisplayResultCode.displayNotConnected,
          message: 'Payment display is not connected.',
        );
      }

      final payee = _upi.resolvePayeeName(shop);
      final shopName = _upi.resolveShopName(shop);
      final qr = _upi.generate(
        upiId: shop.upiId,
        payeeName: payee,
        amount: payable,
        note: billNumber,
      );
      if (!qr.ok || qr.uri == null || qr.svg == null) {
        return ShowPaymentDisplayResult(
          code: ShowPaymentDisplayResultCode.qrGenerationFailed,
          message: qr.error ?? 'QR generation failed.',
        );
      }

      final now = DateTime.now();
      final expiresAt = now.add(manager.qrDuration);
      final payload = PaymentDisplayBillPayload(
        billId: billId,
        billNumber: billNumber,
        amount: payable,
        currency: 'INR',
        shopName: shopName,
        upiId: shop.upiId,
        payeeName: payee,
        qrPayload: qr.uri!,
        qrSvg: qr.svg!,
        displayedAt: now,
        expiresAt: expiresAt,
      );

      return await manager.publishBill(payload);
    } catch (e, st) {
      AppLogger.error('PAYMENT_DISPLAY_ERROR show_invoice', e, st);
      return const ShowPaymentDisplayResult(
        code: ShowPaymentDisplayResultCode.serverError,
        message: 'Could not show payment QR.',
      );
    }
  }

  double _payableAmount(Invoice invoice) {
    if (invoice.totalAmount > 0) {
      return double.parse(invoice.totalAmount.toStringAsFixed(2));
    }
    if (invoice.upiAmount > 0) {
      return double.parse(invoice.upiAmount.toStringAsFixed(2));
    }
    return 0;
  }
}

final paymentDisplayServiceProvider = Provider<PaymentDisplayService>((ref) {
  return PaymentDisplayService(ref);
});

/* Fire-and-forget auto display after a successful new bill. Never throws. */
Future<void> tryAutoShowPaymentDisplayAfterBill(
  WidgetRef ref,
  SavedInvoiceResult result,
) async {
  try {
    final manager = ref.read(displayConnectionManagerProvider.notifier);
    if (!manager.autoDisplayEnabled) return;
    final ui = ref.read(displayConnectionManagerProvider);
    if (!ui.serverRunning || ui.connectedClients < 1) return;
    await ref
        .read(paymentDisplayServiceProvider)
        .showSavedInvoiceResult(result);
  } catch (e, st) {
    AppLogger.error('PAYMENT_DISPLAY_ERROR auto_show', e, st);
  }
}
