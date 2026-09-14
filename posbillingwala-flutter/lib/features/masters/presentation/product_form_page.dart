import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

const productUnits = [
  'Pcs',
  'Kg',
  'Plate',
  'Glass',
  'Bowl',
  'Packet',
  'Litre',
  'Dozen',
  'GRAM',
];

class ProductFormPage extends ConsumerStatefulWidget {
  const ProductFormPage({super.key, this.productId});

  final int? productId;

  @override
  ConsumerState<ProductFormPage> createState() => ProductFormPageState();
}

class ProductFormPageState extends ConsumerState<ProductFormPage> {
  final codeCtrl = TextEditingController();
  final nameCtrl = TextEditingController();
  final priceCtrl = TextEditingController();
  final cgstCtrl = TextEditingController(text: '0');
  final sgstCtrl = TextEditingController(text: '0');
  final portionPriceCtrl = TextEditingController();

  ProductCategory? productFormPageCategory;
  ProductSubcategory? productFormPageSubcategory;
  PortionMaster? productFormPagePortionMaster;
  String unit = productUnits.first;
  bool productFormPageOpenPrice = false;
  bool busy = false;
  bool loaded = false;
  final inlinePortions = <({PortionMaster master, double price})>[];

  bool get isEdit => widget.productId != null;

  @override
  void dispose() {
    codeCtrl.dispose();
    nameCtrl.dispose();
    priceCtrl.dispose();
    cgstCtrl.dispose();
    sgstCtrl.dispose();
    portionPriceCtrl.dispose();
    super.dispose();
  }

  void hydrate(Product product, List<ProductCategory> categories,
      List<ProductSubcategory> subs) {
    if (loaded) return;
    loaded = true;
    codeCtrl.text = product.productCode ?? '';
    nameCtrl.text = product.productName;
    priceCtrl.text = product.productPrice.toStringAsFixed(
      product.productPrice % 1 == 0 ? 0 : 2,
    );
    cgstCtrl.text = product.productCgst.toStringAsFixed(
      product.productCgst % 1 == 0 ? 0 : 1,
    );
    sgstCtrl.text = product.productSgst.toStringAsFixed(
      product.productSgst % 1 == 0 ? 0 : 1,
    );
    productFormPageOpenPrice = product.openPrice == '1';
    unit = (product.productUnit ?? '').trim().isEmpty
        ? productUnits.first
        : product.productUnit!;
    if (!productUnits.contains(unit)) {
      unit = productUnits.first;
    }
    for (final c in categories) {
      if (c.categoryId == product.categoryId) {
        productFormPageCategory = c;
        break;
      }
    }
    for (final s in subs) {
      if (s.subcategoryId == product.subcategoryId) {
        productFormPageSubcategory = s;
        break;
      }
    }
  }

