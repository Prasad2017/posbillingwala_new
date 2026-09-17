import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_checkout_controller.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/payment_mode_sheet.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/connectivity_sync_listener.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Checkout without leaving the billing screen (Cash / UPI sheet → save). */
Future<void> startInlineCheckout(
  BuildContext context,
  WidgetRef ref, {
  bool printAfterSave = true,
  bool preferShare = false,
}) async {
  final summary = ref.read(cartSummaryProvider);
  if (summary.isEmpty) return;

  if (!ref.read(permissionControllerProvider).allows('bill.create')) {
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(AppStrings.of(ref).moduleLocked)),
    );
    return;
  }

  final checkout = ref.read(paymentCheckoutControllerProvider);
  final total = checkout.payableTotal(
    subtotal: summary.subtotal,
    taxTotal: summary.taxTotal,
  );
  final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹ ');

  ref.read(paymentCheckoutControllerProvider.notifier).selectMode(
        checkout.mode,
        total,
      );

  final confirmed = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (sheetContext) {
      final latest = ref.read(paymentCheckoutControllerProvider);
      return PaymentModeSheet(
        totalAmount: total,
        currency: currency,
        initialMode: latest.mode,
        initialCash: latest.cashAmount,
        initialUpi: latest.upiAmount,
        onContinue: (mode, cash, upi) {
          final n = ref.read(paymentCheckoutControllerProvider.notifier);
          n.selectMode(mode, total);
          if (mode == PaymentMode.cashPlusUpi) {
            n.setCashAmount(cash, total);
            if ((cash + upi - total).abs() > 0.05) {
              n.setUpiAmount(upi, total);
            }
          }
          Navigator.pop(sheetContext, true);
        },
      );
    },
  );

  if (confirmed != true || !context.mounted) return;

  await completeInlineCheckout(
    context,
    ref,
    currency: currency,
    printAfterSave: printAfterSave,
    preferShare: preferShare,
  );
}

Future<void> completeInlineCheckout(
  BuildContext context,
  WidgetRef ref, {
  required NumberFormat currency,
  bool printAfterSave = true,
  bool preferShare = false,
}) async {
  final strings = AppStrings.of(ref);
  final summary = ref.read(cartSummaryProvider);
  final result = await ref
      .read(paymentCheckoutControllerProvider.notifier)
      .completePayment(
        subtotal: summary.subtotal,
        taxTotal: summary.taxTotal,
      );
  if (!context.mounted || result == null) return;

  final session = ref.read(billingSessionProvider);
  final online = await isDeviceOnline();
  var retryAutoSync = !online;
  if (AppPlatform.requiresNetwork || online) {
    final sync = await ref
        .read(invoiceSyncControllerProvider.notifier)
        .uploadPending(onlyInvoiceId: result.invoiceId);
    if (!context.mounted) return;
    if (sync.failed > 0 || sync.uploaded < 1) {
      retryAutoSync = true;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            sync.message?.trim().isNotEmpty == true
                ? sync.message!
                : AppPlatform.requiresNetwork
                    ? kWebApiSaveFailedMessage
                    : 'Bill saved on device — cloud upload failed; will retry when online.',
          ),
          backgroundColor: AppPlatform.requiresNetwork
              ? AppColors.danger
              : AppColors.orange,
        ),
      );
      if (AppPlatform.requiresNetwork) return;
    }
  } else if (AppPlatform.requiresNetwork) {
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text(kOnlineRequiredMessage),
        backgroundColor: AppColors.danger,
      ),
    );
    return;
  }

  if (AppPlatform.supportsOfflineSync) {
    unawaited(
      ref.read(connectivitySyncListenerProvider).syncNow(
            force: retryAutoSync,
            reason: 'after-bill',
          ),
    );
  }

  final autoPrint = ref.read(printerSettingsProvider).autoShareOnSave;
  if (!context.mounted) return;
  await showDialog<void>(
    context: context,
    barrierDismissible: false,
    builder: (dialogContext) => AlertDialog(
      title: Text(strings.billSaved),
      content: Text(
        'Invoice: ${result.invoiceNumber}\n'
        'Payment: ${result.paymentMode}\n'
        'Amount: ${currency.format(result.totalAmount)}'
        '${session.tableNumber != null ? '\nTable: ${session.tableNumber}' : ''}'
        '${session.customerName != null ? '\nCustomer: ${session.customerName}' : ''}',
      ),
      actions: [
        TextButton(
          onPressed: () async {
            final printResult = await printInvoiceById(ref, result.invoiceId);
            if (!dialogContext.mounted) return;
            ScaffoldMessenger.of(dialogContext).showSnackBar(
              SnackBar(content: Text(printResult.message ?? 'Print done')),
            );
          },
          child: Text(strings.printShare),
        ),
        AppButton(
          label: strings.addProducts,
          onPressed: () {
            Navigator.of(dialogContext).pop();
            ref.read(paymentCheckoutControllerProvider.notifier).reset();
            context.go(session.billingRoute);
          },
          expanded: false,
        ),
        TextButton(
          onPressed: () {
            Navigator.of(dialogContext).pop();
            ref.read(paymentCheckoutControllerProvider.notifier).reset();
            context.go('/');
          },
          child: Text(strings.home),
        ),
      ],
    ),
  );

  if (autoPrint && printAfterSave && !preferShare && context.mounted) {
    final printResult = await printInvoiceById(ref, result.invoiceId);
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(printResult.message ?? 'Print done')),
    );
  } else if (preferShare && context.mounted) {
    final printResult = await printInvoiceById(
      ref,
      result.invoiceId,
      preferShare: true,
    );
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(printResult.message ?? 'Share done')),
    );
  }
}
