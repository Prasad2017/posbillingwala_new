import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/permissions/app_permission_service.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/home/domain/home_sales_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/notifications/domain/notification_providers.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_pin_gate.dart';
import 'package:pos_billingwala_v2/features/settings/domain/business_hours.dart';
import 'package:pos_billingwala_v2/features/sync/domain/catalog_bootstrap_listener.dart';
import 'package:pos_billingwala_v2/features/sync/domain/web_cloud_refresh_listener.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/// Home dashboard — layout aligned to the product reference design.
class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  static bool _permissionsPrompted = false;
  bool _hidePrimarySales = false;
  bool _hideTodaySales = false;
  String _printerChip = 'Checking…';
  String? _closeTimeLabel;
  String? _openTimeLabel;
  DateTime _now = DateTime.now();
  Timer? _clock;

  @override
  void initState() {
    super.initState();
    _clock = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      setState(() => _now = DateTime.now());
    });
    if (!_permissionsPrompted) {
      _permissionsPrompted = true;
      Future.microtask(() async {
        // Pull catalog if local DB was emptied by a prior wrong-id sync.
        await ref
            .read(catalogBootstrapListenerProvider)
            .ensureCatalogIfEmpty(force: true);
        final service = const AppPermissionService();
        if (!await service.arePrintPermissionsGranted) {
          await service.requestAll();
        }
        final settings = ref.read(printerSettingsProvider);
        final hub = BluetoothPrinterHub.instance
          ..updateSavedAddresses(
            billMac: settings.billBluetoothAddress,
            kotMac: settings.kotBluetoothAddress,
          );
        await hub.autoConnect(PrinterChannelKind.bill);
        if (!mounted) return;
        await _refreshPrinterChip();
        await _refreshHoursLabels();
        if (!mounted) return;
        if (const bool.fromEnvironment('AUTO_TEST_PRINT')) {
          context.go('/settings/test-print?mode=invoice');
        }
      });
    } else {
      Future.microtask(() async {
        await _refreshPrinterChip();
        await _refreshHoursLabels();
      });
    }
  }

  @override
  void dispose() {
    _clock?.cancel();
    super.dispose();
  }

  Future<void> _refreshHoursLabels() async {
    final hours = await BusinessHours.load();
    if (!mounted) return;
    setState(() {
      _closeTimeLabel = _formatMinutesLabel(hours.close, prefix: 'Closes');
      _openTimeLabel = _formatMinutesLabel(hours.open, prefix: 'Opens');
    });
  }

  String? _formatMinutesLabel(int? minutes, {required String prefix}) {
    if (minutes == null) return null;
    final h = minutes ~/ 60;
    final m = minutes % 60;
    final dt = DateTime(2000, 1, 1, h, m);
    return '$prefix ${DateFormat('h:mm a').format(dt)}';
  }

  Future<void> _refreshPrinterChip() async {
    final settings = ref.read(printerSettingsProvider);
    final hub = BluetoothPrinterHub.instance;
    final mac = settings.billBluetoothAddress.trim();
    String label;
    if (mac.isEmpty &&
        settings.billUsbIdentifier.isEmpty &&
        settings.networkHost.trim().isEmpty) {
      label = 'Not set';
    } else if (hub.isConnecting) {
      label = 'Connecting…';
    } else if (await hub.connectionStatus() && hub.connectedAddress.isNotEmpty) {
      label = 'Connected';
    } else if (settings.billTransport == PosPrinterTransport.usb &&
        EscPosUsbHint.isLinked(settings)) {
      label = 'USB ready';
    } else if (settings.billTransport == PosPrinterTransport.network &&
        settings.networkHost.trim().isNotEmpty) {
      label = 'Network';
    } else {
      label = 'Offline';
    }
    if (!mounted) return;
    setState(() => _printerChip = label);
  }

  String _greeting() {
    final hour = _now.hour;
    if (hour < 12) return 'Good Morning';
    if (hour < 17) return 'Good Afternoon';
    return 'Good Evening';
  }

  Future<void> _openFastBilling() async {
    ref.read(billingSessionProvider.notifier).usePos();
    await ref.read(posCartControllerProvider.notifier).clear();
    if (!mounted) return;
    context.push('/pos');
  }

  void _moduleLocked(BuildContext context) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text(
          'This module is not enabled on your licence. Contact support.',
        ),
      ),
    );
  }

  /// When no module flags are set (older sessions), treat as full licence.
  bool _anyBilling(UserSession s) =>
      !s.fastBilling && !s.dineIn && !s.takeAway && !s.mess;

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(authControllerProvider).session;
    final unread = ref.watch(unreadNotificationCountProvider);
    final kpis = ref.watch(homeDashboardKpisProvider);
    final period = ref.watch(homeSalesPeriodProvider);
    final localCatalog = ref.watch(catalogCountsProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹ ');
    final nowLabel =
        DateFormat('EEE, dd MMM yyyy | hh:mm:ss a').format(_now);

    final flagsMissing = session == null || _anyBilling(session);
    final allowFast = flagsMissing || session.fastBilling;
    final allowDine = flagsMissing || session.dineIn;
    final allowTake = flagsMissing || session.takeAway;
    final allowMess = flagsMissing || session.mess;
    final showTotalSales =
        session == null || session.totalSaleData || session.todaySaleData;
    final showTodaySales = session == null || session.todaySaleData;
    final salesMonth = period == HomeSalesPeriod.month;

    final shopName = session?.shopName?.trim() ?? '';
    final printerOnline = _printerChip == 'Connected' ||
        _printerChip == 'USB ready' ||
        _printerChip == 'Network';

    return Scaffold(
      backgroundColor: const Color(0xFFF7F9FC),
      body: RefreshIndicator(
        onRefresh: () async {
          if (AppPlatform.autoCloudRefresh) {
            await ref
                .read(webCloudRefreshListenerProvider)
                .refresh(force: true);
          }
          ref.invalidate(categoriesProvider);
          ref.invalidate(subcategoriesProvider);
          ref.invalidate(allProductsProvider);
          ref.invalidate(combosListProvider);
          ref.invalidate(todayInvoicesProvider);
          ref.invalidate(yesterdayInvoicesProvider);
          ref.invalidate(monthInvoicesProvider);
          ref.invalidate(allTimeInvoicesProvider);
          ref.invalidate(homeSalesOverviewProvider);
          ref.invalidate(shopOpenNowProvider);
          await _refreshPrinterChip();
          await _refreshHoursLabels();
        },
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(
              child: _HomeHeader(
                topInset: MediaQuery.paddingOf(context).top,
                greeting: '${_greeting()} 👋',
                shopName: shopName.isEmpty ? 'Your shop' : shopName,
                shopImageUrl: ApiConstants.mediaUrl(session?.shopImage),
                nowLabel: nowLabel,
                unread: unread,
                printerOnline: printerOnline,
                printerLabel: _printerChip,
                openTimeLabel: _openTimeLabel,
                closeTimeLabel: _closeTimeLabel,
                onNotifications: () => context.push('/notifications'),
                onSettings: () => context.push('/settings'),
                onPrinter: () => context.push('/settings/devices'),
              ),
            ),

            SliverToBoxAdapter(
              child: Transform.translate(
                offset: const Offset(0, -14),
                child: Container(
                  width: double.infinity,
                  decoration: const BoxDecoration(
                    color: Color(0xFFF7F9FC),
                    borderRadius: BorderRadius.vertical(
                      top: Radius.circular(28),
                    ),
                  ),
                  child: ResponsiveContent(
                    dashboard: true,
                    padding: EdgeInsets.fromLTRB(
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      16,
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      28,
                    ),
                    child: _HomeDashboardBody(
                      showTotalSales: showTotalSales,
                      showTodaySales: showTodaySales,
                      salesMonth: salesMonth,
                      onToggleSalesPeriod: () =>
                          ref.read(homeSalesPeriodProvider.notifier).toggle(),
                      primaryTitle: kpis.primaryTitle,
                      primaryAmount: _hidePrimarySales
                          ? '••••••'
                          : currency.format(kpis.primarySales),
                      todayAmount: _hideTodaySales
                          ? '••••••'
                          : currency.format(kpis.todaySales),
                      growthText: kpis.growthText,
                      growthUp: kpis.growthUp,
                      hidePrimarySales: _hidePrimarySales,
                      hideTodaySales: _hideTodaySales,
                      onToggleHidePrimary: () => setState(
                        () => _hidePrimarySales = !_hidePrimarySales,
                      ),
                      onToggleHideToday: () => setState(
                        () => _hideTodaySales = !_hideTodaySales,
                      ),
                      onOpenReports: () =>
                          pushReportsUnlocked(context, ref),
                      categoriesCount: localCatalog.categories,
                      productsCount: kpis.products,
                      combosCount: kpis.combos,
                      subcategoriesCount: kpis.subcategories,
                      allowFast: allowFast,
                      allowDine: allowDine,
                      allowTake: allowTake,
                      allowMess: allowMess,
                      onModuleLocked: () => _moduleLocked(context),
                      onFastBilling: _openFastBilling,
                      onDineIn: () => context.push('/tables'),
                      onTakeAway: () => context.push('/takeaway'),
                      onMess: () => pushReportsUnlocked(
                        context,
                        ref,
                        route: '/mess',
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HomeDashboardBody extends ConsumerWidget {
  const _HomeDashboardBody({
    required this.showTotalSales,
    required this.showTodaySales,
    required this.salesMonth,
    required this.onToggleSalesPeriod,
    required this.primaryTitle,
    required this.primaryAmount,
    required this.todayAmount,
    required this.growthText,
    required this.growthUp,
    required this.hidePrimarySales,
    required this.hideTodaySales,
    required this.onToggleHidePrimary,
    required this.onToggleHideToday,
    required this.onOpenReports,
    required this.categoriesCount,
    required this.productsCount,
    required this.combosCount,
    required this.subcategoriesCount,
    required this.allowFast,
    required this.allowDine,
    required this.allowTake,
    required this.allowMess,
    required this.onModuleLocked,
    required this.onFastBilling,
    required this.onDineIn,
    required this.onTakeAway,
    required this.onMess,
  });

  final bool showTotalSales;
  final bool showTodaySales;
  final bool salesMonth;
  final VoidCallback onToggleSalesPeriod;
  final String primaryTitle;
  final String primaryAmount;
  final String todayAmount;
  final String? growthText;
  final bool growthUp;
  final bool hidePrimarySales;
  final bool hideTodaySales;
  final VoidCallback onToggleHidePrimary;
  final VoidCallback onToggleHideToday;
  final VoidCallback onOpenReports;
  final int categoriesCount;
  final int productsCount;
  final int combosCount;
  final int subcategoriesCount;
  final bool allowFast;
  final bool allowDine;
  final bool allowTake;
  final bool allowMess;
  final VoidCallback onModuleLocked;
  final VoidCallback onFastBilling;
  final VoidCallback onDineIn;
  final VoidCallback onTakeAway;
  final Future<void> Function() onMess;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    final widthClass = context.widthClass;
    final billingCols = AppBreakpoints.moduleColumnsFor(widthClass);
    final billingTiles = <Widget>[
      _BillingTile(
        title: strings.fastBilling,
        subtitle: 'Quick billing for walk-in customers',
        icon: Icons.receipt_long_rounded,
        colors: const [Color(0xFF4C8DFF), Color(0xFF076BF5)],
        onTap: () {
          if (!allowFast) {
            onModuleLocked();
            return;
          }
          onFastBilling();
        },
      ),
      _BillingTile(
        title: strings.dineIn,
        subtitle: 'Create bill for dine-in customers',
        icon: Icons.table_restaurant_rounded,
        colors: const [Color(0xFF2ECF8A), Color(0xFF16A36A)],
        onTap: () {
          if (!allowDine) {
            onModuleLocked();
            return;
          }
          onDineIn();
        },
      ),
      _BillingTile(
        title: strings.takeAway,
        subtitle: 'Create bill for takeaway orders',
        icon: Icons.shopping_bag_rounded,
        colors: const [Color(0xFFFFB347), Color(0xFFFF8A00)],
        onTap: () {
          if (!allowTake) {
            onModuleLocked();
            return;
          }
          onTakeAway();
        },
      ),
      _BillingTile(
        title: strings.mess,
        subtitle: 'Manage mess billing easily',
        icon: Icons.restaurant_rounded,
        colors: const [Color(0xFF9B7BFF), Color(0xFF7C4DFF)],
        onTap: () async {
          if (!allowMess) {
            onModuleLocked();
            return;
          }
          await onMess();
        },
      ),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (showTotalSales || showTodaySales) ...[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  strings.salesOverview,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                        color: AppColors.navy,
                        fontSize: 18,
                      ),
                ),
              ),
              InkWell(
                onTap: onToggleSalesPeriod,
                borderRadius: BorderRadius.circular(20),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        salesMonth ? strings.month : strings.today,
                        style: const TextStyle(
                          color: AppColors.primary,
                          fontWeight: FontWeight.w700,
                          fontSize: 11,
                        ),
                      ),
                      const Icon(
                        Icons.arrow_drop_down,
                        color: AppColors.primary,
                        size: 18,
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              if (showTotalSales)
                Expanded(
                  child: _SalesCard(
                    title: primaryTitle,
                    amount: primaryAmount,
                    color: AppColors.green,
                    icon: Icons.bar_chart_rounded,
                    growthLabel: growthText,
                    growthUp: growthUp,
                    hidden: hidePrimarySales,
                    onToggleHide: onToggleHidePrimary,
                    onTap: onOpenReports,
                  ),
                ),
              if (showTotalSales && showTodaySales) const SizedBox(width: 12),
              if (showTodaySales)
                Expanded(
                  child: _SalesCard(
                    title: strings.todaySales,
                    amount: todayAmount,
                    color: AppColors.primary,
                    icon: Icons.shopping_cart_rounded,
                    growthLabel: growthText,
                    growthUp: growthUp,
                    hidden: hideTodaySales,
                    onToggleHide: onToggleHideToday,
                    onTap: onOpenReports,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 22),
        ],
        Text(
          strings.catalog,
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w800,
                color: AppColors.navy,
                fontSize: 18,
              ),
        ),
        const SizedBox(height: 12),
        Builder(
          builder: (context) {
            final tiles = [
              _CatalogTile(
                icon: Icons.category_rounded,
                label: strings.categories,
                value: '$categoriesCount',
                color: AppColors.primary,
                soft: const Color(0xFFE8F1FF),
                onTap: () => context.push('/masters/categories'),
              ),
              _CatalogTile(
                icon: Icons.grid_view_rounded,
                label: strings.subcategories,
                value: '$subcategoriesCount',
                color: AppColors.purple,
                soft: const Color(0xFFF3EEFF),
                onTap: () => context.push('/masters/subcategories'),
              ),
              _CatalogTile(
                icon: Icons.inventory_2_rounded,
                label: strings.products,
                value: '$productsCount',
                color: AppColors.green,
                soft: const Color(0xFFE8F8F0),
                onTap: () => context.push('/masters/products'),
              ),
              _CatalogTile(
                icon: Icons.layers_rounded,
                label: strings.combos,
                value: '$combosCount',
                color: AppColors.orange,
                soft: const Color(0xFFFFF3E8),
                onTap: () => context.push('/masters/catalog?tab=combos'),
              ),
            ];
            final compact = context.widthClass == AppWidthClass.compact;
            if (compact) {
              return Column(
                children: [
                  Row(
                    children: [
                      Expanded(child: tiles[0]),
                      const SizedBox(width: 10),
                      Expanded(child: tiles[1]),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(child: tiles[2]),
                      const SizedBox(width: 10),
                      Expanded(child: tiles[3]),
                    ],
                  ),
                ],
              );
            }
            return Row(
              children: [
                for (var i = 0; i < tiles.length; i++) ...[
                  if (i > 0) const SizedBox(width: 10),
                  Expanded(child: tiles[i]),
                ],
              ],
            );
          },
        ),
        const SizedBox(height: 22),
        Text(
          'Start Billing',
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w800,
                color: AppColors.navy,
                fontSize: 18,
              ),
        ),
        const SizedBox(height: 12),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: billingTiles.length,
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: billingCols.clamp(2, 4),
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            childAspectRatio: widthClass == AppWidthClass.compact ? 1.35 : 1.55,
          ),
          itemBuilder: (context, index) => billingTiles[index],
        ),
        const SizedBox(height: 20),
        const _PromoBanner(),
      ],
    );
  }
}

class EscPosUsbHint {
  static bool isLinked(PrinterSettings settings) =>
      settings.billUsbIdentifier.trim().isNotEmpty;
}

class _HomeHeader extends ConsumerWidget {
  const _HomeHeader({
    required this.topInset,
    required this.greeting,
    required this.shopName,
    this.shopImageUrl,
    required this.nowLabel,
    required this.unread,
    required this.printerOnline,
    required this.printerLabel,
    required this.openTimeLabel,
    required this.closeTimeLabel,
    required this.onNotifications,
    required this.onSettings,
    required this.onPrinter,
  });

  final double topInset;
  final String greeting;
  final String shopName;
  final String? shopImageUrl;
  final String nowLabel;
  final int unread;
  final bool printerOnline;
  final String printerLabel;
  final String? openTimeLabel;
  final String? closeTimeLabel;
  final VoidCallback onNotifications;
  final VoidCallback onSettings;
  final VoidCallback onPrinter;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      width: double.infinity,
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Color(0xFF076BF5),
            Color(0xFF3B5BDB),
            Color(0xFF5B4BCC),
          ],
        ),
      ),
      padding: EdgeInsets.fromLTRB(18, topInset + 10, 18, 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Stack(
                clipBehavior: Clip.none,
                children: [
                  CircleAvatar(
                    radius: 26,
                    backgroundColor: Colors.white,
                    child: ClipOval(
                      child: shopImageUrl != null
                          ? Image.network(
                              shopImageUrl!,
                              width: 52,
                              height: 52,
                              fit: BoxFit.cover,
                              errorBuilder: (_, _, _) => Image.asset(
                                AppAssets.appLogo,
                                width: 52,
                                height: 52,
                                fit: BoxFit.cover,
                                errorBuilder: (_, _, _) => Text(
                                  shopName.isEmpty
                                      ? 'P'
                                      : shopName.substring(0, 1).toUpperCase(),
                                  style: const TextStyle(
                                    color: AppColors.primary,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 22,
                                  ),
                                ),
                              ),
                            )
                          : Image.asset(
                              AppAssets.appLogo,
                              width: 52,
                              height: 52,
                              fit: BoxFit.cover,
                              errorBuilder: (_, _, _) => Text(
                                shopName.isEmpty
                                    ? 'P'
                                    : shopName.substring(0, 1).toUpperCase(),
                                style: const TextStyle(
                                  color: AppColors.primary,
                                  fontWeight: FontWeight.w800,
                                  fontSize: 22,
                                ),
                              ),
                            ),
                    ),
                  ),
                  Positioned(
                    top: 0,
                    right: 0,
                    child: Container(
                      width: 12,
                      height: 12,
                      decoration: BoxDecoration(
                        color: const Color(0xFF22C55E),
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white, width: 2),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      greeting,
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w800,
                        fontSize: 17,
                        height: 1.2,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 3),
                    Text(
                      shopName,
                      style: const TextStyle(
                        color: Color(0xCCFFFFFF),
                        fontSize: 12.5,
                        fontWeight: FontWeight.w500,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              _HeaderIconButton(
                onTap: onNotifications,
                child: Badge(
                  isLabelVisible: unread > 0,
                  smallSize: 8,
                  backgroundColor: AppColors.red,
                  child: const AppSvg(
                    AppAssets.svgNotification,
                    width: 20,
                    height: 20,
                    color: AppColors.primary,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              _HeaderIconButton(
                onTap: onSettings,
                child: const AppSvg(
                  AppAssets.svgSettings,
                  width: 20,
                  height: 20,
                  color: AppColors.primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Flexible(
                child: ref.watch(shopOpenNowProvider).when(
                      data: (open) => _StatusPill(
                        online: open,
                        label: open
                            ? 'Shop Open${closeTimeLabel != null ? ' | $closeTimeLabel' : ''}'
                            : 'Shop Closed${openTimeLabel != null ? ' | $openTimeLabel' : ''}',
                        dark: true,
                      ),
                      loading: () => const _StatusPill(
                        online: true,
                        label: 'Shop…',
                        dark: true,
                      ),
                      error: (_, _) => const _StatusPill(
                        online: true,
                        label: 'Shop Open',
                        dark: true,
                      ),
                    ),
              ),
              const SizedBox(width: 8),
              Flexible(
                child: InkWell(
                  onTap: onPrinter,
                  borderRadius: BorderRadius.circular(20),
                  child: _StatusPill(
                    online: printerOnline,
                    label: 'Printer | $printerLabel',
                    solid: !printerOnline,
                    icon: Icons.print_rounded,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              const Icon(
                Icons.calendar_today_rounded,
                size: 14,
                color: Color(0xCCFFFFFF),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  nowLabel,
                  style: const TextStyle(
                    color: Color(0xE6FFFFFF),
                    fontSize: 12.5,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              const _GrowthDecoBadge(),
            ],
          ),
        ],
      ),
    );
  }
}

class _HeaderIconButton extends StatelessWidget {
  const _HeaderIconButton({required this.onTap, required this.child});

  final VoidCallback onTap;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: SizedBox(
          width: 40,
          height: 40,
          child: Center(child: child),
        ),
      ),
    );
  }
}

class _StatusPill extends StatelessWidget {
  const _StatusPill({
    required this.online,
    required this.label,
    this.dark = false,
    this.solid = false,
    this.icon,
  });

  final bool online;
  final String label;
  final bool dark;
  final bool solid;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final Color bg;
    if (solid) {
      bg = const Color(0xFFE53935);
    } else if (dark) {
      bg = const Color(0x66000000);
    } else {
      bg = Colors.white.withValues(alpha: 0.18);
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 13, color: Colors.white),
            const SizedBox(width: 5),
          ] else ...[
            Icon(
              Icons.circle,
              size: 8,
              color: online
                  ? const Color(0xFF4ADE80)
                  : const Color(0xFFF87171),
            ),
            const SizedBox(width: 6),
          ],
          Flexible(
            child: Text(
              label,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
                fontSize: 11,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}

class _GrowthDecoBadge extends StatelessWidget {
  const _GrowthDecoBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: 0.06),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.show_chart_rounded, size: 14, color: AppColors.green),
          SizedBox(width: 4),
          Text(
            'Small Business\nBig Growth',
            style: TextStyle(
              fontSize: 8.5,
              height: 1.15,
              fontWeight: FontWeight.w700,
              color: AppColors.navy,
              fontStyle: FontStyle.italic,
            ),
          ),
        ],
      ),
    );
  }
}

class _SalesCard extends StatelessWidget {
  const _SalesCard({
    required this.title,
    required this.amount,
    required this.color,
    required this.icon,
    required this.growthLabel,
    required this.growthUp,
    required this.onToggleHide,
    required this.hidden,
    required this.onTap,
  });

  final String title;
  final String amount;
  final Color color;
  final IconData icon;
  final String? growthLabel;
  final bool growthUp;
  final VoidCallback onToggleHide;
  final bool hidden;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final soft = Color.lerp(Colors.white, color, 0.12)!;
    final softer = Color.lerp(Colors.white, color, 0.05)!;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(22),
        child: Ink(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(22),
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [softer, soft],
            ),
            border: Border.all(color: color.withValues(alpha: 0.14)),
            boxShadow: [
              BoxShadow(
                color: color.withValues(alpha: 0.12),
                blurRadius: 16,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(22),
            child: Stack(
              children: [
                Positioned(
                  left: 0,
                  right: 0,
                  bottom: 0,
                  height: 36,
                  child: CustomPaint(
                    painter: _WavePainter(color.withValues(alpha: 0.22)),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(14, 12, 8, 14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Container(
                            width: 34,
                            height: 34,
                            decoration: BoxDecoration(
                              color: color.withValues(alpha: 0.15),
                              shape: BoxShape.circle,
                            ),
                            child: Icon(icon, size: 18, color: color),
                          ),
                          const Spacer(),
                          IconButton(
                            visualDensity: VisualDensity.compact,
                            padding: EdgeInsets.zero,
                            constraints: const BoxConstraints(
                              minWidth: 28,
                              minHeight: 28,
                            ),
                            onPressed: onToggleHide,
                            icon: Icon(
                              hidden
                                  ? Icons.visibility_off_outlined
                                  : Icons.visibility_outlined,
                              size: 18,
                              color: Colors.black45,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        title,
                        style: const TextStyle(
                          color: Colors.black54,
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        amount,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 17,
                          color: AppColors.navy,
                        ),
                      ),
                      if (growthLabel != null) ...[
                        const SizedBox(height: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: growthUp
                                ? AppColors.green
                                : AppColors.red,
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(
                            growthLabel!,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 10,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _WavePainter extends CustomPainter {
  _WavePainter(this.color);

  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()..color = color;
    final path = Path()
      ..moveTo(0, size.height * 0.55)
      ..cubicTo(
        size.width * 0.25,
        size.height * 0.1,
        size.width * 0.45,
        size.height * 0.95,
        size.width * 0.7,
        size.height * 0.45,
      )
      ..cubicTo(
        size.width * 0.85,
        size.height * 0.15,
        size.width * 0.92,
        size.height * 0.55,
        size.width,
        size.height * 0.35,
      )
      ..lineTo(size.width, size.height)
      ..lineTo(0, size.height)
      ..close();
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _WavePainter oldDelegate) =>
      oldDelegate.color != color;
}

class _CatalogTile extends StatelessWidget {
  const _CatalogTile({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
    required this.soft,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final String value;
  final Color color;
  final Color soft;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Ink(
          padding: const EdgeInsets.fromLTRB(10, 14, 10, 12),
          decoration: BoxDecoration(
            color: soft,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: color.withValues(alpha: 0.12)),
            boxShadow: [
              BoxShadow(
                color: color.withValues(alpha: 0.08),
                blurRadius: 12,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.7),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, size: 20, color: color),
              ),
              const SizedBox(height: 10),
              FittedBox(
                fit: BoxFit.scaleDown,
                child: Text(
                  value,
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 20,
                    color: color,
                  ),
                ),
              ),
              const SizedBox(height: 2),
              Text(
                label,
                style: const TextStyle(
                  fontSize: 10.5,
                  color: Colors.black54,
                  fontWeight: FontWeight.w600,
                  height: 1.2,
                ),
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BillingTile extends StatelessWidget {
  const _BillingTile({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.colors,
    required this.onTap,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final List<Color> colors;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(20),
          child: Ink(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(20),
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: colors,
            ),
            boxShadow: [
              BoxShadow(
                color: colors.last.withValues(alpha: 0.28),
                blurRadius: 14,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(icon, size: 20, color: colors.last),
                  ),
                  const Spacer(),
                  Container(
                    width: 28,
                    height: 28,
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.22),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.arrow_forward_rounded,
                      size: 16,
                      color: Colors.white,
                    ),
                  ),
                ],
              ),
              const Spacer(),
              Text(
                title,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w800,
                  fontSize: 14,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                subtitle,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.9),
                  fontSize: 10.5,
                  height: 1.2,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PromoBanner extends StatelessWidget {
  const _PromoBanner();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 14, 12, 14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF6E8),
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: const Color(0xFFFFE2B8)),
      ),
      child: Row(
        children: [
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Serve Better\nSell Smarter!',
                  style: TextStyle(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                    fontSize: 16,
                    height: 1.2,
                  ),
                ),
                SizedBox(height: 6),
                Text(
                  'Happy Customers\nStronger Business',
                  style: TextStyle(
                    color: Colors.black54,
                    fontSize: 11.5,
                    height: 1.25,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Container(
            width: 108,
            height: 88,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.65),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Stack(
              alignment: Alignment.center,
              children: [
                Positioned(
                  top: 8,
                  child: Text(
                    'Keep Growing!',
                    style: TextStyle(
                      color: AppColors.orangeDark.withValues(alpha: 0.9),
                      fontSize: 9,
                      fontWeight: FontWeight.w700,
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                ),
                const Icon(
                  Icons.storefront_rounded,
                  size: 48,
                  color: AppColors.primary,
                ),
                Positioned(
                  bottom: 8,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 6,
                      vertical: 2,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFE8F3FF),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: const Text(
                      'Good Food Good Mood',
                      style: TextStyle(
                        fontSize: 7.5,
                        fontWeight: FontWeight.w700,
                        color: AppColors.navy,
                      ),
                    ),
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
