import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';

class MastersPage extends ConsumerWidget {
  const MastersPage({super.key, this.initialTab = 0});

  /* 0 = Products, 1 = Combos */
  final int initialTab;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categoriesAsync = ref.watch(categoriesProvider);
    final productsAsync = ref.watch(productsProvider);
    final selectedCategoryId = ref.watch(selectedCategoryIdProvider);
    final syncState = ref.watch(mastersSyncControllerProvider);
    final counts = ref.watch(catalogCountsProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');

    ref.listen(mastersSyncControllerProvider, (prev, next) {
      next.whenOrNull(
        data: (result) {
          if (result == null) return;
          final parts = <String>[];
          if (result.uploadedPending > 0) {
            parts.add('uploaded ${result.uploadedPending} pending');
          }
          if (result.categoryCount > 0 || result.productCount > 0) {
            parts.add(
              'downloaded ${result.categoryCount} categories, '
              '${result.productCount} products, '
              '${result.portionCount} portions'
              '${result.subcategoryCount > 0 ? ', ${result.subcategoryCount} subcategories' : ''}'
              '${result.comboCount > 0 ? ', ${result.comboCount} combos' : ''}'
              '${result.tableCount > 0 ? ', ${result.tableCount} tables' : ''}',
            );
          }
          if (parts.isEmpty) {
            parts.add('Masters sync finished');
          }
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(parts.join(' • '))));
        },
        error: (error, _) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(error.toString())));
        },
      );
    });

    final isSyncing = syncState.isLoading;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Catalog'),
        actions: [
          IconButton(
            tooltip: 'Upload pending',
            onPressed: isSyncing
                ? null
                : () => ref
                      .read(mastersSyncControllerProvider.notifier)
                      .uploadPending(),
            icon: const Icon(Icons.cloud_upload_rounded),
          ),
          PopupMenuButton<String>(
            onSelected: (value) async {
              if (value == 'category') {
                await showAddCategoryDialog(context, ref);
              } else if (value == 'product') {
                await showAddProductDialog(context, ref);
              } else if (value == 'combo') {
                context.push('/masters/combos/form');
              } else if (value == 'subcategories') {
                context.push('/masters/subcategories');
              } else if (value == 'tables') {
                context.push('/masters/tables');
              } else if (value == 'portions') {
                context.push('/masters/portion-masters');
              } else if (value == 'print_catalog') {
                await printCatalog(context, ref);
              }
            },
            itemBuilder: (context) => const [
              PopupMenuItem(value: 'category', child: Text('Add category')),
              PopupMenuItem(value: 'product', child: Text('Add product')),
              PopupMenuItem(value: 'combo', child: Text('Add combo')),
              PopupMenuItem(
                value: 'subcategories',
                child: Text('Subcategories'),
              ),
              PopupMenuItem(value: 'portions', child: Text('Portion masters')),
              PopupMenuItem(value: 'tables', child: Text('Table master')),
              PopupMenuItem(
                value: 'print_catalog',
                child: Text('Print product list'),
              ),
            ],
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: isSyncing ? null : () => showAddProductDialog(context, ref),
        icon: const Icon(Icons.add),
        label: const Text('Add product'),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Row(
              children: [
                Expanded(
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        CountChip(
                          label: 'Categories',
                          value: '${counts.categories}',
                          icon: Icons.category_rounded,
                          color: AppColors.purple,
                        ),
                        const SizedBox(width: 8),
                        CountChip(
                          label: 'Products',
                          value: '${counts.products}',
                          icon: Icons.inventory_2_rounded,
                          color: AppColors.primary,
                        ),
                        const SizedBox(width: 8),
                        CountChip(
                          label: 'Combos',
                          value: '${counts.combos}',
                          icon: Icons.auto_awesome_rounded,
                          color: AppColors.orange,
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          SizedBox(
            height: 48,
            child: categoriesAsync.when(
              data: (categories) {
                if (categories.isEmpty) {
                  return const Center(
                    child: Text('No categories — fetch data from Settings'),
                  );
                }
                return ListView(
                  scrollDirection: Axis.horizontal,
                  padding: EdgeInsets.symmetric(
                    horizontal: AppBreakpoints.pagePaddingFor(
                      context.widthClass,
                    ),
                  ),
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: FilterChip(
                        label: const Text('All'),
                        selected: selectedCategoryId == null,
                        onSelected: (_) => ref
                            .read(selectedCategoryIdProvider.notifier)
                            .select(null),
                      ),
                    ),
                    ...categories.asMap().entries.map((entry) {
                      final index = entry.key;
                      final category = entry.value;
                      return Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: GestureDetector(
                          onLongPress: () async {
                            final action = await showAppBottomSheet<String>(
                              context: context,
                              title: category.categoryName,
                              icon: Icons.category_outlined,
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  ListTile(
                                    contentPadding: EdgeInsets.zero,
                                    leading: const Icon(Icons.edit_outlined),
                                    title: const Text('Edit category'),
                                    onTap: () => Navigator.pop(context, 'edit'),
                                  ),
                                  if (index > 0)
                                    ListTile(
                                      contentPadding: EdgeInsets.zero,
                                      leading: const Icon(
                                        Icons.arrow_back_rounded,
                                      ),
                                      title: const Text('Move earlier'),
                                      onTap: () =>
                                          Navigator.pop(context, 'left'),
                                    ),
                                  if (index < categories.length - 1)
                                    ListTile(
                                      contentPadding: EdgeInsets.zero,
                                      leading: const Icon(
                                        Icons.arrow_forward_rounded,
                                      ),
                                      title: const Text('Move later'),
                                      onTap: () =>
                                          Navigator.pop(context, 'right'),
                                    ),
                                ],
                              ),
                            );
                            if (action == 'edit') {
                              if (!context.mounted) return;
                              await showEditCategoryDialog(
                                context,
                                ref,
                                category,
                              );
                            } else if (action == 'left' || action == 'right') {
                              final swapWith = action == 'left'
                                  ? categories[index - 1]
                                  : categories[index + 1];
                              final aOrder = category.categorySortOrder;
                              final bOrder = swapWith.categorySortOrder;
                              await ref
                                  .read(appDatabaseProvider)
                                  .updateCategorySortOrder(
                                    category.categoryId,
                                    bOrder == aOrder ? aOrder - 1 : bOrder,
                                  );
                              await ref
                                  .read(appDatabaseProvider)
                                  .updateCategorySortOrder(
                                    swapWith.categoryId,
                                    aOrder == bOrder ? bOrder + 1 : aOrder,
                                  );
                            }
                          },
                          child: FilterChip(
                            avatar: category.categorySyncStatus == '0'
                                ? const Icon(Icons.cloud_off, size: 16)
                                : null,
                            label: Text(category.categoryName),
                            selected: selectedCategoryId == category.categoryId,
                            onSelected: (_) => ref
                                .read(selectedCategoryIdProvider.notifier)
                                .select(category.categoryId),
                          ),
                        ),
                      );
                    }),
                  ],
                );
              },
              loading: () => const Center(
                child: SizedBox(
                  width: 24,
                  height: 24,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
              error: (e, _) => Center(child: Text('$e')),
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: DefaultTabController(
              length: 2,
              initialIndex: initialTab.clamp(0, 1),
              child: Column(
                children: [
                  const TabBar(
                    tabs: [
                      Tab(text: 'Products'),
                      Tab(text: 'Combos'),
                    ],
                  ),
                  Expanded(
                    child: TabBarView(
                      children: [
                        productsAsync.when(
                          data: (products) {
                            if (products.isEmpty) {
                              return const Center(
                                child: Text('No products yet'),
                              );
                            }
                            return ResponsiveScrollShell(
                              dashboard: true,
                              child: ListView.separated(
                                padding: EdgeInsets.fromLTRB(
                                  AppBreakpoints.pagePaddingFor(
                                    context.widthClass,
                                  ),
                                  8,
                                  AppBreakpoints.pagePaddingFor(
                                    context.widthClass,
                                  ),
                                  88,
                                ),
                                itemCount: products.length,
                                separatorBuilder: (_, _) =>
                                    const SizedBox(height: 8),
                                itemBuilder: (context, index) {
                                  final product = products[index];
                                  return ProductTile(
                                    product: product,
                                    priceLabel: currency.format(
                                      product.productPrice,
                                    ),
                                    onEdit: () => showEditProductDialog(
                                      context,
                                      ref,
                                      product,
                                    ),
                                    onDelete: () => confirmDeleteProduct(
                                      context,
                                      ref,
                                      product,
                                    ),
                                  );
                                },
                              ),
                            );
                          },
                          loading: () =>
                              const Center(child: CircularProgressIndicator()),
                          error: (e, _) => Center(child: Text('$e')),
                        ),
                        CombosTab(currency: currency),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

Future<void> printCatalog(BuildContext context, WidgetRef ref) async {
  final products = await ref
      .read(appDatabaseProvider)
      .watchActiveProducts()
      .first;
  if (products.isEmpty) {
    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('No products to print')));
    }
    return;
  }
  final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
  final buf = StringBuffer()
    ..writeln('PRODUCT LIST')
    ..writeln(DateFormat('dd MMM yyyy HH:mm').format(DateTime.now()))
    ..writeln('-' * 32);
  for (final p in products) {
    buf.writeln(p.productName);
    buf.writeln('  ${currency.format(p.productPrice)}');
  }
  buf.writeln('-' * 32);
  buf.writeln('Total items: ${products.length}');
  final result = await ref
      .read(printServiceProvider)
      .printRawText(buf.toString(), label: 'Product catalog');
  if (!context.mounted) return;
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text(result.message ?? result.outcome.name)),
  );
}

Future<void> showAddCategoryDialog(BuildContext context, WidgetRef ref) async {
  final controller = TextEditingController();
  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Add Category'),
      content: AppTextField(
        required: true,
        controller: controller,
        label: 'Category name',
      ),
      actions: [
        AppButton(
          label: 'Add Category',
          onPressed: () => Navigator.pop(context, true),
        ),
      ],
    ),
  );
  if (ok == true && controller.text.trim().isNotEmpty) {
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .createCategory(controller.text.trim());
    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Category saved')));
    }
  }
  controller.dispose();
}

const productUnits = [
  'Pcs',
  'Kg',
  'Plate',
  'Glass',
  'Bowl',
  'Packet',
  'Litre',
  'Dozen',
];

Future<void> showAddProductDialog(BuildContext context, WidgetRef ref) async {
  final nameController = TextEditingController();
  final codeController = TextEditingController();
  final priceController = TextEditingController();
  final cgstController = TextEditingController();
  final sgstController = TextEditingController();
  final categories =
      ref
          .read(categoriesProvider)
          .maybeWhen(data: (v) => v, orElse: () => null) ??
      const <ProductCategory>[];
  final allSubcats =
      ref
          .read(subcategoriesProvider)
          .maybeWhen(data: (v) => v, orElse: () => null) ??
      const <ProductSubcategory>[];
  int? categoryId =
      ref.read(selectedCategoryIdProvider) ??
      (categories.isNotEmpty ? categories.first.categoryId : null);
  int? subcategoryId;
  String? productUnit = productUnits.first;
  var openPrice = false;
  final portionMasters =
      ref
          .read(portionMastersProvider)
          .maybeWhen(data: (v) => v, orElse: () => null) ??
      const <PortionMaster>[];
  final inlinePortions = <({PortionMaster master, double price})>[];
  PortionMaster? pendingMaster = portionMasters.isNotEmpty
      ? portionMasters.first
      : null;
  final inlinePortionPriceCtrl = TextEditingController();

  ProductCategory? selectedCategory() {
    if (categoryId == null) return null;
    for (final c in categories) {
      if (c.categoryId == categoryId) return c;
    }
    return null;
  }

  List<ProductSubcategory> subsForCategory() {
    if (categoryId == null) return allSubcats;
    return allSubcats.where((s) => s.categoryId == categoryId).toList();
  }

  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setState) {
        final subs = subsForCategory();
        ProductSubcategory? selectedSub;
        for (final s in subs) {
          if (s.subcategoryId == subcategoryId) {
            selectedSub = s;
            break;
          }
        }
        return AlertDialog(
          title: const Text('Add product'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AppTextField(
                  controller: codeController,
                  label: 'Product code',
                  textCapitalization: TextCapitalization.characters,
                ),
                const SizedBox(height: 12),
                AppTextField(
                  required: true,
                  controller: nameController,
                  label: 'Product name',
                ),
                const SizedBox(height: 12),
                AppSwitchTile(
                  title: 'Open Price',
                  subtitle: openPrice
                      ? 'Price will be entered at billing time'
                      : null,
                  value: openPrice,
                  showDivider: false,
                  onChanged: (v) => setState(() => openPrice = v),
                ),
                if (!openPrice) ...[
                  const SizedBox(height: 12),
                  AppTextField(
                    required: true,
                    controller: priceController,
                    label: 'Price without GST',
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                ],
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: AppTextField(
                        controller: cgstController,
                        label: 'CGST %',
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: AppTextField(
                        controller: sgstController,
                        label: 'SGST %',
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                AppDropdownFormField<ProductCategory>(
                  required: true,
                  label: 'Product Category',
                  items: categories,
                  itemLabel: (c) => c.categoryName,
                  value: selectedCategory(),
                  onChanged: (c) => setState(() {
                    categoryId = c?.categoryId;
                    subcategoryId = null;
                  }),
                ),
                if (subs.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  AppDropdownFormField<ProductSubcategory>(
                    label: 'Subcategory (optional)',
                    items: subs,
                    itemLabel: (s) => s.subcategoryName,
                    value: selectedSub,
                    enableSearch: true,
                    onChanged: (s) =>
                        setState(() => subcategoryId = s?.subcategoryId),
                  ),
                ],
                const SizedBox(height: 12),
                AppDropdownFormField<String>(
                  required: true,
                  label: 'Product Unit',
                  items: productUnits,
                  itemLabel: (u) => u,
                  value: productUnit,
                  onChanged: (u) => setState(() => productUnit = u),
                ),
                if (portionMasters.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  const Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      'Portions (optional)',
                      style: TextStyle(fontWeight: FontWeight.w800),
                    ),
                  ),
                  const SizedBox(height: 8),
                  AppDropdownFormField<PortionMaster>(
                    required: true,
                    label: 'Select Portion',
                    items: portionMasters,
                    itemLabel: (m) => m.portionName,
                    value: pendingMaster,
                    enableSearch: true,
                    onChanged: (m) => setState(() => pendingMaster = m),
                  ),
                  const SizedBox(height: 8),
                  AppTextField(
                    controller: inlinePortionPriceCtrl,
                    label: 'Portion price',
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: AppButton(
                      label: 'Add Portion to Product',
                      expanded: false,
                      variant: AppButtonVariant.outlined,
                      onPressed: () {
                        if (pendingMaster == null) return;
                        final price =
                            double.tryParse(
                              inlinePortionPriceCtrl.text.trim(),
                            ) ??
                            0;
                        setState(() {
                          inlinePortions.add((
                            master: pendingMaster!,
                            price: price,
                          ));
                          inlinePortionPriceCtrl.text = '0';
                        });
                      },
                    ),
                  ),
                  ...inlinePortions.asMap().entries.map((e) {
                    return ListTile(
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      title: Text(e.value.master.portionName),
                      subtitle: Text(e.value.price.toStringAsFixed(2)),
                      trailing: IconButton(
                        icon: const Icon(Icons.close),
                        onPressed: () =>
                            setState(() => inlinePortions.removeAt(e.key)),
                      ),
                    );
                  }),
                ],
              ],
            ),
          ),
          actions: [
            AppButton(
              label: 'Add Product',
              expanded: false,
              onPressed: () => Navigator.pop(context, true),
            ),
          ],
        );
      },
    ),
  );

  if (ok == true && nameController.text.trim().isNotEmpty) {
    final price = double.tryParse(priceController.text.trim()) ?? 0;
    final categoryName = categories
        .where((c) => c.categoryId == categoryId)
        .map((c) => c.categoryName)
        .firstOrNull;
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .createProduct(
          name: nameController.text.trim(),
          price: price,
          categoryId: categoryId,
          categoryName: categoryName,
          productCode: codeController.text.trim(),
          openPrice: openPrice ? '1' : '0',
          productUnit: productUnit,
          productCgst: double.tryParse(cgstController.text.trim()) ?? 0,
          productSgst: double.tryParse(sgstController.text.trim()) ?? 0,
          subcategoryId: subcategoryId,
          portions: [
            for (var i = 0; i < inlinePortions.length; i++)
              (
                portionName: inlinePortions[i].master.portionName,
                portionPrice: inlinePortions[i].price,
                portionSortOrder: i,
                portionMasterId: inlinePortions[i].master.portionMasterId,
              ),
          ],
        );
    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Product saved')));
    }
  }
  nameController.dispose();
  codeController.dispose();
  priceController.dispose();
  cgstController.dispose();
  sgstController.dispose();
  inlinePortionPriceCtrl.dispose();
}

