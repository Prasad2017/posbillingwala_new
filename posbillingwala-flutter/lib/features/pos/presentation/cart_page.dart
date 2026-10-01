import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_date.dart';
import 'package:pos_billingwala_v2/features/pos/domain/kot_providers.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_checkout_controller.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/bill_summary_card.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/payment_page.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/pos_action_footer.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/pos_checkout_flow.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/pos_page.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class CartPage extends ConsumerWidget {
  const CartPage({super.key});

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

  void unawaitedCheckout(
    BuildContext context,
    WidgetRef ref,
    PosCheckoutAction action,
  ) {
    runPosCheckoutAction(context, ref, action: action);
  }

  PosActionFooter buildFooter({
    required BuildContext context,
    required WidgetRef ref,
    required CartSummary summary,
    required NumberFormat currency,
    required bool isTable,
    required bool kotEnabled,
    required double unprintedCount,
    required double payable,
    bool showCartBar = true,
    bool showActions = true,
  }) {
    return PosActionFooter(
      summary: summary,
      currency: currency,
      displayTotal: payable,
      showCartBar: showCartBar,
      showActions: showActions,
      onCartTap: () {},
      onSave: () {
        if (isTable) {
          context.go('/tables');
          return;
        }
        unawaitedCheckout(context, ref, PosCheckoutAction.save);
      },
      onShare: () => unawaitedCheckout(context, ref, PosCheckoutAction.share),
      onPrint: () => unawaitedCheckout(context, ref, PosCheckoutAction.print),
      kotEnabled: isTable && kotEnabled,
      unprintedCount: unprintedCount,
      onKot: () => sendKotTicket(context, ref),
    );
  }

  Widget buildItemsPane({
    required AsyncValue<List<CartItem>> cartAsync,
    required NumberFormat currency,
  }) {
    return cartAsync.when(
      data: (items) {
        if (items.isEmpty) {
          return const EmptyCart();
        }
        return ListView(
          padding: const EdgeInsets.fromLTRB(12, 12, 12, 12),
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          children: [
            _CartItemsTable(items: items, currency: currency),
          ],
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text('$e')),
    );
  }

  /* Portrait: product list scrolls; bill summary + footer stay pinned at bottom. */
  Widget buildPortraitBody({
    required AsyncValue<List<CartItem>> cartAsync,
    required NumberFormat currency,
    required CartSummary summary,
    required WidgetRef ref,
    required BuildContext context,
    required bool isTable,
    required bool kotEnabled,
    required double unprintedCount,
    required double payable,
  }) {
    return _PortraitCartBody(
      cartAsync: cartAsync,
      currency: currency,
      summary: summary,
      isTable: isTable,
      kotEnabled: kotEnabled,
      unprintedCount: unprintedCount,
      payable: payable,
      onClearCheckout: (action) => unawaitedCheckout(context, ref, action),
      onSaveTable: () => context.go('/tables'),
      onKot: () => sendKotTicket(context, ref),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(billingSessionProvider);
    final cartAsync = ref.watch(cartItemsProvider);
    final summary = ref.watch(cartSummaryProvider);
    final checkout = ref.watch(paymentCheckoutControllerProvider);
    final currency = MoneyFormat.inr;
    final strings = AppStrings.of(ref);
    final isTable = session.invoiceType == 'table_wise';
    final kotEnabled = ref.watch(printerSettingsProvider).kotEnable;
    final unprintedCount = ref.watch(unprintedCartCountProvider);
    final qtyLabel = ProductUnits.formatQty(summary.totalQuantity);
    final landscape = context.isLandscapeLayout;
    final payable = checkoutPayable(summary, checkout);
    final printFastBill = ref.watch(
      printerSettingsProvider.select((s) => s.printFastBill),
    );

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FB),
      resizeToAvoidBottomInset: true,
      appBar: AppBar(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Cart ($qtyLabel ${summary.totalQuantity == 1 ? 'Item' : 'Items'})',
              style: const TextStyle(fontWeight: FontWeight.w800),
            ),
            if (printFastBill) const BillingDateBar(inAppBar: true),
          ],
        ),
        actions: [
          if (!summary.isEmpty)
            TextButton.icon(
              onPressed: () => confirmClearCart(context, ref),
              icon: const Icon(
                Icons.delete_outline_rounded,
                color: Colors.white,
                size: 18,
              ),
              label: Text(
                strings.clearCart,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
        ],
      ),
      body: summary.isEmpty
          ? buildItemsPane(cartAsync: cartAsync, currency: currency)
          : landscape
              ? Row(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Expanded(
                      flex: 6,
                      child: Column(
                        children: [
                          Expanded(
                            child: buildItemsPane(
                              cartAsync: cartAsync,
                              currency: currency,
                            ),
                          ),
                          buildFooter(
                            context: context,
                            ref: ref,
                            summary: summary,
                            currency: currency,
                            isTable: isTable,
                            kotEnabled: kotEnabled,
                            unprintedCount: unprintedCount,
                            payable: payable,
                            showCartBar: true,
                            showActions: false,
                          ),
                        ],
                      ),
                    ),
                    Container(
                      width: 1,
                      color: AppColors.border,
                    ),
                    Expanded(
                      flex: 5,
                      child: Column(
                        children: [
                          const Expanded(
                            child: SingleChildScrollView(
                              padding: EdgeInsets.only(top: 8),
                              keyboardDismissBehavior:
                                  ScrollViewKeyboardDismissBehavior.onDrag,
                              child: _CartBillBreakdown(
                                expandable: false,
                              ),
                            ),
                          ),
                          buildFooter(
                            context: context,
                            ref: ref,
                            summary: summary,
                            currency: currency,
                            isTable: isTable,
                            kotEnabled: kotEnabled,
                            unprintedCount: unprintedCount,
                            payable: payable,
                            showCartBar: false,
                            showActions: true,
                          ),
                        ],
                      ),
                    ),
                  ],
                )
              : buildPortraitBody(
                  cartAsync: cartAsync,
                  currency: currency,
                  summary: summary,
                  ref: ref,
                  context: context,
                  isTable: isTable,
                  kotEnabled: kotEnabled,
                  unprintedCount: unprintedCount,
                  payable: payable,
                ),
    );
  }
}

