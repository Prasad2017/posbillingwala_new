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
    final go = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(strings.paymentDisplayShowQr),
        content: Text(strings.paymentDisplayAlreadyPaidConfirm),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(strings.cancel),
          ),
          AppButton(
            label: strings.paymentDisplayShowQr,
            onPressed: () => Navigator.pop(ctx, true),
            expanded: false,
          ),
        ],
      ),
    );
    if (go != true || !context.mounted) return;
  }

  final result = await ref
      .read(paymentDisplayServiceProvider)
      .showInvoiceOnPaymentDisplay(invoice);

  if (!context.mounted) return;
  await _handleShowResult(context, ref, result, strings);
}

Future<void> _handleShowResult(
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
      final action = await showDialog<String>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(strings.paymentDisplayTitle),
          content: Text(strings.paymentDisplayNotConnected),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, 'cancel'),
              child: Text(strings.cancel),
            ),
            AppButton(
              label: strings.paymentDisplayConnect,
              onPressed: () => Navigator.pop(ctx, 'connect'),
              expanded: false,
            ),
          ],
        ),
      );
      if (action == 'connect' && context.mounted) {
        context.push('/settings/payment-display');
      }
      return;
    case ShowPaymentDisplayResultCode.upiNotConfigured:
      final action = await showDialog<String>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(strings.paymentDisplayTitle),
          content: Text(strings.paymentDisplayUpiNotConfigured),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, 'cancel'),
              child: Text(strings.cancel),
            ),
            AppButton(
              label: strings.paymentDisplayOpenPaymentSettings,
              onPressed: () => Navigator.pop(ctx, 'settings'),
              expanded: false,
            ),
          ],
        ),
      );
      if (action == 'settings' && context.mounted) {
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
