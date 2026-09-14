import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/app_states.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/kot_providers.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/kot_preview_page.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/portion_picker.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/tables/presentation/table_ops.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

Future<void> sendKotTicket(BuildContext context, WidgetRef ref) async {
  try {
    final ticket = await ref.read(kotControllerProvider.notifier).createKot();
    if (!context.mounted) return;
    final settings = ref.read(printerSettingsProvider);
    final skipPreview = settings.kotAutoPrint || !settings.kotPreview;
    if (skipPreview) {
      final copies = settings.kotCopies.clamp(1, 5);
      PrintResult? last;
      for (var i = 0; i < copies; i++) {
        last = await ref.read(printServiceProvider).printKot(ticket);
      }
      await ref.read(kotControllerProvider.notifier).markPrinted(ticket.kot.kotId);
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            last?.message ?? 'KOT printed${copies > 1 ? ' ×$copies' : ''}',
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
  final bool openCartOnStart;

  @override
  ConsumerState<PosPage> createState() => _PosPageState();
}

class _PosPageState extends ConsumerState<PosPage> {
  final _searchController = TextEditingController();
  final _speech = stt.SpeechToText();
  bool _showCombos = false;
  bool _listening = false;
  bool _openedCartOnStart = false;

  @override
  void initState() {
    super.initState();
    if (widget.resetSessionOnOpen) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.read(billingSessionProvider.notifier).usePos();
      });
    }
    _searchController.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _searchController.dispose();
    _speech.stop();
    super.dispose();
  }

  Future<void> _voiceSearch() async {
    final available = await _speech.initialize(
      onStatus: (status) {
        if (!mounted) return;
        if (status == 'done' || status == 'notListening') {
          setState(() => _listening = false);
        }
      },
    );
    if (!available) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).speechUnavailable)),
      );
      return;
    }
    setState(() => _listening = true);
    await _speech.listen(
      onResult: (result) {
        _searchController.text = result.recognizedWords;
        _searchController.selection = TextSelection.fromPosition(
          TextPosition(offset: _searchController.text.length),
        );
      },
      listenOptions: stt.SpeechListenOptions(
        partialResults: true,
        listenMode: stt.ListenMode.confirmation,
        localeId: AppStrings.of(ref).speechLocaleId(),
      ),
    );
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
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final showSideCart = context.showPosSideCart;

    final unprintedCount = ref.watch(unprintedCartCountProvider);
    final isTable = session.invoiceType == 'table_wise';
    final kotEnabled = ref.watch(printerSettingsProvider).kotEnable;

    final isFastBilling = session.invoiceType == 'fast_billing';
    final strings = AppStrings.of(ref);

    if (widget.openCartOnStart &&
        !_openedCartOnStart &&
        !cartSummary.isEmpty) {
      _openedCartOnStart = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        _openCartSheet(context, session);
      });
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF3F6FB),
      appBar: AppBar(
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
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w400,
                color: Colors.white70,
              ),
            ),
          ],
        ),
        actions: [
          PopupMenuButton<String>(
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
          if (!cartSummary.isEmpty)
            Padding(
              padding: const EdgeInsets.only(right: 4),
              child: TextButton.icon(
                onPressed: () => _confirmClearCart(context, ref),
                icon: const Icon(Icons.delete, color: AppColors.danger, size: 18),
                label: const Text(
                  'Clear Cart',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w700,
                    fontSize: 13,
                  ),
                ),
                style: TextButton.styleFrom(
                  foregroundColor: Colors.white,
                  side: const BorderSide(color: Colors.white70),
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  visualDensity: VisualDensity.compact,
                ),
              ),
            ),
        ],
      ),
      body: SafeArea(
        child: showSideCart
            ? Row(
                children: [
                  Expanded(
                    flex: 3,
                    child: _CatalogPane(
                      session: session,
                      categoriesAsync: categoriesAsync,
                      productsAsync: productsAsync,
                      selectedCategoryId: selectedCategoryId,
                      selectedSubcategoryId: selectedSubcategoryId,
                      currency: currency,
                      searchController: _searchController,
                      showCombos: _showCombos,
                      listening: _listening,
                      onVoiceSearch: _voiceSearch,
                      onToggleCombos: (v) => setState(() => _showCombos = v),
                    ),
                  ),
                  SizedBox(
                    width: context.isLargeWidth ? 400 : 360,
                    child: _CartPane(
                      session: session,
                      cartAsync: cartAsync,
                      currency: currency,
                    ),
                  ),
                ],
              )
            : Column(
                children: [
                  Expanded(
                    child: _CatalogPane(
                      session: session,
                      categoriesAsync: categoriesAsync,
                      productsAsync: productsAsync,
                      selectedCategoryId: selectedCategoryId,
                      selectedSubcategoryId: selectedSubcategoryId,
                      currency: currency,
                      searchController: _searchController,
                      showCombos: _showCombos,
                      listening: _listening,
                      onVoiceSearch: _voiceSearch,
                      onToggleCombos: (v) => setState(() => _showCombos = v),
                    ),
                  ),
                  if (isTable)
                    _DineInFooter(
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
                    _CartFooter(
                      summary: cartSummary,
                      currency: currency,
                      paymentRoute: session.paymentRoute,
                      onTap: cartSummary.isEmpty
                          ? () {}
                          : () => _openCartSheet(context, session),
                      onPay: cartSummary.isEmpty
                          ? null
                          : () => context.push(session.paymentRoute),
                      accentFooter: isFastBilling,
                    ),
                ],
              ),
        ),
      );
  }

  Future<void> _confirmClearCart(BuildContext context, WidgetRef ref) async {
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

  Future<void> _openCartSheet(BuildContext context, BillingSession session) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (context) => FractionallySizedBox(
        heightFactor: 0.82,
        child: _CartSheetBody(session: session),
      ),
    );
  }
}

