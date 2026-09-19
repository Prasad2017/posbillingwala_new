import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/display_connection_manager.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_models.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_service.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Shared Invoice → Show QR UX (list + detail). */
Future<void> requestShowInvoicePaymentQr(
  BuildContext context,
  WidgetRef ref,
  Invoice invoice, {
  bool confirmAlreadyPaid = true,
}) async {
  final strings = AppStrings.of(ref);

  if (AppPlatform.isWeb) {
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(strings.paymentDisplayAndroidOnly)),
    );
    return;
  }

  final status = invoice.invoiceOrderStatus.trim().toLowerCase();
  if (status == 'cancelled' || status == 'refunded') {
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(strings.paymentDisplayInvoiceInvalid)),
    );
    return;
  }

  if (confirmAlreadyPaid) {
    final go = await showAppConfirmBottomSheet(
      context: context,
      title: strings.paymentDisplayShowQr,
      message:
          '${strings.paymentDisplayAlreadyPaidConfirm}\n\n'
          '${invoice.invoiceNumber} · ₹${invoice.totalAmount.toStringAsFixed(2)}',
      confirmLabel: strings.paymentDisplayShowQr,
      cancelLabel: strings.cancel,
      icon: Icons.qr_code_2_rounded,
    );
    if (!go || !context.mounted) return;
  }

  final result = await ref
      .read(paymentDisplayServiceProvider)
      .showInvoiceOnPaymentDisplay(invoice);

  if (!context.mounted) return;
  await handleShowPaymentDisplayResult(context, ref, result, strings);
}

Future<void> handleShowPaymentDisplayResult(
  BuildContext context,
  WidgetRef ref,
  ShowPaymentDisplayResult result,
  AppStrings strings,
) async {
  switch (result.code) {
    case ShowPaymentDisplayResultCode.success:
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(strings.paymentDisplayQrSent)),
      );
      return;
    case ShowPaymentDisplayResultCode.displayNotConnected:
      final connect = await showAppConfirmBottomSheet(
        context: context,
        title: strings.paymentDisplayTitle,
        message: strings.paymentDisplayNotConnected,
        confirmLabel: strings.paymentDisplayConnect,
        cancelLabel: strings.cancel,
        icon: Icons.cast_connected_rounded,
      );
      if (connect && context.mounted) {
        context.push('/settings/payment-display');
      }
      return;
    case ShowPaymentDisplayResultCode.upiNotConfigured:
      final open = await showAppConfirmBottomSheet(
        context: context,
        title: strings.paymentDisplayTitle,
        message: strings.paymentDisplayUpiNotConfigured,
        confirmLabel: strings.paymentDisplayOpenPaymentSettings,
        cancelLabel: strings.cancel,
        icon: Icons.account_balance_wallet_rounded,
      );
      if (open && context.mounted) {
        context.push('/settings/company');
      }
      return;
    case ShowPaymentDisplayResultCode.invalidAmount:
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(strings.paymentDisplayInvalidAmount)),
      );
      return;
    case ShowPaymentDisplayResultCode.invoiceInvalid:
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(strings.paymentDisplayInvoiceInvalid)),
      );
      return;
    case ShowPaymentDisplayResultCode.notSupported:
    case ShowPaymentDisplayResultCode.qrGenerationFailed:
    case ShowPaymentDisplayResultCode.serverError:
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(result.message ?? strings.paymentDisplayError),
          backgroundColor: AppColors.danger,
        ),
      );
  }
}

bool paymentDisplayHasActiveConnection(WidgetRef ref) {
  final ui = ref.read(displayConnectionManagerProvider);
  return ui.serverRunning && ui.connectedClients > 0;
}
