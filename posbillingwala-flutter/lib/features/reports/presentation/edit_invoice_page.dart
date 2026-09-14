import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/sync/data/invoice_sync_api.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/* WithTable `EditInvoice` — full line editor + header + print after save. */
class EditInvoicePage extends ConsumerWidget {
  const EditInvoicePage({super.key, required this.invoiceId});

  final int invoiceId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    final detailAsync = ref.watch(invoiceDetailProvider(invoiceId));
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');

    return Scaffold(
      appBar: AppBar(
        title: Text(strings.editInvoice),
        actions: [
          IconButton(
            tooltip: strings.saveAndPrint,
            onPressed: () async {
              final result = await printInvoiceById(ref, invoiceId);
              if (!context.mounted) return;
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text(result.message ?? strings.printed)),
              );
              context.pop();
            },
            icon: const Icon(Icons.print_rounded),
          ),
        ],
      ),
      body: detailAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (detail) {
          if (detail == null) {
            return Center(child: Text(strings.billNotFound));
          }
          final invoice = detail.invoice;
          final locked = invoice.invoiceOrderStatus == 'cancelled' ||
              invoice.invoiceOrderStatus == 'refunded';
          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
            children: [
              AppCard(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      invoice.invoiceNumber,
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 18,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(currency.format(invoice.totalAmount)),
                    Text(
                      '${strings.subtotal} ${currency.format(invoice.subTotal)} · ${strings.gst} ${currency.format(invoice.totalGstAmount)}',
                    ),
                    const SizedBox(height: 10),
                    if (!locked)
                      AppButton(
                        label: strings.editCustomerDiscountPayment,
                        variant: AppButtonVariant.outlined,
                        onPressed: () async {
                          await editHeader(context, ref, invoice);
                          ref.invalidate(invoiceDetailProvider(invoiceId));
                        },
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Text(
                    strings.items,
                    style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
                  ),
                  const Spacer(),
                  if (!locked)
                    TextButton.icon(
                      onPressed: () => editInvoicePageAddProduct(context, ref, invoiceId),
                      icon: const Icon(Icons.add_rounded),
                      label: Text(strings.addProduct),
                    ),
                ],
              ),
              for (final item in detail.items)
                AppCard(
                  padding: EdgeInsets.zero,
                  child: ListTile(
                    title: Text(
                      item.productName,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    subtitle: Text(
                      '${currency.format(item.productPrice)} × ${item.productQuantity}'
                      '${item.portionName == null || item.portionName!.isEmpty ? '' : ' · ${item.portionName}'}',
                    ),
                    trailing: locked
                        ? Text(
                            currency.format(
                              item.productPrice * item.productQuantity,
                            ),
                          )
                        : Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              IconButton(
                                icon: const Icon(Icons.edit_note_rounded),
                                onPressed: () => editLine(
                                  context,
                                  ref,
                                  invoiceId,
                                  item,
                                ),
                              ),
                              IconButton(
                                icon: const Icon(Icons.delete_outline),
                                onPressed: () => deleteLine(
                                  context,
                                  ref,
                                  invoiceId,
                                  item,
                                ),
                              ),
                            ],
                          ),
                  ),
                ),
              const SizedBox(height: 16),
              if (!locked)
                AppButton(
                  label: strings.saveAndPrint,
                  onPressed: () async {
                    final result = await printInvoiceById(ref, invoiceId);
                    if (!context.mounted) return;
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text(result.message ?? strings.printed)),
                    );
                    context.pop();
                  },
                ),
            ],
          );
        },
      ),
    );
  }
}

Future<void> editLine(
  BuildContext context,
  WidgetRef ref,
  int invoiceId,
  InvoiceItem item,
) async {
  final strings = AppStrings.of(ref);
  final qtyCtrl = TextEditingController(text: '${item.productQuantity}');
  final priceCtrl =
      TextEditingController(text: item.productPrice.toStringAsFixed(2));
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
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
          ),
        ],
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
  );
  if (ok != true) {
    qtyCtrl.dispose();
    priceCtrl.dispose();
    return;
  }
  final qty = int.tryParse(qtyCtrl.text.trim()) ?? 0;
  final price = double.tryParse(priceCtrl.text.trim());
  qtyCtrl.dispose();
  priceCtrl.dispose();
  try {
    await ref.read(appDatabaseProvider).updateInvoiceItemQuantity(
          invoiceItemId: item.invoiceItemId,
          quantity: qty,
          productPrice: price,
        );
    ref.invalidate(invoiceDetailProvider(invoiceId));
  } catch (e) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }
}