class _CatalogPane extends ConsumerWidget {
  const _CatalogPane({
    required this.session,
    required this.categoriesAsync,
    required this.productsAsync,
    required this.selectedCategoryId,
    required this.selectedSubcategoryId,
    required this.currency,
    required this.searchController,
    required this.showCombos,
    required this.listening,
    required this.onVoiceSearch,
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
  final bool listening;
  final VoidCallback onVoiceSearch;
  final ValueChanged<bool> onToggleCombos;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final query = searchController.text.trim().toLowerCase();
    final combosAsync = ref.watch(posCombosProvider);
    final subsAsync = ref.watch(posSubcategoriesProvider);

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
              suffixIcon: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    tooltip: listening ? 'Listening…' : 'Voice search',
                    onPressed: onVoiceSearch,
                    icon: AppSvg(
                      AppAssets.svgMic,
                      width: 20,
                      height: 20,
                      color: listening ? AppColors.primary : AppColors.textSecondary,
                    ),
                  ),
                  if (query.isNotEmpty)
                    IconButton(
                      onPressed: searchController.clear,
                      icon: const AppSvg(
                        AppAssets.svgClose,
                        width: 18,
                        height: 18,
                        color: AppColors.textSecondary,
                      ),
                    ),
                ],
              ),
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
                child: _CatalogTab(
                  label: 'Products',
                  icon: Icons.shopping_bag_outlined,
                  selected: !showCombos,
                  onTap: () => onToggleCombos(false),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _CatalogTab(
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
                      child: _CategoryChip(
                        label: 'All',
                        selected: selectedCategoryId == null,
                        onSelected: () {
                          ref
                              .read(posSelectedCategoryIdProvider.notifier)
                              .select(null);
                          ref
                              .read(posSelectedSubcategoryIdProvider.notifier)
                              .select(null);
                        },
                      ),
                    ),
                    ...categories.map(
                      (category) => Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: _CategoryChip(
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
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(child: Text('$e')),
            ),
          ),
        if (!showCombos)
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
                      child: _CategoryChip(
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
                        child: _CategoryChip(
                          label: sub.subcategoryName,
                          selected:
                              selectedSubcategoryId == sub.subcategoryId,
                          onSelected: () => ref
                              .read(posSelectedSubcategoryIdProvider.notifier)
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
                                  c.comboName.toLowerCase().contains(query) ||
                                  (c.comboCode ?? '')
                                      .toLowerCase()
                                      .contains(query),
                            )
                            .toList();
                    if (filtered.isEmpty) {
                      return Center(child: Text(AppStrings.of(ref).noCombosFound));
                    }
                    return ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
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
                              style: const TextStyle(fontWeight: FontWeight.w700),
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
                                  p.productName.toLowerCase().contains(query) ||
                                  (p.productCode ?? '')
                                      .toLowerCase()
                                      .contains(query),
                            )
                            .toList();
                    if (filtered.isEmpty) {
                      return _EmptyCatalog(
                        hasCategoryFilter: selectedCategoryId != null,
                      );
                    }
                    return LayoutBuilder(
                      builder: (context, constraints) {
                        final widthClass =
                            AppBreakpoints.ofWidth(constraints.maxWidth);
                        final crossAxisCount =
                            AppBreakpoints.productColumnsFor(widthClass);
                        final aspect = widthClass == AppWidthClass.compact
                            ? 2.35
                            : widthClass == AppWidthClass.medium
                                ? 2.1
                                : 1.9;
                        return GridView.builder(
                          padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
                          gridDelegate:
                              SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: crossAxisCount,
                            mainAxisSpacing: 10,
                            crossAxisSpacing: 10,
                            childAspectRatio: aspect,
                          ),
                          itemCount: filtered.length,
                          itemBuilder: (context, index) => _ProductCard(
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
  }
}

class _CatalogTab extends StatelessWidget {
  const _CatalogTab({
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
              color: selected ? AppColors.primary : AppColors.primary,
              width: 1.4,
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

class _CategoryChip extends StatelessWidget {
  const _CategoryChip({
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
              color: selected ? AppColors.primary : const Color(0xFFB7C4D8),
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

class _EmptyCatalog extends ConsumerWidget {
  const _EmptyCatalog({required this.hasCategoryFilter});

  final bool hasCategoryFilter;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    return AppEmptyState(
      title: hasCategoryFilter ? strings.noProductsCategory : strings.noProductsYet,
      message: hasCategoryFilter
          ? strings.noProductsCategory
          : strings.noProductsYet,
      iconAsset: AppAssets.svgFood,
    );
  }
}

class _ProductCard extends ConsumerWidget {
  const _ProductCard({required this.product, required this.currency});

  final Product product;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cartItems = ref.watch(cartItemsProvider).maybeWhen(
          data: (items) => items,
          orElse: () => const <CartItem>[],
        );
    final qtyInCart = cartItems
        .where((i) => i.productId == product.productId)
        .fold<int>(0, (sum, i) => sum + i.quantity);
    final unit = (product.productUnit?.trim().isNotEmpty ?? false)
        ? product.productUnit!.trim()
        : '';
    final priceLabel = unit.isEmpty
        ? '₹ ${product.productPrice.toStringAsFixed(1)}'
        : '₹ ${product.productPrice.toStringAsFixed(1)}/$unit';

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(12),
      elevation: 0.5,
      shadowColor: Colors.black26,
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => addProductWithPortionPicker(context, ref, product),
        child: Container(
          padding: const EdgeInsets.fromLTRB(12, 10, 8, 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: const Color(0xFFE2E8F2)),
          ),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      product.productName,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 14,
                        color: AppColors.textPrimary,
                        height: 1.2,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      priceLabel,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 13,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 6),
              Material(
                color: AppColors.primary,
                shape: const CircleBorder(),
                child: InkWell(
                  customBorder: const CircleBorder(),
                  onTap: () =>
                      addProductWithPortionPicker(context, ref, product),
                  child: SizedBox(
                    width: 36,
                    height: 36,
                    child: Center(
                      child: qtyInCart > 0
                          ? Text(
                              '$qtyInCart',
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.w800,
                                fontSize: 14,
                              ),
                            )
                          : const Icon(Icons.add, color: Colors.white, size: 22),
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

class _CartPane extends ConsumerWidget {
  const _CartPane({
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
        border: Border(left: BorderSide(color: Colors.black.withValues(alpha: 0.08))),
      ),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 18, 16, 10),
            child: Row(
              children: [
                Text(
                  'Current Bill',
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                ),
                const Spacer(),
                Text(
                  '${summary.totalQuantity} items',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                ),
              ],
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
                  return const _EmptyCart();
                }
                return ListView.separated(
                  padding: const EdgeInsets.all(16),
                  itemCount: items.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                  itemBuilder: (context, index) => _CartItemTile(
                    item: items[index],
                    currency: currency,
                  ),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(child: Text('$e')),
            ),
          ),
          _BillSummary(
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

class _CartSheetBody extends ConsumerWidget {
  const _CartSheetBody({required this.session});

  final BillingSession session;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _CartPane(
      session: session,
      cartAsync: ref.watch(cartItemsProvider),
      currency: NumberFormat.currency(locale: 'en_IN', symbol: 'Rs. '),
    );
  }
}

class _EmptyCart extends ConsumerWidget {
  const _EmptyCart();

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

class _CartItemTile extends ConsumerWidget {
  const _CartItemTile({required this.item, required this.currency});

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
                      label: 'KOT +${item.quantity - item.printedQuantity}',
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
                _QtyButton(
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
                        '${item.quantity}',
                        style: const TextStyle(
                          fontWeight: FontWeight.w600,
                          fontSize: 14,
                        ),
                      ),
                    ),
                  ),
                ),
                _QtyButton(
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

class _QtyButton extends StatelessWidget {
  const _QtyButton({
    required this.onTap,
    required this.isAdd,
  });

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

class _BillSummary extends ConsumerWidget {
  const _BillSummary({
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
        border: Border(top: BorderSide(color: Colors.black.withValues(alpha: 0.08))),
      ),
      child: Column(
        children: [
          _SummaryRow(label: strings.items, value: '${summary.totalQuantity}'),
          _SummaryRow(label: strings.subtotal, value: currency.format(summary.subtotal)),
          _SummaryRow(label: strings.gst, value: currency.format(summary.taxTotal)),
          const Divider(height: 18),
          _SummaryRow(
            label: strings.grandTotal,
            value: currency.format(summary.grandTotal),
            emphasized: true,
          ),
          const SizedBox(height: 14),
          if (showKot) ...[
            AppButton(
              label: unprinted > 0
                  ? '${strings.sendKot} ($unprinted)'
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

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
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
        ? AppTypography.cardTitle()
        : AppTypography.body();
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Text(label, style: style),
          const Spacer(),
          Text(
            value,
            style: emphasized
                ? AppTypography.amount(color: AppColors.primary, size: 18)
                : style,
          ),
        ],
      ),
    );
  }
}

class _CartFooter extends StatelessWidget {
  const _CartFooter({
    required this.summary,
    required this.currency,
    required this.paymentRoute,
    required this.onTap,
    this.onPay,
    this.accentFooter = false,
  });

  final CartSummary summary;
  final NumberFormat currency;
  final String paymentRoute;
  final VoidCallback onTap;
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
                onTap: summary.isEmpty ? null : onTap,
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
                            '${summary.totalQuantity}',
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
                  onTap: onPay ?? (summary.isEmpty ? null : onTap),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 12,
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          'View Cart',
                          style: TextStyle(
                            color: accentFooter
                                ? AppColors.primary
                                : Colors.white,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(width: 6),
                        Icon(
                          Icons.arrow_forward,
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

class _DineInFooter extends StatelessWidget {
  const _DineInFooter({
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
  final int unprintedCount;
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
                            ? 'KOT ($unprintedCount)'
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
                    child: AppButton(
                      label: 'PAY',
                      onPressed: onPay,
                    ),
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
