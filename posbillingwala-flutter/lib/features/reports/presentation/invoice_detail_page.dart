import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/payment_display/presentation/payment_display_actions.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

enum BillDetailAction { showQr, editCustomer, refund }

class InvoiceDetailPage extends ConsumerStatefulWidget {
  const InvoiceDetailPage({super.key, required this.invoiceId});

  final int invoiceId;

  @override
  ConsumerState<InvoiceDetailPage> createState() => InvoiceDetailPageState();
}

class InvoiceDetailPageState extends ConsumerState<InvoiceDetailPage> {
  var summaryExpanded = true;
  var itemsExpanded = true;

  int get invoiceId => widget.invoiceId;

  Future<void> openAddProducts() async {
    final detail = await ref.read(invoiceDetailProvider(invoiceId).future);
    if (!mounted || detail == null) return;
    if (detail.invoice.invoiceOrderStatus == 'cancelled' ||
        detail.invoice.invoiceOrderStatus == 'refunded') {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).voidedCannotEdit)),
      );
      return;
    }
    await context.push('/reports/invoice/$invoiceId/add-products');
    ref.invalidate(invoiceDetailProvider(invoiceId));
  }

  Future<void> runRefund(Invoice invoice) async {
    final strings = AppStrings.of(ref);
    final confirm = await showAppConfirmBottomSheet(
      context: context,
      title: strings.refundBill,
      message: strings.refundBillConfirm,
      confirmLabel: strings.refund,
      cancelLabel: strings.cancel,
      icon: Icons.replay_rounded,
      confirmVariant: AppButtonVariant.primary,
    );
    if (!confirm || !mounted) return;
    await ref.read(appDatabaseProvider).refundInvoiceLocally(invoice.invoiceId);
    if (AppPlatform.requiresNetwork) {
      final sync = await ref
          .read(invoiceSyncControllerProvider.notifier)
          .uploadPending(onlyInvoiceId: invoice.invoiceId);
      if (sync.failed > 0 || sync.uploaded < 1) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              sync.message?.trim().isNotEmpty == true
                  ? sync.message!
                  : kWebApiSaveFailedMessage,
            ),
          ),
        );
        return;
      }
    } else if (await isDeviceOnline()) {
      await ref
          .read(invoiceSyncControllerProvider.notifier)
          .uploadPending(onlyInvoiceId: invoice.invoiceId);
    }
    ref.invalidate(invoiceDetailProvider(invoiceId));
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(strings.billRefunded)),
    );
  }

  Future<void> runAction(BillDetailAction action, Invoice invoice) async {
    switch (action) {
      case BillDetailAction.showQr:
        await requestShowInvoicePaymentQr(context, ref, invoice);
      case BillDetailAction.editCustomer:
        await editInvoiceCustomerPayment(context, ref, invoice);
        ref.invalidate(invoiceDetailProvider(invoiceId));
      case BillDetailAction.refund:
        await runRefund(invoice);
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings.of(ref);
    final detailAsync = ref.watch(invoiceDetailProvider(invoiceId));
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final timeFormat = DateFormat('dd MMM yyyy, hh:mm a');

    return Scaffold(
      appBar: AppBar(
        title: Text(strings.billDetails),
        actions: [
          IconButton(
            tooltip: strings.editBill,
            onPressed: openAddProducts,
            icon: const Icon(Icons.edit_outlined),
          ),
          IconButton(
            tooltip: strings.duplicatePrint,
            onPressed: () => context.push('/print/bill/$invoiceId?duplicate=1'),
            icon: const Icon(Icons.print_rounded),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: detailAsync.when(
              data: (detail) {
                if (detail == null) {
                  return Center(child: Text(strings.billNotFound));
                }
                final invoice = detail.invoice;
                final items = detail.items;
                final cancelled = invoice.invoiceOrderStatus == 'cancelled';
                final refunded = invoice.invoiceOrderStatus == 'refunded';

                return ResponsiveScrollShell(
                  dashboard: false,
                  child: ListView(
                    padding: EdgeInsets.fromLTRB(
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      context.isShortHeight
                          ? AppBreakpoints.densePaddingFor(context.heightClass)
                          : AppBreakpoints.pagePaddingFor(context.widthClass),
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      AppBreakpoints.pagePaddingFor(context.widthClass) + 8,
                    ),
                    children: [
                      AppCard(
                        accentColor: AppColors.primary,
                        padding: EdgeInsets.zero,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            InkWell(
                              onTap: () => setState(
                                () => summaryExpanded = !summaryExpanded,
                              ),
                              borderRadius: const BorderRadius.vertical(
                                top: Radius.circular(14),
                              ),
                              child: Padding(
                                padding: const EdgeInsets.fromLTRB(
                                  16,
                                  14,
                                  12,
                                  14,
                                ),
                                child: Row(
                                  children: [
                                    const AppModuleIcon(
                                      svgPath: AppAssets.svgReceipt,
                                      color: AppColors.primary,
                                      size: 44,
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            invoice.invoiceNumber,
                                            style: Theme.of(context)
                                                .textTheme
                                                .titleMedium
                                                ?.copyWith(
                                                  fontWeight: FontWeight.w800,
                                                  color: AppColors.primary,
                                                ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            timeFormat.format(
                                              invoice.invoiceDate,
                                            ),
                                            style: Theme.of(context)
                                                .textTheme
                                                .bodySmall
                                                ?.copyWith(
                                                  color: AppColors.navy
                                                      .withValues(alpha: 0.55),
                                                ),
                                          ),
                                        ],
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Flexible(
                                      child: Text(
                                        currency.format(invoice.totalAmount),
                                        textAlign: TextAlign.right,
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: const TextStyle(
                                          fontWeight: FontWeight.w800,
                                          fontSize: 16,
                                          color: AppColors.primary,
                                        ),
                                      ),
                                    ),
                                    Icon(
                                      summaryExpanded
                                          ? Icons.expand_less_rounded
                                          : Icons.expand_more_rounded,
                                      color: AppColors.primary,
                                    ),
                                  ],
                                ),
                              ),
                            ),
                            AnimatedCrossFade(
                              firstChild: const SizedBox(width: double.infinity),
                              secondChild: Padding(
                                padding: const EdgeInsets.fromLTRB(
                                  16,
                                  0,
                                  16,
                                  16,
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Divider(height: 1),
                                    const SizedBox(height: 12),
                                    if (invoice.createdByStaffName
                                        .trim()
                                        .isNotEmpty) ...[
                                      Text(
                                        'Billed by: ${invoice.createdByStaffName.trim()}',
                                        style: Theme.of(context)
                                            .textTheme
                                            .bodyMedium
                                            ?.copyWith(
                                              fontWeight: FontWeight.w600,
                                            ),
                                      ),
                                      const SizedBox(height: 8),
                                    ],
                                    Text(
                                      typeLabel(invoice.invoiceType, strings),
                                      style: Theme.of(
                                        context,
                                      ).textTheme.labelLarge,
                                    ),
                                    if (cancelled || refunded) ...[
                                      const SizedBox(height: 8),
                                      Wrap(
                                        spacing: 8,
                                        children: [
                                          if (cancelled)
                                            Chip(
                                              label: Text(strings.voided),
                                              backgroundColor: AppColors.danger
                                                  .withValues(alpha: 0.2),
                                            ),
                                          if (refunded)
                                            Chip(
                                              label: Text(strings.refunded),
                                              backgroundColor: AppColors
                                                  .warning
                                                  .withValues(alpha: 0.2),
                                            ),
                                        ],
                                      ),
                                    ],
                                    if ((invoice.customerName ?? '')
                                        .trim()
                                        .isNotEmpty) ...[
                                      const SizedBox(height: 8),
                                      Text(
                                        '${strings.customer}: ${invoice.customerName}',
                                      ),
                                    ],
                                    if ((invoice.customerMobile ?? '')
                                        .trim()
                                        .isNotEmpty) ...[
                                      const SizedBox(height: 4),
                                      Text(
                                        '${strings.customerMobile}: ${invoice.customerMobile}',
                                      ),
                                    ],
                                    if ((invoice.customerEmail ?? '')
                                        .trim()
                                        .isNotEmpty) ...[
                                      const SizedBox(height: 4),
                                      Text(
                                        '${strings.customerEmail}: ${invoice.customerEmail}',
                                      ),
                                    ],
                                    if ((invoice.customerAddress ?? '')
                                        .trim()
                                        .isNotEmpty) ...[
                                      const SizedBox(height: 4),
                                      Text(
                                        '${strings.customerAddress}: ${invoice.customerAddress}',
                                      ),
                                    ],
                                    const Divider(height: 24),
                                    InvoiceDetailPageRow(
                                      label: strings.payment,
                                      value: invoice.paymentMode,
                                    ),
                                    InvoiceDetailPageRow(
                                      label: strings.cash,
                                      value: currency.format(
                                        invoice.cashAmount,
                                      ),
                                    ),
                                    InvoiceDetailPageRow(
                                      label: strings.upi,
                                      value: currency.format(invoice.upiAmount),
                                    ),
                                    InvoiceDetailPageRow(
                                      label: strings.subtotal,
                                      value: currency.format(invoice.subTotal),
                                    ),
                                    InvoiceDetailPageRow(
                                      label: strings.gst,
                                      value: currency.format(
                                        invoice.totalGstAmount,
                                      ),
                                    ),
                                    const SizedBox(height: 8),
                                    InvoiceDetailPageRow(
                                      label: strings.grandTotal,
                                      value: currency.format(
                                        invoice.totalAmount,
                                      ),
                                      emphasized: true,
                                    ),
                                  ],
                                ),
                              ),
                              crossFadeState: summaryExpanded
                                  ? CrossFadeState.showSecond
                                  : CrossFadeState.showFirst,
                              duration: const Duration(milliseconds: 200),
                            ),
                          ],
                        ),
                      ),
                      if (!cancelled && !refunded) ...[
                        const SizedBox(height: 12),
                        BillDetailActionRow(
                          onRun: (action) => runAction(action, invoice),
                          strings: strings,
                        ),
                      ],
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          InkWell(
                            onTap: () => setState(
                              () => itemsExpanded = !itemsExpanded,
                            ),
                            borderRadius: BorderRadius.circular(8),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(vertical: 4),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Text(
                                    strings.items,
                                    style: Theme.of(context)
                                        .textTheme
                                        .titleMedium
                                        ?.copyWith(fontWeight: FontWeight.w800),
                                  ),
                                  const SizedBox(width: 4),
                                  Icon(
                                    itemsExpanded
                                        ? Icons.expand_less_rounded
                                        : Icons.expand_more_rounded,
                                    color: AppColors.primary,
                                    size: 22,
                                  ),
                                  if (!itemsExpanded) ...[
                                    const SizedBox(width: 6),
                                    Text(
                                      '(${items.length})',
                                      style: TextStyle(
                                        color: AppColors.navy.withValues(
                                          alpha: 0.55,
                                        ),
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                            ),
                          ),
                          const Spacer(),
                          if (!cancelled && !refunded)
                            TextButton.icon(
                              onPressed: openAddProducts,
                              icon: const Icon(Icons.add_rounded),
                              label: Text(strings.add),
                            ),
                        ],
                      ),
                      if (itemsExpanded) ...[
                        const SizedBox(height: 8),
                        AppCard(
                          padding: EdgeInsets.zero,
                          child: items.isEmpty
                              ? Padding(
                                  padding: const EdgeInsets.all(16),
                                  child: Text(strings.noItems),
                                )
                              : Column(
                                  children: [
                                    const InvoiceItemsTableHeader(),
                                    const Divider(height: 1),
                                    for (var i = 0; i < items.length; i++) ...[
                                      if (i > 0) const Divider(height: 1),
                                      InvoiceItemsTableRow(
                                        index: i + 1,
                                        item: items[i],
                                        currency: currency,
                                        comboLabel:
                                            items[i].invoiceItemType == 'combo'
                                            ? strings.combo
                                            : null,
                                      ),
                                    ],
                                  ],
                                ),
                        ),
                      ],
                    ],
                  ),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(child: Text('$e')),
            ),
          ),
        ],
      ),
    );
  }

  String typeLabel(String type, AppStrings strings) {
    return switch (type) {
      'take_away' => strings.takeAway,
      'table_wise' => strings.dineIn,
      _ => strings.posLabel,
    };
  }
}

