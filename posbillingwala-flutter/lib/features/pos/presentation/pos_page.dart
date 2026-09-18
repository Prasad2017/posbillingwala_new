import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/product_image_thumb.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/kot_providers.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/kot_preview_page.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/portion_picker.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/tables/presentation/table_ops.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

Future<void> sendKotTicket(BuildContext context, WidgetRef ref) async {
  try {
    if (!ref.read(permissionControllerProvider).allows('kot.print')) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).moduleLocked)));
      return;
    }
    final ticket = await ref.read(kotControllerProvider.notifier).createKot();
    if (!context.mounted) return;
    final settings = ref.read(printerSettingsProvider);
    final strings = AppStrings.of(ref);
    final skipPreview = settings.kotAutoPrint || !settings.kotPreview;
    if (skipPreview) {
      final copies = settings.kotCopies.clamp(1, 5);
      PrintResult? last;
      for (var i = 0; i < copies; i++) {
        last = await PrintJobDispatcher(ref).printKotRouted(ticket);
      }
      await ref
          .read(kotControllerProvider.notifier)
          .markPrinted(ticket.kot.kotId);
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            last?.message ??
                '${strings.kotPrinted}${copies > 1 ? ' ×$copies' : ''}',
          ),
        ),
      );
      return;
    }
    await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => KotPreviewPage(ticket: ticket)),
    );
  } catch (e) {
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
  }
}

class PosPage extends ConsumerStatefulWidget {
  const PosPage({
    super.key,
    this.resetSessionOnOpen = false,
    this.openCartOnStart = false,
  });

  final bool resetSessionOnOpen;
  /* Kept for route compatibility (`?cart=1`); cart sheet removed — qty on product cards. */
  final bool openCartOnStart;

  @override
  ConsumerState<PosPage> createState() => PosPageState();
}

class PosPageState extends ConsumerState<PosPage> {
  final posPageSearchController = TextEditingController();
  bool posPageShowCombos = false;