Future<void> deleteLine(
  BuildContext context,
  WidgetRef ref,
  int invoiceId,
  InvoiceItem item,
) async {
  final network = item.invoiceItemNetworkStatus?.trim();
  try {
    await ref
        .read(appDatabaseProvider)
        .deleteInvoiceItemAndRecompute(item.invoiceItemId);
    if (network != null && network.isNotEmpty) {
      try {
        final ok = await InvoiceSyncApi(ref.read(apiClientProvider))
            .deleteInvoiceProduct(invoiceProductNetworkStatus: network);
        if (ok) {
          await ref
              .read(appDatabaseProvider)
              .removeInvoiceProductDeleteByNetworkStatus(network);
        }
      } catch (_) {}
    }
    ref.invalidate(invoiceDetailProvider(invoiceId));
  } catch (e) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }
}

Future<void> editInvoicePageAddProduct(
  BuildContext context,
  WidgetRef ref,
  int invoiceId,
) async {
  final strings = AppStrings.of(ref);
  final products =
      await ref.read(appDatabaseProvider).watchActiveProducts().first;
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
  var qty = int.tryParse(qtyCtrl.text.trim()) ?? 1;
  if (qty <= 0) qty = 1;
  qtyCtrl.dispose();

  var price = selected!.productPrice;
  int? portionId;
  String? portionName;
  final portions = await ref
      .read(appDatabaseProvider)
      .getPortionsForProduct(selected!.productId);
  if (context.mounted && portions.isNotEmpty) {
    final picked = await showDialog<ProductPortion>(
      context: context,
      builder: (context) => SimpleDialog(
        title: Text(strings.selectPortion),
        children: [
          for (final p in portions)
            SimpleDialogOption(
              onPressed: () => Navigator.pop(context, p),
              child: Text(
                '${p.portionName}  ₹${p.portionPrice.toStringAsFixed(2)}',
              ),
            ),
        ],
      ),
    );
    if (picked != null) {
      price = picked.portionPrice;
      portionId = picked.portionId;
      portionName = picked.portionName;
    }
  }

  try {
    await ref.read(appDatabaseProvider).addInvoiceItemLine(
          invoiceId: invoiceId,
          productId: selected!.productId,
          productName: selected!.productName,
          productPrice: price,
          quantity: qty,
          productCode: selected!.productCode,
          categoryName: selected!.categoryName,
          cgst: selected!.productCgst,
          sgst: selected!.productSgst,
          portionId: portionId,
          portionName: portionName,
        );
    ref.invalidate(invoiceDetailProvider(invoiceId));
  } catch (e) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }
}

Future<void> editHeader(
  BuildContext context,
  WidgetRef ref,
  Invoice invoice,
) async {
  final strings = AppStrings.of(ref);
  final nameCtrl = TextEditingController(text: invoice.customerName ?? '');
  final mobileCtrl = TextEditingController(text: invoice.customerMobile ?? '');
  final discountCtrl =
      TextEditingController(text: invoice.discount.toStringAsFixed(2));
  final packingCtrl =
      TextEditingController(text: invoice.packingCharge.toStringAsFixed(2));
  final cashCtrl =
      TextEditingController(text: invoice.cashAmount.toStringAsFixed(2));
  final upiCtrl =
      TextEditingController(text: invoice.upiAmount.toStringAsFixed(2));
  var paymentMode = invoice.paymentMode;
  var discountType = invoice.discountType;
  var packingType = invoice.packingChargeType;

  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setLocal) => AlertDialog(
        title: Text(strings.billHeader),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              AppTextField(controller: nameCtrl, label: strings.customerNameField),
              const SizedBox(height: 12),
              AppTextField(
                controller: mobileCtrl,
                label: strings.customerMobile,
                keyboardType: TextInputType.phone,
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
                label: strings.packingLabel,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
              ),
              const SizedBox(height: 12),
              StringDropdownField(
                label: strings.payment,
                value: paymentMode,
                options: const ['Cash', 'UPI', 'Cash + UPI'],
                onChanged: (v) {
                  if (v != null) setLocal(() => paymentMode = v);
                },
              ),
              const SizedBox(height: 12),
              AppTextField(
                controller: cashCtrl,
                label: strings.cash,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
              ),
              const SizedBox(height: 12),
              AppTextField(
                controller: upiCtrl,
                label: strings.upi,
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
  if (ok != true) return;
  await ref.read(appDatabaseProvider).updateInvoiceHeader(
        invoiceId: invoice.invoiceId,
        customerName: nameCtrl.text.trim(),
        customerMobile: mobileCtrl.text.trim(),
        paymentMode: paymentMode,
        cashAmount: double.tryParse(cashCtrl.text.trim()),
        upiAmount: double.tryParse(upiCtrl.text.trim()),
        discount: double.tryParse(discountCtrl.text.trim()),
        discountType: discountType,
        packingCharge: double.tryParse(packingCtrl.text.trim()),
        packingChargeType: packingType,
      );
}
