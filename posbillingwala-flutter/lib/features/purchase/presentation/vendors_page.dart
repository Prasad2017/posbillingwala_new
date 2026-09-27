import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_providers.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/vendor.dart';

class VendorsPage extends ConsumerWidget {
  const VendorsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(vendorsProvider);

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: const Text('Vendors'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: () => ref.invalidate(vendorsProvider),
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/purchase/vendors/new'),
        icon: const Icon(Icons.add),
        label: const Text('Add vendor'),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (vendors) {
          if (vendors.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'No vendors yet.\nAdd suppliers to raise purchase orders.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    color: AppColors.navy.withValues(alpha: .55),
                    height: 1.4,
                  ),
                ),
              ),
            );
          }
          return ResponsiveScrollShell(
            dashboard: true,
            child: ListView.separated(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                16,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                100,
              ),
              itemCount: vendors.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final v = vendors[index];
                return _VendorTile(
                  vendor: v,
                  onTap: () => context.push('/purchase/vendors/${v.id}'),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

class _VendorTile extends StatelessWidget {
  const _VendorTile({required this.vendor, required this.onTap});

  final Vendor vendor;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.glassSolid,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.border.withValues(alpha: .7)),
          ),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: AppColors.orange.withValues(alpha: .12),
                child: Text(
                  vendor.name.isEmpty ? '?' : vendor.name[0].toUpperCase(),
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w800,
                    color: AppColors.orange,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      vendor.name,
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        fontWeight: FontWeight.w800,
                        color: AppColors.navy,
                        fontSize: 15,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      [
                        if (vendor.mobile.isNotEmpty) vendor.mobile,
                        if (vendor.gstin.isNotEmpty) 'GSTIN ${vendor.gstin}',
                        vendor.paymentTerms,
                      ].join(' · '),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 12.5,
                        color: AppColors.navy.withValues(alpha: .5),
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: vendor.isActive
                      ? AppColors.green.withValues(alpha: .12)
                      : AppColors.red.withValues(alpha: .12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  vendor.isActive ? 'Active' : 'Inactive',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: vendor.isActive ? AppColors.green : AppColors.red,
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