class CountChip extends StatelessWidget {
  const CountChip({
    super.key,
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minWidth: 112),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: color.withValues(alpha: .09),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withValues(alpha: .12)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          AppModuleIcon(icon: icon, color: color, size: 36),
          const SizedBox(width: 8),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                value,
                style: const TextStyle(
                  fontWeight: FontWeight.w900,
                  fontSize: 16,
                  color: AppColors.navy,
                ),
              ),
              Text(
                label,
                style: TextStyle(
                  fontSize: 11,
                  color: AppColors.navy.withValues(alpha: .58),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class ProductTile extends StatelessWidget {
  const ProductTile({
    super.key,
    required this.product,
    required this.priceLabel,
    required this.onEdit,
    required this.onDelete,
  });

  final Product product;
  final String priceLabel;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final pending = product.productSyncStatus == '0';
    return AppCard(
      accentColor: pending ? AppColors.orange : AppColors.primary,
      padding: EdgeInsets.zero,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        onTap: onEdit,
        leading: AppModuleIcon(
          icon: Icons.shopping_bag_rounded,
          color: pending ? AppColors.orange : AppColors.primary,
          size: 50,
        ),
        title: Row(
          children: [
            Expanded(
              child: Text(
                product.productName,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            if (pending)
              Chip(
                visualDensity: VisualDensity.compact,
                label: const Text('Pending'),
                backgroundColor: AppColors.warning.withValues(alpha: 0.2),
              ),
          ],
        ),
        subtitle: Text(
          [
            if (product.categoryName != null &&
                product.categoryName!.trim().isNotEmpty)
              product.categoryName!,
            if (product.productCode != null &&
                product.productCode!.trim().isNotEmpty)
              'Code: ${product.productCode}',
            if (product.productCgst > 0 || product.productSgst > 0)
              'GST ${product.productCgst + product.productSgst}%',
          ].join(' • '),
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              priceLabel,
              style: theme.textTheme.titleMedium?.copyWith(
                color: AppColors.primary,
                fontWeight: FontWeight.w900,
              ),
            ),
            PopupMenuButton<String>(
              onSelected: (v) {
                if (v == 'edit') onEdit();
                if (v == 'delete') onDelete();
              },
              itemBuilder: (context) => const [
                PopupMenuItem(value: 'edit', child: Text('Edit')),
                PopupMenuItem(value: 'delete', child: Text('Delete')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class CombosTab extends ConsumerStatefulWidget {
  const CombosTab({super.key, required this.currency});

  final NumberFormat currency;

  @override
  ConsumerState<CombosTab> createState() => CombosTabState();
}

class CombosTabState extends ConsumerState<CombosTab> {
  final mastersPageSearch = TextEditingController();
  String mastersPageQuery = '';

  @override
  void dispose() {
    mastersPageSearch.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final combosAsync = ref.watch(combosListProvider);
    return combosAsync.when(
      data: (combos) {
        final filtered = combos.where((c) {
          if (mastersPageQuery.trim().isEmpty) return true;
          final q = mastersPageQuery.trim().toLowerCase();
          return c.comboName.toLowerCase().contains(q) ||
              (c.comboCode?.toLowerCase().contains(q) ?? false);
        }).toList();
        if (combos.isEmpty) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Text('No combos yet'),
                const SizedBox(height: 12),
                AppButton(
                  label: 'Add combo',
                  onPressed: () => showAddComboDialog(context, ref),
                ),
              ],
            ),
          );
        }
        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
              child: AppTextField(
                controller: mastersPageSearch,
                label: 'Search combo',
                hint: 'Search combo',
                onChanged: (v) => setState(() => mastersPageQuery = v),
              ),
            ),
            Expanded(
              child: filtered.isEmpty
                  ? const Center(child: Text('No combo found'))
                  : ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 0, 16, 88),
                      itemCount: filtered.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 8),
                      itemBuilder: (context, index) {
                        final combo = filtered[index];
                        final price = combo.comboWithGstPrice > 0
                            ? combo.comboWithGstPrice
                            : combo.comboPrice;
                        return AppCard(
                          accentColor: index.isEven
                              ? AppColors.purple
                              : AppColors.orange,
                          padding: EdgeInsets.zero,
                          child: ListTile(
                            contentPadding: const EdgeInsets.symmetric(
                              horizontal: 14,
                              vertical: 8,
                            ),
                            leading: AppModuleIcon(
                              icon: Icons.auto_awesome_rounded,
                              color: index.isEven
                                  ? AppColors.purple
                                  : AppColors.orange,
                              size: 48,
                            ),
                            onTap: () =>
                                showEditComboDialog(context, ref, combo),
                            title: Text(
                              combo.comboName,
                              style: const TextStyle(
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            subtitle: Text(widget.currency.format(price)),
                            trailing: IconButton(
                              tooltip: 'Delete',
                              onPressed: () async {
                                final ok = await showDialog<bool>(
                                  context: context,
                                  builder: (context) => AlertDialog(
                                    title: const Text('Delete combo'),
                                    content: Text('Remove ${combo.comboName}?'),
                                    actions: [
                                      TextButton(
                                        onPressed: () =>
                                            Navigator.pop(context, false),
                                        child: const Text('Cancel'),
                                      ),
                                      AppButton(
                                        label: 'Delete',
                                        onPressed: () =>
                                            Navigator.pop(context, true),
                                      ),
                                    ],
                                  ),
                                );
                                if (ok == true) {
                                  await ref
                                      .read(
                                        mastersSyncControllerProvider.notifier,
                                      )
                                      .deleteCombo(combo.comboId);
                                  ref.invalidate(catalogCountsProvider);
                                }
                              },
                              icon: const Icon(Icons.delete_outline),
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ],
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text('$e')),
    );
  }
}

Future<void> showEditCategoryDialog(
  BuildContext context,
  WidgetRef ref,
  ProductCategory category,
) async {
  final controller = TextEditingController(text: category.categoryName);
  final action = await showDialog<String>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Edit category'),
      content: AppTextField(
        required: true,
        controller: controller,
        label: 'Category name',
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, 'delete'),
          child: const Text('Delete'),
        ),
        TextButton(
          onPressed: () => Navigator.pop(context, 'cancel'),
          child: const Text('Cancel'),
        ),
        AppButton(
          label: 'Save',
          onPressed: () => Navigator.pop(context, 'save'),
        ),
      ],
    ),
  );
  if (action == 'save' && controller.text.trim().isNotEmpty) {
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .updateCategory(
          categoryId: category.categoryId,
          name: controller.text.trim(),
        );
  } else if (action == 'delete') {
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .deleteCategory(category.categoryId);
    ref.read(selectedCategoryIdProvider.notifier).select(null);
  }
  controller.dispose();
  ref.invalidate(catalogCountsProvider);
}

Future<void> showEditProductDialog(
  BuildContext context,
  WidgetRef ref,
  Product product,
) async {
  final nameController = TextEditingController(text: product.productName);
  final codeController = TextEditingController(text: product.productCode ?? '');
  final priceController = TextEditingController(
    text: amountInputText(product.productPrice),
  );
  final cgstController = TextEditingController(
    text: amountInputText(product.productCgst),
  );
  final sgstController = TextEditingController(
    text: amountInputText(product.productSgst),
  );
  final categories =
      ref
          .read(categoriesProvider)
          .maybeWhen(data: (v) => v, orElse: () => null) ??
      const <ProductCategory>[];
  final allSubcats =
      ref
          .read(subcategoriesProvider)
          .maybeWhen(data: (v) => v, orElse: () => null) ??
      const <ProductSubcategory>[];
  int? categoryId = product.categoryId;
  int? subcategoryId = product.subcategoryId;
  String? productUnit = product.productUnit?.trim().isNotEmpty == true
      ? product.productUnit
      : productUnits.first;
  if (productUnit != null && !productUnits.contains(productUnit)) {
    productUnit = [...productUnits, productUnit].last;
  }
  var openPrice = product.openPrice == '1';

  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setState) {
        final units = productUnit != null && !productUnits.contains(productUnit)
            ? [...productUnits, productUnit!]
            : productUnits;
        final subs = categoryId == null
            ? allSubcats
            : allSubcats.where((s) => s.categoryId == categoryId).toList();
        ProductSubcategory? selectedSub;
        for (final s in subs) {
          if (s.subcategoryId == subcategoryId) {
            selectedSub = s;
            break;
          }
        }
        return AlertDialog(
          title: const Text('Edit product'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AppTextField(
                  controller: codeController,
                  label: 'Product code',
                  textCapitalization: TextCapitalization.characters,
                ),
                const SizedBox(height: 12),
                AppTextField(
                  required: true,
                  controller: nameController,
                  label: 'Product name',
                ),
                const SizedBox(height: 12),
                AppTextField(
                  required: true,
                  controller: priceController,
                  label: 'Price',
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: AppTextField(
                        controller: cgstController,
                        label: 'CGST %',
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: AppTextField(
                        controller: sgstController,
                        label: 'SGST %',
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                AppDropdownFormField<ProductCategory>(
                  required: true,
                  label: 'Product Category',
                  items: categories,
                  itemLabel: (c) => c.categoryName,
                  value: categoryId == null
                      ? null
                      : categories.cast<ProductCategory?>().firstWhere(
                          (c) => c?.categoryId == categoryId,
                          orElse: () => null,
                        ),
                  onChanged: (c) => setState(() {
                    categoryId = c?.categoryId;
                    subcategoryId = null;
                  }),
                ),
                const SizedBox(height: 12),
                AppDropdownFormField<ProductSubcategory>(
                  label: 'Subcategory (optional)',
                  items: subs,
                  itemLabel: (s) => s.subcategoryName,
                  value: selectedSub,
                  enableSearch: true,
                  onChanged: (s) =>
                      setState(() => subcategoryId = s?.subcategoryId),
                ),
                const SizedBox(height: 12),
                AppDropdownFormField<String>(
                  required: true,
                  label: 'Product Unit',
                  items: units,
                  itemLabel: (u) => u,
                  value: productUnit,
                  onChanged: (u) => setState(() => productUnit = u),
                ),
                AppSwitchTile(
                  title: 'Open Price',
                  value: openPrice,
                  showDivider: false,
                  onChanged: (v) => setState(() => openPrice = v),
                ),
                const SizedBox(height: 8),
                AppButton(
                  label: 'Manage portions',
                  icon: Icons.straighten_rounded,
                  variant: AppButtonVariant.outlined,
                  expanded: false,
                  onPressed: () async {
                    Navigator.pop(context, false);
                    await showManagePortionsDialog(context, ref, product);
                  },
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel'),
            ),
            AppButton(
              label: 'Save',
              onPressed: () => Navigator.pop(context, true),
            ),
          ],
        );
      },
    ),
  );

  if (ok == true && nameController.text.trim().isNotEmpty) {
    final price = double.tryParse(priceController.text.trim()) ?? 0;
    final categoryName = categories
        .where((c) => c.categoryId == categoryId)
        .map((c) => c.categoryName)
        .firstOrNull;
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .updateProduct(
          productId: product.productId,
          name: nameController.text.trim(),
          price: price,
          categoryId: categoryId,
          categoryName: categoryName,
          productCode: codeController.text.trim(),
          openPrice: openPrice ? '1' : '0',
          productUnit: productUnit,
          productCgst: double.tryParse(cgstController.text.trim()) ?? 0,
          productSgst: double.tryParse(sgstController.text.trim()) ?? 0,
          subcategoryId: subcategoryId,
        );
    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Product saved')));
    }
  }
  nameController.dispose();
  codeController.dispose();
  priceController.dispose();
  cgstController.dispose();
  sgstController.dispose();
}

Future<void> showManagePortionsDialog(
  BuildContext context,
  WidgetRef ref,
  Product product,
) async {
  final db = ref.read(appDatabaseProvider);
  var portions = await db.getPortionsForProduct(product.productId);
  if (!context.mounted) return;

  await showDialog<void>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setLocal) => AlertDialog(
        title: Text('Portions • ${product.productName}'),
        content: SizedBox(
          width: 360,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (portions.isEmpty)
                const Padding(
                  padding: EdgeInsets.all(12),
                  child: Text('No portions yet'),
                )
              else
                ...portions.map(
                  (p) => ListTile(
                    dense: true,
                    title: Text(p.portionName),
                    subtitle: Text('â‚¹${p.portionPrice.toStringAsFixed(2)}'),
                    trailing: IconButton(
                      icon: const Icon(Icons.delete_outline),
                      onPressed: () async {
                        await ref
                            .read(mastersSyncControllerProvider.notifier)
                            .deletePortion(p.portionId);
                        portions = await db.getPortionsForProduct(
                          product.productId,
                        );
                        setLocal(() {});
                      },
                    ),
                  ),
                ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Close'),
          ),
          AppButton(
            label: 'Add Portion',
            onPressed: () async {
              final masters =
                  ref
                      .read(portionMastersProvider)
                      .maybeWhen(data: (v) => v, orElse: () => null) ??
                  const <PortionMaster>[];
              final priceCtrl = TextEditingController();
              final sortCtrl = TextEditingController();
              PortionMaster? selectedMaster = masters.isNotEmpty
                  ? masters.first
                  : null;
              final add = await showDialog<bool>(
                context: context,
                builder: (context) => StatefulBuilder(
                  builder: (context, setInner) => AlertDialog(
                    title: const Text('Add Portion'),
                    content: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        AppDropdownFormField<PortionMaster>(
                          required: true,
                          label: 'Select Portion',
                          items: masters,
                          itemLabel: (m) => m.portionName,
                          value: selectedMaster,
                          enableSearch: true,
                          onChanged: (m) => setInner(() => selectedMaster = m),
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: priceCtrl,
                          label: 'Portion price',
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: sortCtrl,
                          label: 'Sort order',
                          keyboardType: TextInputType.number,
                        ),
                      ],
                    ),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.pop(context, false),
                        child: const Text('Cancel'),
                      ),
                      AppButton(
                        label: 'Save',
                        expanded: false,
                        onPressed: () => Navigator.pop(context, true),
                      ),
                    ],
                  ),
                ),
              );
              if (add == true && selectedMaster != null) {
                await ref
                    .read(mastersSyncControllerProvider.notifier)
                    .createPortion(
                      productId: product.productId,
                      portionName: selectedMaster!.portionName,
                      portionPrice:
                          double.tryParse(priceCtrl.text.trim()) ?? 0,
                      portionSortOrder:
                          int.tryParse(sortCtrl.text.trim()) ?? 0,
                      portionMasterId: selectedMaster!.portionMasterId,
                    );
                portions = await db.getPortionsForProduct(product.productId);
                setLocal(() {});
              }
              priceCtrl.dispose();
              sortCtrl.dispose();
            },
          ),
        ],
      ),
    ),
  );
}

