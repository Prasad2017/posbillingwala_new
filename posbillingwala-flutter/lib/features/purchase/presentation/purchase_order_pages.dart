import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_document.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_providers.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/vendor.dart';

class PurchaseOrdersPage extends ConsumerWidget {
  const PurchaseOrdersPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(purchaseDocumentsProvider);
    final orders = ref.watch(purchaseOrdersProvider);

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(title: const Text('Purchase Orders')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/purchase/orders/new'),
        icon: const Icon(Icons.add),
        label: const Text('New PO'),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (_) {
          if (orders.isEmpty) {
            return const Center(child: Text('No purchase orders yet'));
          }
          return ResponsiveScrollShell(
            dashboard: true,
            child: ListView.separated(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                16,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                100,
              ),
              itemCount: orders.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final doc = orders[index];
                final money = MoneyFormat.inrSpaced;
                final date = doc.updatedAt ?? doc.createdAt;
                return Material(
                  color: AppColors.glassSolid,
                  borderRadius: BorderRadius.circular(14),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(14),
                    onTap: () => context.push('/purchase/orders/${doc.id}'),
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  doc.docNo,
                                  style: const TextStyle(
                                    fontFamily: AppFonts.family,
                                    fontWeight: FontWeight.w800,
                                    color: AppColors.navy,
                                  ),
                                ),
                                const SizedBox(height: 3),
                                Text(
                                  '${doc.vendorName} · ${doc.status.label}'
                                  '${date == null ? '' : ' · ${DateFormat('dd MMM').format(date)}'}',
                                  style: TextStyle(
                                    fontFamily: AppFonts.family,
                                    fontSize: 12.5,
                                    color: AppColors.navy.withValues(alpha: .5),
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Text(
                            money.format(doc.subTotal),
                            style: const TextStyle(
                              fontFamily: AppFonts.family,
                              fontWeight: FontWeight.w800,
                              color: AppColors.primary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

class PurchaseGrnsPage extends ConsumerWidget {
  const PurchaseGrnsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final grns = ref.watch(purchaseGrnsProvider);
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(title: const Text('GRN History')),
      body: grns.isEmpty
          ? const Center(child: Text('No GRNs yet — receive a PO to create one'))
          : ResponsiveScrollShell(
              dashboard: true,
              child: ListView.separated(
                padding: EdgeInsets.fromLTRB(
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  16,
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  32,
                ),
                itemCount: grns.length,
                separatorBuilder: (_, _) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  final doc = grns[index];
                  final money = MoneyFormat.inrSpaced;
                  return Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: AppColors.glassSolid,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                doc.docNo,
                                style: const TextStyle(
                                  fontFamily: AppFonts.family,
                                  fontWeight: FontWeight.w800,
                                  color: AppColors.navy,
                                ),
                              ),
                            ),
                            Text(
                              money.format(doc.subTotal),
                              style: const TextStyle(
                                fontFamily: AppFonts.family,
                                fontWeight: FontWeight.w800,
                                color: AppColors.green,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${doc.vendorName} · Ref ${doc.referenceNo.isEmpty ? '—' : doc.referenceNo}'
                          '${doc.receivedAt == null ? '' : ' · ${DateFormat('dd MMM yyyy HH:mm').format(doc.receivedAt!)}'}',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontSize: 12.5,
                            color: AppColors.navy.withValues(alpha: .55),
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          '${doc.lines.length} items received into stock',
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: AppColors.green,
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
    );
  }
}

class PurchaseOrderFormPage extends ConsumerStatefulWidget {
  const PurchaseOrderFormPage({super.key, this.orderId});

  final String? orderId;

  @override
  ConsumerState<PurchaseOrderFormPage> createState() =>
      PurchaseOrderFormPageState();
}

class PurchaseOrderFormPageState extends ConsumerState<PurchaseOrderFormPage> {
  Vendor? vendor;
  final notes = TextEditingController();
  final reference = TextEditingController();
  final lines = <_EditableLine>[];
  var busy = false;

  bool get isEdit => widget.orderId != null && widget.orderId!.isNotEmpty;

  @override
  void dispose() {
    notes.dispose();
    reference.dispose();
    for (final l in lines) {
      l.dispose();
    }
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    Future.microtask(_hydrate);
  }

  Future<void> _hydrate() async {
    if (!isEdit) return;
    final docs = await ref.read(purchaseDocumentsProvider.future);
    PurchaseDocument? match;
    for (final d in docs) {
      if (d.id == widget.orderId) {
        match = d;
        break;
      }
    }
    if (match == null || !mounted) return;
    final vendors = await ref.read(vendorsProvider.future);
    Vendor? v;
    for (final item in vendors) {
      if (item.id == match.vendorId) {
        v = item;
        break;
      }
    }
    setState(() {
      vendor = v;
      notes.text = match!.notes;
      reference.text = match.referenceNo;
      for (final line in match.lines) {
        lines.add(_EditableLine.fromPurchaseLine(line));
      }
    });
  }

  void addLine(Product product) {
    setState(() {
      lines.add(
        _EditableLine(
          productId: product.productId,
          productName: product.productName,
          unit: product.productUnit ?? '',
          qtyCtrl: TextEditingController(text: '1'),
          costCtrl: TextEditingController(
            text: (product.productPrice).toStringAsFixed(2),
          ),
        ),
      );
    });
  }

  Future<void> pickProduct() async {
    final products = await ref.read(allProductsProvider.future);
    if (!mounted) return;
    if (products.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Sync products from Masters first')),
      );
      return;
    }
    final selected = await showModalBottomSheet<Product>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.glassSolid,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        var query = '';
        return StatefulBuilder(
          builder: (context, setModal) {
            final filtered = products.where((p) {
              if (query.trim().isEmpty) return true;
              return p.productName.toLowerCase().contains(query.toLowerCase());
            }).toList();
            return SafeArea(
              child: SizedBox(
                height: MediaQuery.sizeOf(ctx).height * 0.7,
                child: Column(
                  children: [
                    const Padding(
                      padding: EdgeInsets.fromLTRB(16, 16, 16, 8),
                      child: Text(
                        'Select product',
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          fontWeight: FontWeight.w800,
                          fontSize: 16,
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: TextField(
                        decoration: const InputDecoration(
                          hintText: 'Search…',
                          prefixIcon: Icon(Icons.search),
                        ),
                        onChanged: (v) => setModal(() => query = v),
                      ),
                    ),
                    Expanded(
                      child: ListView.builder(
                        itemCount: filtered.length,
                        itemBuilder: (_, i) {
                          final p = filtered[i];
                          return ListTile(
                            title: Text(p.productName),
                            subtitle: Text(
                              MoneyFormat.inrSpaced.format(p.productPrice),
                            ),
                            onTap: () => Navigator.pop(ctx, p),
                          );
                        },
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
    if (selected != null) addLine(selected);
  }

  Future<void> save() async {
    final v = vendor;
    if (v == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Select a vendor')),
      );
      return;
    }
    final built = <PurchaseLine>[];
    for (final line in lines) {
      final qty = double.tryParse(line.qtyCtrl.text.trim()) ?? 0;
      if (qty <= 0) continue;
      built.add(
        PurchaseLine(
          productId: line.productId,
          productName: line.productName,
          quantity: qty,
          unitCost: double.tryParse(line.costCtrl.text.trim()) ?? 0,
          unit: line.unit,
        ),
      );
    }
    setState(() => busy = true);
    try {
      final saved = await ref.read(purchaseControllerProvider.notifier).savePurchaseOrder(
            id: widget.orderId,
            vendorId: v.id,
            vendorName: v.name,
            lines: built,
            notes: notes.text,
            referenceNo: reference.text,
          );
      if (!mounted) return;
      context.go('/purchase/orders/${saved.id}');
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$e'.replaceFirst('Bad state: ', ''))),
      );
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final vendors = ref.watch(activeVendorsProvider);
    final money = MoneyFormat.inrSpaced;
    var total = 0.0;
    for (final line in lines) {
      final qty = double.tryParse(line.qtyCtrl.text.trim()) ?? 0;
      final cost = double.tryParse(line.costCtrl.text.trim()) ?? 0;
      total += qty * cost;
    }

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(title: Text(isEdit ? 'Edit PO' : 'New Purchase Order')),
      body: ResponsivePageBody(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (vendors.isEmpty)
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppColors.orange.withValues(alpha: .1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    const Expanded(
                      child: Text('Add a vendor before creating a PO.'),
                    ),
                    TextButton(
                      onPressed: () => context.push('/purchase/vendors/new'),
                      child: const Text('Add vendor'),
                    ),
                  ],
                ),
              )
            else
              AppDropdownFormField<Vendor>(
                required: true,
                label: 'Vendor',
                items: vendors,
                itemLabel: (v) => v.name,
                value: vendor,
                enableSearch: true,
                onChanged: (v) => setState(() => vendor = v),
              ),
            const SizedBox(height: 12),
            AppTextField(controller: reference, label: 'Vendor bill / ref no'),
            const SizedBox(height: 12),
            AppTextField(controller: notes, label: 'Notes', maxLines: 2),
            const SizedBox(height: 16),
            Row(
              children: [
                const Expanded(
                  child: Text(
                    'Line items',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w800,
                      fontSize: 15,
                      color: AppColors.navy,
                    ),
                  ),
                ),
                TextButton.icon(
                  onPressed: pickProduct,
                  icon: const Icon(Icons.add),
                  label: const Text('Add product'),
                ),
              ],
            ),
            if (lines.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 20),
                child: Text(
                  'No products added yet.',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    color: AppColors.navy.withValues(alpha: .5),
                  ),
                ),
              )
            else
              for (var i = 0; i < lines.length; i++)
                _LineCard(
                  line: lines[i],
                  onRemove: () => setState(() {
                    lines[i].dispose();
                    lines.removeAt(i);
                  }),
                  onChanged: () => setState(() {}),
                ),
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerRight,
              child: Text(
                'Total ${money.format(total)}',
                style: const TextStyle(
                  fontFamily: AppFonts.family,
                  fontWeight: FontWeight.w800,
                  fontSize: 18,
                  color: AppColors.primary,
                ),
              ),
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: busy ? null : save,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(52),
                backgroundColor: AppColors.primary,
              ),
              child: Text(busy ? 'Saving…' : 'Save purchase order'),
            ),
          ],
        ),
      ),
    );
  }
}

class _EditableLine {
  _EditableLine({
    required this.productId,
    required this.productName,
    required this.unit,
    required this.qtyCtrl,
    required this.costCtrl,
  });

  final int productId;
  final String productName;
  final String unit;
  final TextEditingController qtyCtrl;
  final TextEditingController costCtrl;

  factory _EditableLine.fromPurchaseLine(PurchaseLine line) {
    return _EditableLine(
      productId: line.productId,
      productName: line.productName,
      unit: line.unit,
      qtyCtrl: TextEditingController(text: line.quantity.toString()),
      costCtrl: TextEditingController(text: line.unitCost.toStringAsFixed(2)),
    );
  }

  void dispose() {
    qtyCtrl.dispose();
    costCtrl.dispose();
  }
}

class _LineCard extends StatelessWidget {
  const _LineCard({
    required this.line,
    required this.onRemove,
    required this.onChanged,
  });

  final _EditableLine line;
  final VoidCallback onRemove;
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.glassFill,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  line.productName,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w700,
                    color: AppColors.navy,
                  ),
                ),
              ),
              IconButton(
                onPressed: onRemove,
                icon: const Icon(Icons.delete_outline_rounded, color: AppColors.red),
              ),
            ],
          ),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: line.qtyCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                  decoration: InputDecoration(
                    labelText: 'Qty${line.unit.isEmpty ? '' : ' (${line.unit})'}',
                  ),
                  onChanged: (_) => onChanged(),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: TextField(
                  controller: line.costCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                  decoration: const InputDecoration(labelText: 'Unit cost'),
                  onChanged: (_) => onChanged(),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