/* One row: Show QR | Edit Customer | Refund Bill — no duplicate button below. */
class BillDetailActionRow extends StatelessWidget {
  const BillDetailActionRow({
    super.key,
    required this.onRun,
    required this.strings,
  });

  final ValueChanged<BillDetailAction> onRun;
  final AppStrings strings;

  @override
  Widget build(BuildContext context) {
    final actions = <({BillDetailAction id, IconData icon, String label})>[
      (
        id: BillDetailAction.showQr,
        icon: Icons.qr_code_2_rounded,
        label: strings.paymentDisplayShowQr,
      ),
      (
        id: BillDetailAction.editCustomer,
        icon: Icons.person_outline_rounded,
        label: strings.editCustomerPayment,
      ),
      (
        id: BillDetailAction.refund,
        icon: Icons.replay_rounded,
        label: strings.refundBill,
      ),
    ];

    Widget actionChip(int i, {required bool expanded}) {
      final chip = Material(
        color: i == 0 ? AppColors.primary : Colors.white,
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: () => onRun(actions[i].id),
          child: Container(
            height: 48,
            padding: const EdgeInsets.symmetric(horizontal: 6),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: AppColors.primary.withValues(
                  alpha: i == 0 ? 0 : 0.35,
                ),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  actions[i].icon,
                  size: 18,
                  color: i == 0 ? Colors.white : AppColors.primary,
                ),
                const SizedBox(width: 4),
                Flexible(
                  child: Text(
                    actions[i].label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: i == 0 ? Colors.white : AppColors.primary,
                      fontWeight: FontWeight.w800,
                      fontSize: 12,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
      if (expanded) return Expanded(child: chip);
      return chip;
    }

    if (context.isCompactWidth) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          for (var i = 0; i < actions.length; i++) ...[
            if (i > 0) const SizedBox(height: 8),
            actionChip(i, expanded: false),
          ],
        ],
      );
    }

    return Row(
      children: [
        for (var i = 0; i < actions.length; i++) ...[
          if (i > 0) const SizedBox(width: 8),
          actionChip(i, expanded: true),
        ],
      ],
    );
  }
}

