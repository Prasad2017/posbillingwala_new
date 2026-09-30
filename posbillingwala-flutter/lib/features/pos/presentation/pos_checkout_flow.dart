import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_service.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_checkout_controller.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/payment_mode_sheet.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_auto_connect.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_device_picker_page.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/connectivity_sync_listener.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

enum PosCheckoutAction { save, share, print }

/* Shows Cash / UPI / Split sheet. Dismiss → false (same UI). Continue → true. */
Future<bool> promptPaymentMode(BuildContext context, WidgetRef ref) async {
  final summary = ref.read(cartSummaryProvider);
  if (summary.isEmpty) return false;

  final checkout = ref.read(paymentCheckoutControllerProvider);
  final total = checkout.payableTotal(
    subtotal: summary.subtotal,
    taxTotal: summary.taxTotal,
  );
  final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹ ');

  ref
      .read(paymentCheckoutControllerProvider.notifier)
      .selectMode(
        checkout.mode == PaymentMode.card ? PaymentMode.cash : checkout.mode,
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
        initialMode: latest.mode == PaymentMode.card
            ? PaymentMode.cash
            : latest.mode,
        initialCash: latest.cashAmount,
        initialUpi: latest.upiAmount,
        onContinue: (mode, cash, upi) {
          final n = ref.read(paymentCheckoutControllerProvider.notifier);
          if (mode == PaymentMode.cashPlusUpi) {
            n.setSplitAmounts(cash: cash, upi: upi);
          } else {
            n.selectMode(mode, total);
          }
          Navigator.pop(sheetContext, true);
        },
      );
    },
  );

  return confirmed == true && context.mounted;
}

/* Save / Share / Print from Cart or POS — sheet on current screen, no navigation. */
Future<void> runPosCheckoutAction(
  BuildContext context,
  WidgetRef ref, {
  required PosCheckoutAction action,
}) async {
  final summary = ref.read(cartSummaryProvider);
  if (summary.isEmpty) {
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Cart is empty — add products first')),
    );
    return;
  }

  if (!ref.read(permissionControllerProvider).allows('bill.create')) {
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(AppStrings.of(ref).moduleLocked)),
    );
    return;
  }

  /* Dismiss keeps the same Cart / POS UI — do not navigate away. */
  if (!await promptPaymentMode(context, ref)) return;
  if (!context.mounted) return;

  if (action == PosCheckoutAction.print) {
    final printerReady = await ensureBillPrinterReady(context, ref);
    if (!context.mounted) return;
    if (!printerReady) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Connect a bill printer to print'),
          backgroundColor: AppColors.danger,
        ),
      );
      return;
    }
  }

  await completePosCheckout(
    context,
    ref,
    printAfterSave: action == PosCheckoutAction.print,
    preferShare: action == PosCheckoutAction.share,
    requirePrintSuccess: action == PosCheckoutAction.print,
  );
}

Future<void> completePosCheckout(
  BuildContext context,
  WidgetRef ref, {
  bool printAfterSave = false,
  bool preferShare = false,
  bool requirePrintSuccess = false,
}) async {
  final summary = ref.read(cartSummaryProvider);
  final result = await ref
      .read(paymentCheckoutControllerProvider.notifier)
      .completePayment(subtotal: summary.subtotal, taxTotal: summary.taxTotal);
  if (!context.mounted || result == null) return;

  final session = ref.read(billingSessionProvider);

  if (requirePrintSuccess || printAfterSave) {
    final printResult = await printInvoiceById(
      ref,
      result.invoiceId,
      preferShare: preferShare,
    );
    if (!context.mounted) return;
    final printedOk =
        printResult.outcome != PrintOutcome.failed &&
        printResult.outcome != PrintOutcome.previewOnly;
    if (requirePrintSuccess && !printedOk) {
      ref.read(paymentCheckoutControllerProvider.notifier).reset();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            printResult.message?.trim().isNotEmpty == true
                ? '${printResult.message} Bill saved — reprint from Invoice List.'
                : 'Print failed — bill saved. Reprint from Invoice List.',
          ),
          backgroundColor: AppColors.orange,
        ),
      );
      /* Capture container before navigate — WidgetRef dies with the route. */
      unawaited(
        _uploadBillInBackground(
          ProviderScope.containerOf(context),
          result.invoiceId,
        ),
      );
      context.go(session.billingRoute);
      return;
    }
    if (!requirePrintSuccess && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(printResult.message ?? 'Print done')),
      );
    }
  } else if (preferShare) {
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

  /* Snapshot container while route is still mounted for fire-and-forget work. */
  final container = ProviderScope.containerOf(context);
  unawaited(tryAutoShowPaymentDisplayAfterBill(container, result));

  if (AppPlatform.requiresNetwork) {
    final online = await isDeviceOnline();
    if (!online) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(kOnlineRequiredMessage),
          backgroundColor: AppColors.danger,
        ),
      );
      return;
    }
    final sync = await ref
        .read(invoiceSyncControllerProvider.notifier)
        .uploadPending(onlyInvoiceId: result.invoiceId);
    if (!context.mounted) return;
    if (sync.failed > 0 || sync.uploaded < 1) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            sync.message?.trim().isNotEmpty == true
                ? sync.message!
                : kWebApiSaveFailedMessage,
          ),
          backgroundColor: AppColors.danger,
        ),
      );
      return;
    }
  } else {
    unawaited(_uploadBillInBackground(container, result.invoiceId));
  }

  if (!context.mounted) return;
  ref.read(paymentCheckoutControllerProvider.notifier).reset();
  context.go(session.billingRoute);
}

