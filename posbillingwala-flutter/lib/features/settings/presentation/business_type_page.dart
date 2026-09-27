import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type_providers.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type_registry.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

/* Owner selects business vertical → dynamic menus / POS / masters. */
class BusinessTypePage extends ConsumerWidget {
  const BusinessTypePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final current = ref.watch(businessTypeControllerProvider);
    final profile = BusinessTypeRegistry.of(current);

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: const Text('Business Type'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: ResponsivePageBody(
        dashboard: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.primarySoft,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                children: [
                  const Icon(Icons.storefront_rounded, color: AppColors.primary),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Active: ${current.displayName}',
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            fontWeight: FontWeight.w800,
                            color: AppColors.navy,
                            fontSize: 15,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Menus, terminology and modules update instantly. '
                          '${profile.features.length} modules enabled.',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            color: AppColors.navy.withValues(alpha: .62),
                            fontSize: 12.5,
                            height: 1.35,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),
            Text(
              'SELECT BUSINESS TYPE',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 11,
                letterSpacing: 1.0,
                color: AppColors.textSecondary.withValues(alpha: .85),
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 12),
            LayoutBuilder(
              builder: (context, constraints) {
                final cols = AppBreakpoints.cardColumnsForWidth(
                  constraints.maxWidth,
                );
                final types = BusinessType.values;
                if (cols <= 1) {
                  return Column(
                    children: [
                      for (final type in types)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: _TypeTile(
                            type: type,
                            selected: type == current,
                            onTap: () => _select(context, ref, type),
                          ),
                        ),
                    ],
                  );
                }
                return GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: types.length,
                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: cols,
                    mainAxisSpacing: 12,
                    crossAxisSpacing: 12,
                    childAspectRatio: 2.4,
                  ),
                  itemBuilder: (context, index) {
                    final type = types[index];
                    return _TypeTile(
                      type: type,
                      selected: type == current,
                      onTap: () => _select(context, ref, type),
                    );
                  },
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _select(
    BuildContext context,
    WidgetRef ref,
    BusinessType type,
  ) async {
    await ref.read(businessTypeControllerProvider.notifier).setBusinessType(type);
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Business type set to ${type.displayName}. UI modules updated.',
        ),
      ),
    );
  }
}

class _TypeTile extends StatelessWidget {
  const _TypeTile({
    required this.type,
    required this.selected,
    required this.onTap,
  });

  final BusinessType type;
  final bool selected;
  final VoidCallback onTap;

  IconData get _icon => switch (type) {
    BusinessType.restaurant || BusinessType.cafe || BusinessType.bar =>
      Icons.restaurant_rounded,
    BusinessType.hotel => Icons.hotel_rounded,
    BusinessType.clothingStore || BusinessType.footwear => Icons.checkroom_rounded,
    BusinessType.supermarket || BusinessType.groceryStore => Icons.store_mall_directory_rounded,
    BusinessType.pharmacy => Icons.local_pharmacy_rounded,
    BusinessType.electronicsHardware => Icons.devices_other_rounded,
    BusinessType.cakeShopBakery => Icons.cake_rounded,
    BusinessType.coldDrinksBeverage => Icons.local_drink_rounded,
    BusinessType.cosmetics => Icons.spa_rounded,
    BusinessType.homeKitchen => Icons.kitchen_rounded,
    _ => Icons.storefront_rounded,
  };

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected ? AppColors.primarySoft : AppColors.glassSolid,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: selected ? AppColors.primary : AppColors.border,
              width: selected ? 1.6 : 1,
            ),
          ),
          child: Row(
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: (selected ? AppColors.primary : AppColors.navy)
                      .withValues(alpha: .10),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  _icon,
                  color: selected ? AppColors.primary : AppColors.navy,
                  size: 22,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      type.displayName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontWeight: FontWeight.w800,
                        fontSize: 14,
                        color: selected ? AppColors.primary : AppColors.navy,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      type.description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 11.5,
                        height: 1.3,
                        color: AppColors.navy.withValues(alpha: .55),
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                selected
                    ? Icons.radio_button_checked
                    : Icons.radio_button_off,
                color: selected
                    ? AppColors.primary
                    : AppColors.navy.withValues(alpha: .35),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
