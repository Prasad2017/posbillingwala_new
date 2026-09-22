import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/takeaway/domain/takeaway_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Take Away parcel counter: open parcels waiting for billing. */
class TakeawayPage extends ConsumerStatefulWidget {
  const TakeawayPage({super.key});

  @override
  ConsumerState<TakeawayPage> createState() => TakeawayPageState();
}

class TakeawayPageState extends ConsumerState<TakeawayPage> {
  Future<void> startNewParcel({String? name, String? phone}) async {
    final nameCtrl = TextEditingController(text: name ?? '');
    final phoneCtrl = TextEditingController(text: phone ?? '');
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) {
        final maxW = MediaQuery.sizeOf(context).width;
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
          ),
          insetPadding: EdgeInsets.symmetric(
            horizontal: maxW < 400 ? 16 : 40,
            vertical: 24,
          ),
          title: Row(
            children: [
              const AppSvg(AppAssets.svgTakeaway, width: 28, height: 28),
              const SizedBox(width: 10),
              Flexible(child: Text(AppStrings.of(ref).newParcel)),
            ],
          ),
          content: SizedBox(
            width: maxW < 600 ? double.maxFinite : 420,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AppTextField(
                  controller: nameCtrl,
                  label: 'Customer name (optional)',
                ),
                const SizedBox(height: 12),
                AppTextField(
                  controller: phoneCtrl,
                  label: 'Mobile number (optional)',
                  keyboardType: TextInputType.phone,
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
              label: 'Start order',
              onPressed: () => Navigator.pop(context, true),
            ),
          ],
        );
      },
    );
    final customerName = nameCtrl.text;
    final customerPhone = phoneCtrl.text;
    disposeTextControllers([nameCtrl, phoneCtrl]);
    if (ok != true || !mounted) return;

    final parcel = await ref
        .read(appDatabaseProvider)
        .nextTakeAwayParcelNumber();
    if (!mounted) return;
    ref
        .read(billingSessionProvider.notifier)
        .startTakeaway(
          customerName: customerName,
          customerPhone: customerPhone,
          parcelNumber: parcel,
        );
    context.push('/takeaway/billing');
  }

  void openParcelBilling(TakeawayParcel parcel) {
    ref
        .read(billingSessionProvider.notifier)
        .startTakeaway(parcelNumber: parcel.parcelNumber);
    context.push('/takeaway/billing');
  }

  void openParcelCart(TakeawayParcel parcel) {
    ref
        .read(billingSessionProvider.notifier)
        .startTakeaway(parcelNumber: parcel.parcelNumber);
    /* Opens product menu; qty edits stay on product cards (no cart sheet). */
    context.push('/takeaway/billing');
  }

  @override
  Widget build(BuildContext context) {
    final parcels = ref.watch(openTakeawayParcelsProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹ ');
    final useCards = context.isCompactWidth;

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(AppStrings.of(ref).takeAway),
            Text(
              'Parcel counter — open parcels waiting for billing',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w400,
                color: Colors.white70,
              ),
            ),
          ],
        ),
        actions: [
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert),
            onSelected: (value) {
              if (value == 'new') startNewParcel();
            },
            itemBuilder: (context) => const [
              PopupMenuItem(value: 'new', child: Text('New Parcel')),
            ],
          ),
        ],
      ),
      body: parcels.isEmpty
          ? AppEmptyState(
              title: 'No data found',
              message: 'No open parcels. Tap New Parcel to start billing.',
              actionLabel: 'New Parcel',
              onAction: () => startNewParcel(),
            )
          : ResponsiveScrollShell(
              dashboard: true,
              child: Column(
                children: [
                  if (!useCards) ...[
                    const ParcelTableHeader(),
                    const Divider(
                      height: 1,
                      thickness: 1,
                      color: Colors.black87,
                    ),
                  ],
                  Expanded(
                    child: ListView.separated(
                      padding: EdgeInsets.only(
                        top: useCards ? 8 : 0,
                        bottom: 88,
                        left: AppBreakpoints.pagePaddingFor(context.widthClass),
                        right: AppBreakpoints.pagePaddingFor(
                          context.widthClass,
                        ),
                      ),
                      itemCount: parcels.length,
                      separatorBuilder: (_, _) => useCards
                          ? const SizedBox(height: 8)
                          : const Divider(height: 1),
                      itemBuilder: (context, index) {
                        final parcel = parcels[index];
                        if (useCards) {
                          return ParcelCard(
                            index: index + 1,
                            parcelNumber: parcel.parcelNumber,
                            billAmount: currency.format(parcel.billAmount),
                            onAddProducts: () => openParcelBilling(parcel),
                            onOpenCart: () => openParcelCart(parcel),
                          );
                        }
                        return ParcelTableRow(
                          index: index + 1,
                          parcelNumber: parcel.parcelNumber,
                          billAmount: currency.format(parcel.billAmount),
                          onAddProducts: () => openParcelBilling(parcel),
                          onOpenCart: () => openParcelCart(parcel),
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),
      floatingActionButton: parcels.isEmpty
          ? null
          : FloatingActionButton.extended(
              onPressed: () => startNewParcel(),
              icon: const Icon(Icons.add),
              label: const Text('New Parcel'),
            ),
    );
  }
}

class ParcelTableHeader extends StatelessWidget {
  const ParcelTableHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.fromLTRB(16, 14, 8, 10),
      child: Row(
        children: [
          SizedBox(width: 56, child: HeaderCell('Sr No.')),
          Expanded(child: HeaderCell('Parcel No.')),
          Expanded(child: HeaderCell('Bill Amount')),
          SizedBox(width: 88),
        ],
      ),
    );
  }
}

class HeaderCell extends StatelessWidget {
  const HeaderCell(this.label, {super.key});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Text(
      label,
      style: TextStyle(
        fontFamily: AppFonts.family,
        fontSize: 14,
        fontWeight: FontWeight.w700,
        color: AppColors.navy.withValues(alpha: .75),
      ),
    );
  }
}

