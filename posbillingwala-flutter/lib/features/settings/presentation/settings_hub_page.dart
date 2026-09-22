import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/licence_display.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_pin_gate.dart';
import 'package:pos_billingwala_v2/features/settings/domain/in_app_update_service.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_widgets.dart';
import 'package:pos_billingwala_v2/features/sync/domain/full_sync_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_progress.dart';
import 'package:pos_billingwala_v2/language/app_languages.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Settings hub — card groups matching the Settings reference UI. */
class SettingsHubPage extends ConsumerWidget {
  const SettingsHubPage({super.key});

  Future<void> rateUs(BuildContext context) async {
    final ok = await inAppUpdateService.openPlayStore();
    if (!ok && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Could not open Play Store')),
      );
    }
  }

  Future<void> checkUpdate(BuildContext context, WidgetRef ref) async {
    final strings = AppStrings.of(ref);
    final outcome = await inAppUpdateService.checkAvailability();
    if (!context.mounted) return;
    if (outcome.status == InAppUpdateStatus.notAvailable) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(strings.appUpdateNotAvailable)));
      return;
    }
    if (outcome.status == InAppUpdateStatus.failed &&
        InAppUpdateService.isAndroidPlay) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(strings.appFailedToUpdate)));
      return;
    }
    if (outcome.status == InAppUpdateStatus.available ||
        outcome.status == InAppUpdateStatus.downloaded) {
      final go = await showAppConfirmBottomSheet(
        context: context,
        title: strings.newVersionAvailable,
        message: strings.updateBeforeContinue,
        confirmLabel: strings.updateApp,
        cancelLabel: strings.cancel,
        icon: Icons.system_update_rounded,
      );
      if (!context.mounted || !go) return;
      final started = await inAppUpdateService.startUpdate(
        preferImmediate: true,
      );
      if (!context.mounted) return;
      if (started.status == InAppUpdateStatus.failed) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(strings.appFailedToUpdate)));
      }
      return;
    }
    await inAppUpdateService.openPlayStore();
  }

  Future<void> pickLanguage(BuildContext context, WidgetRef ref) async {
    final current = ref.read(appLocaleProvider).languageCode;
    final selected = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.glassSolid,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      builder: (ctx) {
        final maxH = MediaQuery.sizeOf(ctx).height * 0.72;
        return SafeArea(
          child: ConstrainedBox(
            constraints: BoxConstraints(maxHeight: maxH),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(24, 4, 24, 12),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColors.primary.withValues(alpha: .10),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: const Icon(
                          Icons.language_rounded,
                          color: AppColors.primary,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          AppStrings.of(ref).language,
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            fontSize: 18,
                            fontWeight: FontWeight.w800,
                            color: AppColors.navy,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                Flexible(
                  child: ListView.separated(
                    shrinkWrap: true,
                    padding: const EdgeInsets.fromLTRB(12, 0, 12, 20),
                    itemCount: AppLanguages.options.length,
                    separatorBuilder: (_, _) => Divider(
                      height: 1,
                      color: AppColors.border.withValues(alpha: .7),
                    ),
                    itemBuilder: (context, index) {
                      final option = AppLanguages.options[index];
                      final selectedLang = current == option.code;
                      return ListTile(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        leading: Icon(
                          selectedLang
                              ? Icons.radio_button_checked
                              : Icons.radio_button_off,
                          color: selectedLang
                              ? AppColors.primary
                              : AppColors.navy.withValues(alpha: .45),
                        ),
                        title: Text(
                          option.nativeLabel,
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontWeight: FontWeight.w700,
                            color: AppColors.navy,
                          ),
                        ),
                        subtitle: option.label == option.nativeLabel
                            ? null
                            : Text(
                                option.label,
                                style: TextStyle(
                                  fontFamily: AppFonts.family,
                                  color: AppColors.navy.withValues(alpha: .55),
                                ),
                              ),
                        trailing: selectedLang
                            ? const Icon(
                                Icons.check_rounded,
                                color: AppColors.primary,
                              )
                            : null,
                        onTap: () => Navigator.pop(ctx, option.code),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
    if (selected == null || selected == current) return;

    await ref.read(appLocaleProvider.notifier).setLanguage(selected);
    if (!context.mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).languageApplied)));
  }

  Future<void> confirmFetchFromCloud(
    BuildContext context,
    WidgetRef ref,
  ) async {
    if (!await ensureOnline(force: true)) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text(kOnlineRequiredMessage)));
      return;
    }
    if (!context.mounted) return;

    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Do you want to confirm to fetch from cloud?',
      message: AppPlatform.supportsOfflineSync
          ? 'Local data will be replaced with cloud data. Unsynced bills cannot be overwritten — sync them first.'
          : 'Local cache will be replaced with the latest cloud data. Internet is required.',
      confirmLabel: 'YES',
      cancelLabel: 'NO',
      icon: Icons.cloud_download_rounded,
    );
    if (!ok || !context.mounted) return;

    final db = ref.read(appDatabaseProvider);
    final pendingBills = await db.countPendingSyncInvoices();
    if (!context.mounted) return;
    if (pendingBills > 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            AppPlatform.supportsOfflineSync
                ? '$pendingBills unsynced bill(s) — sync them first before fetch.'
                : '$pendingBills bill(s) not uploaded yet — wait for upload or retry payment sync before refresh.',
          ),
        ),
      );
      return;
    }

    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Data fetching started')));

    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return PopScope(
          canPop: false,
          child: Consumer(
            builder: (context, ref, _) {
              final progress = ref.watch(syncProgressProvider);
              final current = progress.currentIndex.clamp(
                1,
                progress.totalCount,
              );
              final total = progress.totalCount;
              return AlertDialog(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                title: const Text(
                  'Loading...',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w800,
                    fontSize: 18,
                    color: AppColors.navy,
                  ),
                ),
                content: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const SizedBox(height: 8),
                    const SizedBox(
                      width: 42,
                      height: 42,
                      child: CircularProgressIndicator(
                        strokeWidth: 3.2,
                        color: AppColors.teal,
                      ),
                    ),
                    const SizedBox(height: 18),
                    Text(
                      'Fetching data... ($current/$total)',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        color: AppColors.navy.withValues(alpha: .72),
                        fontWeight: FontWeight.w500,
                        fontSize: 14.5,
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        );
      },
    );

    final result = await ref
        .read(fullSyncControllerProvider.notifier)
        .resetAndFetchWithProgress();

    if (!context.mounted) return;
    Navigator.of(context, rootNavigator: true).pop();

    if (result.localCounts != null) {
      await context.push('/sync/fetch-result');
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          result.failed == 0
              ? 'Fetch complete — local data updated from cloud'
              : result.message,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    final topInset = MediaQuery.paddingOf(context).top;
    final perms = ref.watch(permissionControllerProvider);

    final billingItems = <SettingsItem>[
      if (perms.allows('bill.view'))
        SettingsItem(
          icon: Icons.receipt_long_rounded,
          color: AppColors.purple,
          title: strings.invoiceDetails,
          subtitle: 'Bill format, reprints & invoice list.',
          onTap: () {
            ref.read(reportPeriodProvider.notifier).useAll();
            context.push('/reports/invoices');
          },
        ),
      if (perms.allows('report.view'))
        SettingsItem(
          icon: Icons.bar_chart_rounded,
          color: AppColors.green,
          title: strings.reports,
          subtitle: 'Sales, product & payment reports.',
          onTap: () => pushReportsUnlocked(context, ref),
        ),
      if (perms.allows('product.view'))
        SettingsItem(
          icon: Icons.inventory_2_rounded,
          color: AppColors.primary,
          title: strings.masterData,
          subtitle: 'Categories, products & combos',
          onTap: () => context.push('/masters'),
        ),
    ];

    final storeItems = <SettingsItem>[
      if (perms.allows('settings.view'))
        SettingsItem(
          icon: Icons.storefront_rounded,
          color: AppColors.orange,
          title: strings.shopDetails,
          subtitle: 'Business profile & cloud company',
          onTap: () => context.push('/settings/company'),
        ),
      if (perms.allows('printer.view'))
        SettingsItem(
          icon: Icons.print_rounded,
          color: AppColors.primary,
          title: strings.printerDetails,
          subtitle: AppPlatform.isWeb
              ? 'Bill / KOT · paper size · bill format'
              : 'Bluetooth / USB · 2-Inch / 3-Inch',
          onTap: () => context.push('/settings/devices'),
        ),
      if ((ref.watch(authControllerProvider).session?.userManagementEnabled ??
              false) &&
          perms.allows('user.view'))
        SettingsItem(
          icon: Icons.people_alt_rounded,
          color: AppColors.purple,
          title: 'Users',
          subtitle: 'Staff, roles and permissions',
          onTap: () => context.push('/settings/users'),
        ),
      if ((ref.watch(authControllerProvider).session?.userManagementEnabled ??
              false) &&
          perms.allows('user.view'))
        SettingsItem(
          icon: Icons.payments_rounded,
          color: AppColors.green,
          title: 'Salary',
          subtitle: 'Monthly salary and payments',
          onTap: () => context.push('/settings/salary'),
        ),
      if (perms.allows('device.view'))
        SettingsItem(
          icon: Icons.phonelink_setup_rounded,
          color: AppColors.orange,
          title: 'Devices',
          subtitle: 'Authorized POS devices for this store',
          onTap: () => context.push('/settings/pos-devices'),
        ),
      if (perms.allows('settings.view'))
        SettingsItem(
          icon: Icons.schedule_rounded,
          color: AppColors.green,
          title: strings.businessHours,
          subtitle: 'Opening and closing times',
          onTap: () => context.push('/settings/business-hours'),
        ),
      if (perms.allows('settings.view') && !AppPlatform.isWeb)
        SettingsItem(
          icon: Icons.qr_code_2_rounded,
          color: AppColors.primary,
          title: strings.paymentDisplayTitle,
          subtitle: strings.paymentDisplaySubtitle,
          onTap: () => context.push('/settings/payment-display'),
        ),
      if (perms.allows('inventory.view'))
        SettingsItem(
          icon: Icons.warehouse_rounded,
          color: AppColors.teal,
          title: strings.inventory,
          subtitle: 'Stock ledger',
          onTap: () => context.push('/inventory'),
        ),
      if (perms.allows('expense.view'))
        SettingsItem(
          icon: Icons.account_balance_wallet_rounded,
          color: const Color(0xFFE91E63),
          title: strings.expenses,
          subtitle: 'Shop expenses',
          onTap: () => context.push('/expenses'),
        ),
    ];

    final cloudItems = <SettingsItem>[
      SettingsItem(
        icon: Icons.cloud_download_rounded,
        color: AppColors.orange,
        title: AppPlatform.requiresNetwork
            ? 'Refresh Data From Cloud'
            : 'Fetch Data From Cloud',
        subtitle: AppPlatform.requiresNetwork
            ? 'Download latest data (online required)'
            : 'Download latest data from cloud',
        onTap: () => confirmFetchFromCloud(context, ref),
      ),
      if (AppPlatform.supportsOfflineSync)
        SettingsItem(
          icon: Icons.cloud_upload_rounded,
          color: AppColors.primary,
          title: 'Offline Data Synchronize with Cloud',
          subtitle: 'Upload offline bills to cloud',
          onTap: () => context.push('/sync?mode=sync'),
        ),
      SettingsItem(
        icon: Icons.support_agent_rounded,
        color: AppColors.purple,
        title: strings.helpSupport,
        subtitle: 'Tickets and contact',
        onTap: () => context.push('/support'),
      ),
      if (!AppPlatform.isWeb)
        SettingsItem(
          icon: Icons.system_update_rounded,
          color: AppColors.primary,
          title: strings.updateApp,
          subtitle: strings.updateAppHint,
          onTap: () => checkUpdate(context, ref),
        ),
      if (!AppPlatform.isWeb)
        SettingsItem(
          icon: Icons.star_rounded,
          color: AppColors.yellow,
          title: 'Rate Us',
          subtitle: 'Open Play Store listing',
          onTap: () => rateUs(context),
        ),
      SettingsItem(
        icon: Icons.language_rounded,
        color: const Color(0xFFE91E63),
        title: strings.language,
        subtitle: 'English / हिंदी / मराठी',
        onTap: () => pickLanguage(context, ref),
      ),
      SettingsItem(
        icon: Icons.info_outline_rounded,
        color: AppColors.green,
        title: strings.about,
        subtitle: AppConstants.appVersionLabel,
        onTap: () => context.push('/settings/about'),
      ),
      if (!AppPlatform.isWeb)
        SettingsItem(
          icon: Icons.share_rounded,
          color: AppColors.orange,
          title: strings.shareApp,
          subtitle: 'Invite others via Play Store link',
          onTap: () => context.push('/settings/share'),
        ),
    ];

    final session = ref.watch(authControllerProvider).session;
    final accountItems = <SettingsItem>[
      SettingsItem(
        icon: LicenceDisplay.isTestingLicence(session)
            ? Icons.science_outlined
            : Icons.verified_outlined,
        color: LicenceDisplay.isTestingLicence(session)
            ? const Color(0xFFB45309)
            : AppColors.primary,
        title: LicenceDisplay.planLabel(session),
        subtitle: LicenceDisplay.subtitle(session),
        onTap: () {
          if (LicenceDisplay.isTestingLicence(session)) {
            callSupport(context);
          }
        },
      ),
      SettingsItem(
        icon: Icons.pin_rounded,
        color: AppColors.primary,
        title: 'Change App Login PB-PIN',
        subtitle: 'Used after Logout to unlock the app',
        onTap: () => context.push('/settings/change-pin'),
      ),
      SettingsItem(
        icon: Icons.logout_rounded,
        color: AppColors.red,
        title: 'Logout',
        subtitle: 'Lock app — next open asks for PB-PIN',
        onTap: () async {
          final um =
              ref.read(authControllerProvider).session?.userManagementEnabled ??
              false;
          final ok = await showAppConfirmBottomSheet(
            context: context,
            title: um ? 'Switch user' : 'Logout',
            message: um
                ? 'Return to staff / PB-PIN login? Licence stays on this device.'
                : 'Lock this app? You will need your PB-PIN next time.',
            confirmLabel: 'Logout',
            confirmVariant: AppButtonVariant.danger,
            icon: Icons.logout_rounded,
          );
          if (ok) {
            await ref.read(authControllerProvider.notifier).logout();
          }
        },
      ),
    ];

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Column(
        children: [
          SettingsHeader(
            topInset: AppPlatform.useDesktopShell ? 12 : topInset,
            title: strings.settings,
            subtitle: 'Manage your business, devices and app preferences',
            onBack: () {
              if (context.canPop()) {
                context.pop();
              } else {
                context.go('/');
              }
            },
            showBack: !AppPlatform.useDesktopShell,
          ),
          Expanded(
            child: ResponsiveScrollShell(
              dashboard: true,
              child: ListView(
                padding: EdgeInsets.fromLTRB(
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  context.isShortHeight
                      ? AppBreakpoints.densePaddingFor(context.heightClass)
                      : 18,
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  28,
                ),
                children: [
                  if (AppBreakpoints.settingsColumnsFor(context.widthClass) > 1)
                    ResponsiveSplit(
                      breakpoint: AppWidthClass.large,
                      primary: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          SettingsSectionCard(
                            accent: AppColors.purple,
                            headerIcon: Icons.description_rounded,
                            title: strings.billingCatalog,
                            subtitle: 'Manage billing, products and data',
                            watermark: Icons.shopping_cart_outlined,
                            items: billingItems,
                          ),
                          const SizedBox(height: 14),
                          SettingsSectionCard(
                            accent: AppColors.orange,
                            headerIcon: Icons.storefront_rounded,
                            title: strings.store,
                            subtitle: 'Manage your shop and devices',
                            watermark: Icons.store_mall_directory_outlined,
                            items: storeItems,
                          ),
                        ],
                      ),
                      secondary: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          SettingsSectionCard(
                            accent: AppColors.primary,
                            headerIcon: Icons.cloud_download_rounded,
                            title: strings.cloudApp,
                            subtitle: 'App, sync and support',
                            watermark: Icons.cloud_outlined,
                            items: cloudItems,
                          ),
                          const SizedBox(height: 14),
                          SettingsSectionCard(
                            accent: AppColors.green,
                            headerIcon: Icons.person_rounded,
                            title: strings.account,
                            subtitle: 'Manage account and security',
                            watermark: Icons.person_outline_rounded,
                            items: accountItems,
                          ),
                        ],
                      ),
                    )
                  else ...[
                    SettingsSectionCard(
                      accent: AppColors.purple,
                      headerIcon: Icons.description_rounded,
                      title: strings.billingCatalog,
                      subtitle: 'Manage billing, products and data',
                      watermark: Icons.shopping_cart_outlined,
                      items: billingItems,
                    ),
                    const SizedBox(height: 14),
                    SettingsSectionCard(
                      accent: AppColors.orange,
                      headerIcon: Icons.storefront_rounded,
                      title: strings.store,
                      subtitle: 'Manage your shop and devices',
                      watermark: Icons.store_mall_directory_outlined,
                      items: storeItems,
                    ),
                    const SizedBox(height: 14),
                    SettingsSectionCard(
                      accent: AppColors.primary,
                      headerIcon: Icons.cloud_download_rounded,
                      title: strings.cloudApp,
                      subtitle: 'App, sync and support',
                      watermark: Icons.cloud_outlined,
                      items: cloudItems,
                    ),
                    const SizedBox(height: 14),
                    SettingsSectionCard(
                      accent: AppColors.green,
                      headerIcon: Icons.person_rounded,
                      title: strings.account,
                      subtitle: 'Manage account and security',
                      watermark: Icons.person_outline_rounded,
                      items: accountItems,
                    ),
                  ],
                  const SizedBox(height: 22),
                  Text(
                    AppConstants.appVersionLabel,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      color: AppColors.navy.withValues(alpha: .38),
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    'Developed by Billingwala',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      color: AppColors.navy.withValues(alpha: .32),
                      fontSize: 11,
                      fontWeight: FontWeight.w400,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class SettingsItem {
  const SettingsItem({
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

class SettingsHeader extends StatelessWidget {
  const SettingsHeader({
    super.key,
    required this.topInset,
    required this.title,
    required this.subtitle,
    required this.onBack,
    this.showBack = true,
  });

  final double topInset;
  final String title;
  final String subtitle;
  final VoidCallback onBack;
  final bool showBack;

  @override
  Widget build(BuildContext context) {
    if (!showBack) {
      return Padding(
        padding: EdgeInsets.fromLTRB(24, topInset + 8, 24, 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                color: AppColors.navy,
                fontWeight: FontWeight.w800,
                fontSize: 26,
                height: 1.1,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              subtitle,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                color: AppColors.textSecondary,
                fontWeight: FontWeight.w400,
                fontSize: 13.5,
                height: 1.35,
              ),
            ),
          ],
        ),
      );
    }

    return ClipPath(
      clipper: const HeaderCurveClipper(),
      child: Container(
        width: double.infinity,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              AppColors.primaryDark,
              AppColors.primary,
              AppColors.primaryBright,
            ],
          ),
        ),
        padding: EdgeInsets.fromLTRB(8, topInset + 6, 16, 36),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            IconButton(
              onPressed: onBack,
              icon: const Icon(
                Icons.arrow_back_rounded,
                color: Colors.white,
                size: 24,
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.only(top: 10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        color: Colors.white,
                        fontWeight: FontWeight.w800,
                        fontSize: 28,
                        height: 1.1,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      subtitle,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        color: Colors.white.withValues(alpha: .88),
                        fontWeight: FontWeight.w400,
                        fontSize: 12.5,
                        height: 1.35,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(width: 8),
            if (!context.isCompactWidth)
              const Padding(
                padding: EdgeInsets.only(top: 4),
                child: HeaderDecoration(),
              ),
          ],
        ),
      ),
    );
  }
}

class HeaderDecoration extends StatelessWidget {
  const HeaderDecoration({super.key});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 96,
      child: Column(
        children: [
          SizedBox(
            height: 52,
            width: 72,
            child: Stack(
              alignment: Alignment.center,
              children: [
                Positioned(
                  left: 4,
                  top: 6,
                  child: Icon(
                    Icons.settings_rounded,
                    size: 34,
                    color: Colors.white.withValues(alpha: .92),
                  ),
                ),
                Positioned(
                  right: 2,
                  bottom: 2,
                  child: Icon(
                    Icons.settings_rounded,
                    size: 26,
                    color: Colors.white.withValues(alpha: .72),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 2),
          Text(
            'Simple Settings\nBetter Business',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: AppFonts.family,
              fontStyle: FontStyle.italic,
              color: Colors.white.withValues(alpha: .92),
              fontSize: 10,
              height: 1.2,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}

class HeaderCurveClipper extends CustomClipper<Path> {
  const HeaderCurveClipper();

  @override
  Path getClip(Size size) {
    final path = Path()
      ..lineTo(0, size.height - 22)
      ..quadraticBezierTo(
        size.width * 0.5,
        size.height + 8,
        size.width,
        size.height - 22,
      )
      ..lineTo(size.width, 0)
      ..close();
    return path;
  }

  @override
  bool shouldReclip(covariant CustomClipper<Path> oldClipper) => false;
}

class SettingsSectionCard extends StatelessWidget {
  const SettingsSectionCard({
    super.key,
    required this.accent,
    required this.headerIcon,
    required this.title,
    required this.subtitle,
    required this.watermark,
    required this.items,
  });

  final Color accent;
  final IconData headerIcon;
  final String title;
  final String subtitle;
  final IconData watermark;
  final List<SettingsItem> items;

  @override
  Widget build(BuildContext context) {
    final badgeBg = Color.lerp(Colors.white, accent, 0.12)!;
    final badgeFg = Color.lerp(accent, AppColors.navy, 0.15)!;

    return Container(
      decoration: BoxDecoration(
        color: AppColors.glassFill,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: AppColors.glassBorder, width: 1.2),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: .06),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          Stack(
            children: [
              Positioned(
                right: -6,
                top: -8,
                child: Icon(
                  watermark,
                  size: 78,
                  color: accent.withValues(alpha: .07),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(14, 14, 14, 10),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 44,
                      height: 44,
                      decoration: BoxDecoration(
                        color: accent,
                        borderRadius: BorderRadius.circular(14),
                        boxShadow: [
                          BoxShadow(
                            color: accent.withValues(alpha: .28),
                            blurRadius: 10,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: Icon(headerIcon, color: Colors.white, size: 22),
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
                              color: AppColors.navy,
                              fontWeight: FontWeight.w800,
                              fontSize: 16.5,
                              height: 1.2,
                            ),
                          ),
                          const SizedBox(height: 3),
                          Text(
                            subtitle,
                            style: TextStyle(
                              fontFamily: AppFonts.family,
                              color: AppColors.navy.withValues(alpha: .5),
                              fontWeight: FontWeight.w400,
                              fontSize: 12,
                              height: 1.3,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 5,
                      ),
                      decoration: BoxDecoration(
                        color: badgeBg,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        '${items.length} Options',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          color: badgeFg,
                          fontWeight: FontWeight.w700,
                          fontSize: 11,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          for (var i = 0; i < items.length; i++) ...[
            Divider(
              height: 1,
              thickness: 1,
              color: AppColors.border.withValues(alpha: .7),
              indent: 14,
              endIndent: 14,
            ),
            SettingsRowTile(item: items[i]),
          ],
        ],
      ),
    );
  }
}

class SettingsRowTile extends StatelessWidget {
  const SettingsRowTile({super.key, required this.item});

  final SettingsItem item;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: item.onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          child: Row(
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: item.color.withValues(alpha: .12),
                  borderRadius: BorderRadius.circular(13),
                ),
                child: Icon(item.icon, color: item.color, size: 21),
              ),
              const SizedBox(width: 12),
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
                        fontSize: 14.5,
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
                        fontSize: 12,
                        height: 1.25,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 10),
              Container(
                width: 34,
                height: 34,
                decoration: BoxDecoration(
                  color: item.color.withValues(alpha: .12),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.chevron_right_rounded,
                  color: item.color,
                  size: 22,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