class _PortraitCartBody extends ConsumerStatefulWidget {
  const _PortraitCartBody({
    required this.cartAsync,
    required this.currency,
    required this.summary,
    required this.isTable,
    required this.kotEnabled,
    required this.unprintedCount,
    required this.payable,
    required this.onClearCheckout,
    required this.onSaveTable,
    required this.onKot,
  });

  final AsyncValue<List<CartItem>> cartAsync;
  final NumberFormat currency;
  final CartSummary summary;
  final bool isTable;
  final bool kotEnabled;
  final double unprintedCount;
  final double payable;
  final void Function(PosCheckoutAction action) onClearCheckout;
  final VoidCallback onSaveTable;
  final VoidCallback onKot;

  @override
  ConsumerState<_PortraitCartBody> createState() => _PortraitCartBodyState();
}

class _PortraitCartBodyState extends ConsumerState<_PortraitCartBody> {
  bool billExpanded = false;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Expanded(
          child: widget.cartAsync.when(
            data: (items) {
              if (items.isEmpty) {
                return const EmptyCart();
              }
              return ListView(
                padding: const EdgeInsets.fromLTRB(12, 12, 12, 12),
                keyboardDismissBehavior:
                    ScrollViewKeyboardDismissBehavior.onDrag,
                children: [
                  _CartItemsTable(items: items, currency: widget.currency),
                ],
              );
            },
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (e, _) => Center(child: Text('$e')),
          ),
        ),
        /* Pinned — never scrolls with the product list.
         * Cap height when expanded so Save/Share/Print stay visible. */
        ConstrainedBox(
          constraints: BoxConstraints(
            maxHeight: MediaQuery.sizeOf(context).height * 0.42,
          ),
          child: SingleChildScrollView(
            child: _CartBillBreakdown(
              expandable: true,
              expanded: billExpanded,
              onToggleExpanded: () =>
                  setState(() => billExpanded = !billExpanded),
            ),
          ),
        ),
        PosActionFooter(
          summary: widget.summary,
          currency: widget.currency,
          displayTotal: widget.payable,
          onCartTap: () => setState(() => billExpanded = !billExpanded),
          onSave: () {
            if (widget.isTable) {
              widget.onSaveTable();
              return;
            }
            widget.onClearCheckout(PosCheckoutAction.save);
          },
          onShare: () => widget.onClearCheckout(PosCheckoutAction.share),
          onPrint: () => widget.onClearCheckout(PosCheckoutAction.print),
          kotEnabled: widget.isTable && widget.kotEnabled,
          unprintedCount: widget.unprintedCount,
          onKot: widget.onKot,
        ),
      ],
    );
  }
}

class _CartItemsTable extends StatelessWidget {
  const _CartItemsTable({required this.items, required this.currency});

