import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_checkout_controller.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/portion_picker.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_providers.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

class PaymentPage extends ConsumerStatefulWidget {
  const PaymentPage({super.key});

  @override
  ConsumerState<PaymentPage> createState() => PaymentPageState();
}

class PaymentPageState extends ConsumerState<PaymentPage> {
  final cashController = TextEditingController();
  final upiController = TextEditingController();
  final paymentPageDiscountController = TextEditingController(text: '0');
  final paymentPagePackingController = TextEditingController(text: '0');
  final customerNameController = TextEditingController();
  final customerPhoneController = TextEditingController();
  final customerEmailController = TextEditingController();
  final customerAddressController = TextEditingController();
  late final NumberFormat paymentPageCurrency;

  @override
  void initState() {
    super.initState();
    paymentPageCurrency = NumberFormat.currency(locale: 'en_IN', symbol: '₹ ');
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final session = ref.read(billingSessionProvider);
      customerNameController.text = session.customerName ?? '';
      customerPhoneController.text = session.customerPhone ?? '';
      customerEmailController.text = session.customerEmail ?? '';
      customerAddressController.text = session.customerAddress ?? '';
      final summary = ref.read(cartSummaryProvider);
      final total = ref.read(paymentCheckoutControllerProvider).payableTotal(
            subtotal: summary.subtotal,
            taxTotal: summary.taxTotal,
          );
      ref
          .read(paymentCheckoutControllerProvider.notifier)
          .setDiscount(0, type: 'Percent');
      ref
          .read(paymentCheckoutControllerProvider.notifier)
          .selectMode(PaymentMode.cash, total);
      syncControllers();
    });
  }

  @override
  void dispose() {
    cashController.dispose();
    upiController.dispose();
    paymentPageDiscountController.dispose();
    paymentPagePackingController.dispose();
    customerNameController.dispose();
    customerPhoneController.dispose();
    customerEmailController.dispose();
    customerAddressController.dispose();
    super.dispose();
  }

  void persistCustomer() {
    ref.read(billingSessionProvider.notifier).updateCustomer(
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
    cashController.text = state.cashAmount.toStringAsFixed(2);
    upiController.text = state.upiAmount.toStringAsFixed(2);
  }

  double paymentPagePayable(CartSummary summary, PaymentCheckoutState checkout) =>
      checkout.payableTotal(
        subtotal: summary.subtotal,
        taxTotal: summary.taxTotal,
      );

  Future<void> openPaymentModeDialog() async {
    final summary = ref.read(cartSummaryProvider);
    final checkout = ref.read(paymentCheckoutControllerProvider);
    final total = paymentPagePayable(summary, checkout);
    if (summary.isEmpty) return;

    final confirmed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return PaymentModeSheet(
          totalAmount: total,
          currency: paymentPageCurrency,
          initialMode: checkout.mode,
          initialCash: checkout.cashAmount,
          initialUpi: checkout.upiAmount,
          onContinue: (mode, cash, upi) {
            final n = ref.read(paymentCheckoutControllerProvider.notifier);
            n.selectMode(mode, total);
            if (mode == PaymentMode.cashPlusUpi) {
              n.setCashAmount(cash, total);
              // Keep explicit UPI if setCashAmount overwrote it incorrectly.
              if ((cash + upi - total).abs() > 0.05) {
                n.setUpiAmount(upi, total);
              }
            }
            Navigator.pop(sheetContext, true);
          },
        );
      },
    );

    if (confirmed == true && mounted) {
      await complete();
    }
  }

  Future<void> complete({
    bool printAfterSave = true,
    bool preferShare = false,
  }) async {
    final strings = AppStrings.of(ref);
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
    // Online (all platforms): dual-write — local already saved, await API upload.
    // Offline mobile: keep local pending; ConnectivitySyncListener uploads later.
    final online = await isDeviceOnline();
    if (AppPlatform.requiresNetwork || online) {
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
                  : AppPlatform.requiresNetwork
                      ? 'Bill saved locally but cloud upload failed. Check internet and retry.'
                      : 'Bill saved on device — cloud upload failed; will retry when online.',
            ),
            backgroundColor: AppPlatform.requiresNetwork
                ? AppColors.danger
                : AppColors.orange,
          ),
        );
      }
    }

    final autoPrint = ref.read(printerSettingsProvider).autoShareOnSave;
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: Text(strings.billSaved),
        content: Text(
          'Invoice: ${result.invoiceNumber}\n'
          'Payment: ${result.paymentMode}\n'
          'Amount: ${paymentPageCurrency.format(result.totalAmount)}'
          '${session.tableNumber != null ? '\nTable: ${session.tableNumber}' : ''}'
          '${session.customerName != null ? '\nCustomer: ${session.customerName}' : ''}',
        ),
        actions: [
          TextButton(
            onPressed: () async {
              final printResult =
                  await printInvoiceById(ref, result.invoiceId);
              if (!context.mounted) return;
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(printResult.message ?? 'Print done'),
                ),
              );
            },
            child: Text(strings.printShare),
          ),
          AppButton(
            label: strings.addProducts,
            onPressed: () {
              Navigator.of(context).pop();
              ref.read(paymentCheckoutControllerProvider.notifier).reset();
              context.go(session.billingRoute);
            },
            expanded: false,
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              ref.read(paymentCheckoutControllerProvider.notifier).reset();
              context.go('/');
            },
            child: Text(strings.home),
          ),
        ],
      ),
    );

    if (autoPrint && printAfterSave && !preferShare && mounted) {
      final printResult = await printInvoiceById(ref, result.invoiceId);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(printResult.message ?? 'Print done')),
      );
    } else if (preferShare && mounted) {
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
  }

  @override
  Widget build(BuildContext context) {
    final summary = ref.watch(cartSummaryProvider);
    final cartAsync = ref.watch(cartItemsProvider);
    final checkout = ref.watch(paymentCheckoutControllerProvider);
    final session = ref.watch(billingSessionProvider);
    final printerSettings = ref.watch(printerSettingsProvider);
    final showCustomer = printerSettings.customerUse ||
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
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
      }
    });

    if (summary.isEmpty && checkout.result == null) {
      return Scaffold(
        appBar: AppBar(
          title: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(strings.invoicePreview),
              Text(
                strings.reviewOrder,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w400,
                  color: Colors.white70,
                ),
              ),
            ],
          ),
        ),
        body: Center(
          child: Text(AppStrings.of(ref).cartEmptyPay),
        ),
      );
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF7F9FC),
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(strings.invoicePreview),
            Text(
              strings.reviewOrder,
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w400,
                color: Colors.white70,
              ),
            ),
          ],
        ),
        actions: [
          if (checkout.result == null)
            PopupMenuButton<String>(
              onSelected: (value) async {
                if (value == 'save') {
                  await complete(printAfterSave: false);
                } else if (value == 'share') {
                  await complete(preferShare: true);
                }
              },
              itemBuilder: (context) => [
                PopupMenuItem(
                  value: 'save',
                  child: Text(strings.saveWithoutPrint),
                ),
                PopupMenuItem(
                  value: 'share',
                  child: Text(strings.saveAndShare),
                ),
              ],
            ),
          if (checkout.result == null)
            Padding(
              padding: const EdgeInsets.only(right: 4),
              child: TextButton(
                onPressed: confirmClearCart,
                style: TextButton.styleFrom(
                  side: const BorderSide(color: Colors.white70),
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  visualDensity: VisualDensity.compact,
                ),
                child: Text(
                  strings.clearCart,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
          if (session.tableNumber != null)
            Padding(
              padding: const EdgeInsets.only(right: 12),
              child: Center(
                child: Text(
                  'T${session.tableNumber}',
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: BoxConstraints(
              maxWidth: AppBreakpoints.contentMaxWidthFor(
                context.widthClass.index >= AppWidthClass.expanded.index
                    ? AppWidthClass.expanded
                    : context.widthClass,
              ),
            ),
            child: Column(
              children: [
                Expanded(
                  child: ListView(
                    padding: EdgeInsets.fromLTRB(
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      12,
                      AppBreakpoints.pagePaddingFor(context.widthClass),
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
                                style: const TextStyle(
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              const SizedBox(height: 12),
                              AppTextField(
                                controller: customerNameController,
                                label: strings.customerName,
                                textCapitalization: TextCapitalization.words,
                                onChanged: (_) => persistCustomer(),
                              ),
                              const SizedBox(height: 12),
                              AppTextField(
                                controller: customerPhoneController,
                                label: strings.customerMobile,
                                keyboardType: TextInputType.phone,
                                onChanged: (_) => persistCustomer(),
                              ),
                              const SizedBox(height: 12),
                              AppTextField(
                                controller: customerEmailController,
                                label: strings.customerEmail,
                                keyboardType: TextInputType.emailAddress,
                                onChanged: (_) => persistCustomer(),
                              ),
                              const SizedBox(height: 12),
                              AppTextField(
                                controller: customerAddressController,
                                label: strings.customerAddress,
                                textCapitalization:
                                    TextCapitalization.sentences,
                                maxLines: 2,
                                minLines: 2,
                                onChanged: (_) => persistCustomer(),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),
                      ],
                      Container(
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: const Color(0xFFE2E8F2)),
                        ),
                        child: Column(
                          children: [
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 12,
                                vertical: 10,
                              ),
                              decoration: const BoxDecoration(
                                color: AppColors.primaryDark,
                                borderRadius: BorderRadius.vertical(
                                  top: Radius.circular(12),
                                ),
                              ),
                              child: const Row(
                                children: [
                                  Expanded(
                                    flex: 5,
                                    child: Text(
                                      'Product Name',
                                      style: TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.w700,
                                        fontSize: 13,
                                      ),
                                    ),
                                  ),
                                  Expanded(
                                    flex: 4,
                                    child: Text(
                                      'Quantity',
                                      textAlign: TextAlign.center,
                                      style: TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.w700,
                                        fontSize: 13,
                                      ),
                                    ),
                                  ),
                                  Expanded(
                                    flex: 3,
                                    child: Text(
                                      'Unit Price',
                                      textAlign: TextAlign.end,
                                      style: TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.w700,
                                        fontSize: 13,
                                      ),
                                    ),
                                  ),
                                  SizedBox(width: 36),
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
                                        item: items[i],
                                        currency: paymentPageCurrency,
                                      ),
                                      if (i < items.length - 1)
                                        const Divider(height: 1),
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
                      ),
                      const SizedBox(height: 14),
                      BillSummaryCard(
                        summary: summary,
                        checkout: checkout,
                        currency: paymentPageCurrency,
                        discountController: paymentPageDiscountController,
                        packingController: paymentPagePackingController,
                        payable: payable,
                        onDiscountChanged: (value) {
                          final d = double.tryParse(value) ?? 0;
                          final n = ref.read(
                            paymentCheckoutControllerProvider.notifier,
                          );
                          n.setDiscount(d, type: 'Percent');
                          n.selectMode(
                            checkout.mode,
                            paymentPagePayable(
                              summary,
                              ref.read(paymentCheckoutControllerProvider),
                            ),
                          );
                          syncControllers();
                        },
                        onPackingChanged: (value) {
                          final p = double.tryParse(value) ?? 0;
                          final n = ref.read(
                            paymentCheckoutControllerProvider.notifier,
                          );
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
                      ),
                      const SizedBox(height: 14),
                      cartAsync.maybeWhen(
                        data: (items) {
                          if (items.isEmpty) {
                            return const SizedBox.shrink();
                          }
                          final preview = checkoutPreviewBill(
                            items: items,
                            summary: summary,
                            checkout: checkout,
                            session: session,
                            payable: payable,
                          );
                          final service = ref.read(printServiceProvider);
                          final shop = ref.read(shopReceiptProfileProvider);
                          final name = shop.companyName;
                          return Column(
                            children: [
                              ReceiptPreviewCard(
                                title: strings.paper2Inch,
                                text: service.billPreviewText(
                                  invoice: preview.invoice,
                                  items: preview.items,
                                  shopName: name,
                                  paperSize: PrinterPaperSize.inch2,
                                ),
                              ),
                              const SizedBox(height: 12),
                              ReceiptPreviewCard(
                                title: strings.paper3Inch,
                                text: service.billPreviewText(
                                  invoice: preview.invoice,
                                  items: preview.items,
                                  shopName: name,
                                  paperSize: PrinterPaperSize.inch3,
                                ),
                              ),
                            ],
                          );
                        },
                        orElse: () => const SizedBox.shrink(),
                      ),
                      const SizedBox(height: 14),
                      PaymentModeCard(
                        selected: checkout.mode,
                        busy: checkout.busy,
                        onSelected: (mode) {
                          ref
                              .read(paymentCheckoutControllerProvider.notifier)
                              .selectMode(mode, payable);
                          syncControllers();
                        },
                      ),
                      const SizedBox(height: 80),
                    ],
                  ),
                ),
                Material(
                  color: AppColors.primary,
                  elevation: 12,
                  child: SafeArea(
                    top: false,
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  'Payable Amount',
                                  style: TextStyle(
                                    color: Colors.white70,
                                    fontSize: 12,
                                  ),
                                ),
                                Text(
                                  paymentPageCurrency.format(payable),
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w900,
                                    fontSize: 22,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Material(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(14),
                            child: InkWell(
                              borderRadius: BorderRadius.circular(14),
                              onTap: summary.isEmpty || checkout.busy
                                  ? null
                                  : openPaymentModeDialog,
                              child: SizedBox(
                                width: 56,
                                height: 56,
                                child: checkout.busy
                                    ? const Padding(
                                        padding: EdgeInsets.all(14),
                                        child: CircularProgressIndicator(
                                          strokeWidth: 2.5,
                                        ),
                                      )
                                    : const Icon(
                                        Icons.print_rounded,
                                        color: AppColors.primary,
                                        size: 28,
                                      ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class InvoiceLineRow extends ConsumerWidget {
  const InvoiceLineRow({super.key, 
    required this.item,
    required this.currency,
  });

  final CartItem item;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(10, 10, 4, 10),
      child: Row(
        children: [
          Expanded(
            flex: 5,
            child: Text(
              item.productName,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontWeight: FontWeight.w600,
                fontSize: 14,
              ),
            ),
          ),
          Expanded(
            flex: 4,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                CircleQtyButton(
                  icon: Icons.remove,
                  onTap: () => ref
                      .read(posCartControllerProvider.notifier)
                      .decrement(item),
                ),
                InkWell(
                  onTap: () => editCartLineDialog(context, ref, item),
                  child: SizedBox(
                    width: 28,
                    child: Text(
                      '${item.quantity}',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                      ),
                    ),
                  ),
                ),
                CircleQtyButton(
                  icon: Icons.add,
                  onTap: () => ref
                      .read(posCartControllerProvider.notifier)
                      .increment(item),
                ),
              ],
            ),
          ),
          Expanded(
            flex: 3,
            child: InkWell(
              onTap: () => editCartLineDialog(context, ref, item),
              child: Text(
                currency.format(item.unitPrice),
                textAlign: TextAlign.end,
                style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
              ),
            ),
          ),
          IconButton(
            tooltip: 'Remove item',
            onPressed: () =>
                ref.read(posCartControllerProvider.notifier).remove(item),
            icon: const Icon(Icons.delete, color: AppColors.danger, size: 22),
          ),
        ],
      ),
    );
  }
}

class CircleQtyButton extends StatelessWidget {
  const CircleQtyButton({super.key, required this.icon, required this.onTap});

  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.primaryLight,
      shape: const CircleBorder(),
      child: InkWell(
        customBorder: const CircleBorder(),
        onTap: onTap,
        child: SizedBox(
          width: 28,
          height: 28,
          child: Icon(icon, size: 16, color: AppColors.primary),
        ),
      ),
    );
  }
}

class BillSummaryCard extends StatelessWidget {
  const BillSummaryCard({super.key, 
    required this.summary,
    required this.checkout,
    required this.currency,
    required this.discountController,
    required this.packingController,
    required this.payable,
    required this.onDiscountChanged,
    required this.onPackingChanged,
  });

  final CartSummary summary;
  final PaymentCheckoutState checkout;
  final NumberFormat currency;
  final TextEditingController discountController;
  final TextEditingController packingController;
  final double payable;
  final ValueChanged<String> onDiscountChanged;
  final ValueChanged<String> onPackingChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFE2E8F2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Bill Summary',
            style: TextStyle(
              color: AppColors.primary,
              fontWeight: FontWeight.w800,
              fontSize: 15,
            ),
          ),
          const SizedBox(height: 12),
          SummaryField(
            label: 'SUBTOTAL',
            child: ValueBox(currency.format(summary.subtotal)),
          ),
          const SizedBox(height: 10),
          SummaryField(
            label: 'DISCOUNT (%)',
            child: EditableValueBox(
              controller: discountController,
              suffix: '%',
              enabled: !checkout.busy,
              onChanged: onDiscountChanged,
            ),
          ),
          const SizedBox(height: 10),
          SummaryField(
            label: 'Packing Charges',
            child: EditableValueBox(
              controller: packingController,
              prefix: '₹ ',
              enabled: !checkout.busy,
              onChanged: onPackingChanged,
            ),
          ),
          const Divider(height: 22),
          Row(
            children: [
              const Text(
                'TOTAL AMOUNT',
                style: TextStyle(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w900,
                  fontSize: 15,
                ),
              ),
              const Spacer(),
              Text(
                currency.format(payable),
                style: const TextStyle(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w900,
                  fontSize: 18,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class SummaryField extends StatelessWidget {
  const SummaryField({super.key, required this.label, required this.child});

  final String label;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Text(
            label,
            style: const TextStyle(
              fontWeight: FontWeight.w700,
              fontSize: 13,
              color: AppColors.textPrimary,
            ),
          ),
        ),
        SizedBox(width: 140, child: child),
      ],
    );
  }
}

class ValueBox extends StatelessWidget {
  const ValueBox(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFF1F4F8),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        text,
        textAlign: TextAlign.center,
        style: const TextStyle(fontWeight: FontWeight.w700),
      ),
    );
  }
}

class EditableValueBox extends StatelessWidget {
  const EditableValueBox({super.key, 
    required this.controller,
    required this.onChanged,
    this.prefix,
    this.suffix,
    this.enabled = true,
  });

  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final String? prefix;
  final String? suffix;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFF1F4F8),
        borderRadius: BorderRadius.circular(8),
      ),
      child: TextField(
        controller: controller,
        enabled: enabled,
        textAlign: TextAlign.center,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        inputFormatters: [
          FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
        ],
        decoration: InputDecoration(
          border: InputBorder.none,
          isDense: true,
          prefixText: prefix,
          suffixText: suffix,
        ),
        style: const TextStyle(fontWeight: FontWeight.w700),
        onChanged: onChanged,
      ),
    );
  }
}

