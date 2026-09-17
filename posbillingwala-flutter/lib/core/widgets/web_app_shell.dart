import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/notifications/domain/notification_providers.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/web_cloud_refresh_listener.dart';

class WebNavDestination {
  const WebNavDestination({
    required this.label,
    required this.icon,
    required this.route,
    this.permission,
    this.aliases = const [],
    this.licenceAllows,
  });

  final String label;
  final IconData icon;
  final String route;
  final String? permission;
  final List<String> aliases;
  final bool Function(UserSession session)? licenceAllows;

  bool matches(String location) {
    if (route == '/') return location == '/';
    if (location == route || location.startsWith('$route/')) return true;
    for (final alias in aliases) {
      if (location == alias || location.startsWith('$alias/')) return true;
    }
    return false;
  }
}

const webNavDestinations = <WebNavDestination>[
  WebNavDestination(
    label: 'Home',
    icon: Icons.home_rounded,
    route: '/',
  ),
  WebNavDestination(
    label: 'Billing',
    icon: Icons.point_of_sale_rounded,
    route: '/pos',
    permission: 'billing.create',
    licenceAllows: _licenceFastBilling,
  ),
  WebNavDestination(
    label: 'Tables',
    icon: Icons.table_restaurant_rounded,
    route: '/tables',
    permission: 'table.view',
    licenceAllows: _licenceDineIn,
  ),
  WebNavDestination(
    label: 'Takeaway',
    icon: Icons.takeout_dining_rounded,
    route: '/takeaway',
    permission: 'takeaway.view',
    licenceAllows: _licenceTakeAway,
  ),
  WebNavDestination(
    label: 'Mess',
    icon: Icons.restaurant_rounded,
    route: '/mess',
    permission: 'mess.view',
    licenceAllows: _licenceMess,
  ),
  WebNavDestination(
    label: 'Masters',
    icon: Icons.inventory_2_rounded,
    route: '/masters',
    permission: 'product.view',
  ),
  WebNavDestination(
    label: 'Inventory',
    icon: Icons.warehouse_rounded,
    route: '/inventory',
    permission: 'inventory.view',
    aliases: ['/expenses'],
  ),
  WebNavDestination(
    label: 'Reports',
    icon: Icons.bar_chart_rounded,
    route: '/reports',
    permission: 'report.view',
  ),
  WebNavDestination(
    label: 'Settings',
    icon: Icons.settings_rounded,
    route: '/settings',
  ),
];

bool _licenceAnyBilling(UserSession s) =>
    !s.fastBilling && !s.dineIn && !s.takeAway && !s.mess;

bool _licenceFastBilling(UserSession s) =>
    _licenceAnyBilling(s) || s.fastBilling;

bool _licenceDineIn(UserSession s) => _licenceAnyBilling(s) || s.dineIn;

bool _licenceTakeAway(UserSession s) => _licenceAnyBilling(s) || s.takeAway;

bool _licenceMess(UserSession s) => _licenceAnyBilling(s) || s.mess;

/* Desktop chrome for Flutter web. Mobile apps pass the child through. */
class WebAppShell extends ConsumerWidget {
  const WebAppShell({super.key, required this.child});

  final Widget child;

  static const railWidth = 248.0;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (!AppPlatform.useDesktopShell) return child;

    final wide = context.widthClass.index >= AppWidthClass.expanded.index;
    final online = ref.watch(deviceOnlineProvider).maybeWhen(
          data: (value) => value,
          orElse: () => true,
        );

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: wide
          ? null
          : AppBar(
              backgroundColor: AppColors.navy,
              title: const Text('Billingwala'),
            ),
      drawer: wide ? null : Drawer(child: WebSideNav(location: GoRouterState.of(context).uri.path)),
      body: Column(
        children: [
          if (!online) const WebOfflineBanner(),
          Expanded(
            child: Row(
              children: [
                if (wide)
                  const SizedBox(
                    width: railWidth,
                    child: WebSideNav(),
                  ),
                Expanded(child: child),
              ],
            ),
          ),
        ],
      ),
    );
  }

  static Future<void> refreshCloud(BuildContext context, WidgetRef ref) async {
    if (!await isDeviceOnline()) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(kOnlineRequiredMessage)),
      );
      return;
    }
    await ref.read(webCloudRefreshListenerProvider).refresh(force: true);
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Refreshed from cloud')),
    );
  }
}

