import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/product_image_thumb.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/portion_picker.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/pos_page.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Fast-billing style catalog to append products onto a saved invoice. */
class InvoiceAddProductsPage extends ConsumerStatefulWidget {
  const InvoiceAddProductsPage({super.key, required this.invoiceId});

  final int invoiceId;

  @override
  ConsumerState<InvoiceAddProductsPage> createState() =>
      InvoiceAddProductsPageState();
}

class InvoiceAddProductsPageState extends ConsumerState<InvoiceAddProductsPage> {
  final searchController = TextEditingController();
  int? selectedCategoryId;
  var query = '';

  @override
  void initState() {
    super.initState();
    searchController.addListener(() {
      setState(() => query = searchController.text.trim().toLowerCase());
    });
  }

  @override
  void dispose() {
    searchController.dispose();
    super.dispose();
  }

  Future<void> onProductTap(Product product) async {
    final added = await addInvoiceProductWithPortionPicker(
      context,
      ref,
      invoiceId: widget.invoiceId,
      product: product,
    );
    if (!added || !mounted) return;
    ref.invalidate(invoiceDetailProvider(widget.invoiceId));
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(AppStrings.of(ref).itemAddedPending)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings.of(ref);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final categoriesAsync = ref.watch(categoriesProvider);
    final db = ref.watch(appDatabaseProvider);
    final widthClass = context.widthClass;
    final cols = widthClass == AppWidthClass.compact ? 2 : 3;

    return Scaffold(
      backgroundColor: const Color(0xFFF7F9FC),
      appBar: AppBar(title: Text(strings.addProduct)),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: searchController,
              textInputAction: TextInputAction.search,
              decoration: InputDecoration(
                filled: true,
                fillColor: Colors.white,
                hintText: 'search product by name, product code',
                hintStyle: TextStyle(
                  color: AppColors.textSecondary.withValues(alpha: 0.85),
                  fontSize: 14,
                ),
                prefixIcon: const Padding(
                  padding: EdgeInsets.all(12),
                  child: AppSvg(
                    AppAssets.svgSearch,
                    width: 20,
                    height: 20,
                    color: AppColors.textSecondary,
                  ),
                ),
                suffixIcon: query.isNotEmpty
                    ? IconButton(
                        onPressed: searchController.clear,
                        icon: const Icon(Icons.close_rounded),
                      )
                    : null,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(28),
                  borderSide: BorderSide(color: AppColors.border),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(28),
                  borderSide: BorderSide(color: AppColors.border),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(28),
                  borderSide: const BorderSide(color: AppColors.primary),
                ),
                contentPadding: const EdgeInsets.symmetric(vertical: 12),
                isDense: true,
              ),
            ),
          ),
          SizedBox(
            height: 40,
            child: categoriesAsync.when(
              data: (categories) {
                return ListView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: CategoryChip(
                        label: 'All',
                        selected: selectedCategoryId == null,
                        onSelected: () =>
                            setState(() => selectedCategoryId = null),
                      ),
                    ),
                    ...categories.map(
                      (category) => Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: CategoryChip(
                          label: category.categoryName,
                          selected: selectedCategoryId == category.categoryId,
                          onSelected: () => setState(
                            () => selectedCategoryId = category.categoryId,
                          ),
                        ),
                      ),
                    ),
                  ],
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (_, _) => const SizedBox.shrink(),
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: StreamBuilder<List<Product>>(
              stream: db.watchActiveProducts(),
              builder: (context, snap) {
                if (snap.connectionState == ConnectionState.waiting &&
                    !snap.hasData) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (snap.hasError) {
                  return Center(child: Text('${snap.error}'));
                }
                final products = snap.data ?? const <Product>[];
                final filtered = products.where((p) {
                  if (selectedCategoryId != null &&
                      p.categoryId != selectedCategoryId) {
                    return false;
                  }
                  if (query.isEmpty) return true;
                  return p.productName.toLowerCase().contains(query) ||
                      (p.productCode?.toLowerCase().contains(query) ?? false);
                }).toList();
                if (filtered.isEmpty) {
                  return AppEmptyState(
                    title: strings.noProductsYet,
                    message: strings.noProductsYet,
                    iconAsset: AppAssets.svgFood,
                  );
                }
                return GridView.builder(
                  padding: EdgeInsets.fromLTRB(
                    AppBreakpoints.pagePaddingFor(widthClass),
                    4,
                    AppBreakpoints.pagePaddingFor(widthClass),
                    24,
                  ),
                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: cols,
                    crossAxisSpacing: 10,
                    mainAxisSpacing: 10,
                    childAspectRatio: 1.15,
                  ),
                  itemCount: filtered.length,
                  itemBuilder: (context, index) {
                    final product = filtered[index];
                    return InvoiceProductTile(
                      product: product,
                      currency: currency,
                      onTap: () => onProductTap(product),
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class InvoiceProductTile extends StatelessWidget {
  const InvoiceProductTile({
    super.key,
    required this.product,
    required this.currency,
    required this.onTap,
  });

  final Product product;
  final NumberFormat currency;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final unit = (product.productUnit?.trim().isNotEmpty ?? false)
        ? product.productUnit!.trim()
        : '';
    final priceLabel = unit.isEmpty
        ? currency.format(product.productPrice)
        : '${currency.format(product.productPrice)}/$unit';
    final showImage = hasProductImage(product.productImage);

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.fromLTRB(10, 8, 10, 8),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: AppColors.primary.withValues(alpha: 0.12),
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (showImage) ...[
                    ProductImageThumb(
                      value: product.productImage,
                      size: 48,
                      radius: 10,
                      showPlaceholder: false,
                    ),
                    const SizedBox(width: 8),
                  ],
                  Expanded(
                    child: Text(
                      product.productName,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 13,
                        color: AppColors.navy,
                        height: 1.15,
                      ),
                    ),
                  ),
                ],
              ),
              const Spacer(),
              Text(
                priceLabel,
                style: const TextStyle(
                  fontWeight: FontWeight.w800,
                  fontSize: 13,
                  color: AppColors.primary,
                ),
              ),
              const SizedBox(height: 6),
              SizedBox(
                height: 34,
                child: FilledButton(
                  onPressed: onTap,
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    padding: EdgeInsets.zero,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                  ),
                  child: const Text(
                    'Add',
                    style: TextStyle(fontWeight: FontWeight.w800),
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

/* Same portion UI as POS, but appends the line onto [invoiceId]. */
Future<bool> addInvoiceProductWithPortionPicker(
  BuildContext context,
  WidgetRef ref, {
  required int invoiceId,
  required Product product,
}) async {
  final portions = await ref
      .read(appDatabaseProvider)
      .getPortionsForProduct(product.productId);
  if (!context.mounted) return false;

  final isOpen = product.openPrice == '1';
  double qty = 1;
  double price = product.productPrice;
  ProductPortion? portion;

  if (portions.isEmpty) {
    if (isOpen) {
      final priceCtrl = TextEditingController(
        text: amountInputText(product.productPrice),
      );
      final qtyCtrl = TextEditingController(
        text: ProductUnits.formatQty(1, unit: product.productUnit),
      );
      final ok = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(product.productName),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              AppTextField(
                required: true,
                controller: priceCtrl,
                label: AppStrings.of(ref).productPrice,
                hint: AppStrings.of(ref).productPrice,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
              ),
              const SizedBox(height: 12),
              AppTextField(
                required: true,
                controller: qtyCtrl,
                label: AppStrings.of(ref).productQuantity,
                hint: AppStrings.of(ref).productQuantity,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text(AppStrings.of(ref).cancel),
            ),
            AppButton(
              label: AppStrings.of(ref).add,
              onPressed: () => Navigator.pop(context, true),
            ),
          ],
        ),
      );
      price = double.tryParse(priceCtrl.text.trim()) ?? 0;
      qty = double.tryParse(qtyCtrl.text.trim()) ?? 1;
      priceCtrl.dispose();
      qtyCtrl.dispose();
      if (ok != true || price <= 0) return false;
    }
  } else {
    final strings = AppStrings.of(ref);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final options = <({String label, double price, ProductPortion portion})>[
      for (final p in portions)
        (
          label: p.portionName.trim().isEmpty
              ? 'Portion'
              : p.portionName.trim(),
          price: p.portionPrice,
          portion: p,
        ),
    ];
    var selectedIndex = 0;
    var pickQty = 1.0;
    final step = ProductUnits.stepFor(product.productUnit);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setLocal) {
          return Dialog(
            insetPadding: const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 24,
            ),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 18, 16, 0),
                  child: Text(
                    product.productName,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 18,
                      color: AppColors.navy,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
                  child: Text(
                    strings.selectPortion,
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      fontSize: 15,
                      color: AppColors.navy,
                    ),
                  ),
                ),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
                  child: Row(
                    children: [
                      for (var i = 0; i < options.length; i++)
                        Padding(
                          padding: const EdgeInsets.only(right: 10),
                          child: ChoiceChip(
                            label: Text(
                              '${options[i].label}\n${currency.format(options[i].price)}',
                              textAlign: TextAlign.center,
                            ),
                            selected: selectedIndex == i,
                            onSelected: (_) =>
                                setLocal(() => selectedIndex = i),
                          ),
                        ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                  child: Row(
                    children: [
                      QtyBox(
                        label: '−',
                        onTap: pickQty <= step
                            ? null
                            : () => setLocal(() {
                                pickQty = double.parse(
                                  (pickQty - step).toStringAsFixed(3),
                                );
                              }),
                      ),
                      Expanded(
                        child: Center(
                          child: Text(
                            ProductUnits.formatQty(
                              pickQty,
                              unit: product.productUnit,
                            ),
                            style: const TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ),
                      QtyBox(
                        label: '+',
                        onTap: () => setLocal(() {
                          pickQty = double.parse(
                            (pickQty + step).toStringAsFixed(3),
                          );
                        }),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: TextButton(
                        onPressed: () => Navigator.pop(context, false),
                        child: Text(strings.dismiss),
                      ),
                    ),
                    Expanded(
                      child: AppButton(
                        label: strings.add,
                        onPressed: () => Navigator.pop(context, true),
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
    if (confirmed != true) return false;
    portion = options[selectedIndex].portion;
    price = options[selectedIndex].price;
    qty = pickQty;
  }

  try {
    await ref.read(appDatabaseProvider).addInvoiceItemLine(
          invoiceId: invoiceId,
          productId: product.productId,
          productName: product.productName,
          productPrice: price,
          quantity: qty <= 0 ? 1 : qty,
          productCode: product.productCode,
          categoryName: product.categoryName,
          cgst: product.productCgst,
          sgst: product.productSgst,
          portionId: portion?.portionId,
          portionName: portion?.portionName,
        );
    return true;
  } catch (e) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
    return false;
  }
}