class PaymentModeCard extends StatelessWidget {
  const PaymentModeCard({super.key, 
    required this.selected,
    required this.busy,
    required this.onSelected,
  });

  final PaymentMode selected;
  final bool busy;
  final ValueChanged<PaymentMode> onSelected;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFE2E8F2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Payment Mode',
            style: TextStyle(
              color: AppColors.primary,
              fontWeight: FontWeight.w800,
              fontSize: 15,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              for (final mode in PaymentMode.values) ...[
                Expanded(
                  child: PaymentModeChip(
                    mode: mode,
                    selected: selected == mode,
                    onTap: busy ? null : () => onSelected(mode),
                  ),
                ),
                if (mode != PaymentMode.values.last) const SizedBox(width: 8),
              ],
            ],
          ),
        ],
      ),
    );
  }
}

class PaymentModeChip extends StatelessWidget {
  const PaymentModeChip({super.key, 
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
      PaymentMode.cashPlusUpi => 'CASH + UPI',
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
                selected
                    ? Icons.radio_button_checked
                    : Icons.radio_button_off,
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
  const PaymentModeSheet({super.key, 
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
      text: widget.initialCash.toStringAsFixed(2),
    );
    upiController = TextEditingController(
      text: widget.initialUpi.toStringAsFixed(2),
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
      if (mode == PaymentMode.cash) {
        cashController.text = widget.totalAmount.toStringAsFixed(2);
        upiController.text = '0.00';
      } else if (mode == PaymentMode.upi) {
        cashController.text = '0.00';
        upiController.text = widget.totalAmount.toStringAsFixed(2);
      } else {
        cashController.text = '0.00';
        upiController.text = '0.00';
      }
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
              style: const TextStyle(
                fontWeight: FontWeight.w800,
                fontSize: 16,
              ),
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
                    child: TextField(
                      controller: cashController,
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                      ],
                      decoration: InputDecoration(
                        labelText: strings.cashAmount,
                      ),
                      onChanged: (_) => setState(() {}),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextField(
                      controller: upiController,
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                      ],
                      decoration: InputDecoration(
                        labelText: strings.upiAmount,
                      ),
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
                        : () => widget.onContinue(paymentPageMode, paymentPageCash, paymentPageUpi),
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

({Invoice invoice, List<InvoiceItem> items}) checkoutPreviewBill({
  required List<CartItem> items,
  required CartSummary summary,
  required PaymentCheckoutState checkout,
  required BillingSession session,
  required double payable,
}) {
  final now = DateTime.now();
  final invoice = Invoice(
    organizationId: '',
    branchId: '',
    deviceId: '',
    invoiceId: 0,
    invoiceNumber: 'PREVIEW',
    invoiceDate: now,
    invoiceType: session.invoiceType,
    subTotal: summary.subtotal,
    totalGstAmount: summary.taxTotal,
    discount: checkout.discount,
    discountType: checkout.discountType,
    packingCharge: checkout.packingCharge,
    packingChargeType: checkout.packingChargeType,
    totalAmount: payable,
    paymentMode: checkout.mode.label,
    cashAmount: checkout.cashAmount,
    upiAmount: checkout.upiAmount,
    invoiceOrderStatus: 'preview',
    invoiceNetworkStatus: 'preview',
    invoiceSyncStatus: '0',
    noOfTable: session.tableNumber ?? '',
    customerName: session.customerName,
    customerMobile: session.customerPhone,
    customerEmail: session.customerEmail,
    customerAddress: session.customerAddress,
    billPrintStatus: '',
    itemCount: items.length,
    createdAt: now,
  );
  final lines = <InvoiceItem>[
    for (var i = 0; i < items.length; i++)
      InvoiceItem(
        organizationId: '',
        branchId: '',
        deviceId: '',
        invoiceItemId: -i - 1,
        invoiceNumber: 'PREVIEW',
        productId: items[i].productId,
        productName: items[i].productName,
        productCode: items[i].productCode,
        productPrice: items[i].unitPrice,
        productQuantity: items[i].quantity,
        productCgst: items[i].productCgst,
        productSgst: items[i].productSgst,
        productUnit: items[i].productUnit,
        categoryName: items[i].categoryName,
        portionId: items[i].portionId == 0 ? null : items[i].portionId,
        portionName: items[i].portionName,
        invoiceItemType: 'product',
        productStatus: 'completed',
        invoiceItemSyncStatus: '0',
      ),
  ];
  return (invoice: invoice, items: lines);
}

class ReceiptPreviewCard extends StatelessWidget {
  const ReceiptPreviewCard({super.key, required this.title, required this.text});

  final String title;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFE2E8F2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
          const SizedBox(height: 8),
          SelectableText(
            text,
            style: const TextStyle(
              fontFamily: 'monospace',
              fontSize: 11,
              height: 1.35,
            ),
          ),
        ],
      ),
    );
  }
}