class WebOfflineBanner extends StatelessWidget {
  const WebOfflineBanner({super.key});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.danger,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        child: Row(
          children: [
            const Icon(Icons.cloud_off_rounded, color: Colors.white, size: 18),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                kOnlineRequiredMessage,
                style: const TextStyle(
                  fontFamily: AppFonts.family,
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                  fontSize: 13,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class WebSideNav extends ConsumerWidget {
  const WebSideNav({super.key, this.location});

  final String? location;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final loc = location ?? GoRouterState.of(context).uri.path;
    final session = ref.watch(authControllerProvider).session;
    final perms = ref.watch(permissionControllerProvider);
    final unread = ref.watch(unreadNotificationCountProvider);
    final online = ref.watch(deviceOnlineProvider).maybeWhen(
          data: (value) => value,
          orElse: () => true,
        );
    final shopName = session?.displayName ?? 'Billingwala';
    final staffName = perms.staff?.name.trim().isNotEmpty == true
        ? perms.staff!.name.trim()
        : (session?.userName?.trim().isNotEmpty == true
            ? session!.userName!.trim()
            : 'Owner');
    final staffRole = perms.staff?.roleLabel.trim().isNotEmpty == true
        ? perms.staff!.roleLabel.trim()
        : 'Licence';

    final items = webNavDestinations.where((item) {
      if (item.permission != null && !perms.allows(item.permission!)) {
        return false;
      }
      if (session != null &&
          item.licenceAllows != null &&
          !item.licenceAllows!(session)) {
        return false;
      }
      return true;
    }).toList();

    return ColoredBox(
      color: AppColors.navy,
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 18, 18, 12),
              child: Row(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: Image.asset(
                      AppAssets.appLogo,
                      width: 40,
                      height: 40,
                      fit: BoxFit.cover,
                      errorBuilder: (_, _, _) => Container(
                        width: 40,
                        height: 40,
                        color: Colors.white,
                        alignment: Alignment.center,
                        child: Text(
                          shopName.isEmpty ? 'P' : shopName[0].toUpperCase(),
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            color: AppColors.primary,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          shopName,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            color: Colors.white,
                            fontWeight: FontWeight.w800,
                            fontSize: 14,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          AppConstants.appName,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            color: Colors.white.withValues(alpha: .65),
                            fontWeight: FontWeight.w500,
                            fontSize: 11,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: .08),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.circle,
                      size: 8,
                      color: online
                          ? const Color(0xFF4ADE80)
                          : const Color(0xFFF87171),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        online ? 'Online' : 'Offline',
                        style: const TextStyle(
                          fontFamily: AppFonts.family,
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                          fontSize: 12,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(10, 4, 10, 8),
                children: [
                  for (final item in items)
                    WebNavTile(
                      label: item.label,
                      icon: item.icon,
                      selected: item.matches(loc),
                      onTap: () {
                        if (Scaffold.maybeOf(context)?.isDrawerOpen == true) {
                          Navigator.of(context).pop();
                        }
                        if (!item.matches(loc)) {
                          context.go(item.route);
                        }
                      },
                    ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 16),
              child: Column(
                children: [
                  InkWell(
                    onTap: () {
                      if (Scaffold.maybeOf(context)?.isDrawerOpen == true) {
                        Navigator.of(context).pop();
                      }
                      context.push('/notifications');
                    },
                    borderRadius: BorderRadius.circular(12),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 8,
                      ),
                      child: Row(
                        children: [
                          Badge(
                            isLabelVisible: unread > 0,
                            smallSize: 8,
                            backgroundColor: AppColors.orange,
                            child: const Icon(
                              Icons.notifications_none_rounded,
                              color: Colors.white70,
                              size: 20,
                            ),
                          ),
                          const SizedBox(width: 10),
                          const Expanded(
                            child: Text(
                              'Notifications',
                              style: TextStyle(
                                fontFamily: AppFonts.family,
                                color: Colors.white70,
                                fontWeight: FontWeight.w600,
                                fontSize: 13,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      CircleAvatar(
                        radius: 16,
                        backgroundColor: Colors.white.withValues(alpha: .12),
                        child: Text(
                          staffName.isEmpty
                              ? 'S'
                              : staffName[0].toUpperCase(),
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            color: Colors.white,
                            fontWeight: FontWeight.w700,
                            fontSize: 13,
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              staffName,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontFamily: AppFonts.family,
                                color: Colors.white,
                                fontWeight: FontWeight.w700,
                                fontSize: 13,
                              ),
                            ),
                            Text(
                              staffRole,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontFamily: AppFonts.family,
                                color: Colors.white.withValues(alpha: .6),
                                fontSize: 11,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class WebNavTile extends StatelessWidget {
  const WebNavTile({
    super.key,
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
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Material(
        color: selected
            ? Colors.white.withValues(alpha: .14)
            : Colors.transparent,
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 11),
            child: Row(
              children: [
                Icon(
                  icon,
                  size: 20,
                  color: selected ? Colors.white : Colors.white70,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    label,
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      color: selected ? Colors.white : Colors.white70,
                      fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                      fontSize: 13.5,
                    ),
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