  Future<void> addInlinePortion() async {
    final master = productFormPagePortionMaster;
    if (master == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Select a portion')),
      );
      return;
    }
    final price = double.tryParse(portionPriceCtrl.text.trim());
    if (price == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter selling price for portion')),
      );
      return;
    }
    setState(() {
      inlinePortions.removeWhere(
        (e) => e.master.portionMasterId == master.portionMasterId,
      );
      inlinePortions.add((master: master, price: price));
      portionPriceCtrl.clear();
    });
  }

  Future<void> save() async {
    final name = nameCtrl.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Product name is required')),
      );
      return;
    }
    if (productFormPageCategory == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Select a category')),
      );
      return;
    }
    final price = productFormPageOpenPrice
        ? 0.0
        : (double.tryParse(priceCtrl.text.trim()) ?? -1);
    if (!productFormPageOpenPrice && price < 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a valid product price')),
      );
      return;
    }

    setState(() => busy = true);
    try {
      final sync = ref.read(mastersSyncControllerProvider.notifier);
      if (isEdit) {
        await sync.updateProduct(
          productId: widget.productId!,
          name: name,
          price: price,
          categoryId: productFormPageCategory!.categoryId,
          categoryName: productFormPageCategory!.categoryName,
          productCode: codeCtrl.text.trim().isEmpty
              ? null
              : codeCtrl.text.trim(),
          openPrice: productFormPageOpenPrice ? '1' : '0',
          productUnit: unit,
          productCgst: double.tryParse(cgstCtrl.text.trim()) ?? 0,
          productSgst: double.tryParse(sgstCtrl.text.trim()) ?? 0,
          subcategoryId: productFormPageSubcategory?.subcategoryId,
        );
      } else {
        final id = await sync.createProduct(
          name: name,
          price: price,
          categoryId: productFormPageCategory!.categoryId,
          categoryName: productFormPageCategory!.categoryName,
          productCode: codeCtrl.text.trim().isEmpty
              ? null
              : codeCtrl.text.trim(),
          openPrice: productFormPageOpenPrice ? '1' : '0',
          productUnit: unit,
          productCgst: double.tryParse(cgstCtrl.text.trim()) ?? 0,
          productSgst: double.tryParse(sgstCtrl.text.trim()) ?? 0,
          subcategoryId: productFormPageSubcategory?.subcategoryId,
        );
        final db = ref.read(appDatabaseProvider);
        var sort = 1;
        for (final portion in inlinePortions) {
          await db.insertLocalPortion(
            productId: id,
            portionName: portion.master.portionName,
            portionPrice: portion.price,
            portionSortOrder: sort++,
            portionMasterId: portion.master.portionMasterId,
          );
        }
      }
      ref.invalidate(catalogCountsProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(isEdit ? 'Product updated' : 'Product saved'),
        ),
      );
      context.pop();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final categories = ref.watch(categoriesProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <ProductCategory>[],
        );
    final allSubs = ref.watch(subcategoriesProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <ProductSubcategory>[],
        );
    final portionMasters = ref.watch(portionMastersProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <PortionMaster>[],
        );

    if (isEdit) {
      final products = ref.watch(productsProvider).maybeWhen(
            data: (v) => v,
            orElse: () => const <Product>[],
          );
      Product? existing;
      for (final p in products) {
        if (p.productId == widget.productId) {
          existing = p;
          break;
        }
      }
      if (existing != null) {
        hydrate(existing, categories, allSubs);
      }
    } else if (productFormPageCategory == null && categories.isNotEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || productFormPageCategory != null) return;
        setState(() {
          productFormPageCategory = categories.first;
          productFormPagePortionMaster =
              portionMasters.isNotEmpty ? portionMasters.first : null;
        });
      });
    }

    final category = productFormPageCategory != null &&
            categories.any((c) => c.categoryId == productFormPageCategory!.categoryId)
        ? categories.firstWhere((c) => c.categoryId == productFormPageCategory!.categoryId)
        : (categories.isNotEmpty ? categories.first : null);
    final subs = category == null
        ? const <ProductSubcategory>[]
        : allSubs.where((s) => s.categoryId == category.categoryId).toList();
    final subcategory = productFormPageSubcategory != null &&
            subs.any((s) => s.subcategoryId == productFormPageSubcategory!.subcategoryId)
        ? subs.firstWhere((s) => s.subcategoryId == productFormPageSubcategory!.subcategoryId)
        : null;
    final portionMaster = productFormPagePortionMaster != null &&
            portionMasters.any(
              (m) => m.portionMasterId == productFormPagePortionMaster!.portionMasterId,
            )
        ? portionMasters.firstWhere(
            (m) => m.portionMasterId == productFormPagePortionMaster!.portionMasterId,
          )
        : (portionMasters.isNotEmpty ? portionMasters.first : null);

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: Text(isEdit ? 'Edit Product' : 'Add Product'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
        padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            28),
        children: [
          const MasterSectionLabel('Product Category*'),
          const SizedBox(height: 10),
          MasterCard(
            child: Column(
              children: [
                MasterDropdown<ProductCategory>(
                  value: category,
                  items: categories,
                  hint: 'Select category',
                  itemLabel: (c) => c.categoryName,
                  onChanged: (v) => setState(() {
                    productFormPageCategory = v;
                    productFormPageSubcategory = null;
                  }),
                ),
                const SizedBox(height: 12),
                MasterDropdown<ProductSubcategory>(
                  value: subcategory,
                  items: subs,
                  hint: 'None',
                  itemLabel: (s) => s.subcategoryName,
                  onChanged: (v) => setState(() => productFormPageSubcategory = v),
                ),
              ],
            ),
          ),
          if (!isEdit) ...[
            const SizedBox(height: 18),
            MasterSectionLabel(
              'Portions (Optional)',
              trailing: MasterLinkButton(
                label: 'Manage Portion Master',
                onTap: () => context.push('/masters/portion-masters'),
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Product price is used when no portions are added.',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 12,
                color: AppColors.navy.withValues(alpha: .45),
              ),
            ),
            const SizedBox(height: 10),
            MasterCard(
              child: Column(
                children: [
                  MasterDropdown<PortionMaster>(
                    value: portionMaster,
                    items: portionMasters,
                    hint: 'Select portion',
                    itemLabel: (m) => m.portionName,
                    onChanged: (v) => setState(() => productFormPagePortionMaster = v),
                  ),
                  const SizedBox(height: 12),
                  MasterOutlinedField(
                    controller: portionPriceCtrl,
                    hint: 'Selling price for this product + portion',
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                  ),
                  const SizedBox(height: 12),
                  MasterPrimaryButton(
                    label: 'Add Portion to Product',
                    onPressed: addInlinePortion,
                  ),
                  if (inlinePortions.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    for (final p in inlinePortions)
                      ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text(p.master.portionName),
                        subtitle: Text('₹ ${p.price.toStringAsFixed(2)}'),
                        trailing: IconButton(
                          icon: const Icon(
                            Icons.close_rounded,
                            color: AppColors.red,
                          ),
                          onPressed: () => setState(
                            () => inlinePortions.remove(p),
                          ),
                        ),
                      ),
                  ],
                ],
              ),
            ),
          ] else ...[
            const SizedBox(height: 14),
            MasterPrimaryButton(
              label: 'Manage Portions',
              onPressed: () => context.push(
                '/masters/products/portions?id=${widget.productId}',
              ),
            ),
          ],
          const SizedBox(height: 18),
          const MasterSectionLabel('Product Details'),
          const SizedBox(height: 10),
          MasterCard(
            child: Column(
              children: [
                MasterOutlinedField(
                  controller: codeCtrl,
                  hint: 'Product Code',
                  textCapitalization: TextCapitalization.characters,
                ),
                const SizedBox(height: 12),
                MasterOutlinedField(
                  controller: nameCtrl,
                  hint: 'Product Name',
                ),
                const SizedBox(height: 8),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text(
                    'Open Price',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w600,
                      fontSize: 14.5,
                      color: AppColors.navy,
                    ),
                  ),
                  subtitle: Text(
                    'Price is entered while billing (tap amount on cart)',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontSize: 12,
                      color: AppColors.navy.withValues(alpha: .45),
                    ),
                  ),
                  value: productFormPageOpenPrice,
                  activeThumbColor: AppColors.primary,
                  onChanged: (v) => setState(() => productFormPageOpenPrice = v),
                ),
                if (!productFormPageOpenPrice) ...[
                  const SizedBox(height: 4),
                  MasterOutlinedField(
                    controller: priceCtrl,
                    hint: 'Product Price (without GST)',
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                  ),
                ],
                const SizedBox(height: 12),
                MasterDropdown<String>(
                  value: unit,
                  items: productUnits,
                  itemLabel: (u) => u,
                  onChanged: (v) {
                    if (v != null) setState(() => unit = v);
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          const MasterSectionLabel('Tax / GST'),
          const SizedBox(height: 10),
          MasterCard(
            child: Row(
              children: [
                Expanded(
                  child: MasterOutlinedField(
                    controller: cgstCtrl,
                    hint: 'Product CGST',
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: MasterOutlinedField(
                    controller: sgstCtrl,
                    hint: 'Product SGST',
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 22),
          MasterPrimaryButton(
            label: isEdit ? 'Save Product' : 'Add Product',
            isLoading: busy,
            onPressed: busy ? null : save,
          ),
        ],
      ),
      ),
    );
  }
}
