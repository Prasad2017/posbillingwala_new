import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_date.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_checkout_controller.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/bill_summary_card.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/payment_tender_panel.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/portion_picker.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/pos_checkout_flow.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_device_picker_page.dart';
import 'package:pos_billingwala_v2/features/sync/domain/connectivity_sync_listener.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class PaymentPage extends ConsumerStatefulWidget {
  const PaymentPage({super.key, this.initialAction});

  /* Optional deep-link from POS footer: save | share | print */
  final String? initialAction;

  @override
  ConsumerState<PaymentPage> createState() => PaymentPageState();
}

class PaymentPageState extends ConsumerState<PaymentPage> {
  final cashController = TextEditingController();
  final upiController = TextEditingController();
  final receivedController = TextEditingController();
  final paymentPageDiscountController = TextEditingController();
  final paymentPagePackingController = TextEditingController();
  final customerNameController = TextEditingController();
  final customerPhoneController = TextEditingController();
  final customerEmailController = TextEditingController();
  final customerAddressController = TextEditingController();
  late final NumberFormat paymentPageCurrency;
  bool initialActionHandled = false;

  @override
  void initState() {
    super.initState();
    paymentPageCurrency = NumberFormat.currency(locale: 'en_IN', symbol: '₹ ');
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final session = ref.read(billingSessionProvider);
      customerNameController.text = session.customerName ?? '';
      customerPhoneController.text = session.customerPhone ?? '';
      customerEmailController.text = session.customerEmail ?? '';
      customerAddressController.text = session.customerAddress ?? '';
      final summary = ref.read(cartSummaryProvider);
      final total = ref
          .read(paymentCheckoutControllerProvider)
          .payableTotal(subtotal: summary.subtotal, taxTotal: summary.taxTotal);
      ref
          .read(paymentCheckoutControllerProvider.notifier)
          .setDiscount(0, type: 'Percent');
      ref
          .read(paymentCheckoutControllerProvider.notifier)
          .selectMode(PaymentMode.cash, total);
      syncControllers();
      maybeRunInitialAction();
    });
  }

  @override
  void dispose() {
    cashController.dispose();
    upiController.dispose();
    receivedController.dispose();
    paymentPageDiscountController.dispose();
    paymentPagePackingController.dispose();
    customerNameController.dispose();
    customerPhoneController.dispose();
    customerEmailController.dispose();
    customerAddressController.dispose();
    super.dispose();
  }

  void maybeRunInitialAction() {
    if (initialActionHandled) return;
    final action = widget.initialAction?.trim().toLowerCase();
    if (action == null || action.isEmpty) return;
    initialActionHandled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;
      switch (action) {
        case 'save':
          await openSaveInvoiceFlow();
        case 'share':
          await openShareInvoiceFlow();
        case 'print':
          await openPrintBillFlow();
      }
    });
  }

  void persistCustomer() {
    ref
        .read(billingSessionProvider.notifier)
        .updateCustomer(
          name: customerNameController.text,
          phone: customerPhoneController.text,
          email: customerEmailController.text,
          address: customerAddressController.text,
        );
  }

  Future<void> confirmClearCart() async {
    final strings = AppStrings.of(ref);
    final confirm = await showAppConfirmBottomSheet(
      context: context,
      title: strings.clearCart,
      message: strings.clearCartConfirm,
      confirmLabel: strings.clearCart,
      cancelLabel: strings.cancel,
      confirmVariant: AppButtonVariant.danger,
      icon: Icons.remove_shopping_cart_outlined,
    );
    if (confirm != true || !mounted) return;
    await ref.read(posCartControllerProvider.notifier).clear();
    ref.read(paymentCheckoutControllerProvider.notifier).reset();
    if (!mounted) return;
    final session = ref.read(billingSessionProvider);
    context.go(session.billingRoute);
  }

  void syncControllers() {
    final state = ref.read(paymentCheckoutControllerProvider);
    cashController.text = amountInputText(state.cashAmount);
    upiController.text = amountInputText(state.upiAmount);
    receivedController.text = amountInputText(state.receivedAmount);
  }

  double paymentPagePayable(
    CartSummary summary,
    PaymentCheckoutState checkout,
  ) => checkoutPayable(summary, checkout);

  /* Validate tender via payment mode sheet (Cash / UPI / Split). */
  Future<bool> confirmPaymentMode() => promptPaymentMode(context, ref);

  Future<bool> ensureBillPrinterReady() async {
    if (AppPlatform.isWeb) return true;

    var settings = ref.read(printerSettingsProvider);
    final hub = BluetoothPrinterHub.instance;
    final usbHub = EscPosTransportHub.instance;
    hub.updateSavedAddresses(
      billMac: settings.billBluetoothAddress,
      kotMac: settings.kotBluetoothAddress,
    );

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
      if (picked == null || !mounted) return false;
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

  Future<void> openPrintBillFlow() async {
    if (!await confirmPaymentMode()) return;
    if (!mounted) return;

    final printerReady = await ensureBillPrinterReady();
    if (!mounted) return;
    if (!printerReady) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Connect a bill printer to print'),
          backgroundColor: AppColors.danger,
        ),
      );
      return;
    }

    await complete(
      printAfterSave: true,
      requirePrintSuccess: true,
      preferShare: false,
    );
  }

  /* Save Invoice: payment mode dialog → Continue → save only. */
  Future<void> openSaveInvoiceFlow() async {
    if (!await confirmPaymentMode()) return;
    if (!mounted) return;
    await complete(printAfterSave: false, preferShare: false);
  }

  /* Share Invoice: payment mode dialog → Continue → save then share. */
  Future<void> openShareInvoiceFlow() async {
    if (!await confirmPaymentMode()) return;
    if (!mounted) return;
    await complete(printAfterSave: false, preferShare: true);
  }

  Future<void> complete({
    bool printAfterSave = false,
    bool preferShare = false,
    bool requirePrintSuccess = false,
  }) async {
    final strings = AppStrings.of(ref);
    if (!ref.read(permissionControllerProvider).allows('bill.create')) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(strings.moduleLocked)));
      return;
    }
    persistCustomer();
    final summary = ref.read(cartSummaryProvider);
    final result = await ref
        .read(paymentCheckoutControllerProvider.notifier)
        .completePayment(
          subtotal: summary.subtotal,
          taxTotal: summary.taxTotal,
        );
    if (!mounted || result == null) return;

    final session = ref.read(billingSessionProvider);

    if (requirePrintSuccess || printAfterSave) {
      final printResult = await printInvoiceById(
        ref,
        result.invoiceId,
        preferShare: preferShare,
      );
      if (!mounted) return;
      final printedOk =
          printResult.outcome != PrintOutcome.failed &&
          printResult.outcome != PrintOutcome.previewOnly;
      if (requirePrintSuccess && !printedOk) {
        /* Keep local bill (Android parity) — reprint from Invoice List. */
        if (!mounted) return;
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
        if (!mounted) return;
        unawaited(
          _uploadBillInBackground(
            ProviderScope.containerOf(context),
            result.invoiceId,
          ),
        );
        context.go(session.billingRoute);
        return;
      }
      if (!requirePrintSuccess && mounted) {
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
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(printResult.message ?? 'Share done')),
      );
    }

    /* Snapshot container while still mounted — navigate away right after. */
    final container = ProviderScope.containerOf(context);

    /* Payment display must never block save/print. */
    unawaited(tryAutoShowPaymentDisplayAfterBill(container, result));

    /* Web still awaits cloud confirm; mobile returns to billing immediately. */
    if (AppPlatform.requiresNetwork) {
      final online = await isDeviceOnline();
      if (!online) {
        if (!mounted) return;
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
      if (!mounted) return;
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

    if (!mounted) return;
    ref.read(paymentCheckoutControllerProvider.notifier).reset();
    /* Return to Fast Billing / Takeaway / Table billing for the next order. */
    context.go(session.billingRoute);
  }

  /* ProviderContainer outlives this State after context.go. */
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

  @override
  Widget build(BuildContext context) {
    final summary = ref.watch(cartSummaryProvider);
    final cartAsync = ref.watch(cartItemsProvider);
    final checkout = ref.watch(paymentCheckoutControllerProvider);
    final session = ref.watch(billingSessionProvider);
    final printerSettings = ref.watch(printerSettingsProvider);
    final showCustomer =
        printerSettings.customerUse ||
        session.invoiceType == 'take_away' ||
        (session.customerName?.trim().isNotEmpty ?? false) ||
        (session.customerPhone?.trim().isNotEmpty ?? false) ||
        (session.customerEmail?.trim().isNotEmpty ?? false) ||
        (session.customerAddress?.trim().isNotEmpty ?? false);
    final strings = AppStrings.of(ref);
    final payable = paymentPagePayable(summary, checkout);

    ref.listen(paymentCheckoutControllerProvider, (prev, next) {
      if (next.errorMessage != null &&
          next.errorMessage != prev?.errorMessage) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(next.errorMessage!)));
      }
    });

    if (summary.isEmpty && checkout.result == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Payment')),
        body: Center(child: Text(AppStrings.of(ref).cartEmptyPay)),
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final contentClass = AppBreakpoints.ofWidth(constraints.maxWidth);
        final orient = context.orientationClass;
        final showSideCheckout = AppBreakpoints.isPosSideCart(
          contentClass,
          height: context.heightClass,
          orientation: orient,
          availableWidth: constraints.maxWidth,
        );
        final persistentCheckout = AppBreakpoints.isPosPersistentCart(
          contentClass,
          height: context.heightClass,
          orientation: orient,
          availableWidth: constraints.maxWidth,
        );
        final usePhoneFooter = !showSideCheckout && !persistentCheckout;

        return Scaffold(
          backgroundColor: Colors.transparent,
          appBar: AppBar(
            backgroundColor: AppColors.primary,
            foregroundColor: Colors.white,
            elevation: 0,
            leading: Padding(
              padding: const EdgeInsets.only(left: 8),
              child: Center(
                child: Material(
                  color: Colors.white.withValues(alpha: 0.2),
                  shape: const CircleBorder(),
                  child: InkWell(
                    customBorder: const CircleBorder(),
                    onTap: () {
                      if (context.canPop()) {
                        context.pop();
                      } else {
                        context.go(session.billingRoute);
                      }
                    },
                    child: const SizedBox(
                      width: 40,
                      height: 40,
                      child: Icon(
                        Icons.arrow_back_rounded,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
              ),
            ),
            titleSpacing: 8,
            title: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Payment',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w800,
                    fontSize: 20,
                  ),
                ),
                if (printerSettings.printFastBill)
                  const BillingDateBar(inAppBar: true)
                else
                  const Text(
                    'Review your order',
                    style: TextStyle(
                      color: Color(0xFFB8D4FF),
                      fontWeight: FontWeight.w500,
                      fontSize: 12.5,
                    ),
                  ),
              ],
            ),
            actions: [
              if (checkout.result == null) ...[
                Padding(
                  padding: const EdgeInsets.only(right: 4),
                  child: TextButton.icon(
                    onPressed: confirmClearCart,
                    icon: const Icon(
                      Icons.delete_outline_rounded,
                      color: AppColors.danger,
                      size: 18,
                    ),
                    label: Text(
                      strings.clearCart,
                      style: const TextStyle(
                        color: AppColors.danger,
                        fontWeight: FontWeight.w700,
                        fontSize: 13,
                      ),
                    ),
                    style: TextButton.styleFrom(
                      backgroundColor: Colors.white,
                      foregroundColor: AppColors.danger,
                      padding: const EdgeInsets.symmetric(horizontal: 10),
                      visualDensity: VisualDensity.compact,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                  ),
                ),
                IconButton(
                  icon: const Icon(
                    Icons.more_vert_rounded,
                    color: Colors.white,
                  ),
                  onPressed: () async {
                    final value = await showAppActionSheet(
                      context: context,
                      title: 'Invoice actions',
                      actions: const [
                        AppSheetAction(
                          value: 'save',
                          label: 'Save Invoice',
                          icon: Icons.save_outlined,
                        ),
                        AppSheetAction(
                          value: 'share',
                          label: 'Share Invoice',
                          icon: Icons.share_outlined,
                        ),
                      ],
                    );
                    if (value == 'save') {
                      await openSaveInvoiceFlow();
                    } else if (value == 'share') {
                      await openShareInvoiceFlow();
                    }
                  },
                ),
              ],
              if (session.tableNumber != null)
                Padding(
                  padding: const EdgeInsets.only(right: 12),
                  child: Center(
                    child: Text(
                      'T${session.tableNumber}',
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
            ],
          ),
          body: SafeArea(
            bottom: false,
            child: Builder(
              builder: (context) {
                final invoice = buildInvoicePane(
                  showCustomer: showCustomer,
                  strings: strings,
                  cartAsync: cartAsync,
                );
                final checkoutPanel = buildCheckoutPanel(
                  summary: summary,
                  checkout: checkout,
                  payable: payable,
                  sidePanel: showSideCheckout,
                );

                if (showSideCheckout) {
                  return Row(
                    children: [
                      Expanded(
                        flex: AppBreakpoints.posCatalogFlex,
                        child: invoice,
                      ),
                      Expanded(
                        flex: AppBreakpoints.posCartFlex,
                        child: checkoutPanel,
                      ),
                    ],
                  );
                }

                if (persistentCheckout) {
                  final invoiceFlex = context.isShortHeight ? 12 : 11;
                  final checkoutFlex = context.isShortHeight ? 8 : 9;
                  return Column(
                    children: [
                      Expanded(flex: invoiceFlex, child: invoice),
                      Expanded(flex: checkoutFlex, child: checkoutPanel),
                    ],
                  );
                }

                return invoice;
              },
            ),
          ),
          bottomNavigationBar: usePhoneFooter
              ? buildPhoneCheckoutBar(
                  summary: summary,
                  checkout: checkout,
                  payable: payable,
                )
              : null,
        );
      },
    );
  }

  Widget buildInvoicePane({
    required bool showCustomer,
    required AppStrings strings,
    required AsyncValue<List<CartItem>> cartAsync,
  }) {
    final pad = AppBreakpoints.pagePaddingFor(context.widthClass);
    return ListView(
      padding: EdgeInsets.fromLTRB(
        pad,
        context.isShortHeight ? 6 : 12,
        pad,
        24,
      ),
      children: [
        if (showCustomer) ...[
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  strings.customer,
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
                const SizedBox(height: 12),
                ResponsiveFormColumns(
                  maxColumns: 2,
                  children: [
                    AppTextField(
                      controller: customerNameController,
                      label: strings.customerName,
                      textCapitalization: TextCapitalization.words,
                      onChanged: (_) => persistCustomer(),
                    ),
                    AppTextField(
                      controller: customerPhoneController,
                      label: strings.customerMobile,
                      keyboardType: TextInputType.phone,
                      onChanged: (_) => persistCustomer(),
                    ),
                    AppTextField(
                      controller: customerEmailController,
                      label: strings.customerEmail,
                      keyboardType: TextInputType.emailAddress,
                      onChanged: (_) => persistCustomer(),
                    ),
                    AppTextField(
                      controller: customerAddressController,
                      label: strings.customerAddress,
                      textCapitalization: TextCapitalization.sentences,
                      maxLines: 2,
                      minLines: 2,
                      onChanged: (_) => persistCustomer(),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
        ],
        LayoutBuilder(
          builder: (context, constraints) {
            final narrow = constraints.maxWidth < 320;
            return Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AppColors.border),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.navy.withValues(alpha: 0.04),
                    blurRadius: 10,
                    offset: const Offset(0, 3),
                  ),
                ],
              ),
              clipBehavior: Clip.antiAlias,
              child: Column(
                children: [
                  Container(
                    padding: EdgeInsets.symmetric(
                      horizontal: narrow ? 8 : 10,
                      vertical: context.isShortHeight ? 8 : 11,
                    ),
                    color: AppColors.primarySoft,
                    child: Row(
                      children: [
                        const SizedBox(
                          width: 24,
                          child: Text(
                            '#',
                            style: TextStyle(
                              color: AppColors.navy,
                              fontWeight: FontWeight.w800,
                              fontSize: 12.5,
                            ),
                          ),
                        ),
                        const Expanded(
                          flex: 5,
                          child: Text(
                            'Product Name',
                            style: TextStyle(
                              color: AppColors.navy,
                              fontWeight: FontWeight.w800,
                              fontSize: 12.5,
                            ),
                          ),
                        ),
                        Expanded(
                          flex: narrow ? 3 : 4,
                          child: const Text(
                            'Quantity',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: AppColors.navy,
                              fontWeight: FontWeight.w800,
                              fontSize: 12.5,
                            ),
                          ),
                        ),
                        if (!narrow)
                          const Expanded(
                            flex: 3,
                            child: Text(
                              'Unit Price',
                              textAlign: TextAlign.end,
                              style: TextStyle(
                                color: AppColors.navy,
                                fontWeight: FontWeight.w800,
                                fontSize: 12.5,
                              ),
                            ),
                          ),
                        const SizedBox(width: 36),
                      ],
                    ),
                  ),
                  cartAsync.when(
                    data: (items) {
                      if (items.isEmpty) {
                        return Padding(
                          padding: const EdgeInsets.all(24),
                          child: Text(strings.noItems),
                        );
                      }
                      return Column(
                        children: [
                          for (var i = 0; i < items.length; i++) ...[
                            InvoiceLineRow(
                              index: i + 1,
                              item: items[i],
                              currency: paymentPageCurrency,
                              showUnitPrice: !narrow,
                            ),
                            if (i < items.length - 1)
                              const Divider(
                                height: 1,
                                color: AppColors.border,
                              ),
                          ],
                        ],
                      );
                    },
                    loading: () => const Padding(
                      padding: EdgeInsets.all(16),
                      child: CircularProgressIndicator(),
                    ),
                    error: (e, _) => ListTile(title: Text('$e')),
                  ),
                ],
              ),
            );
          },
        ),
      ],
    );
  }

  /* Tablet / web: tender UI + Done & Print. */
  Widget buildCheckoutPanel({
    required CartSummary summary,
    required PaymentCheckoutState checkout,
    required double payable,
    required bool sidePanel,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        border: sidePanel
            ? Border(
                left: BorderSide(color: Colors.black.withValues(alpha: 0.08)),
              )
            : Border(
                top: BorderSide(color: Colors.black.withValues(alpha: 0.08)),
              ),
      ),
      child: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(12, 12, 12, 8),
              child: Column(
                children: [
                  PaymentTenderPanel(
                    summary: summary,
                    checkout: checkout,
                    payable: payable,
                    currency: paymentPageCurrency,
                    discountController: paymentPageDiscountController,
                    packingController: paymentPagePackingController,
                    receivedController: receivedController,
                    cashController: cashController,
                    upiController: upiController,
                    sideBySide: sidePanel && !context.isShortHeight,
                    onDiscountChanged: (value) {
                      final d = double.tryParse(value) ?? 0;
                      final n =
                          ref.read(paymentCheckoutControllerProvider.notifier);
                      n.setDiscount(
                        d,
                        type: checkout.discountType,
                        subtotal: summary.subtotal,
                      );
                      final clamped =
                          ref.read(paymentCheckoutControllerProvider).discount;
                      if ((clamped - d).abs() > 0.001) {
                        final text = clamped == clamped.roundToDouble()
                            ? clamped.toStringAsFixed(0)
                            : clamped.toStringAsFixed(2);
                        paymentPageDiscountController.value = TextEditingValue(
                          text: text,
                          selection: TextSelection.collapsed(offset: text.length),
                        );
                      }
                      n.selectMode(
                        checkout.mode,
                        paymentPagePayable(
                          summary,
                          ref.read(paymentCheckoutControllerProvider),
                        ),
                      );
                      syncControllers();
                    },
                    onDiscountTypeChanged: (type) {
                      final d =
                          double.tryParse(paymentPageDiscountController.text) ??
                              0;
                      final n =
                          ref.read(paymentCheckoutControllerProvider.notifier);
                      n.setDiscount(d, type: type, subtotal: summary.subtotal);
                      n.selectMode(
                        checkout.mode,
                        paymentPagePayable(
                          summary,
                          ref.read(paymentCheckoutControllerProvider),
                        ),
                      );
                      syncControllers();
                      setState(() {});
                    },
                    onPackingChanged: (value) {
                      final p = double.tryParse(value) ?? 0;
                      final n =
                          ref.read(paymentCheckoutControllerProvider.notifier);
                      n.setPacking(p, type: 'Amount');
                      n.selectMode(
                        checkout.mode,
                        paymentPagePayable(
                          summary,
                          ref.read(paymentCheckoutControllerProvider),
                        ),
                      );
                      syncControllers();
                    },
                    onSyncControllers: syncControllers,
                  ),
                  const SizedBox(height: 12),
                  PaymentBalanceBar(
                    checkout: checkout,
                    payable: payable,
                    currency: paymentPageCurrency,
                  ),
                ],
              ),
            ),
          ),
          PaymentDoneBar(
            busy: checkout.busy,
            enabled: !summary.isEmpty,
            onCancel: () {
              if (context.canPop()) {
                context.pop();
              } else {
                context.go(ref.read(billingSessionProvider).billingRoute);
              }
            },
            onDonePrint: openPrintBillFlow,
            onSave: openSaveInvoiceFlow,
            onShare: openShareInvoiceFlow,
          ),
        ],
      ),
    );
  }

  /* Phone portrait: tender + sticky Done & Print. */
  Widget buildPhoneCheckoutBar({
    required CartSummary summary,
    required PaymentCheckoutState checkout,
    required double payable,
  }) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        ConstrainedBox(
          constraints: BoxConstraints(
            maxHeight: MediaQuery.sizeOf(context).height *
                (context.isShortHeight ? 0.48 : 0.58),
          ),
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
            child: Column(
              children: [
                PaymentTenderPanel(
                  summary: summary,
                  checkout: checkout,
                  payable: payable,
                  currency: paymentPageCurrency,
                  discountController: paymentPageDiscountController,
                  packingController: paymentPagePackingController,
                  receivedController: receivedController,
                  cashController: cashController,
                  upiController: upiController,
                  onDiscountChanged: (value) {
                    final d = double.tryParse(value) ?? 0;
                    final n =
                        ref.read(paymentCheckoutControllerProvider.notifier);
                    n.setDiscount(
                      d,
                      type: checkout.discountType,
                      subtotal: summary.subtotal,
                    );
                    n.selectMode(
                      checkout.mode,
                      paymentPagePayable(
                        summary,
                        ref.read(paymentCheckoutControllerProvider),
                      ),
                    );
                    syncControllers();
                  },
                  onDiscountTypeChanged: (type) {
                    final d =
                        double.tryParse(paymentPageDiscountController.text) ??
                            0;
                    final n =
                        ref.read(paymentCheckoutControllerProvider.notifier);
                    n.setDiscount(d, type: type, subtotal: summary.subtotal);
                    n.selectMode(
                      checkout.mode,
                      paymentPagePayable(
                        summary,
                        ref.read(paymentCheckoutControllerProvider),
                      ),
                    );
                    syncControllers();
                    setState(() {});
                  },
                  onPackingChanged: (value) {
                    final p = double.tryParse(value) ?? 0;
                    final n =
                        ref.read(paymentCheckoutControllerProvider.notifier);
                    n.setPacking(p, type: 'Amount');
                    n.selectMode(
                      checkout.mode,
                      paymentPagePayable(
                        summary,
                        ref.read(paymentCheckoutControllerProvider),
                      ),
                    );
                    syncControllers();
                  },
                  onSyncControllers: syncControllers,
                ),
                const SizedBox(height: 10),
                PaymentBalanceBar(
                  checkout: checkout,
                  payable: payable,
                  currency: paymentPageCurrency,
                ),
              ],
            ),
          ),
        ),
        Material(
          color: Colors.white,
          elevation: 10,
          child: SafeArea(
            top: false,
            child: PaymentDoneBar(
              busy: checkout.busy,
              enabled: !summary.isEmpty,
              onCancel: () {
                if (context.canPop()) {
                  context.pop();
                } else {
                  context.go(ref.read(billingSessionProvider).billingRoute);
                }
              },
              onDonePrint: openPrintBillFlow,
              onSave: openSaveInvoiceFlow,
              onShare: openShareInvoiceFlow,
            ),
          ),
        ),
      ],
    );
  }
}