  final List<CartItem> items;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final narrow = constraints.maxWidth < 360;
        return Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.border),
            boxShadow: [
              BoxShadow(
                color: AppColors.navy.withValues(alpha: 0.04),
                blurRadius: 10,
                offset: const Offset(0, 3),
              ),
            ],
          ),
          clipBehavior: Clip.antiAlias,
          child: Column(
            children: [
              Container(
                padding: EdgeInsets.symmetric(
                  horizontal: narrow ? 8 : 10,
                  vertical: 11,
                ),
                color: AppColors.primarySoft,
                child: Row(
                  children: [
                    const SizedBox(
                      width: 24,
                      child: Text(
                        '#',
                        style: TextStyle(
                          color: AppColors.navy,
                          fontWeight: FontWeight.w800,
                          fontSize: 12.5,
                        ),
                      ),
                    ),
                    const Expanded(
                      flex: 5,
                      child: Text(
                        'Product Name',
                        style: TextStyle(
                          color: AppColors.navy,
                          fontWeight: FontWeight.w800,
                          fontSize: 12.5,
                        ),
                      ),
                    ),
                    Expanded(
                      flex: narrow ? 3 : 4,
                      child: const Text(
                        'Quantity',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: AppColors.navy,
                          fontWeight: FontWeight.w800,
                          fontSize: 12.5,
                        ),
                      ),
                    ),
                    if (!narrow)
                      const Expanded(
                        flex: 3,
                        child: Text(
                          'Unit Price',
                          textAlign: TextAlign.end,
                          style: TextStyle(
                            color: AppColors.navy,
                            fontWeight: FontWeight.w800,
                            fontSize: 12.5,
                          ),
                        ),
                      ),
                    const SizedBox(width: 36),
                  ],
                ),
              ),
              for (var i = 0; i < items.length; i++) ...[
                InvoiceLineRow(
                  key: ValueKey('cart-line-${items[i].cartId}'),
                  index: i + 1,
                  item: items[i],
                  currency: currency,
                  showUnitPrice: !narrow,
                ),
                if (i < items.length - 1)
                  const Divider(height: 1, color: AppColors.border),
              ],
            ],
          ),
        );
      },
    );
  }
}

class _CartBillBreakdown extends ConsumerStatefulWidget {
  const _CartBillBreakdown({
    this.expandable = true,
    this.expanded,
    this.onToggleExpanded,
  });

  final bool expandable;
  /* When set with [onToggleExpanded], parent owns expand state (pinned portrait). */
  final bool? expanded;
  final VoidCallback? onToggleExpanded;

  @override
  ConsumerState<_CartBillBreakdown> createState() => _CartBillBreakdownState();
}

class _CartBillBreakdownState extends ConsumerState<_CartBillBreakdown> {
  late final TextEditingController discountController;
  late final TextEditingController packingController;
  bool billExpanded = false;

  static String formatFieldValue(double value) {
    if (value <= 0) return '';
    if (value == value.roundToDouble()) return value.round().toString();
    return value.toString();
  }

  @override
  void initState() {
    super.initState();
    /* Restore from Riverpod so values survive orientation rebuilds. */
    final checkout = ref.read(paymentCheckoutControllerProvider);
    discountController = TextEditingController(
      text: formatFieldValue(checkout.discount),
    );
    packingController = TextEditingController(
      text: formatFieldValue(checkout.packingCharge),
    );
  }

  @override
  void dispose() {
    discountController.dispose();
    packingController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final summary = ref.watch(cartSummaryProvider);
    final checkout = ref.watch(paymentCheckoutControllerProvider);
    final currency = MoneyFormat.inr;
    final payable = checkoutPayable(summary, checkout);
    final expanded = !widget.expandable
        ? true
        : (widget.expanded ?? billExpanded);

    return BillSummaryCard(
      summary: summary,
      checkout: checkout,
      currency: currency,
      discountController: discountController,
      packingController: packingController,
      payable: payable,
      expanded: expanded,
      expandable: widget.expandable,
      showCollapsedAmount: false,
      onToggleExpanded: widget.expandable
          ? (widget.onToggleExpanded ??
                () => setState(() => billExpanded = !billExpanded))
          : () {},
      onDiscountChanged: (value) {
        final d = double.tryParse(value) ?? 0;
        final n = ref.read(paymentCheckoutControllerProvider.notifier);
        n.setDiscount(
          d,
          type: checkout.discountType,
          subtotal: summary.subtotal,
        );
        n.selectMode(
          checkout.mode,
          checkoutPayable(
            summary,
            ref.read(paymentCheckoutControllerProvider),
          ),
        );
      },
      onDiscountTypeChanged: (type) {
        final d = double.tryParse(discountController.text) ?? 0;
        final n = ref.read(paymentCheckoutControllerProvider.notifier);
        n.setDiscount(d, type: type, subtotal: summary.subtotal);
        n.selectMode(
          checkout.mode,
          checkoutPayable(
            summary,
            ref.read(paymentCheckoutControllerProvider),
          ),
        );
        setState(() {});
      },
      onPackingChanged: (value) {
        final p = double.tryParse(value) ?? 0;
        final n = ref.read(paymentCheckoutControllerProvider.notifier);
        n.setPacking(p, type: 'Amount');
        n.selectMode(
          checkout.mode,
          checkoutPayable(
            summary,
            ref.read(paymentCheckoutControllerProvider),
          ),
        );
      },
    );
  }
}
