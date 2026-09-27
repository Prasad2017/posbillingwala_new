import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_document.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_providers.dart';

/* Purchase hub — vendors, POs, GRNs for retail business types. */
class PurchaseHubPage extends ConsumerWidget {
  const PurchaseHubPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final vendors = ref.watch(vendorsProvider);
    final orders = ref.watch(purchaseOrdersProvider);
    final grns = ref.watch(purchaseGrnsProvider);
    final openOrders = orders
        .where(
          (d) =>
              d.status != PurchaseDocStatus.received &&
              d.status != PurchaseDocStatus.cancelled,
        )
        .length;
    final vendorCount = vendors.maybeWhen(data: (v) => v.length, orElse: () => 0);

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: const Text('Purchase'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () {
            if (context.canPop()) {
              context.pop();
            } else {
              context.go('/');
            }
          },
        ),
      ),
      body: ResponsivePageBody(
        dashboard: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            LayoutBuilder(
              builder: (context, constraints) {
                final cols = AppBreakpoints.cardColumnsForWidth(
                  constraints.maxWidth.clamp(0, 900),
                ).clamp(1, 3);
                final cards = [
                  _Kpi(
                    label: 'Vendors',
                    value: '$vendorCount',
                    icon: Icons.storefront_rounded,
                    color: AppColors.orange,
                  ),
                  _Kpi(
                    label: 'Open POs',
                    value: '$openOrders',
                    icon: Icons.local_shipping_rounded,
                    color: AppColors.primary,
                  ),
                  _Kpi(
                    label: 'GRNs',
                    value: '${grns.length}',
                    icon: Icons.inventory_2_rounded,
                    color: AppColors.green,
                  ),
                ];
                if (cols <= 1) {
                  return Column(
                    children: [
                      for (final c in cards) ...[
                        c,
                        const SizedBox(height: 10),
                      ],
                    ],
                  );
                }
                return Row(
                  children: [
                    for (var i = 0; i < cards.length; i++) ...[
                      if (i > 0) const SizedBox(width: 10),
                      Expanded(child: cards[i]),
                    ],
                  ],
                );
              },
            ),
            const SizedBox(height: 18),
            const Text(
              'ACTIONS',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 11,
                letterSpacing: 1,
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: 10),
            _ActionTile(
              icon: Icons.add_business_rounded,
              color: AppColors.orange,
              title: 'Vendors',
              subtitle: 'Supplier master, GSTIN, credit terms',
              onTap: () => context.push('/purchase/vendors'),
            ),
            const SizedBox(height: 10),
            _ActionTile(
              icon: Icons.post_add_rounded,
              color: AppColors.primary,
              title: 'New Purchase Order',
              subtitle: 'Create PO against a vendor',
              onTap: () => context.push('/purchase/orders/new'),
            ),
            const SizedBox(height: 10),
            _ActionTile(
              icon: Icons.list_alt_rounded,
              color: AppColors.green,
              title: 'Purchase Orders',
              subtitle: 'View, edit and receive stock (GRN)',
              onTap: () => context.push('/purchase/orders'),
            ),
            const SizedBox(height: 10),
            _ActionTile(
              icon: Icons.receipt_long_rounded,
              color: const Color(0xFF5B6CFF),
              title: 'GRN History',
              subtitle: 'Goods received notes & stock updates',
              onTap: () => context.push('/purchase/grns'),
            ),
            const SizedBox(height: 22),
            const Text(
              'RECENT ORDERS',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 11,
                letterSpacing: 1,
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: 10),
            if (orders.isEmpty)
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: AppColors.glassFill,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.glassBorder),
                ),
                child: Text(
                  'No purchase orders yet. Create a vendor, then raise a PO.',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    color: AppColors.navy.withValues(alpha: .55),
                  ),
                ),
              )
            else
              for (final doc in orders.take(5))
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: _DocTile(
                    doc: doc,
                    onTap: () => context.push('/purchase/orders/${doc.id}'),
                  ),
                ),
          ],
        ),
      ),
    );
  }
}

class _Kpi extends StatelessWidget {
  const _Kpi({
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
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: color.withValues(alpha: .08),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withValues(alpha: .2)),
      ),
      child: Row(
        children: [
          Icon(icon, color: color, size: 22),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  value,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w800,
                    fontSize: 20,
                    color: color,
                  ),
                ),
                Text(
                  label,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12,
                    color: AppColors.navy.withValues(alpha: .55),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionTile extends StatelessWidget {
  const _ActionTile({
    required this.icon,
    required this.color,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final Color color;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.glassSolid,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: color.withValues(alpha: .12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: color),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        fontWeight: FontWeight.w800,
                        fontSize: 15,
                        color: AppColors.navy,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 12.5,
                        color: AppColors.navy.withValues(alpha: .5),
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right_rounded,
                color: AppColors.navy.withValues(alpha: .3),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _DocTile extends StatelessWidget {
  const _DocTile({required this.doc, required this.onTap});

  final PurchaseDocument doc;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final money = MoneyFormat.inrSpaced;
    final date = doc.updatedAt ?? doc.createdAt;
    return Material(
      color: AppColors.glassSolid,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${doc.docNo} · ${doc.vendorName}',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        fontWeight: FontWeight.w700,
                        color: AppColors.navy,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '${doc.status.label}'
                      '${date == null ? '' : ' · ${DateFormat('dd MMM yyyy').format(date)}'}',
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 12,
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
  }
}