  @override
  void initState() {
    super.initState();
    if (widget.resetSessionOnOpen) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.read(billingSessionProvider.notifier).usePos();
      });
    }
  }

  @override
  void dispose() {
    posPageSearchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(billingSessionProvider);
    final categoriesAsync = ref.watch(categoriesProvider);
    final productsAsync = ref.watch(posProductsProvider);
    final cartAsync = ref.watch(cartItemsProvider);
    final cartSummary = ref.watch(cartSummaryProvider);
    final selectedCategoryId = ref.watch(posSelectedCategoryIdProvider);
    final selectedSubcategoryId = ref.watch(posSelectedSubcategoryIdProvider);
    final currency = MoneyFormat.inr;
    final showSideCart = context.showPosSideCart;
    final persistentCart = context.showPosPersistentCart;

    final unprintedCount = ref.watch(unprintedCartCountProvider);
    final isTable = session.invoiceType == 'table_wise';
    final kotEnabled = ref.watch(printerSettingsProvider).kotEnable;

    final isFastBilling = session.invoiceType == 'fast_billing';
    final strings = AppStrings.of(ref);

    return Scaffold(
      backgroundColor: const Color(0xFFF3F6FB),
      appBar: AppBar(
        backgroundColor: isFastBilling ? Colors.white : AppColors.primary,
        foregroundColor: isFastBilling ? AppColors.navy : Colors.white,
        surfaceTintColor: Colors.transparent,
        elevation: isFastBilling ? 0 : null,
        iconTheme: IconThemeData(
          color: isFastBilling ? AppColors.navy : Colors.white,
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              isFastBilling
                  ? strings.fastBilling
                  : session.invoiceType == 'take_away'
                  ? strings.takeAway
                  : session.invoiceType == 'table_wise'
                  ? strings.dineIn
                  : session.title,
              style: TextStyle(
                color: isFastBilling ? AppColors.navy : Colors.white,
                fontWeight: FontWeight.w800,
              ),
            ),
            Text(
              isFastBilling
                  ? strings.productMenu
                  : session.invoiceType == 'take_away'
                  ? (session.customerName?.trim().isNotEmpty == true
                        ? session.customerName!
                        : strings.addProducts)
                  : session.tableNumber != null
                  ? '${strings.tableNo} ${session.tableNumber}'
                  : (session.customerName ?? ''),
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w400,
                color: isFastBilling ? AppColors.textSecondary : Colors.white70,
              ),
            ),
          ],
        ),
        actions: [
          if (!cartSummary.isEmpty)
            Padding(
              padding: const EdgeInsets.only(right: 4),
              child: TextButton.icon(
                onPressed: () => confirmClearCart(context, ref),
                icon: const Icon(
                  Icons.delete_outline_rounded,
                  color: AppColors.danger,
                  size: 18,
                ),
                label: Text(
                  strings.clearCart,
                  style: const TextStyle(
                    color: AppColors.danger,
                    fontWeight: FontWeight.w700,
                    fontSize: 13,
                  ),
                ),
                style: TextButton.styleFrom(
                  backgroundColor: const Color(0xFFFFE8EA),
                  foregroundColor: AppColors.danger,
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  visualDensity: VisualDensity.compact,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
              ),
            ),
          PopupMenuButton<String>(
            icon: Icon(
              Icons.more_vert_rounded,
              color: isFastBilling ? AppColors.navy : Colors.white,
            ),
            onSelected: (value) async {
              if (value == 'duplicate') {
                await printLatestInvoiceDuplicate(
                  context,
                  ref,
                  tableNumber: isTable ? session.tableNumber : null,
                  invoiceType: isTable ? null : session.invoiceType,
                );
                return;
              }
              if (value == 'table_ops') {
                final floor = floorForTable(ref, session.tableNumber);
                if (floor == null) {
                  if (!context.mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(strings.openTableFirst)),
                  );
                  return;
                }
                await showPosTableOverflow(context, ref, floor);
              }
            },
            itemBuilder: (context) => [
              PopupMenuItem(
                value: 'duplicate',
                child: Text(strings.duplicatePrint),
              ),
              if (isTable)
                PopupMenuItem(
                  value: 'table_ops',
                  child: Text(strings.tableActions),
                ),
            ],
          ),
        ],
      ),
      body: SafeArea(
        child: Builder(
          builder: (context) {
            final catalog = CatalogPane(
              session: session,
              categoriesAsync: categoriesAsync,
              productsAsync: productsAsync,
              selectedCategoryId: selectedCategoryId,
              selectedSubcategoryId: selectedSubcategoryId,
              currency: currency,
              searchController: posPageSearchController,
              showCombos: posPageShowCombos,
              onToggleCombos: (v) => setState(() => posPageShowCombos = v),
            );
            final cart = CartPane(
              session: session,
              cartAsync: cartAsync,
              currency: currency,
            );
            if (showSideCart) {
              return Row(
                children: [
                  Expanded(flex: 3, child: catalog),
                  SizedBox(
                    width: AppBreakpoints.posSideCartWidth(context.widthClass),
                    child: cart,
                  ),
                ],
              );
            }
            if (persistentCart) {
              return Column(
                children: [
                  Expanded(flex: 11, child: catalog),
                  Expanded(flex: 9, child: cart),
                ],
              );
            }
            return Column(
              children: [
                Expanded(child: catalog),
                if (isTable)
                  DineInFooter(
                    summary: cartSummary,
                    currency: currency,
                    unprintedCount: unprintedCount,
                    kotEnabled: kotEnabled,
                    onKot: () => sendKotTicket(context, ref),
                    onSave: () {
                      if (!context.mounted) return;
                      context.go('/tables');
                    },
                    onPay: cartSummary.isEmpty
                        ? null
                        : () => context.push(session.paymentRoute),
                  )
                else
                  CartFooter(
                    summary: cartSummary,
                    currency: currency,
                    paymentRoute: session.paymentRoute,
                    actionLabel: strings.proceedToPayment,
                    onTap: () {
                      if (cartSummary.isEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text(
                              'Cart is empty — add products first',
                            ),
                          ),
                        );
                        return;
                      }
                      context.push(session.paymentRoute);
                    },
                    accentFooter: isFastBilling,
                  ),
              ],
            );
          },
        ),
      ),
    );
  }

  Future<void> confirmClearCart(BuildContext context, WidgetRef ref) async {
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

    if (confirm == true) {
      await ref.read(posCartControllerProvider.notifier).clear();
    }
  }
}

