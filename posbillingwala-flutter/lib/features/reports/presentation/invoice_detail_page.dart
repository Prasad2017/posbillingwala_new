import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/sync/data/invoice_sync_api.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_providers.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';

class InvoiceDetailPage extends ConsumerWidget {
  const InvoiceDetailPage({super.key, required this.invoiceId});

  final int invoiceId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
            onPressed: () async {
              final detail = await ref.read(invoiceDetailProvider(invoiceId).future);
              if (!context.mounted || detail == null) return;
              if (detail.invoice.invoiceOrderStatus == 'cancelled' ||
                  detail.invoice.invoiceOrderStatus == 'refunded') {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(strings.voidedCannotEdit),
                  ),
                );
                return;
              }
              await context.push('/reports/invoice/$invoiceId/edit');
              ref.invalidate(invoiceDetailProvider(invoiceId));
            },
            icon: const Icon(Icons.edit_outlined),
          ),
          IconButton(
            tooltip: strings.printShare,
            onPressed: () => context.push('/print/bill/$invoiceId'),
            icon: const Icon(Icons.print_rounded),
          ),
          IconButton(
            tooltip: strings.duplicatePrint,
            onPressed: () => context.push(
              '/print/bill/$invoiceId?duplicate=1',
            ),
            icon: const Icon(Icons.copy_all_rounded),
          ),
        ],
      ),
      body: Column(children: [
        Expanded(child: detailAsync.when(
        data: (detail) {
          if (detail == null) {
            return Center(child: Text(strings.billNotFound));
          }
          final invoice = detail.invoice;
          final items = detail.items;
          final pending = invoice.invoiceSyncStatus == '0';
          final cancelled = invoice.invoiceOrderStatus == 'cancelled';
          final refunded = invoice.invoiceOrderStatus == 'refunded';

          return ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
            padding: EdgeInsets.all(
            AppBreakpoints.pagePaddingFor(context.widthClass),
          ),
            children: [
              AppCard(
            accentColor: AppColors.primary,
            padding: const EdgeInsets.all(16),
            child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const AppModuleIcon(svgPath: AppAssets.svgReceipt, color: AppColors.primary, size: 56),
                      const SizedBox(height: 10),
                      Text(
                        invoice.invoiceNumber,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w800,
                              color: AppColors.primary,
                            ),
                      ),
                      const SizedBox(height: 8),
                      Text(timeFormat.format(invoice.invoiceDate)),
                      if (invoice.createdByStaffName.trim().isNotEmpty) ...[
                        const SizedBox(height: 8),
                        Text(
                          'Billed by: ${invoice.createdByStaffName.trim()}',
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                fontWeight: FontWeight.w600,
                              ),
                        ),
                      ],
                      const SizedBox(height: 8),
                      Text(
                        typeLabel(invoice.invoiceType, strings),
                        style: Theme.of(context).textTheme.labelLarge,
                      ),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        children: [
                          Chip(
                            label: Text(pending ? strings.pendingSync : strings.synced),
                            backgroundColor: pending
                                ? AppColors.warning.withValues(alpha: 0.2)
                                : AppColors.success.withValues(alpha: 0.2),
                          ),
                          if (cancelled)
                            Chip(
                              label: Text(strings.voided),
                              backgroundColor:
                                  AppColors.danger.withValues(alpha: 0.2),
                            ),
                          if (refunded)
                            Chip(
                              label: Text(strings.refunded),
                              backgroundColor:
                                  AppColors.warning.withValues(alpha: 0.2),
                            ),
                        ],
                      ),
                      if (invoice.customerName != null) ...[
                        const SizedBox(height: 8),
                        Text('${strings.customer}: ${invoice.customerName}'),
                      ],
                      if (invoice.customerMobile != null) ...[
                        const SizedBox(height: 4),
                        Text('${strings.customerMobile}: ${invoice.customerMobile}'),
                      ],
                      if (invoice.customerEmail != null) ...[
                        const SizedBox(height: 4),
                        Text('${strings.customerEmail}: ${invoice.customerEmail}'),
                      ],
                      if (invoice.customerAddress != null) ...[
                        const SizedBox(height: 4),
                        Text('${strings.customerAddress}: ${invoice.customerAddress}'),
                      ],
                      const Divider(height: 24),
                      InvoiceDetailPageRow(label: strings.payment, value: invoice.paymentMode),
                      InvoiceDetailPageRow(
                        label: strings.cash,
                        value: currency.format(invoice.cashAmount),
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
                        value: currency.format(invoice.totalGstAmount),
                      ),
                      const SizedBox(height: 8),
                      InvoiceDetailPageRow(
                        label: strings.grandTotal,
                        value: currency.format(invoice.totalAmount),
                        emphasized: true,
                      ),
                    ],
                  ),
              ),
              if (!cancelled && !refunded) ...[
                const SizedBox(height: 8),
                AppButton(
            label: strings.editCustomerPayment,
            icon: Icons.edit_outlined,
            variant: AppButtonVariant.outlined,
            expanded: false,
            onPressed: () async {
                    await editInvoiceHeader(context, ref, invoice);
                    ref.invalidate(invoiceDetailProvider(invoiceId));
                  },
          ),
                const SizedBox(height: 8),
                AppButton(
            label: strings.refundBill,
            icon: Icons.replay_rounded,
            variant: AppButtonVariant.outlined,
            expanded: false,
            onPressed: () async {
                    final confirm = await showDialog<bool>(
                      context: context,
                      builder: (context) => AlertDialog(
                        title: Text(strings.refundBill),
                        content: Text(strings.refundBillConfirm),
                        actions: [
                          TextButton(
                            onPressed: () => Navigator.pop(context, false),
                            child: Text(strings.cancel),
                          ),
                          AppButton(
            label: strings.refund,
            onPressed: () => Navigator.pop(context, true),
          ),
                        ],
                      ),
                    );
                    if (confirm != true) return;
                    await ref
                        .read(appDatabaseProvider)
                        .refundInvoiceLocally(invoice.invoiceId);
                    if (AppPlatform.requiresNetwork) {
                      final sync = await ref
                          .read(invoiceSyncControllerProvider.notifier)
                          .uploadPending(onlyInvoiceId: invoice.invoiceId);
                      if (sync.failed > 0 || sync.uploaded < 1) {
                        if (!context.mounted) return;
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
                    if (!context.mounted) return;
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text(strings.billRefunded)),
                    );
                  },
          ),
              ],
              if (pending) ...[
                const SizedBox(height: 8),
                AppButton(
            label: strings.uploadToCloud,
            icon: Icons.cloud_upload_rounded,
            expanded: false,
            onPressed: () async {
                    final result = await ref
                        .read(invoiceSyncControllerProvider.notifier)
                        .uploadPending(onlyInvoiceId: invoice.invoiceId);
                    if (!context.mounted) return;
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(result.message ?? strings.syncFinished),
                      ),
                    );
                  },
          ),
                if (!cancelled) ...[
                  const SizedBox(height: 8),
                  AppButton(
            label: strings.voidBill,
            icon: Icons.cancel_outlined,
            variant: AppButtonVariant.outlined,
            expanded: false,
            onPressed: () async {
                      final confirm = await showDialog<bool>(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: Text(strings.voidBill),
                          content: Text(strings.voidPendingBillConfirm),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(context, false),
                              child: Text(strings.cancel),
                            ),
                            AppButton(
            label: strings.voidAction,
            onPressed: () => Navigator.pop(context, true),
          ),
                          ],
                        ),
                      );
                      if (confirm != true) return;
                      await ref
                          .read(appDatabaseProvider)
                          .voidInvoiceLocally(invoice.invoiceId);
                      ref.invalidate(invoiceDetailProvider(invoiceId));
                      if (!context.mounted) return;
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(strings.billVoided)),
                      );
                    },
          ),
                ],
              ],
              const SizedBox(height: 16),
              Row(
                children: [
                  Text(
                    strings.items,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                  const Spacer(),
                  if (!cancelled && !refunded)
                    TextButton.icon(
                      onPressed: () => addInvoiceProduct(
                        context,
                        ref,
                        invoice.invoiceId,
                      ),
                      icon: const Icon(Icons.add_rounded),
                      label: Text(strings.add),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              ...items.map(
                (item) => AppCard(
            padding: EdgeInsets.zero,
            child: ListTile(
                    title: Text(
                      item.productName,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    subtitle: Text(
                      '${currency.format(item.productPrice)} × ${item.productQuantity}'
                      '${item.invoiceItemType == 'combo' ? ' · ${strings.combo}' : ''}',
                    ),
                    trailing: (!cancelled && !refunded)
                        ? Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              IconButton(
                                tooltip: strings.editLine,
                                icon: const Icon(Icons.edit_note_rounded),
                                onPressed: () async {
                                  final qtyCtrl = TextEditingController(
                                    text: '${item.productQuantity}',
                                  );
                                  final priceCtrl = TextEditingController(
                                    text: item.productPrice.toStringAsFixed(2),
                                  );
                                  final ok = await showDialog<bool>(
                                    context: context,
                                    builder: (context) => AlertDialog(
                                      title: Text(strings.editItem),
                                      content: Column(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          AppTextField(
                                            controller: qtyCtrl,
                                            label: strings.productQuantity,
                                            keyboardType: TextInputType.number,
                                          ),
                                          const SizedBox(height: 12),
                                          AppTextField(
                                            controller: priceCtrl,
                                            label: strings.unitPrice,
                                            keyboardType:
                                                const TextInputType
                                                    .numberWithOptions(
                                              decimal: true,
                                            ),
                                          ),
                                        ],
                                      ),
                                      actions: [
                                        TextButton(
                                          onPressed: () =>
                                              Navigator.pop(context, false),
                                          child: Text(strings.cancel),
                                        ),
                                        AppButton(
                                          label: strings.save,
                                          onPressed: () =>
                                              Navigator.pop(context, true),
                                        ),
                                      ],
                                    ),
                                  );
                                  if (ok != true) {
                                    qtyCtrl.dispose();
                                    priceCtrl.dispose();
                                    return;
                                  }
                                  final qty =
                                      double.tryParse(qtyCtrl.text.trim()) ?? 0;
                                  final price =
                                      double.tryParse(priceCtrl.text.trim());
                                  qtyCtrl.dispose();
                                  priceCtrl.dispose();
                                  try {
                                    await ref
                                        .read(appDatabaseProvider)
                                        .updateInvoiceItemQuantity(
                                          invoiceItemId: item.invoiceItemId,
                                          quantity: qty,
                                          productPrice: price,
                                        );
                                    ref.invalidate(
                                      invoiceDetailProvider(invoiceId),
                                    );
                                  } catch (e) {
                                    if (!context.mounted) return;
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(content: Text('$e')),
                                    );
                                  }
                                },
                              ),
                              IconButton(
                                tooltip: strings.deleteLine,
                                icon: const Icon(Icons.delete_outline),
                                onPressed: () async {
                                  final network =
                                      item.invoiceItemNetworkStatus?.trim();
                                  try {
                                    await ref
                                        .read(appDatabaseProvider)
                                        .deleteInvoiceItemAndRecompute(
                                          item.invoiceItemId,
                                        );
                                    if (network != null && network.isNotEmpty) {
                                      try {
                                        final ok = await InvoiceSyncApi(
                                          ref.read(apiClientProvider),
                                        ).deleteInvoiceProduct(
                                          invoiceProductNetworkStatus: network,
                                        );
                                        if (ok) {
                                          await ref
                                              .read(appDatabaseProvider)
                                              .removeInvoiceProductDeleteByNetworkStatus(
                                                network,
                                              );
                                        }
                                      } catch (_) {}
                                    }
                                    ref.invalidate(
                                      invoiceDetailProvider(invoiceId),
                                    );
                                    if (!context.mounted) return;
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(
                                        content: Text(strings.lineRemoved),
                                      ),
                                    );
                                  } catch (e) {
                                    if (!context.mounted) return;
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(content: Text('$e')),
                                    );
                                  }
                                },
                              ),
                            ],
                          )
                        : Text(
                            currency.format(
                              item.productPrice * item.productQuantity,
                            ),
                            style: const TextStyle(fontWeight: FontWeight.w800),
                          ),
                  ),
                ),
              ),
            ],
          ),
      );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
      )),
      ]),
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

