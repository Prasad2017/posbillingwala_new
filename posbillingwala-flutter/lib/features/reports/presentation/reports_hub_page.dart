import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/license_modules.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Reports Hub — section cards with pastel icon rows (reference UI). */
class ReportsHubPage extends ConsumerWidget {
  const ReportsHubPage({super.key});

  void resetFilters(WidgetRef ref) {
    ref
        .read(reportInvoiceTypeFilterProvider.notifier)
        .select(ReportInvoiceTypeFilter.all);
    ref
        .read(reportPaymentFilterProvider.notifier)
        .select(ReportPaymentFilter.all);
  }

  Future<void> reportsHubPageClearAllInvoices(BuildContext context, WidgetRef ref) async {
    final db = ref.read(appDatabaseProvider);
    final pending = await db.countPendingInvoiceSync();
    if (!context.mounted) return;
    if (pending > 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${AppStrings.of(ref).sync} $pending ${AppStrings.of(ref).syncPendingBills}')),
      );
      return;
    }
    final strings = AppStrings.of(ref);
    final confirm = await showAppConfirmBottomSheet(
      context: context,
      title: strings.deleteAllInvoice,
      message: strings.deleteAllInvoicesHint,
      confirmLabel: strings.ui('ui_delete'),
      cancelLabel: strings.cancel,
      confirmVariant: AppButtonVariant.danger,
      icon: Icons.delete_forever_rounded,
    );
    if (!confirm || !context.mounted) return;
    await db.clearAllInvoices();
    ref.invalidate(todayInvoicesProvider);
    ref.invalidate(monthInvoicesProvider);
    ref.invalidate(periodInvoicesProvider);
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(strings.allInvoicesCleared)),
      );
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(authControllerProvider).session;
    final strings = AppStrings.of(ref);
    final salesItems = <ReportItem>[
      ReportItem(
        icon: Icons.dashboard_customize_rounded,
        color: AppColors.primary,
        title: strings.salesDashboard,
        subtitle: strings.thisBranchOnly,
        onTap: () => context.push('/reports/dashboard'),
      ),
      ReportItem(
        icon: Icons.bar_chart_rounded,
        color: AppColors.purple,
        title: strings.salesOverview,
        subtitle: strings.thisBranchOnly,
        onTap: () => context.push('/reports/overview'),
      ),
      ReportItem(
        icon: Icons.list_alt_rounded,
        color: AppColors.green,
        title: strings.salesList,
        subtitle: strings.billsAndLines,
        onTap: () => context.push('/reports/sales-list'),
      ),
      if (session?.userManagementEnabled ?? false)
        ReportItem(
          icon: Icons.badge_rounded,
          color: AppColors.purple,
          title: 'User-wise sales',
          subtitle: 'Bills and business by staff',
          onTap: () => context.push('/reports/staff-wise'),
        ),
    ];

    final operationalItems = <ReportItem>[
      ReportItem(
        icon: Icons.receipt_long_rounded,
        color: AppColors.green,
        title: strings.invoiceReport,
        subtitle: strings.reportDetail,
        onTap: () {
          resetFilters(ref);
          context.push('/reports/invoices');
        },
      ),
      if (LicenseModules.fastBilling(session))
        ReportItem(
          icon: Icons.trending_up_rounded,
          color: AppColors.orange,
          title: strings.saleWiseReport,
          subtitle: strings.saleReports,
          onTap: () => context.push('/reports/sale'),
        ),
      if (LicenseModules.dineIn(session)) ...[
        ReportItem(
          icon: Icons.table_restaurant_rounded,
          color: const Color(0xFF1A4FD8),
          title: strings.invoiceTableReport,
          subtitle: strings.saleReports,
          onTap: () => context.push('/reports/table'),
        ),
        ReportItem(
          icon: Icons.table_rows_rounded,
          color: const Color(0xFF1A4FD8),
          title: strings.invoiceTableListReport,
          subtitle: strings.saleReports,
          onTap: () => context.push('/reports/table-list'),
        ),
      ],
      if (LicenseModules.takeAway(session))
        ReportItem(
          icon: Icons.shopping_basket_rounded,
          color: AppColors.purple,
          title: strings.invoiceTakeAwayReport,
          subtitle: strings.saleReports,
          onTap: () => context.push('/reports/takeaway'),
        ),
      ReportItem(
        icon: Icons.credit_card_rounded,
        color: AppColors.green,
        title: strings.invoicePaymentModeReport,
        subtitle: strings.saleReports,
        onTap: () => context.push('/reports/payment-mode'),
      ),
      ReportItem(
        icon: Icons.trending_up_rounded,
        color: AppColors.orange,
        title: strings.discountWiseReport,
        subtitle: strings.saleReports,
        onTap: () => context.push('/reports/discount'),
      ),
      ReportItem(
        icon: Icons.trending_down_rounded,
        color: AppColors.red,
        title: strings.refundWiseReport,
        subtitle: strings.saleReports,
        onTap: () => context.push('/reports/refund'),
      ),
      ReportItem(
        icon: Icons.inventory_2_rounded,
        color: const Color(0xFFE6A100),
        title: strings.productWiseReport,
        subtitle: strings.saleReports,
        onTap: () => context.push('/reports/products'),
      ),
      ReportItem(
        icon: Icons.filter_none_rounded,
        color: AppColors.primary,
        title: strings.comboWiseReport,
        subtitle: strings.saleReports,
        onTap: () => context.push('/reports/products?type=combo'),
      ),
      ReportItem(
        icon: Icons.money_off_rounded,
        color: AppColors.purple,
        title: strings.expenseWiseReport,
        subtitle: strings.saleReports,
        onTap: () => context.push('/reports/expense'),
      ),
      if (LicenseModules.mess(session)) ...[
        ReportItem(
          icon: Icons.groups_rounded,
          color: AppColors.green,
          title: strings.invoiceMemberReport,
          subtitle: strings.saleReports,
          onTap: () => context.push('/reports/mess-members'),
        ),
        ReportItem(
          icon: Icons.payments_rounded,
          color: AppColors.primary,
          title: strings.memberPaymentReport,
          subtitle: strings.saleReports,
          onTap: () => context.push('/reports/mess-payments'),
        ),
        ReportItem(
          icon: Icons.restaurant_rounded,
          color: const Color(0xFFE6A100),
          title: strings.invoiceMessReport,
          subtitle: strings.saleReports,
          onTap: () => context.push('/reports/mess'),
        ),
      ],
    ];

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(strings.reports),
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
            ReportSectionLabel(strings.salesAndAnalytics),
            const SizedBox(height: 12),
            ReportGroupCard(items: salesItems),
            const SizedBox(height: 18),
            ReportSectionLabel(strings.operationalReports),
            const SizedBox(height: 12),
            ReportGroupCard(items: operationalItems),
            const SizedBox(height: 18),
            ReportSectionLabel(strings.dataManagement),
            const SizedBox(height: 12),
            ReportGroupCard(
              items: [
                ReportItem(
                  icon: Icons.delete_forever_rounded,
                  color: AppColors.red,
                  title: strings.deleteAllInvoice,
                  subtitle: strings.deleteAllInvoicesHint,
                  titleSecondary: '(सर्व बिले हटवा)',
                  onTap: () => reportsHubPageClearAllInvoices(context, ref),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class ReportItem {
  const ReportItem({
    required this.icon,
    required this.color,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.titleSecondary,
  });

  final IconData icon;
  final Color color;
  final String title;
  final String subtitle;
  final String? titleSecondary;
  final VoidCallback onTap;
}

class ReportGroupCard extends StatelessWidget {
  const ReportGroupCard({super.key, required this.items});

  final List<ReportItem> items;

  @override
  Widget build(BuildContext context) {
    return ReportSurfaceCard(
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
            ReportRowTile(item: items[i]),
          ],
        ],
      ),
    );
  }
}

class ReportRowTile extends StatelessWidget {
  const ReportRowTile({super.key, required this.item});

  final ReportItem item;

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
                    if (item.titleSecondary != null) ...[
                      const SizedBox(height: 2),
                      Text(
                        item.titleSecondary!,
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          color: AppColors.navy.withValues(alpha: .55),
                          fontWeight: FontWeight.w600,
                          fontSize: 12.5,
                        ),
                      ),
                    ],
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