class CatalogPane extends ConsumerWidget {
  const CatalogPane({
    super.key,
    required this.session,
    required this.categoriesAsync,
    required this.productsAsync,
    required this.selectedCategoryId,
    required this.selectedSubcategoryId,
    required this.currency,
    required this.searchController,
    required this.showCombos,
    required this.onToggleCombos,
  });

  final BillingSession session;
  final AsyncValue<List<ProductCategory>> categoriesAsync;
  final AsyncValue<List<Product>> productsAsync;
  final int? selectedCategoryId;
  final int? selectedSubcategoryId;
  final NumberFormat currency;
  final TextEditingController searchController;
  final bool showCombos;
  final ValueChanged<bool> onToggleCombos;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final combosAsync = ref.watch(posCombosProvider);
    final subsAsync = ref.watch(posSubcategoriesProvider);

    return ListenableBuilder(
      listenable: searchController,
      builder: (context, _) {
        final query = searchController.text.trim().toLowerCase();
        return Column(
          children: [
            if (session.tableNumber != null || session.customerName != null)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    session.tableNumber != null
                        ? 'Table ${session.tableNumber}'
                        : 'Customer: ${session.customerName}',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 10, 16, 8),
              child: TextField(
                controller: searchController,
                autofocus: AppPlatform.useDesktopShell,
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
                          icon: const AppSvg(
                            AppAssets.svgClose,
                            width: 18,
                            height: 18,
                            color: AppColors.textSecondary,
                          ),
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
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  Expanded(
                    child: CatalogTab(
                      label: 'Products',
                      icon: Icons.shopping_bag_outlined,
                      selected: !showCombos,
                      onTap: () => onToggleCombos(false),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: CatalogTab(
                      label: 'Combos',
                      icon: Icons.card_giftcard_outlined,
                      selected: showCombos,
                      onTap: () => onToggleCombos(true),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 10),
            if (!showCombos)
              SizedBox(
                height: 40,
                child: categoriesAsync.when(
                  data: (categories) {
                    if (categories.isEmpty) {
                      return Center(
                        child: Text(AppStrings.of(ref).noCategoriesFound),
                      );
                    }
                    return ListView(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(horizontal: 12),
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: CategoryChip(
                            label: 'All',
                            selected: selectedCategoryId == null,
                            onSelected: () {
                              ref
                                  .read(posSelectedCategoryIdProvider.notifier)
                                  .select(null);
                              ref
                                  .read(
                                    posSelectedSubcategoryIdProvider.notifier,
                                  )
                                  .select(null);
                            },
                          ),
                        ),
                        ...categories.map(
                          (category) => Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: CategoryChip(
                              label: category.categoryName,
                              selected:
                                  selectedCategoryId == category.categoryId,
                              onSelected: () => ref
                                  .read(posSelectedCategoryIdProvider.notifier)
                                  .select(category.categoryId),
                            ),
                          ),
                        ),
                      ],
                    );
                  },
                  loading: () =>
                      const Center(child: CircularProgressIndicator()),
                  error: (e, _) => Center(child: Text('$e')),
                ),
              ),
            if (!showCombos && selectedCategoryId != null)
              SizedBox(
                height: 40,
                child: subsAsync.when(
                  data: (subs) {
                    if (subs.isEmpty) return const SizedBox.shrink();
                    return ListView(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.fromLTRB(12, 4, 12, 0),
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: CategoryChip(
                            label: 'All',
                            selected: selectedSubcategoryId == null,
                            onSelected: () => ref
                                .read(posSelectedSubcategoryIdProvider.notifier)
                                .select(null),
                          ),
                        ),
                        ...subs.map(
                          (sub) => Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: CategoryChip(
                              label: sub.subcategoryName,
                              selected:
                                  selectedSubcategoryId == sub.subcategoryId,
                              onSelected: () => ref
                                  .read(
                                    posSelectedSubcategoryIdProvider.notifier,
                                  )
                                  .select(sub.subcategoryId),
                            ),
                          ),
                        ),
                      ],
                    );
                  },
                  loading: () => const SizedBox.shrink(),
                  error: (_, _) => const SizedBox.shrink(),
                ),
              ),
            Expanded(
              child: showCombos
                  ? combosAsync.when(
                      data: (combos) {
                        final filtered = query.isEmpty
                            ? combos
                            : combos
                                  .where(
                                    (c) =>
                                        c.comboName.toLowerCase().contains(
                                          query,
                                        ) ||
                                        (c.comboCode ?? '')
                                            .toLowerCase()
                                            .contains(query),
                                  )
                                  .toList();
                        if (filtered.isEmpty) {
                          return Center(
                            child: Text(AppStrings.of(ref).noCombosFound),
                          );
                        }
                        return ListView.separated(
                          padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                          addAutomaticKeepAlives: false,
                          itemCount: filtered.length,
                          separatorBuilder: (_, _) => const SizedBox(height: 8),
                          itemBuilder: (context, index) {
                            final combo = filtered[index];
                            final price = combo.comboWithGstPrice > 0
                                ? combo.comboWithGstPrice
                                : combo.comboPrice;
                            return AppCard(
                              padding: EdgeInsets.zero,
                              child: ListTile(
                                title: Text(
                                  combo.comboName,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                                subtitle: Text(currency.format(price)),
                                trailing: IconButton.filled(
                                  onPressed: () => ref
                                      .read(posCartControllerProvider.notifier)
                                      .addCombo(combo),
                                  icon: const Icon(Icons.add),
                                ),
                              ),
                            );
                          },
                        );
                      },
                      loading: () =>
                          const Center(child: CircularProgressIndicator()),
                      error: (e, _) => Center(child: Text('$e')),
                    )
                  : productsAsync.when(
                      data: (products) {
                        final filtered = query.isEmpty
                            ? products
                            : products
                                  .where(
                                    (p) =>
                                        p.productName.toLowerCase().contains(
                                          query,
                                        ) ||
                                        (p.productCode ?? '')
                                            .toLowerCase()
                                            .contains(query),
                                  )
                                  .toList();
                        if (filtered.isEmpty) {
                          return EmptyCatalog(
                            hasCategoryFilter: selectedCategoryId != null,
                          );
                        }
                        return LayoutBuilder(
                          builder: (context, constraints) {
                            final widthClass = AppBreakpoints.ofWidth(
                              constraints.maxWidth,
                            );
                            final crossAxisCount =
                                AppBreakpoints.productColumnsFor(widthClass);
                            final aspect = widthClass == AppWidthClass.compact
                                ? 1.72
                                : widthClass == AppWidthClass.medium
                                ? 1.78
                                : 1.85;
                            return GridView.builder(
                              padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
                              addAutomaticKeepAlives: false,
                              gridDelegate:
                                  SliverGridDelegateWithFixedCrossAxisCount(
                                    crossAxisCount: crossAxisCount,
                                    mainAxisSpacing: 10,
                                    crossAxisSpacing: 10,
                                    childAspectRatio: aspect,
                                  ),
                              itemCount: filtered.length,
                              itemBuilder: (context, index) => ProductCard(
                                key: ValueKey(filtered[index].productId),
                                product: filtered[index],
                                currency: currency,
                              ),
                            );
                          },
                        );
                      },
                      loading: () =>
                          const Center(child: CircularProgressIndicator()),
                      error: (e, _) => Center(child: Text('$e')),
                    ),
            ),
          ],
        );
      },
    );
  }
}

