import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/product_image_thumb.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';

const productUnits = ProductUnits.list;

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
  final mrpCtrl = TextEditingController();
  final stockCtrl = TextEditingController();
  final cgstCtrl = TextEditingController();
  final sgstCtrl = TextEditingController();
  final portionPriceCtrl = TextEditingController();

  ProductCategory? productFormPageCategory;
  ProductSubcategory? productFormPageSubcategory;
  PortionMaster? productFormPagePortionMaster;

  /* Stable ids so selection survives async category list reloads. */
  int? selectedCategoryId;
  String? selectedCategoryLabel;
  int? selectedSubcategoryId;
  String unit = productUnits.first;
  bool productFormPageOpenPrice = false;
  bool priceIncludesGst = false;
  bool busy = false;
  bool loaded = false;
  bool addCategorySeeded = false;
  String? productImageValue;
  bool productImageDirty = false;
  final inlinePortions = <({PortionMaster master, double price})>[];

  bool get isEdit => widget.productId != null;

  @override
  void dispose() {
    codeCtrl.dispose();
    nameCtrl.dispose();
    priceCtrl.dispose();
    mrpCtrl.dispose();
    stockCtrl.dispose();
    cgstCtrl.dispose();
    sgstCtrl.dispose();
    portionPriceCtrl.dispose();
    super.dispose();
  }

  void selectCategory(ProductCategory? category) {
    productFormPageCategory = category;
    selectedCategoryId = category?.categoryId;
    selectedCategoryLabel = category?.categoryName;
    productFormPageSubcategory = null;
    selectedSubcategoryId = null;
  }

  void selectSubcategory(ProductSubcategory? subcategory) {
    productFormPageSubcategory = subcategory;
    selectedSubcategoryId = subcategory?.subcategoryId;
  }

  /* Bind category/subcategory objects from stable ids once lists are ready. */
  bool bindCategorySelection(
    List<ProductCategory> categories,
    List<ProductSubcategory> subs,
  ) {
    var changed = false;
    if (selectedCategoryId != null) {
      ProductCategory? match;
      for (final c in categories) {
        if (c.categoryId == selectedCategoryId) {
          match = c;
          break;
        }
      }
      if (match != null) {
        if (productFormPageCategory?.categoryId != match.categoryId) {
          productFormPageCategory = match;
          changed = true;
        }
        selectedCategoryLabel = match.categoryName;
      }
    }
    if (selectedSubcategoryId != null) {
      ProductSubcategory? match;
      for (final s in subs) {
        if (s.subcategoryId == selectedSubcategoryId &&
            (selectedCategoryId == null ||
                s.categoryId == selectedCategoryId)) {
          match = s;
          break;
        }
      }
      if (match != null &&
          productFormPageSubcategory?.subcategoryId != match.subcategoryId) {
        productFormPageSubcategory = match;
        changed = true;
      }
    } else if (productFormPageSubcategory != null) {
      productFormPageSubcategory = null;
      changed = true;
    }
    return changed;
  }

  void hydrate(
    Product product,
    List<ProductCategory> categories,
    List<ProductSubcategory> subs,
  ) {
    if (!loaded) {
      loaded = true;
      codeCtrl.text = product.productCode ?? '';
      nameCtrl.text = product.productName;
      final exclusive = product.productPrice;
      final includes = product.priceIncludesGst == '1';
      final taxPct = product.productCgst + product.productSgst;
      final displayPrice = includes && taxPct > 0
          ? exclusive * (1 + taxPct / 100)
          : exclusive;
      priceCtrl.text = displayPrice.toStringAsFixed(
        displayPrice % 1 == 0 ? 0 : 2,
      );
      mrpCtrl.text = product.productMrp > 0
          ? product.productMrp.toStringAsFixed(
              product.productMrp % 1 == 0 ? 0 : 2,
            )
          : '';
      cgstCtrl.text = amountInputText(product.productCgst);
      sgstCtrl.text = amountInputText(product.productSgst);
      productFormPageOpenPrice = product.openPrice == '1';
      priceIncludesGst = product.priceIncludesGst == '1';
      productImageValue = (product.productImage?.trim().isNotEmpty ?? false)
          ? product.productImage!.trim()
          : null;
      productImageDirty = false;
      unit = ProductUnits.normalize(product.productUnit);
      selectedCategoryId = product.categoryId;
      selectedCategoryLabel = (product.categoryName?.trim().isNotEmpty ?? false)
          ? product.categoryName!.trim()
          : null;
      selectedSubcategoryId = product.subcategoryId;
    }
    bindCategorySelection(categories, subs);
  }

  Future<void> addInlinePortion() async {
    final master = productFormPagePortionMaster;
    if (master == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Select a portion')));
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

  Future<void> pickProductImage(ImageSource source) async {
    final picked = await ImagePicker().pickImage(
      source: source,
      maxWidth: 480,
      maxHeight: 480,
      imageQuality: 70,
    );
    if (picked == null) return;
    final encoded = await encodePickedProductImage(picked);
    if (!mounted) return;
    if (encoded == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Could not use that image (try a smaller photo)'),
        ),
      );
      return;
    }
    setState(() {
      productImageValue = encoded;
      productImageDirty = true;
    });
  }

  Future<void> save() async {
    final name = nameCtrl.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Product name is required')));
      return;
    }
    if (productFormPageCategory == null && selectedCategoryId == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Select a category')));
      return;
    }
    final categoryId =
        productFormPageCategory?.categoryId ?? selectedCategoryId!;
    final categoryName =
        productFormPageCategory?.categoryName ?? selectedCategoryLabel ?? '';
    if (categoryName.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Select a category')));
      return;
    }
    final subcategoryId =
        productFormPageSubcategory?.subcategoryId ?? selectedSubcategoryId;
    final price = productFormPageOpenPrice
        ? 0.0
        : (double.tryParse(priceCtrl.text.trim()) ?? -1);
    if (!productFormPageOpenPrice && price < 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a valid selling price')),
      );
      return;
    }
    final mrp = double.tryParse(mrpCtrl.text.trim()) ?? 0;
    final openingStock = double.tryParse(stockCtrl.text.trim()) ?? 0;
    final cgst = double.tryParse(cgstCtrl.text.trim()) ?? 0;
    final sgst = double.tryParse(sgstCtrl.text.trim()) ?? 0;
    /* Always persist exclusive selling price for billing. */
    var exclusivePrice = price;
    if (!productFormPageOpenPrice && priceIncludesGst) {
      final tax = cgst + sgst;
      if (tax > 0) {
        exclusivePrice = double.parse(
          (price / (1 + tax / 100)).toStringAsFixed(2),
        );
      }
    }

    setState(() => busy = true);
    try {
      final sync = ref.read(mastersSyncControllerProvider.notifier);
      if (isEdit) {
        await sync.updateProduct(
          productId: widget.productId!,
          name: name,
          price: exclusivePrice,
          mrp: mrp,
          categoryId: categoryId,
          categoryName: categoryName,
          productCode: codeCtrl.text.trim().isEmpty
              ? null
              : codeCtrl.text.trim(),
          productImage: productImageDirty ? productImageValue : null,
          clearProductImage: productImageDirty && productImageValue == null,
          openPrice: productFormPageOpenPrice ? '1' : '0',
          priceIncludesGst: priceIncludesGst ? '1' : '0',
          productUnit: unit,
          productCgst: cgst,
          productSgst: sgst,
          subcategoryId: subcategoryId,
        );
      } else {
        final id = await sync.createProduct(
          name: name,
          price: exclusivePrice,
          mrp: mrp,
          categoryId: categoryId,
          categoryName: categoryName,
          productCode: codeCtrl.text.trim().isEmpty
              ? null
              : codeCtrl.text.trim(),
          productImage: productImageValue,
          openPrice: productFormPageOpenPrice ? '1' : '0',
          priceIncludesGst: priceIncludesGst ? '1' : '0',
          productUnit: unit,
          productCgst: cgst,
          productSgst: sgst,
          subcategoryId: subcategoryId,
          portions: [
            for (var i = 0; i < inlinePortions.length; i++)
              (
                portionName: inlinePortions[i].master.portionName,
                portionPrice: inlinePortions[i].price,
                portionSortOrder: i + 1,
                portionMasterId: inlinePortions[i].master.portionMasterId,
              ),
          ],
        );
        final db = ref.read(appDatabaseProvider);
        if (openingStock > 0) {
          await db.addStockIn(
            productId: id,
            productName: name,
            quantity: openingStock,
            movementType: 'opening',
            note: 'Opening stock',
          );
        }
        if (openingStock > 0) {
          await ref
              .read(inventoryControllerProvider.notifier)
              .uploadPendingIfOnline();
        }
      }
      ref.invalidate(catalogCountsProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Product saved')),
      );
      context.pop();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final categories = ref
        .watch(categoriesProvider)
        .maybeWhen(data: (v) => v, orElse: () => const <ProductCategory>[]);
    final allSubs = ref
        .watch(subcategoriesProvider)
        .maybeWhen(data: (v) => v, orElse: () => const <ProductSubcategory>[]);
    final portionMasters = ref
        .watch(portionMastersProvider)
        .maybeWhen(data: (v) => v, orElse: () => const <PortionMaster>[]);

    if (isEdit) {
      final products = ref
          .watch(productsProvider)
          .maybeWhen(data: (v) => v, orElse: () => const <Product>[]);
      Product? existing;
      for (final p in products) {
        if (p.productId == widget.productId) {
          existing = p;
          break;
        }
      }
      if (existing != null) {
        final product = existing;
        final needsHydrate = !loaded;
        final needsBind =
            selectedCategoryId != null &&
            (productFormPageCategory == null ||
                productFormPageCategory!.categoryId != selectedCategoryId) &&
            categories.isNotEmpty;
        if (needsHydrate || needsBind) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (!mounted) return;
            setState(() => hydrate(product, categories, allSubs));
          });
        } else {
          bindCategorySelection(categories, allSubs);
        }
      }
    } else if (!addCategorySeeded &&
        selectedCategoryId == null &&
        categories.isNotEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || addCategorySeeded || selectedCategoryId != null) {
          return;
        }
        setState(() {
          addCategorySeeded = true;
          selectCategory(categories.first);
          productFormPagePortionMaster = portionMasters.isNotEmpty
              ? portionMasters.first
              : null;
        });
      });
    } else {
      bindCategorySelection(categories, allSubs);
    }

    /* Always resolve dropdown value from the live items list by id — never
       silently swap to another category. */
    ProductCategory? category;
    if (selectedCategoryId != null) {
      for (final c in categories) {
        if (c.categoryId == selectedCategoryId) {
          category = c;
          break;
        }
      }
    }
    category ??= productFormPageCategory;
    final selectedLabel = category?.categoryName ?? selectedCategoryLabel;

    final subs = selectedCategoryId == null
        ? const <ProductSubcategory>[]
        : allSubs.where((s) => s.categoryId == selectedCategoryId).toList();
    ProductSubcategory? subcategory;
    if (selectedSubcategoryId != null) {
      for (final s in subs) {
        if (s.subcategoryId == selectedSubcategoryId) {
          subcategory = s;
          break;
        }
      }
    }
    subcategory ??= productFormPageSubcategory;
    final portionMaster =
        productFormPagePortionMaster != null &&
            portionMasters.any(
              (m) =>
                  m.portionMasterId ==
                  productFormPagePortionMaster!.portionMasterId,
            )
        ? portionMasters.firstWhere(
            (m) =>
                m.portionMasterId ==
                productFormPagePortionMaster!.portionMasterId,
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
            28,
          ),
          children: [
            const MasterSectionLabel('Product Category*'),
            const SizedBox(height: 10),
            MasterCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (selectedLabel != null && selectedLabel.isNotEmpty) ...[
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 10,
                      ),
                      decoration: BoxDecoration(
                        color: AppColors.primaryLight.withValues(alpha: .7),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: AppColors.primary.withValues(alpha: .25),
                        ),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.category_rounded,
                            size: 18,
                            color: AppColors.primary,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Category: $selectedLabel',
                              style: const TextStyle(
                                fontFamily: AppFonts.family,
                                fontSize: 14,
                                fontWeight: FontWeight.w700,
                                color: AppColors.navy,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                  ],
                  MasterDropdown<ProductCategory>(
                    required: true,
                    value: category,
                    items: categories,
                    hint: 'Select category',
                    itemLabel: (c) => c.categoryName,
                    onChanged: (v) => setState(() => selectCategory(v)),
                  ),
                  const SizedBox(height: 12),
                  MasterDropdown<ProductSubcategory>(
                    value: subcategory,
                    items: subs,
                    hint: 'None',
                    itemLabel: (s) => s.subcategoryName,
                    onChanged: (v) => setState(() => selectSubcategory(v)),
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
                      onChanged: (v) =>
                          setState(() => productFormPagePortionMaster = v),
                    ),
                    const SizedBox(height: 12),
                    MasterOutlinedField(
                      controller: portionPriceCtrl,
                      hint: 'Selling price for this product + portion',
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
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
                            onPressed: () =>
                                setState(() => inlinePortions.remove(p)),
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
            const MasterSectionLabel('Product Image (Optional)'),
            const SizedBox(height: 10),
            MasterCard(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (hasProductImage(productImageValue))
                    ProductImageThumb(
                      value: productImageValue,
                      size: 72,
                      radius: 12,
                    )
                  else
                    Container(
                      width: 72,
                      height: 72,
                      decoration: BoxDecoration(
                        color: const Color(0xFFF1F5F9),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFFE2E8F2)),
                      ),
                      child: Icon(
                        Icons.image_outlined,
                        color: AppColors.navy.withValues(alpha: .35),
                      ),
                    ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          hasProductImage(productImageValue)
                              ? 'Image ready — shown on billing only for this product'
                              : 'No image — billing cards stay text-only',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontSize: 13,
                            color: AppColors.navy.withValues(alpha: .7),
                          ),
                        ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Expanded(
                              child: OutlinedButton.icon(
                                onPressed: busy
                                    ? null
                                    : () =>
                                          pickProductImage(ImageSource.gallery),
                                icon: const Icon(
                                  Icons.photo_library_outlined,
                                  size: 18,
                                ),
                                label: const Text('Gallery'),
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: OutlinedButton.icon(
                                onPressed: busy
                                    ? null
                                    : () =>
                                          pickProductImage(ImageSource.camera),
                                icon: const Icon(
                                  Icons.photo_camera_outlined,
                                  size: 18,
                                ),
                                label: const Text('Camera'),
                              ),
                            ),
                          ],
                        ),
                        if (hasProductImage(productImageValue))
                          Align(
                            alignment: Alignment.centerLeft,
                            child: TextButton(
                              onPressed: busy
                                  ? null
                                  : () => setState(() {
                                      productImageValue = null;
                                      productImageDirty = true;
                                    }),
                              child: const Text(
                                'Remove',
                                style: TextStyle(color: AppColors.red),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
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
                    required: true,
                    controller: nameCtrl,
                    hint: 'Product Name',
                  ),
                  const SizedBox(height: 8),
                  AppSwitchTile(
                    title: 'Open Price',
                    subtitle:
                        'Price is entered while billing (tap amount on cart)',
                    value: productFormPageOpenPrice,
                    showDivider: false,
                    onChanged: (v) =>
                        setState(() => productFormPageOpenPrice = v),
                  ),
                  if (!productFormPageOpenPrice) ...[
                    const SizedBox(height: 4),
                    AppSwitchTile(
                      title: 'Price includes GST',
                      subtitle: priceIncludesGst
                          ? 'Entered selling price is with GST; system stores exclusive price for billing'
                          : 'Entered selling price is without GST; tax added on bill',
                      value: priceIncludesGst,
                      showDivider: false,
                      onChanged: (v) => setState(() => priceIncludesGst = v),
                    ),
                    const SizedBox(height: 4),
                    MasterOutlinedField(
                      controller: mrpCtrl,
                      hint: 'MRP (optional)',
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                    ),
                    const SizedBox(height: 12),
                    MasterOutlinedField(
                      required: true,
                      controller: priceCtrl,
                      hint: priceIncludesGst
                          ? 'Selling Price (with GST)'
                          : 'Selling Price (without GST)',
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
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
                  if (!isEdit) ...[
                    const SizedBox(height: 12),
                    MasterOutlinedField(
                      controller: stockCtrl,
                      hint: 'Opening Stock (optional)',
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                    ),
                  ],
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
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: MasterOutlinedField(
                      controller: sgstCtrl,
                      hint: 'Product SGST',
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
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
