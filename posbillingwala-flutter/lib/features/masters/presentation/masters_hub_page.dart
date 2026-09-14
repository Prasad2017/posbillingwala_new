import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/// Master Data hub — section card with pastel icon rows (reference UI).
class MastersHubPage extends ConsumerWidget {
  const MastersHubPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);

    final items = <MasterItem>[
      MasterItem(
        icon: Icons.category_rounded,
        color: AppColors.primary,
        title: strings.categories,
        subtitle: 'Food groups such as Veg, Non Veg',
        onTap: () => context.push('/masters/categories'),
      ),
      MasterItem(
        icon: Icons.folder_rounded,
        color: AppColors.purple,
        title: strings.subcategories,
        subtitle: 'Starter, Main Course, Beverage',
        onTap: () => context.push('/masters/subcategories'),
      ),
      MasterItem(
        icon: Icons.layers_rounded,
        color: AppColors.orange,
        title: strings.portions,
        subtitle: 'Half, Full and other sizes',
        onTap: () => context.push('/masters/portion-masters'),
      ),
      MasterItem(
        icon: Icons.inventory_2_rounded,
        color: AppColors.green,
        title: strings.products,
        subtitle: 'Menu items and prices',
        onTap: () => context.push('/masters/products'),
      ),
      MasterItem(
        icon: Icons.filter_none_rounded,
        color: const Color(0xFF5B6CFF),
        title: strings.combos,
        subtitle: 'Combo meals and offers',
        onTap: () => context.push('/masters/combos'),
      ),
      MasterItem(
        icon: Icons.table_restaurant_rounded,
        color: const Color(0xFFE6A100),
        title: strings.tableMaster,
        subtitle: 'Areas, types and tables with seats',
        onTap: () => context.push('/masters/tables'),
      ),
    ];

    return Scaffold(
      backgroundColor: const Color(0xFFF3F7FC),
      appBar: AppBar(
        title: Text(strings.masterData),
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
            18,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            32,
          ),
          children: [
            Text(
              'MASTER DATA',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 11,
                letterSpacing: 1.0,
                color: AppColors.textSecondary.withValues(alpha: .85),
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 12),
            context.widthClass.index >= AppWidthClass.expanded.index
                ? GridView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: items.length,
                    gridDelegate:
                        SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: AppBreakpoints.cardColumnsFor(
                        context.widthClass,
                      ),
                      mainAxisSpacing: 12,
                      crossAxisSpacing: 12,
                      childAspectRatio: 3.2,
                    ),
                    itemBuilder: (context, index) {
                      return Material(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(18),
                        child: InkWell(
                          onTap: items[index].onTap,
                          borderRadius: BorderRadius.circular(18),
                          child: MasterRowTile(item: items[index]),
                        ),
                      );
                    },
                  )
                : Container(
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(
                        color: AppColors.border.withValues(alpha: .75),
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: AppColors.navy.withValues(alpha: .05),
                          blurRadius: 16,
                          offset: const Offset(0, 6),
                        ),
                      ],
                    ),
                    clipBehavior: Clip.antiAlias,
                    child: Column(
                      children: [
                        for (var i = 0; i < items.length; i++) ...[
                          if (i > 0)
                            Divider(
                              height: 1,
                              thickness: 1,
                              color: AppColors.border.withValues(alpha: .7),
                              indent: 72,
                              endIndent: 16,
                            ),
                          MasterRowTile(item: items[i]),
                        ],
                      ],
                    ),
                  ),
          ],
        ),
      ),
    );
  }
}

class MasterItem {
  const MasterItem({
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
}

class MasterRowTile extends StatelessWidget {
  const MasterRowTile({super.key, required this.item});

  final MasterItem item;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: item.onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 14, 12, 14),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: item.color.withValues(alpha: .12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(item.icon, color: item.color, size: 22),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.title,
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        color: AppColors.navy,
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                        height: 1.2,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      item.subtitle,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        color: AppColors.navy.withValues(alpha: .48),
                        fontWeight: FontWeight.w400,
                        fontSize: 12.5,
                        height: 1.3,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Icon(
                Icons.chevron_right_rounded,
                color: AppColors.navy.withValues(alpha: .28),
                size: 26,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