class CatalogTab extends StatelessWidget {
  const CatalogTab({
    super.key,
    required this.label,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected ? AppColors.primary : Colors.white,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: selected
                  ? AppColors.primary
                  : AppColors.primary.withValues(alpha: 0.28),
              width: 1,
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 18,
                color: selected ? Colors.white : AppColors.primary,
              ),
              const SizedBox(width: 8),
              Text(
                label,
                style: TextStyle(
                  fontWeight: FontWeight.w800,
                  color: selected ? Colors.white : AppColors.primary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class CategoryChip extends StatelessWidget {
  const CategoryChip({
    super.key,
    required this.label,
    required this.selected,
    required this.onSelected,
  });

  final String label;
  final bool selected;
  final VoidCallback onSelected;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected ? AppColors.primary : Colors.white,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        onTap: onSelected,
        borderRadius: BorderRadius.circular(22),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(22),
            border: Border.all(
              color: selected
                  ? AppColors.primary
                  : AppColors.primary.withValues(alpha: 0.28),
              width: 1,
            ),
          ),
          child: Text(
            label,
            style: TextStyle(
              fontWeight: FontWeight.w700,
              fontSize: 13,
              color: selected ? Colors.white : AppColors.textPrimary,
            ),
          ),
        ),
      ),
    );
  }
}

