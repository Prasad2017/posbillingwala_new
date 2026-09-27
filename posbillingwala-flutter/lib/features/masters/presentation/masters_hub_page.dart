import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type_providers.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Master Data hub — items filtered by active business type profile. */
class MastersHubPage extends ConsumerWidget {
  const MastersHubPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    final profile = ref.watch(businessProfileProvider);

    final items = <MasterItem>[
      for (final m in profile.masterItems)
        if (profile.has(m.feature))
          MasterItem(
            icon: m.icon,
            color: m.color,
            title: m.title,
            subtitle: m.subtitle,
            onTap: () => context.push(m.route),
          ),
    ];

    return Scaffold(
      backgroundColor: Colors.transparent,
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
            context.isShortHeight
                ? AppBreakpoints.densePaddingFor(context.heightClass)
                : 18,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            32,
          ),
          children: [
            Text(
              '${profile.type.shortName.toUpperCase()} · MASTER DATA',
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
                if (cols <= 1) {
                  return Container(
                    decoration: BoxDecoration(
                      color: AppColors.glassFill,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(
                        color: AppColors.glassBorder,
                        width: 1.2,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: AppColors.navy.withValues(alpha: .06),
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
                  );
                }
                return GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: items.length,
                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: cols,
                    mainAxisSpacing: 12,
                    crossAxisSpacing: 12,
                    childAspectRatio: 3.2,
                  ),
                  itemBuilder: (context, index) {
                    return Material(
                      color: AppColors.glassSolid,
                      borderRadius: BorderRadius.circular(18),
                      child: InkWell(
                        onTap: items[index].onTap,
                        borderRadius: BorderRadius.circular(18),
                        child: MasterRowTile(item: items[index]),
                      ),
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