/* Customer + payment editor — all fields editable, chip pickers (no dropdown errors). */
Future<void> editInvoiceCustomerPayment(
  BuildContext context,
  WidgetRef ref,
  Invoice invoice,
) async {
  final strings = AppStrings.of(ref);
  final nameCtrl = TextEditingController(text: invoice.customerName ?? '');
  final mobileCtrl = TextEditingController(text: invoice.customerMobile ?? '');
  final emailCtrl = TextEditingController(text: invoice.customerEmail ?? '');
  final addressCtrl = TextEditingController(
    text: invoice.customerAddress ?? '',
  );
  final discountCtrl = TextEditingController(
    text: amountInputText(invoice.discount),
  );
  final packingCtrl = TextEditingController(
    text: amountInputText(invoice.packingCharge),
  );
  final cashCtrl = TextEditingController(
    text: amountInputText(invoice.cashAmount),
  );
  final upiCtrl = TextEditingController(
    text: amountInputText(invoice.upiAmount),
  );

  var paymentMode = PaymentMode.fromLabel(invoice.paymentMode);
  var discountType = normalizeDiscountType(invoice.discountType);
  var packingType = normalizeDiscountType(invoice.packingChargeType);

  final ok = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: AppColors.glassSolid,
    showDragHandle: true,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
    ),
    builder: (sheetContext) {
      return Padding(
        padding: EdgeInsets.fromLTRB(
          20,
          8,
          20,
          MediaQuery.viewInsetsOf(sheetContext).bottom + 20,
        ),
        child: StatefulBuilder(
          builder: (context, setLocal) {
            void syncAmountsForMode(PaymentMode mode) {
              final total = invoice.totalAmount;
              if (mode == PaymentMode.cash) {
                cashCtrl.text = amountInputText(total);
                upiCtrl.text = '';
              } else if (mode == PaymentMode.upi) {
                upiCtrl.text = amountInputText(total);
                cashCtrl.text = '';
              }
            }

            return SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    strings.editCustomerPayment,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: AppColors.navy,
                    ),
                  ),
                  const SizedBox(height: 16),
                  AppTextField(
                    controller: nameCtrl,
                    label: strings.customerNameField,
                    hint: strings.customerNameField,
                    enabled: true,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: mobileCtrl,
                    label: strings.customerMobile,
                    hint: strings.customerMobile,
                    enabled: true,
                    keyboardType: TextInputType.phone,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: emailCtrl,
                    label: strings.customerEmail,
                    hint: strings.customerEmail,
                    enabled: true,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: addressCtrl,
                    label: strings.customerAddress,
                    hint: strings.customerAddress,
                    enabled: true,
                    maxLines: 2,
                    minLines: 2,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    strings.discountType,
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      color: AppColors.navy,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: [
                      for (final t in const ['Amount', 'Percent'])
                        ChoiceChip(
                          label: Text(t),
                          selected: discountType == t,
                          onSelected: (_) =>
                              setLocal(() => discountType = t),
                        ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: discountCtrl,
                    label: strings.discountLabel,
                    hint: '0',
                    enabled: true,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    strings.packingType,
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      color: AppColors.navy,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: [
                      for (final t in const ['Amount', 'Percent'])
                        ChoiceChip(
                          label: Text(t),
                          selected: packingType == t,
                          onSelected: (_) => setLocal(() => packingType = t),
                        ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: packingCtrl,
                    label: strings.packingCharge,
                    hint: '0',
                    enabled: true,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    strings.paymentMode,
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      color: AppColors.navy,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: [
                      for (final mode in PaymentMode.values)
                        ChoiceChip(
                          label: Text(mode.label),
                          selected: paymentMode == mode,
                          onSelected: (_) => setLocal(() {
                            paymentMode = mode;
                            syncAmountsForMode(mode);
                          }),
                        ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: cashCtrl,
                    label: strings.cashAmount,
                    hint: '0',
                    enabled: true,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: upiCtrl,
                    label: strings.upiAmount,
                    hint: '0',
                    enabled: true,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                  const SizedBox(height: 20),
                  Row(
                    children: [
                      Expanded(
                        child: AppButton(
                          label: strings.cancel,
                          variant: AppButtonVariant.outlined,
                          onPressed: () =>
                              Navigator.of(sheetContext).pop(false),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: AppButton(
                          label: strings.save,
                          onPressed: () =>
                              Navigator.of(sheetContext).pop(true),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            );
          },
        ),
      );
    },
  );

  if (ok == true) {
    try {
      await ref.read(appDatabaseProvider).updateInvoiceHeader(
            invoiceId: invoice.invoiceId,
            customerName: nameCtrl.text.trim().isEmpty
                ? null
                : nameCtrl.text.trim(),
            customerMobile: mobileCtrl.text.trim().isEmpty
                ? null
                : mobileCtrl.text.trim(),
            customerEmail: emailCtrl.text.trim().isEmpty
                ? null
                : emailCtrl.text.trim(),
            customerAddress: addressCtrl.text.trim().isEmpty
                ? null
                : addressCtrl.text.trim(),
            paymentMode: paymentMode.label,
            cashAmount: double.tryParse(cashCtrl.text.trim()) ?? 0,
            upiAmount: double.tryParse(upiCtrl.text.trim()) ?? 0,
            discount: double.tryParse(discountCtrl.text.trim()) ?? 0,
            discountType: discountType,
            packingCharge: double.tryParse(packingCtrl.text.trim()) ?? 0,
            packingChargeType: packingType,
            clearCustomerWhenNull: true,
          );
      ref.invalidate(invoiceDetailProvider(invoice.invoiceId));
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(strings.billUpdatedPending)),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  nameCtrl.dispose();
  mobileCtrl.dispose();
  emailCtrl.dispose();
  addressCtrl.dispose();
  discountCtrl.dispose();
  packingCtrl.dispose();
  cashCtrl.dispose();
  upiCtrl.dispose();
}

String normalizeDiscountType(String raw) {
  final v = raw.trim().toLowerCase();
  if (v.startsWith('p')) return 'Percent';
  return 'Amount';
}

class InvoiceDetailPageRow extends StatelessWidget {
  const InvoiceDetailPageRow({
    super.key,
    required this.label,
    required this.value,
    this.emphasized = false,
  });

  final String label;
  final String value;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    final style = emphasized
        ? Theme.of(
            context,
          ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800)
        : Theme.of(context).textTheme.bodyMedium;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Text(label, style: style),
          const Spacer(),
          Text(
            value,
            style: style?.copyWith(
              color: emphasized ? AppColors.primary : null,
            ),
          ),
        ],
      ),
    );
  }
}

/* Invoice-style columns inside the items card: # | Item | Qty | Rate | Amt */
class InvoiceItemsTableHeader extends StatelessWidget {
  const InvoiceItemsTableHeader({super.key});

  @override
  Widget build(BuildContext context) {
    const style = TextStyle(
      fontWeight: FontWeight.w800,
      fontSize: 12,
      color: AppColors.navy,
    );
    return Container(
      color: AppColors.primary.withValues(alpha: 0.06),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      child: const Row(
        children: [
          SizedBox(width: 28, child: Text('#', style: style)),
          Expanded(flex: 4, child: Text('Item', style: style)),
          SizedBox(
            width: 40,
            child: Text('Qty', textAlign: TextAlign.center, style: style),
          ),
          SizedBox(
            width: 64,
            child: Text('Rate', textAlign: TextAlign.right, style: style),
          ),
          SizedBox(
            width: 72,
            child: Text('Amt', textAlign: TextAlign.right, style: style),
          ),
        ],
      ),
    );
  }
}

class InvoiceItemsTableRow extends StatelessWidget {
  const InvoiceItemsTableRow({
    super.key,
    required this.index,
    required this.item,
    required this.currency,
    this.comboLabel,
  });

  final int index;
  final InvoiceItem item;
  final NumberFormat currency;
  final String? comboLabel;

  @override
  Widget build(BuildContext context) {
    final qty = item.productQuantity == item.productQuantity.roundToDouble()
        ? item.productQuantity.toInt().toString()
        : item.productQuantity.toStringAsFixed(2);
    final lineTotal = item.productPrice * item.productQuantity;
    const body = TextStyle(fontSize: 12.5, color: AppColors.navy, height: 1.2);
    const bold = TextStyle(
      fontSize: 12.5,
      fontWeight: FontWeight.w700,
      color: AppColors.navy,
      height: 1.2,
    );

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 28,
            child: Text('$index', style: body),
          ),
          Expanded(
            flex: 4,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.productName,
                  style: bold,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                if (comboLabel != null)
                  Text(
                    comboLabel!,
                    style: body.copyWith(
                      fontSize: 11,
                      color: AppColors.navy.withValues(alpha: 0.55),
                    ),
                  ),
                if ((item.portionName ?? '').trim().isNotEmpty)
                  Text(
                    item.portionName!.trim(),
                    style: body.copyWith(
                      fontSize: 11,
                      color: AppColors.navy.withValues(alpha: 0.55),
                    ),
                  ),
              ],
            ),
          ),
          SizedBox(
            width: 40,
            child: Text(qty, textAlign: TextAlign.center, style: body),
          ),
          SizedBox(
            width: 64,
            child: Text(
              currency.format(item.productPrice),
              textAlign: TextAlign.right,
              style: body,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          SizedBox(
            width: 72,
            child: Text(
              currency.format(lineTotal),
              textAlign: TextAlign.right,
              style: bold.copyWith(color: AppColors.primary),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}