class ParcelTableRow extends StatelessWidget {
  const ParcelTableRow({
    super.key,
    required this.index,
    required this.parcelNumber,
    required this.billAmount,
    required this.onAddProducts,
    required this.onOpenCart,
  });

  final int index;
  final String parcelNumber;
  final String billAmount;
  final VoidCallback onAddProducts;
  final VoidCallback onOpenCart;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 4, 4, 4),
      child: Row(
        children: [
          SizedBox(
            width: 56,
            child: Text(
              '$index',
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 15,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          Expanded(
            child: Text(
              parcelNumber,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 15,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          Expanded(
            child: Text(
              billAmount,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 15,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          IconButton(
            tooltip: 'Add products',
            onPressed: onAddProducts,
            icon: AppSvg(
              AppAssets.svgTakeaway,
              width: 22,
              height: 22,
              color: AppColors.primary,
            ),
          ),
          IconButton(
            tooltip: 'View cart',
            onPressed: onOpenCart,
            icon: const Icon(Icons.print_outlined, color: AppColors.primary),
          ),
        ],
      ),
    );
  }
}

/* Compact mobile presentation — same actions as the table row. */
class ParcelCard extends StatelessWidget {
  const ParcelCard({
    super.key,
    required this.index,
    required this.parcelNumber,
    required this.billAmount,
    required this.onAddProducts,
    required this.onOpenCart,
  });

  final int index;
  final String parcelNumber;
  final String billAmount;
  final VoidCallback onAddProducts;
  final VoidCallback onOpenCart;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                '#$index',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 13,
                  color: AppColors.textSecondary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const Spacer(),
              Text(
                billAmount,
                style: const TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: AppColors.primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            parcelNumber,
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontSize: 16,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: onAddProducts,
                  icon: const Icon(Icons.add_shopping_cart_outlined, size: 18),
                  label: const Text('Add products'),
                ),
              ),
              const SizedBox(width: 8),
              IconButton.filledTonal(
                tooltip: 'View cart',
                onPressed: onOpenCart,
                icon: const Icon(Icons.print_outlined),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
