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
  ConsumerState<PaymentPage> createState() => _PaymentPageState();
}

class _PaymentPageState extends ConsumerState<PaymentPage> {
  final _cashController = TextEditingController();
  final _upiController = TextEditingController();
  final _discountController = TextEditingController(text: '0');
  final _packingController = TextEditingController(text: '0');
  final _customerNameController = TextEditingController();
  final _customerPhoneController = TextEditingController();
  final _customerEmailController = TextEditingController();
  final _customerAddressController = TextEditingController();
  late final NumberFormat _currency;

  @override
  void initState() {
    super.initState();
    _currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹ ');
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final session = ref.read(billingSessionProvider);
      _customerNameController.text = session.customerName ?? '';
      _customerPhoneController.text = session.customerPhone ?? '';
      _customerEmailController.text = session.customerEmail ?? '';
      _customerAddressController.text = session.customerAddress ?? '';
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
      _syncControllers();
    });
  }

  @override
  void dispose() {
    _cashController.dispose();
    _upiController.dispose();
    _discountController.dispose();
    _packingController.dispose();
    _customerNameController.dispose();
    _customerPhoneController.dispose();
    _customerEmailController.dispose();
    _customerAddressController.dispose();
    super.dispose();
  }

  void _persistCustomer() {
    ref.read(billingSessionProvider.notifier).updateCustomer(
          name: _customerNameController.text,
          phone: _customerPhoneController.text,
          email: _customerEmailController.text,
          address: _customerAddressController.text,
        );
  }

  Future<void> _confirmClearCart() async {
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

  void _syncControllers() {
    final state = ref.read(paymentCheckoutControllerProvider);
    _cashController.text = state.cashAmount.toStringAsFixed(2);
    _upiController.text = state.upiAmount.toStringAsFixed(2);
  }

  double _payable(CartSummary summary, PaymentCheckoutState checkout) =>
      checkout.payableTotal(
        subtotal: summary.subtotal,
        taxTotal: summary.taxTotal,
      );

  Future<void> _openPaymentModeDialog() async {
    final summary = ref.read(cartSummaryProvider);
    final checkout = ref.read(paymentCheckoutControllerProvider);
    final total = _payable(summary, checkout);
    if (summary.isEmpty) return;

    final confirmed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return _PaymentModeSheet(
          totalAmount: total,
          currency: _currency,
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
      await _complete();
    }
  }

  Future<void> _complete({
    bool printAfterSave = true,
    bool preferShare = false,
  }) async {
    final strings = AppStrings.of(ref);
    _persistCustomer();
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
          'Amount: ${_currency.format(result.totalAmount)}'
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
    final payable = _payable(summary, checkout);

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
                  await _complete(printAfterSave: false);
                } else if (value == 'share') {
                  await _complete(preferShare: true);
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
                onPressed: _confirmClearCart,
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
                                controller: _customerNameController,
                                label: strings.customerName,
                                textCapitalization: TextCapitalization.words,
                                onChanged: (_) => _persistCustomer(),
                              ),
                              const SizedBox(height: 12),
                              AppTextField(
                                controller: _customerPhoneController,
                                label: strings.customerMobile,
                                keyboardType: TextInputType.phone,
                                onChanged: (_) => _persistCustomer(),
                              ),
                              const SizedBox(height: 12),
                              AppTextField(
                                controller: _customerEmailController,
                                label: strings.customerEmail,
                                keyboardType: TextInputType.emailAddress,
                                onChanged: (_) => _persistCustomer(),
                              ),
                              const SizedBox(height: 12),
                              AppTextField(
                                controller: _customerAddressController,
                                label: strings.customerAddress,
                                textCapitalization:
                                    TextCapitalization.sentences,
                                maxLines: 2,
                                minLines: 2,
                                onChanged: (_) => _persistCustomer(),
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
                                      _InvoiceLineRow(
                                        item: items[i],
                                        currency: _currency,
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
                      _BillSummaryCard(
                        summary: summary,
                        checkout: checkout,
                        currency: _currency,
                        discountController: _discountController,
                        packingController: _packingController,
                        payable: payable,
                        onDiscountChanged: (value) {
                          final d = double.tryParse(value) ?? 0;
                          final n = ref.read(
                            paymentCheckoutControllerProvider.notifier,
                          );
                          n.setDiscount(d, type: 'Percent');
                          n.selectMode(
                            checkout.mode,
                            _payable(
                              summary,
                              ref.read(paymentCheckoutControllerProvider),
                            ),
                          );
                          _syncControllers();
                        },
                        onPackingChanged: (value) {
                          final p = double.tryParse(value) ?? 0;
                          final n = ref.read(
                            paymentCheckoutControllerProvider.notifier,
                          );
                          n.setPacking(p, type: 'Amount');
                          n.selectMode(
                            checkout.mode,
                            _payable(
                              summary,
                              ref.read(paymentCheckoutControllerProvider),
                            ),
                          );
                          _syncControllers();
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
                              _ReceiptPreviewCard(
                                title: strings.paper2Inch,
                                text: service.billPreviewText(
                                  invoice: preview.invoice,
                                  items: preview.items,
                                  shopName: name,
                                  paperSize: PrinterPaperSize.inch2,
                                ),
                              ),
                              const SizedBox(height: 12),
                              _ReceiptPreviewCard(
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
                      _PaymentModeCard(
                        selected: checkout.mode,
                        busy: checkout.busy,
                        onSelected: (mode) {
                          ref
                              .read(paymentCheckoutControllerProvider.notifier)
                              .selectMode(mode, payable);
                          _syncControllers();
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
                                  _currency.format(payable),
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
                                  : _openPaymentModeDialog,
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

class _InvoiceLineRow extends ConsumerWidget {
  const _InvoiceLineRow({
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
                _CircleQtyButton(
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
                _CircleQtyButton(
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

class _CircleQtyButton extends StatelessWidget {
  const _CircleQtyButton({required this.icon, required this.onTap});

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

class _BillSummaryCard extends StatelessWidget {
  const _BillSummaryCard({
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
          _SummaryField(
            label: 'SUBTOTAL',
            child: _ValueBox(currency.format(summary.subtotal)),
          ),
          const SizedBox(height: 10),
          _SummaryField(
            label: 'DISCOUNT (%)',
            child: _EditableValueBox(
              controller: discountController,
              suffix: '%',
              enabled: !checkout.busy,
              onChanged: onDiscountChanged,
            ),
          ),
          const SizedBox(height: 10),
          _SummaryField(
            label: 'Packing Charges',
            child: _EditableValueBox(
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

class _SummaryField extends StatelessWidget {
  const _SummaryField({required this.label, required this.child});

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

class _ValueBox extends StatelessWidget {
  const _ValueBox(this.text);

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

class _EditableValueBox extends StatelessWidget {
  const _EditableValueBox({
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

class _PaymentModeCard extends StatelessWidget {
  const _PaymentModeCard({
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
                  child: _PaymentModeChip(
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

class _PaymentModeChip extends StatelessWidget {
  const _PaymentModeChip({
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

class _PaymentModeSheet extends ConsumerStatefulWidget {
  const _PaymentModeSheet({
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
  ConsumerState<_PaymentModeSheet> createState() => _PaymentModeSheetState();
}

class _PaymentModeSheetState extends ConsumerState<_PaymentModeSheet> {
  late PaymentMode _mode;
  late final TextEditingController _cashController;
  late final TextEditingController _upiController;

  @override
  void initState() {
    super.initState();
    _mode = widget.initialMode;
    _cashController = TextEditingController(
      text: widget.initialCash.toStringAsFixed(2),
    );
    _upiController = TextEditingController(
      text: widget.initialUpi.toStringAsFixed(2),
    );
  }

  @override
  void dispose() {
    _cashController.dispose();
    _upiController.dispose();
    super.dispose();
  }

  double get _cash => double.tryParse(_cashController.text) ?? 0;
  double get _upi => double.tryParse(_upiController.text) ?? 0;
  double get _settlement =>
      double.parse((_cash + _upi).toStringAsFixed(2));

  bool get _settlementOk {
    if (_mode != PaymentMode.cashPlusUpi) return true;
    return (_settlement - widget.totalAmount).abs() <= 0.05;
  }

  void _selectMode(PaymentMode mode) {
    setState(() {
      _mode = mode;
      if (mode == PaymentMode.cash) {
        _cashController.text = widget.totalAmount.toStringAsFixed(2);
        _upiController.text = '0.00';
      } else if (mode == PaymentMode.upi) {
        _cashController.text = '0.00';
        _upiController.text = widget.totalAmount.toStringAsFixed(2);
      } else {
        _cashController.text = '0.00';
        _upiController.text = '0.00';
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
                    child: _PaymentModeChip(
                      mode: mode,
                      selected: _mode == mode,
                      onTap: () => _selectMode(mode),
                    ),
                  ),
                  if (mode != PaymentMode.values.last) const SizedBox(width: 8),
                ],
              ],
            ),
            if (_mode == PaymentMode.cashPlusUpi) ...[
              const SizedBox(height: 18),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _cashController,
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
                      controller: _upiController,
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
                  '${strings.totalSettlement}: ${widget.currency.format(_settlement)}',
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
              if (!_settlementOk) ...[
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
                    onPressed: !_settlementOk
                        ? null
                        : () => widget.onContinue(_mode, _cash, _upi),
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

class _ReceiptPreviewCard extends StatelessWidget {
  const _ReceiptPreviewCard({required this.title, required this.text});

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