Future<void> confirmDeleteProduct(
  BuildContext context,
  WidgetRef ref,
  Product product,
) async {
  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Delete product'),
      content: Text('Remove ${product.productName}?'),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, false),
          child: const Text('Cancel'),
        ),
        AppButton(
          label: 'Delete',
          onPressed: () => Navigator.pop(context, true),
        ),
      ],
    ),
  );
  if (ok == true) {
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .deleteProduct(product.productId);
    ref.invalidate(catalogCountsProvider);
  }
}

Future<void> showAddComboDialog(BuildContext context, WidgetRef ref) async {
  final nameController = TextEditingController();
  final codeController = TextEditingController();
  final priceController = TextEditingController();
  final cgstController = TextEditingController();
  final sgstController = TextEditingController();
  final searchController = TextEditingController();
  final products =
      ref
          .read(productsProvider)
          .maybeWhen(data: (v) => v, orElse: () => null) ??
      const <Product>[];
  final allProducts = await ref
      .read(appDatabaseProvider)
      .watchActiveProducts()
      .first;
  final catalog = allProducts.isNotEmpty ? allProducts : products;
  final selected = <int, int>{};
  var activeOnPos = true;
  var query = '';
  var pickingItems = false;

  if (!context.mounted) return;
  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setState) {
        final filtered = catalog.where((p) {
          if (query.trim().isEmpty) return true;
          final q = query.trim().toLowerCase();
          return p.productName.toLowerCase().contains(q) ||
              (p.productCode?.toLowerCase().contains(q) ?? false);
        }).toList();
        final selectedEntries = selected.entries.toList();
        return AlertDialog(
          title: const Text('Add combo'),
          content: SizedBox(
            width: 420,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  AppTextField(
                    controller: codeController,
                    label: 'Combo code',
                    textCapitalization: TextCapitalization.characters,
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    required: true,
                    controller: nameController,
                    label: 'Combo name',
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    required: true,
                    controller: priceController,
                    label: 'Combo selling price',
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: AppTextField(
                          controller: cgstController,
                          label: 'CGST',
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: AppTextField(
                          controller: sgstController,
                          label: 'SGST',
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                        ),
                      ),
                    ],
                  ),
                  AppSwitchTile(
                    title: 'Combo active',
                    value: activeOnPos,
                    showDivider: false,
                    onChanged: (v) => setState(() => activeOnPos = v),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Combo items',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: AppButton(
                      label: pickingItems ? 'Done adding' : 'Add combo item',
                      onPressed: () =>
                          setState(() => pickingItems = !pickingItems),
                    ),
                  ),
                  if (selectedEntries.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    ...selectedEntries.map((e) {
                      Product? product;
                      for (final p in catalog) {
                        if (p.productId == e.key) {
                          product = p;
                          break;
                        }
                      }
                      return ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text(product?.productName ?? 'Product ${e.key}'),
                        subtitle: Text('Qty: ${e.value}'),
                        trailing: IconButton(
                          icon: const Icon(Icons.delete_outline),
                          onPressed: () =>
                              setState(() => selected.remove(e.key)),
                        ),
                      );
                    }),
                  ],
                  if (pickingItems) ...[
                    const SizedBox(height: 8),
                    AppTextField(
                      controller: searchController,
                      label: 'Search product',
                      hint: 'search product by name, product code',
                      onChanged: (v) => setState(() => query = v),
                    ),
                    const SizedBox(height: 8),
                    if (catalog.isEmpty)
                      const Text('No products — sync Masters first.')
                    else if (filtered.isEmpty)
                      const Text('No product found. Please add new product.')
                    else
                      ...filtered.take(40).map((p) {
                        final qty = selected[p.productId] ?? 0;
                        return CheckboxListTile(
                          dense: true,
                          value: qty > 0,
                          title: Text(p.productName),
                          subtitle: qty > 0 ? Text('Qty: $qty') : null,
                          secondary: qty > 0
                              ? Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    IconButton(
                                      icon: const Icon(Icons.remove),
                                      onPressed: () => setState(() {
                                        final next = qty - 1;
                                        if (next <= 0) {
                                          selected.remove(p.productId);
                                        } else {
                                          selected[p.productId] = next;
                                        }
                                      }),
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.add),
                                      onPressed: () => setState(() {
                                        selected[p.productId] = qty + 1;
                                      }),
                                    ),
                                  ],
                                )
                              : null,
                          onChanged: (on) => setState(() {
                            if (on == true) {
                              selected[p.productId] = 1;
                            } else {
                              selected.remove(p.productId);
                            }
                          }),
                        );
                      }),
                  ],
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel'),
            ),
            AppButton(
              label: 'Save Combo',
              onPressed: () => Navigator.pop(context, true),
            ),
          ],
        );
      },
    ),
  );
  if (ok == true && nameController.text.trim().isNotEmpty) {
    final price = double.tryParse(priceController.text.trim()) ?? 0;
    final items = selected.entries
        .map((e) => (productId: e.key, quantity: e.value))
        .toList();
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .createCombo(
          name: nameController.text.trim(),
          price: price,
          comboCode: codeController.text.trim(),
          comboCgst: double.tryParse(cgstController.text.trim()) ?? 0,
          comboSgst: double.tryParse(sgstController.text.trim()) ?? 0,
          activeOnPos: activeOnPos,
          items: items,
        );
    ref.invalidate(catalogCountsProvider);
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Combo saved'),
        ),
      );
    }
  }
  nameController.dispose();
  codeController.dispose();
  priceController.dispose();
  cgstController.dispose();
  sgstController.dispose();
  searchController.dispose();
}