class InvoiceLineRow extends ConsumerWidget {
  const InvoiceLineRow({
    super.key,
    required this.index,
    required this.item,
    required this.currency,
    this.showUnitPrice = true,
  });

  final int index;
  final CartItem item;
  final NumberFormat currency;
  final bool showUnitPrice;

  String get qtyLabel {
    if (item.quantity == item.quantity.roundToDouble()) {
      return '${item.quantity.round()}';
    }
    return item.quantity.toStringAsFixed(2);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cart = ref.read(posCartControllerProvider.notifier);

    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 8, 4, 8),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final compact = constraints.maxWidth < 340;
          final qtyControls = SizedBox(
            width: compact ? 96 : 108,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                CircleQtyButton(
                  icon: Icons.remove,
                  onTap: () => cart.decrement(item),
                ),
                Expanded(
                  child: InkWell(
                    onTap: () => editCartLineDialog(context, ref, item),
                    child: Text(
                      qtyLabel,
                      textAlign: TextAlign.center,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 14,
                        color: AppColors.navy,
                      ),
                    ),
                  ),
                ),
                CircleQtyButton(
                  icon: Icons.add,
                  onTap: () => cart.increment(item),
                ),
              ],
            ),
          );

          return Row(
            children: [
              SizedBox(
                width: compact ? 18 : 24,
                child: Text(
                  '$index',
                  style: TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: compact ? 12 : 13,
                    color: AppColors.navy,
                  ),
                ),
              ),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.productName,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: compact ? 12.5 : 13.5,
                        color: AppColors.navy,
                      ),
                    ),
                    if (!showUnitPrice) ...[
                      const SizedBox(height: 2),
                      Text(
                        currency.format(item.unitPrice),
                        style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              qtyControls,
              if (showUnitPrice)
                SizedBox(
                  width: 72,
                  child: InkWell(
                    onTap: () => editCartLineDialog(context, ref, item),
                    child: Text(
                      currency.format(item.unitPrice),
                      textAlign: TextAlign.end,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w700,
                        color: AppColors.navy,
                      ),
                    ),
                  ),
                ),
              IconButton(
                tooltip: 'Remove item',
                visualDensity: VisualDensity.compact,
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(minWidth: 36, minHeight: 36),
                onPressed: () => cart.remove(item),
                icon: const Icon(
                  Icons.delete_outline_rounded,
                  color: AppColors.danger,
                  size: 22,
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class CircleQtyButton extends StatelessWidget {
  const CircleQtyButton({
    super.key,
    required this.icon,
    required this.onTap,
    this.filled = false,
  });

  final IconData icon;
  final VoidCallback onTap;
  /* Kept for call-site compatibility; − and + use the same style. */
  final bool filled;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: const Color(0xFF9BBDE8),
      shape: const CircleBorder(),
      child: InkWell(
        customBorder: const CircleBorder(),
        onTap: onTap,
        child: SizedBox(
          width: 30,
          height: 30,
          child: Icon(
            icon,
            size: 18,
            color: AppColors.primaryDark,
          ),
        ),
      ),
    );
  }
}