Future<void> addInvoiceProduct(
  BuildContext context,
  WidgetRef ref,
  int invoiceId,
) async {
  final strings = AppStrings.of(ref);
  final products = await ref.read(appDatabaseProvider).watchActiveProducts().first;
  if (!context.mounted) return;
  if (products.isEmpty) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(strings.noProductsCatalog)),
    );
    return;
  }
  Product? selected = products.first;
  final qtyCtrl = TextEditingController(text: '1');
  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setLocal) => AlertDialog(
        title: Text(strings.addProduct),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AppDropdownFormField<Product>(
              label: strings.product,
              items: products,
              itemLabel: (p) => p.productName,
              value: selected,
              enableSearch: true,
              onChanged: (v) => setLocal(() => selected = v),
            ),
            const SizedBox(height: 12),
            AppTextField(
                      controller: qtyCtrl,
                      label: strings.productQuantity,
                      keyboardType: TextInputType.number,
                    ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(strings.cancel),
          ),
          AppButton(
            label: strings.add,
            onPressed: () => Navigator.pop(context, true),
          ),
        ],
      ),
    ),
  );
  if (ok != true || selected == null) {
    qtyCtrl.dispose();
    return;
  }
  final qty = double.tryParse(qtyCtrl.text.trim()) ?? 1;
  qtyCtrl.dispose();
  try {
    await ref.read(appDatabaseProvider).addInvoiceItemLine(
          invoiceId: invoiceId,
          productId: selected!.productId,
          productName: selected!.productName,
          productPrice: selected!.productPrice,
          quantity: qty <= 0 ? 1 : qty,
          productCode: selected!.productCode,
          categoryName: selected!.categoryName,
          cgst: selected!.productCgst,
          sgst: selected!.productSgst,
        );
    ref.invalidate(invoiceDetailProvider(invoiceId));
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(strings.itemAddedPending)),
      );
    }
  } catch (e) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }
}