/* Use ProviderContainer — WidgetRef is invalid after the checkout route disposes. */
Future<void> _uploadBillInBackground(
  ProviderContainer container,
  int invoiceId,
) async {
  final online = await isDeviceOnline();
  var retryAutoSync = !online;
  if (online) {
    try {
      final sync = await container
          .read(invoiceSyncControllerProvider.notifier)
          .uploadPending(onlyInvoiceId: invoiceId);
      if (sync.failed > 0 || sync.uploaded < 1) retryAutoSync = true;
    } catch (_) {
      retryAutoSync = true;
    }
  }
  if (AppPlatform.supportsOfflineSync) {
    unawaited(
      container
          .read(connectivitySyncListenerProvider)
          .syncNow(force: retryAutoSync, reason: 'after-bill'),
    );
  }
}

Future<bool> ensureBillPrinterReady(BuildContext context, WidgetRef ref) async {
  if (AppPlatform.isWeb) return true;

  var settings = ref.read(printerSettingsProvider);
  await PrinterAutoConnect.ensureSavedPrinters(settings);
  settings = ref.read(printerSettingsProvider);
  final hub = BluetoothPrinterHub.instance;
  final usbHub = EscPosTransportHub.instance;

  Future<bool> pickBillPrinter() async {
    final picked = await Navigator.of(context).push<PickedPrinter>(
      MaterialPageRoute(
        builder: (_) => PrinterDevicePickerPage(
          channel: PrinterChannelKind.bill,
          initialTransport: settings.billTransport == PosPrinterTransport.usb
              ? PosPrinterTransport.usb
              : settings.billTransport == PosPrinterTransport.network
              ? PosPrinterTransport.network
              : PosPrinterTransport.bluetooth,
          title: settings.billTransport == PosPrinterTransport.usb
              ? 'Select USB printer'
              : settings.billTransport == PosPrinterTransport.network
              ? 'Select network printer'
              : 'Select Bluetooth printer',
        ),
      ),
    );
    if (picked == null || !context.mounted) return false;
    final current = ref.read(printerSettingsProvider);
    final updated = current.copyWith(
      billBluetoothAddress: picked.transport == PosPrinterTransport.bluetooth
          ? picked.bluetoothMac
          : current.billBluetoothAddress,
      billUsbIdentifier: picked.transport == PosPrinterTransport.usb
          ? picked.usbIdentifier
          : current.billUsbIdentifier,
      billUsbName: picked.transport == PosPrinterTransport.usb
          ? picked.usbName
          : current.billUsbName,
      networkHost: picked.transport == PosPrinterTransport.network
          ? picked.networkHost
          : current.networkHost,
      networkPort: picked.transport == PosPrinterTransport.network
          ? picked.networkPort
          : current.networkPort,
      billTransport: picked.transport,
    );
    await ref.read(printerSettingsProvider.notifier).update(updated);
    settings = updated;
    hub.updateSavedAddresses(
      billMac: updated.billBluetoothAddress,
      kotMac: updated.kotBluetoothAddress,
    );
    return true;
  }

  switch (settings.billTransport) {
    case PosPrinterTransport.bluetooth:
      var mac = settings.billBluetoothAddress.trim();
      if (mac.isEmpty) {
        if (!await pickBillPrinter()) return false;
        settings = ref.read(printerSettingsProvider);
        mac = settings.billBluetoothAddress.trim();
      }
      if (mac.isEmpty) return false;
      if (await hub.ensureReady(PrinterChannelKind.bill)) return true;
      final connected = await hub.connect(
        PrinterChannelKind.bill,
        address: mac,
      );
      if (connected) return true;
      if (!await pickBillPrinter()) return false;
      settings = ref.read(printerSettingsProvider);
      mac = settings.billBluetoothAddress.trim();
      return mac.isNotEmpty &&
          await hub.connect(PrinterChannelKind.bill, address: mac);
    case PosPrinterTransport.usb:
      usbHub.updateSavedUsb(
        identifier: settings.billUsbIdentifier,
        name: settings.billUsbName,
      );
      var id = settings.billUsbIdentifier.trim();
      if (id.isEmpty) {
        if (!await pickBillPrinter()) return false;
        settings = ref.read(printerSettingsProvider);
        id = settings.billUsbIdentifier.trim();
        usbHub.updateSavedUsb(
          identifier: settings.billUsbIdentifier,
          name: settings.billUsbName,
        );
      }
      if (id.isEmpty) return false;
      if (await usbHub.ensureUsbReady()) return true;
      if (!await pickBillPrinter()) return false;
      settings = ref.read(printerSettingsProvider);
      usbHub.updateSavedUsb(
        identifier: settings.billUsbIdentifier,
        name: settings.billUsbName,
      );
      return settings.billUsbIdentifier.trim().isNotEmpty &&
          await usbHub.ensureUsbReady();
    case PosPrinterTransport.network:
      if (settings.networkHost.trim().isEmpty) {
        if (!await pickBillPrinter()) return false;
        settings = ref.read(printerSettingsProvider);
      }
      return settings.networkHost.trim().isNotEmpty;
  }
}