class EmptyCatalog extends ConsumerWidget {
  const EmptyCatalog({super.key, required this.hasCategoryFilter});

  final bool hasCategoryFilter;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    return AppEmptyState(
      title: hasCategoryFilter
          ? strings.noProductsCategory
          : strings.noProductsYet,
      message: hasCategoryFilter
          ? strings.noProductsCategory
          : strings.noProductsYet,
      iconAsset: AppAssets.svgFood,
    );
  }
}

class ProductCard extends ConsumerWidget {
  const ProductCard({super.key, required this.product, required this.currency});

  final Product product;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final unit = (product.productUnit?.trim().isNotEmpty ?? false)
        ? product.productUnit!.trim()
        : '';
    final priceLabel = unit.isEmpty
        ? '₹ ${product.productPrice.toStringAsFixed(1)}'
        : '₹ ${product.productPrice.toStringAsFixed(1)}/$unit';
    final showImage = hasProductImage(product.productImage);

    return RepaintBoundary(
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        elevation: 0,
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: () => addProductWithPortionPicker(context, ref, product),
          child: Container(
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 8),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: AppColors.primary.withValues(alpha: 0.12),
                width: 1,
              ),
              boxShadow: [
                BoxShadow(
                  color: AppColors.navy.withValues(alpha: 0.04),
                  blurRadius: 10,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (showImage) ...[
                      ProductImageThumb(
                        key: ValueKey('img-${product.productId}'),
                        value: product.productImage,
                        size: 48,
                        radius: 10,
                        showPlaceholder: false,
                      ),
                      const SizedBox(width: 8),
                    ],
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
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
                          const SizedBox(height: 4),
                          Text(
                            priceLabel,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontWeight: FontWeight.w700,
                              fontSize: 12,
                              color: AppColors.textPrimary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                GestureDetector(
                  onTap: () {},
                  behavior: HitTestBehavior.opaque,
                  child: ProductQtyButton(product: product),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class ProductQtyButton extends ConsumerWidget {
  const ProductQtyButton({super.key, required this.product});

  final Product product;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final qtyInCart = ref.watch(
      cartItemsProvider.select((async) {
        final items = async.maybeWhen(
          data: (items) => items,
          orElse: () => const <CartItem>[],
        );
        return items
            .where((i) => i.productId == product.productId)
            .fold<double>(0, (sum, i) => sum + i.quantity);
      }),
    );

    List<CartItem> productLines() {
      return ref
          .read(cartItemsProvider)
          .maybeWhen(
            data: (items) => items
                .where((i) => i.productId == product.productId)
                .toList(growable: false),
            orElse: () => const <CartItem>[],
          );
    }

    Future<void> addOrIncrement() async {
      final lines = productLines();
      if (lines.isEmpty) {
        await addProductWithPortionPicker(context, ref, product);
        return;
      }
      await ref.read(posCartControllerProvider.notifier).increment(lines.first);
    }

    Future<void> removeOrDecrement() async {
      final lines = productLines();
      if (lines.isEmpty) return;
      await ref.read(posCartControllerProvider.notifier).decrement(lines.first);
    }

    if (qtyInCart <= 0) {
      return SizedBox(
        width: double.infinity,
        height: 34,
        child: Material(
          color: AppColors.primary,
          borderRadius: BorderRadius.circular(10),
          child: InkWell(
            borderRadius: BorderRadius.circular(10),
            onTap: addOrIncrement,
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.add, color: Colors.white, size: 18),
                SizedBox(width: 4),
                Text(
                  'Add',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w800,
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Container(
      width: double.infinity,
      height: 34,
      padding: const EdgeInsets.symmetric(horizontal: 4),
      decoration: BoxDecoration(
        color: AppColors.primaryLight,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.primary.withValues(alpha: 0.25)),
      ),
      child: Row(
        children: [
          _qtyIconButton(icon: Icons.remove, onTap: removeOrDecrement),
          Expanded(
            child: Text(
              ProductUnits.formatQty(qtyInCart, unit: product.productUnit),
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.navy,
                fontWeight: FontWeight.w800,
                fontSize: 13,
              ),
            ),
          ),
          _qtyIconButton(icon: Icons.add, onTap: addOrIncrement),
        ],
      ),
    );
  }

  Widget _qtyIconButton({required IconData icon, required VoidCallback onTap}) {
    return Material(
      color: Colors.transparent,
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

class CartPane extends ConsumerWidget {
  const CartPane({
    super.key,
    required this.session,
    required this.cartAsync,
    required this.currency,
  });

  final BillingSession session;
  final AsyncValue<List<CartItem>> cartAsync;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summary = ref.watch(cartSummaryProvider);
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border(
          left: BorderSide(color: Colors.black.withValues(alpha: 0.08)),
        ),
      ),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 18, 16, 10),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text(
                'Current Bill',
                style: Theme.of(
                  context,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
              ),
            ),
          ),
          const Divider(height: 1),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: Row(
              children: [
                Expanded(
                  flex: 24,
                  child: Text(
                    'Product Name',
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                const SizedBox(
                  width: 88,
                  child: Text(
                    'Qty',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontWeight: FontWeight.w600, fontSize: 12),
                  ),
                ),
                const Expanded(
                  flex: 8,
                  child: Text(
                    'Unit Price',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontWeight: FontWeight.w600, fontSize: 12),
                  ),
                ),
                const SizedBox(width: 32),
              ],
            ),
          ),
          Expanded(
            child: cartAsync.when(
              data: (items) {
                if (items.isEmpty) {
                  return const EmptyCart();
                }
                return ListView.separated(
                  padding: const EdgeInsets.all(16),
                  itemCount: items.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                  itemBuilder: (context, index) =>
                      CartItemTile(item: items[index], currency: currency),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(child: Text('$e')),
            ),
          ),
          BillSummary(
            summary: summary,
            currency: currency,
            paymentRoute: session.paymentRoute,
            showKot: session.invoiceType == 'table_wise',
            onKot: () => sendKotTicket(context, ref),
          ),
        ],
      ),
    );
  }
}


class EmptyCart extends ConsumerWidget {
  const EmptyCart({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    return AppEmptyState(
      title: strings.cartIsEmpty,
      message: strings.cartEmptyHint,
      iconAsset: AppAssets.svgCart,
    );
  }
}

class CartItemTile extends ConsumerWidget {
  const CartItemTile({super.key, required this.item, required this.currency});

  final CartItem item;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Expanded(
            flex: 24,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.productName,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 14),
                ),
                if (item.quantity > item.printedQuantity)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: AppStatusBadge(
                      label:
                          'KOT +${ProductUnits.formatQty(item.quantity - item.printedQuantity, unit: item.productUnit)}',
                      color: AppColors.warning,
                    ),
                  ),
              ],
            ),
          ),
          SizedBox(
            width: 88,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                QtyButton(
                  isAdd: false,
                  onTap: () => ref
                      .read(posCartControllerProvider.notifier)
                      .decrement(item),
                ),
                InkWell(
                  onTap: () => editCartLineDialog(context, ref, item),
                  child: SizedBox(
                    width: 28,
                    height: 32,
                    child: Center(
                      child: Text(
                        ProductUnits.formatQty(
                          item.quantity,
                          unit: item.productUnit,
                        ),
                        style: const TextStyle(
                          fontWeight: FontWeight.w600,
                          fontSize: 14,
                        ),
                      ),
                    ),
                  ),
                ),
                QtyButton(
                  isAdd: true,
                  onTap: () => ref
                      .read(posCartControllerProvider.notifier)
                      .increment(item),
                ),
              ],
            ),
          ),
          Expanded(
            flex: 8,
            child: InkWell(
              onTap: () => editCartLineDialog(context, ref, item),
              child: Text(
                currency.format(item.unitPrice),
                textAlign: TextAlign.center,
                maxLines: 1,
                style: const TextStyle(fontSize: 13),
              ),
            ),
          ),
          IconButton(
            tooltip: 'Remove item',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
            onPressed: () =>
                ref.read(posCartControllerProvider.notifier).remove(item),
            icon: const AppSvg(
              AppAssets.svgDelete,
              width: 20,
              height: 20,
              color: AppColors.danger,
            ),
          ),
        ],
      ),
    );
  }
}