Future<void> editInvoiceHeader(
  BuildContext context,
  WidgetRef ref,
  Invoice invoice,
) async {
  final strings = AppStrings.of(ref);
  final nameCtrl = TextEditingController(text: invoice.customerName ?? '');
  final mobileCtrl = TextEditingController(text: invoice.customerMobile ?? '');
  final emailCtrl = TextEditingController(text: invoice.customerEmail ?? '');
  final addressCtrl = TextEditingController(text: invoice.customerAddress ?? '');
  final discountCtrl =
      TextEditingController(text: invoice.discount.toStringAsFixed(2));
  final packingCtrl =
      TextEditingController(text: invoice.packingCharge.toStringAsFixed(2));
  final cashCtrl =
      TextEditingController(text: invoice.cashAmount.toStringAsFixed(2));
  final upiCtrl =
      TextEditingController(text: invoice.upiAmount.toStringAsFixed(2));
  var paymentMode = normalizePaymentMode(invoice.paymentMode);
  var discountType = normalizeDiscountType(invoice.discountType);
  var packingType = normalizeDiscountType(invoice.packingChargeType);

  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setLocal) => AlertDialog(
        title: Text(strings.editBill),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              AppTextField(
                controller: nameCtrl,
                label: strings.customerNameField,
              ),
              const SizedBox(height: 12),
              AppTextField(
                controller: mobileCtrl,
                label: strings.customerMobile,
                keyboardType: TextInputType.phone,
              ),
              const SizedBox(height: 12),
              AppTextField(
                controller: emailCtrl,
                label: strings.customerEmail,
                keyboardType: TextInputType.emailAddress,
              ),
              const SizedBox(height: 12),
              AppTextField(
                controller: addressCtrl,
                label: strings.customerAddress,
                maxLines: 2,
                minLines: 2,
              ),
              const SizedBox(height: 12),
              StringDropdownField(
                label: strings.discountType,
                value: discountType,
                options: const ['Amount', 'Percent'],
                onChanged: (v) {
                  if (v != null) setLocal(() => discountType = v);
                },
              ),
              const SizedBox(height: 12),
              AppTextField(
                controller: discountCtrl,
                label: strings.discountLabel,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
              ),
              const SizedBox(height: 12),
              StringDropdownField(
                label: strings.packingType,
                value: packingType,
                options: const ['Amount', 'Percent'],
                onChanged: (v) {
                  if (v != null) setLocal(() => packingType = v);
                },
              ),
              const SizedBox(height: 12),
              AppTextField(
                controller: packingCtrl,
                label: strings.packingCharge,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
              ),
              const SizedBox(height: 12),
              StringDropdownField(
                label: strings.paymentMode,
                value: paymentMode,
                options: const ['Cash', 'UPI', 'Mixed', 'Card'],
                onChanged: (v) {
                  if (v != null) setLocal(() => paymentMode = v);
                },
              ),
              const SizedBox(height: 12),
              AppTextField(
                controller: cashCtrl,
                label: strings.cashAmount,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
              ),
              const SizedBox(height: 12),
              AppTextField(
                controller: upiCtrl,
                label: strings.upiAmount,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(strings.cancel),
          ),
          AppButton(
            label: strings.save,
            onPressed: () => Navigator.pop(context, true),
          ),
        ],
      ),
    ),
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
            paymentMode: paymentMode,
            cashAmount: double.tryParse(cashCtrl.text.trim()) ?? 0,
            upiAmount: double.tryParse(upiCtrl.text.trim()) ?? 0,
            discount: double.tryParse(discountCtrl.text.trim()) ?? 0,
            discountType: discountType,
            packingCharge: double.tryParse(packingCtrl.text.trim()) ?? 0,
            packingChargeType: packingType,
          );
      ref.invalidate(invoiceDetailProvider(invoice.invoiceId));
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(strings.billUpdatedPending)),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$e')),
        );
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

String normalizePaymentMode(String raw) {
  final v = raw.trim().toLowerCase();
  if (v.contains('card')) return 'Card';
  if (v.contains('mixed') || (v.contains('cash') && v.contains('upi'))) {
    return 'Mixed';
  }
  if (v.contains('upi')) return 'UPI';
  return 'Cash';
}

class InvoiceDetailPageRow extends StatelessWidget {
  const InvoiceDetailPageRow({super.key, 
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
        ? Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w800,
            )
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