Future<void> showEditComboDialog(
  BuildContext context,
  WidgetRef ref,
  Combo combo,
) async {
  final nameController = TextEditingController(text: combo.comboName);
  final codeController = TextEditingController(text: combo.comboCode ?? '');
  final priceController = TextEditingController(
    text: amountInputText(
      combo.comboWithGstPrice > 0 ? combo.comboWithGstPrice : combo.comboPrice,
    ),
  );
  final cgstController = TextEditingController(
    text: amountInputText(combo.comboCgst),
  );
  final sgstController = TextEditingController(
    text: amountInputText(combo.comboSgst),
  );
  final searchController = TextEditingController();
  final allProducts = await ref
      .read(appDatabaseProvider)
      .watchActiveProducts()
      .first;
  final existing = await ref
      .read(appDatabaseProvider)
      .getComboItemsForCombo(combo.comboId);
  final selected = <int, int>{
    for (final item in existing)
      if (item.productId != null && item.comboItemQuantity > 0)
        item.productId!: item.comboItemQuantity,
  };
  var activeOnPos = combo.comboActiveStatus != '0';
  var query = '';
  var pickingItems = false;

  if (!context.mounted) return;
  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setState) {
        final filtered = allProducts.where((p) {
          if (query.trim().isEmpty) return true;
          final q = query.trim().toLowerCase();
          return p.productName.toLowerCase().contains(q) ||
              (p.productCode?.toLowerCase().contains(q) ?? false);
        }).toList();
        final selectedEntries = selected.entries.toList();
        return AlertDialog(
          title: const Text('Edit combo'),
          content: SizedBox(
            width: 420,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  AppTextField(
                    controller: codeController,
                    label: 'Combo code',
                    textCapitalization: TextCapitalization.characters,
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    required: true,
                    controller: nameController,
                    label: 'Combo name',
                  ),
                  const SizedBox(height: 12),
                  AppTextField(
                    required: true,
                    controller: priceController,
                    label: 'Combo selling price',
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: AppTextField(
                          controller: cgstController,
                          label: 'CGST',
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: AppTextField(
                          controller: sgstController,
                          label: 'SGST',
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                        ),
                      ),
                    ],
                  ),
                  AppSwitchTile(
                    title: 'Combo active',
                    value: activeOnPos,
                    showDivider: false,
                    onChanged: (v) => setState(() => activeOnPos = v),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Combo items',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: AppButton(
                      label: pickingItems ? 'Done adding' : 'Add combo item',
                      onPressed: () =>
                          setState(() => pickingItems = !pickingItems),
                    ),
                  ),
                  if (selectedEntries.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    ...selectedEntries.map((e) {
                      Product? product;
                      for (final p in allProducts) {
                        if (p.productId == e.key) {
                          product = p;
                          break;
                        }
                      }
                      return ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text(product?.productName ?? 'Product ${e.key}'),
                        subtitle: Text('Qty: ${e.value}'),
                        trailing: IconButton(
                          icon: const Icon(Icons.delete_outline),
                          onPressed: () =>
                              setState(() => selected.remove(e.key)),
                        ),
                      );
                    }),
                  ],
                  if (pickingItems) ...[
                    const SizedBox(height: 8),
                    AppTextField(
                      controller: searchController,
                      label: 'Search product',
                      hint: 'search product by name, product code',
                      onChanged: (v) => setState(() => query = v),
                    ),
                    const SizedBox(height: 8),
                    if (allProducts.isEmpty)
                      const Text('No products — sync Masters first.')
                    else if (filtered.isEmpty)
                      const Text('No product found. Please add new product.')
                    else
                      ...filtered.take(40).map((p) {
                        final qty = selected[p.productId] ?? 0;
                        return CheckboxListTile(
                          dense: true,
                          value: qty > 0,
                          title: Text(p.productName),
                          subtitle: qty > 0 ? Text('Qty: $qty') : null,
                          secondary: qty > 0
                              ? Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    IconButton(
                                      icon: const Icon(Icons.remove),
                                      onPressed: () => setState(() {
                                        final next = qty - 1;
                                        if (next <= 0) {
                                          selected.remove(p.productId);
                                        } else {
                                          selected[p.productId] = next;
                                        }
                                      }),
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.add),
                                      onPressed: () => setState(() {
                                        selected[p.productId] = qty + 1;
                                      }),
                                    ),
                                  ],
                                )
                              : null,
                          onChanged: (checked) => setState(() {
                            if (checked == true) {
                              selected[p.productId] = 1;
                            } else {
                              selected.remove(p.productId);
                            }
                          }),
                        );
                      }),
                  ],
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel'),
            ),
            AppButton(
              label: 'Save Combo',
              onPressed: () => Navigator.pop(context, true),
            ),
          ],
        );
      },
    ),
  );
  if (ok == true && nameController.text.trim().isNotEmpty) {
    final price = double.tryParse(priceController.text.trim()) ?? 0;
    final items = selected.entries
        .map((e) => (productId: e.key, quantity: e.value))
        .toList();
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .updateCombo(
          comboId: combo.comboId,
          name: nameController.text.trim(),
          price: price,
          comboCode: codeController.text.trim(),
          comboCgst: double.tryParse(cgstController.text.trim()) ?? 0,
          comboSgst: double.tryParse(sgstController.text.trim()) ?? 0,
          activeOnPos: activeOnPos,
          items: items,
        );
    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Combo saved')));
    }
  }
  nameController.dispose();
  codeController.dispose();
  priceController.dispose();
  cgstController.dispose();
  sgstController.dispose();
  searchController.dispose();
}