class QtyButton extends StatelessWidget {
  const QtyButton({super.key, required this.onTap, required this.isAdd});

  final VoidCallback onTap;
  final bool isAdd;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.primaryLight,
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: onTap,
        child: SizedBox(
          width: 28,
          height: 28,
          child: Center(
            child: Text(
              isAdd ? '+' : '−',
              style: const TextStyle(
                color: AppColors.primary,
                fontWeight: FontWeight.w700,
                fontSize: 16,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class BillSummary extends ConsumerWidget {
  const BillSummary({
    super.key,
    required this.summary,
    required this.currency,
    required this.paymentRoute,
    this.showKot = false,
    this.onKot,
  });

  final CartSummary summary;
  final NumberFormat currency;
  final String paymentRoute;
  final bool showKot;
  final VoidCallback? onKot;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    final unprinted = ref.watch(unprintedCartCountProvider);
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        border: Border(
          top: BorderSide(color: Colors.black.withValues(alpha: 0.08)),
        ),
      ),
      child: Column(
        children: [
          Row(
            children: [
              Text(
                '${ProductUnits.formatQty(summary.totalQuantity)} ${summary.totalQuantity == 1 ? 'item' : 'items'}',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppColors.textSecondary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const Spacer(),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    'Payable Amount',
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                  Text(
                    currency.format(summary.grandTotal),
                    style: AppTypography.amount(
                      color: AppColors.primary,
                      size: 20,
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 14),
          if (showKot) ...[
            AppButton(
              label: unprinted > 0
                  ? '${strings.sendKot} (${ProductUnits.formatQty(unprinted)})'
                  : strings.kotUpToDate,
              icon: Icons.print_outlined,
              variant: AppButtonVariant.outlined,
              onPressed: unprinted <= 0 ? null : onKot,
            ),
            const SizedBox(height: 8),
          ],
          SizedBox(
            width: double.infinity,
            child: AppButton(
              label: strings.proceedToPayment,
              icon: Icons.receipt_long_rounded,
              onPressed: summary.isEmpty
                  ? null
                  : () => context.push(paymentRoute),
            ),
          ),
        ],
      ),
    );
  }
}

class CartFooter extends StatelessWidget {
  const CartFooter({
    super.key,
    required this.summary,
    required this.currency,
    required this.paymentRoute,
    required this.onTap,
    this.actionLabel = 'Proceed to Payment',
    this.onPay,
    this.accentFooter = false,
  });

  final CartSummary summary;
  final NumberFormat currency;
  final String paymentRoute;
  final VoidCallback onTap;
  final String actionLabel;
  final VoidCallback? onPay;
  final bool accentFooter;

  @override
  Widget build(BuildContext context) {
    final bg = accentFooter ? AppColors.primary : Colors.white;
    final amountColor = accentFooter ? Colors.white : AppColors.primary;
    final labelColor = accentFooter ? Colors.white70 : AppColors.textSecondary;

    return Material(
      color: bg,
      elevation: 10,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          child: Row(
            children: [
              InkWell(
                onTap: onTap,
                borderRadius: BorderRadius.circular(14),
                child: Stack(
                  clipBehavior: Clip.none,
                  children: [
                    Container(
                      width: 46,
                      height: 46,
                      decoration: BoxDecoration(
                        color: accentFooter
                            ? Colors.white.withValues(alpha: 0.15)
                            : AppColors.primaryLight,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(
                        Icons.shopping_cart_outlined,
                        color: accentFooter ? Colors.white : AppColors.primary,
                      ),
                    ),
                    if (summary.totalQuantity > 0)
                      Positioned(
                        right: -4,
                        top: -4,
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 6,
                            vertical: 2,
                          ),
                          decoration: BoxDecoration(
                            color: AppColors.danger,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            ProductUnits.formatQty(summary.totalQuantity),
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 11,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'Payable Amount',
                      style: TextStyle(
                        color: labelColor,
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    Text(
                      currency.format(summary.grandTotal),
                      style: TextStyle(
                        color: amountColor,
                        fontWeight: FontWeight.w900,
                        fontSize: 20,
                      ),
                    ),
                  ],
                ),
              ),
              Material(
                color: accentFooter ? Colors.white : AppColors.primary,
                borderRadius: BorderRadius.circular(10),
                child: InkWell(
                  borderRadius: BorderRadius.circular(10),
                  onTap: onTap,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 12,
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          actionLabel,
                          style: TextStyle(
                            color: accentFooter
                                ? AppColors.primary
                                : Colors.white,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(width: 6),
                        Icon(
                          Icons.receipt_long_rounded,
                          size: 18,
                          color: accentFooter
                              ? AppColors.primary
                              : Colors.white,
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
    );
  }
}

class DineInFooter extends StatelessWidget {
  const DineInFooter({
    super.key,
    required this.summary,
    required this.currency,
    required this.unprintedCount,
    this.kotEnabled = true,
    required this.onKot,
    required this.onSave,
    required this.onPay,
  });

  final CartSummary summary;
  final NumberFormat currency;
  final double unprintedCount;
  final bool kotEnabled;
  final VoidCallback onKot;
  final VoidCallback onSave;
  final VoidCallback? onPay;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      elevation: 10,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                children: [
                  Text(
                    'Payable',
                    style: Theme.of(context).textTheme.labelMedium,
                  ),
                  const Spacer(),
                  Text(
                    currency.format(summary.grandTotal),
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  if (kotEnabled) ...[
                    Expanded(
                      child: AppButton(
                        label: unprintedCount > 0
                            ? 'KOT (${ProductUnits.formatQty(unprintedCount)})'
                            : 'KOT',
                        variant: AppButtonVariant.outlined,
                        onPressed: unprintedCount <= 0 ? null : onKot,
                      ),
                    ),
                    const SizedBox(width: 8),
                  ],
                  Expanded(
                    child: AppButton(
                      label: 'SAVE',
                      onPressed: onSave,
                      variant: AppButtonVariant.outlined,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: AppButton(label: 'PAY', onPressed: onPay),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
