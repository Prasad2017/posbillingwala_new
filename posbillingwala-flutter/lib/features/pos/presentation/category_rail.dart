import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Vertical category rail for tablet / landscape POS (mockup 3-column). */
class CategoryRail extends ConsumerWidget {
  const CategoryRail({
    super.key,
    required this.categoriesAsync,
    required this.selectedCategoryId,
    this.width = 92,
  });

  final AsyncValue<List<ProductCategory>> categoriesAsync;
  final int? selectedCategoryId;
  final double width;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      width: width,
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border(
          right: BorderSide(color: Colors.black.withValues(alpha: 0.08)),
        ),
      ),
      child: categoriesAsync.when(
        data: (categories) {
          if (categories.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: Text(
                  AppStrings.of(ref).noCategoriesFound,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 11),
                ),
              ),
            );
          }
          return ListView(
            padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 6),
            children: [
              _RailItem(
                label: 'All',
                icon: Icons.grid_view_rounded,
                selected: selectedCategoryId == null,
                onTap: () {
                  ref.read(posSelectedCategoryIdProvider.notifier).select(null);
                  ref
                      .read(posSelectedSubcategoryIdProvider.notifier)
                      .select(null);
                },
              ),
              const SizedBox(height: 8),
              ...categories.map(
                (category) => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: _RailItem(
                    label: category.categoryName,
                    icon: Icons.restaurant_menu_rounded,
                    selected: selectedCategoryId == category.categoryId,
                    onTap: () => ref
                        .read(posSelectedCategoryIdProvider.notifier)
                        .select(category.categoryId),
                  ),
                ),
              ),
            ],
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e', textAlign: TextAlign.center)),
      ),
    );
  }
}

class _RailItem extends StatelessWidget {
  const _RailItem({
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
      color: selected ? AppColors.primarySoft : Colors.transparent,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 4),
          child: Column(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: selected ? AppColors.primary : AppColors.primarySoft,
                ),
                child: Icon(
                  icon,
                  size: 22,
                  color: selected ? Colors.white : AppColors.primary,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                label,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: selected ? FontWeight.w800 : FontWeight.w600,
                  color: selected ? AppColors.primary : AppColors.navy,
                  height: 1.15,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
